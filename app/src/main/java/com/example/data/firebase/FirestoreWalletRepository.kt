package com.example.data.firebase

import android.content.Context
import android.util.Base64
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.cancellation.CancellationException

class FirestoreWalletRepository(private val db: FirebaseFirestore? = null) {

    companion object {
        const val BACKUP_MANIFEST_DOC_ID = "latest"
        const val BACKUP_CHUNK_PREFIX = "latest_chunk_"
        const val PAYLOAD_ENCODING_GZIP_BASE64 = "gzip_base64"
        const val PAYLOAD_ENCODING_GZIP_BASE64_CHUNKED = "gzip_base64_chunked"
        // Safe margin well below Firestore's 1 MiB (1,048,576 byte) per-document limit
        const val MAX_INLINE_PAYLOAD_CHARS = 650_000
        const val CHUNK_SIZE_CHARS = 650_000
        const val MAX_CHUNKS = 50
    }

    constructor(context: Context) : this(
        try {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        } catch (_: Exception) {
            null
        }
    )

    private val auth: FirebaseAuth? by lazy {
        try {
            Firebase.auth
        } catch (_: Exception) {
            null
        }
    }

    private val currentUser get() = auth?.currentUser

    private fun requireDb(): FirebaseFirestore {
        return db ?: throw IllegalStateException("Firestore is not available.")
    }

    private fun requireUserId(): String {
        return currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeAccounts(): Flow<List<AccountEntity>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/accounts"
        val collectionRef = requireDb().collection("users").document(uid).collection("accounts")

        val accountFlow = collectionRef.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val typeStr = data["type"] as? String ?: "CASH"
                    val type = try { AccountType.valueOf(typeStr) } catch (e: Exception) { AccountType.CASH }
                    val balance = (data["balance"] as? Number)?.toDouble() ?: 0.0
                    val openingBalance = (data["openingBalance"] as? Number)?.toDouble() ?: 0.0
                    val creditLimit = (data["creditLimit"] as? Number)?.toDouble() ?: 0.0
                    val minPayment = (data["minPayment"] as? Number)?.toDouble() ?: 0.0
                    val sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0

                    AccountEntity(
                        id = doc.id,
                        name = data["name"] as? String ?: "Account",
                        type = type,
                        institution = data["institution"] as? String ?: "",
                        balance = balance,
                        openingBalance = openingBalance,
                        accountNumberLast4 = data["accountNumberLast4"] as? String ?: "",
                        colorHex = data["colorHex"] as? String ?: "#4F46E5",
                        iconName = data["iconName"] as? String ?: "account_balance",
                        isHidden = data["isHidden"] as? Boolean ?: false,
                        sortOrder = sortOrder,
                        creditLimit = creditLimit,
                        billingDate = data["billingDate"] as? String ?: "",
                        paymentDueDate = data["paymentDueDate"] as? String ?: "",
                        minPayment = minPayment,
                        notes = data["notes"] as? String ?: "",
                        includeInTotal = data["includeInTotal"] as? Boolean ?: true
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }.catch { error ->
            if (error is Exception) {
                handleFirestoreError(error, OperationType.LIST, path)
            }
            throw error
        }

        emitAll(accountFlow)
    }

    suspend fun saveAccount(account: AccountEntity) {
        val uid = requireUserId()
        val docRef = requireDb().collection("users").document(uid).collection("accounts").document(account.id)
        val path = docRef.path

        try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "name" to account.name,
                "type" to account.type.name,
                "institution" to account.institution,
                "balance" to account.balance,
                "openingBalance" to account.openingBalance,
                "accountNumberLast4" to account.accountNumberLast4,
                "colorHex" to account.colorHex,
                "iconName" to account.iconName,
                "isHidden" to account.isHidden,
                "sortOrder" to account.sortOrder,
                "creditLimit" to account.creditLimit,
                "billingDate" to account.billingDate,
                "paymentDueDate" to account.paymentDueDate,
                "minPayment" to account.minPayment,
                "notes" to account.notes,
                "includeInTotal" to account.includeInTotal,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Check if exists to preserve createdAt or set it
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.set(payload, SetOptions.merge()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun deleteAccount(accountId: String) {
        val uid = requireUserId()
        val docRef = requireDb().collection("users").document(uid).collection("accounts").document(accountId)
        val path = docRef.path
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    fun observeTransactions(): Flow<List<TransactionEntity>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        val collectionRef = requireDb().collection("users").document(uid).collection("transactions")

        val txFlow = collectionRef.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val typeStr = data["type"] as? String ?: "EXPENSE"
                    val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                    val amount = (data["amount"] as? Number)?.toDouble() ?: 0.0
                    val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val isRecurring = data["isRecurring"] as? Boolean ?: false

                    TransactionEntity(
                        id = doc.id,
                        type = type,
                        amount = amount,
                        title = data["title"] as? String ?: "Transaction",
                        category = data["category"] as? String ?: "Miscellaneous",
                        subcategory = data["subcategory"] as? String ?: "",
                        accountId = data["accountId"] as? String ?: "",
                        toAccountId = data["toAccountId"] as? String,
                        timestamp = timestamp,
                        notes = data["notes"] as? String ?: "",
                        isRecurring = isRecurring,
                        payeeOrPayer = data["payeeOrPayer"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }.catch { error ->
            if (error is Exception) {
                handleFirestoreError(error, OperationType.LIST, path)
            }
            throw error
        }

        emitAll(txFlow)
    }

    suspend fun saveTransaction(tx: TransactionEntity) {
        val uid = requireUserId()
        val docRef = requireDb().collection("users").document(uid).collection("transactions").document(tx.id)
        val path = docRef.path

        try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "type" to tx.type.name,
                "amount" to tx.amount,
                "title" to tx.title,
                "category" to tx.category,
                "subcategory" to tx.subcategory,
                "accountId" to tx.accountId,
                "timestamp" to tx.timestamp,
                "notes" to tx.notes,
                "isRecurring" to tx.isRecurring,
                "payeeOrPayer" to tx.payeeOrPayer,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            tx.toAccountId?.let {
                if (it.isNotBlank()) payload["toAccountId"] = it
            }

            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.set(payload, SetOptions.merge()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun deleteTransaction(txId: String) {
        val uid = requireUserId()
        val docRef = requireDb().collection("users").document(uid).collection("transactions").document(txId)
        val path = docRef.path
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    suspend fun syncLocalToCloud(accounts: List<AccountEntity>, transactions: List<TransactionEntity>) {
        if (currentUser == null) return
        for (acc in accounts) {
            try {
                saveAccount(acc)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Ignore individual sync errors so remaining items sync
            }
        }
        for (tx in transactions) {
            try {
                saveTransaction(tx)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Ignore individual sync errors so remaining items sync
            }
        }
    }

    private fun compressToBase64(jsonString: String): String {
        val rawBytes = jsonString.toByteArray(Charsets.UTF_8)
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gzip ->
            gzip.write(rawBytes)
        }
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    private fun decompressFromBase64(encoded: String): String {
        val compressedBytes = Base64.decode(encoded, Base64.DEFAULT)
        GZIPInputStream(ByteArrayInputStream(compressedBytes)).use { gzip ->
            return gzip.readBytes().toString(Charsets.UTF_8)
        }
    }

    private suspend fun getDocumentServerFirst(docRef: DocumentReference): DocumentSnapshot {
        return try {
            docRef.get(Source.SERVER).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                e.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
            ) {
                docRef.get().await()
            } else {
                throw e
            }
        }
    }

    private suspend fun cleanupStaleBackupChunks(
        backupsCol: CollectionReference,
        startIndex: Int,
        previousChunkCount: Int
    ) {
        if (previousChunkCount <= startIndex) return
        for (index in startIndex until previousChunkCount) {
            try {
                backupsCol.document("$BACKUP_CHUNK_PREFIX$index").delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort cleanup of stale trailing chunks
            }
        }
    }

    private fun parseCloudBackupInfo(
        snapshot: DocumentSnapshot,
        fallbackFileSizeBytes: Long = 0L
    ): CloudBackupInfo {
        val uploadedAtMillis = (snapshot.get("uploadedAtMillis") as? Number)?.toLong()
            ?: snapshot.getTimestamp("uploadedAt")?.toDate()?.time
            ?: 0L
        return CloudBackupInfo(
            backupDate = snapshot.getString("backupDate") ?: "Latest Cloud Backup",
            uploadedAtTimestamp = uploadedAtMillis,
            accountsCount = (snapshot.get("accountsCount") as? Number)?.toInt() ?: 0,
            transactionsCount = (snapshot.get("transactionsCount") as? Number)?.toInt() ?: 0,
            categoriesCount = (snapshot.get("categoriesCount") as? Number)?.toInt() ?: 0,
            debtsCount = (snapshot.get("debtsCount") as? Number)?.toInt() ?: 0,
            fileSizeBytes = (snapshot.get("fileSizeBytes") as? Number)?.toLong() ?: fallbackFileSizeBytes,
            subcategoriesCount = (snapshot.get("subcategoriesCount") as? Number)?.toInt() ?: 0,
            budgetsCount = (snapshot.get("budgetsCount") as? Number)?.toInt() ?: 0,
            savingsGoalsCount = (snapshot.get("savingsGoalsCount") as? Number)?.toInt() ?: 0,
            compressedSizeBytes = (snapshot.get("compressedSizeBytes") as? Number)?.toLong() ?: 0L,
            chunkCount = (snapshot.get("chunkCount") as? Number)?.toInt() ?: 0,
            schemaVersion = (snapshot.get("schemaVersion") as? Number)?.toInt() ?: 2
        )
    }

    /**
     * Uploads a full JSON wallet snapshot to Firestore under users/{uid}/backups/latest.
     * Compresses with GZIP + Base64 and automatically chunks across sibling documents
     * (latest_chunk_0, latest_chunk_1, ...) if the compressed payload approaches Firestore's 1 MiB limit.
     */
    suspend fun saveCloudBackup(
        jsonString: String,
        accountsCount: Int,
        transactionsCount: Int,
        categoriesCount: Int,
        debtsCount: Int,
        subcategoriesCount: Int = 0,
        budgetsCount: Int = 0,
        savingsGoalsCount: Int = 0,
        backupDate: String = "",
        schemaVersion: Int = 2
    ): Result<CloudBackupInfo> {
        val uid = currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Please sign in with Google to use Cloud Backup."))
        val firestore = try {
            requireDb()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val backupsCol = firestore.collection("users").document(uid).collection("backups")
        val manifestRef = backupsCol.document(BACKUP_MANIFEST_DOC_ID)

        return try {
            val rawSizeBytes = jsonString.toByteArray(Charsets.UTF_8).size.toLong()
            val compressedBase64 = compressToBase64(jsonString)
            val compressedSizeBytes = compressedBase64.length.toLong()
            val now = System.currentTimeMillis()
            val effectiveBackupDate = backupDate.ifBlank {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date(now))
            }

            val previousChunkCount = try {
                val existing = manifestRef.get().await()
                if (existing.exists()) {
                    (existing.get("chunkCount") as? Number)?.toInt() ?: 0
                } else {
                    0
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                0
            }

            val savedChunkCount: Int
            if (compressedBase64.length <= MAX_INLINE_PAYLOAD_CHARS) {
                savedChunkCount = 0
                val payload = hashMapOf<String, Any>(
                    "userId" to uid,
                    "docType" to "manifest",
                    "schemaVersion" to schemaVersion,
                    "payloadEncoding" to PAYLOAD_ENCODING_GZIP_BASE64,
                    "backupJsonCompressed" to compressedBase64,
                    "chunkCount" to 0,
                    "backupDate" to effectiveBackupDate,
                    "accountsCount" to accountsCount,
                    "transactionsCount" to transactionsCount,
                    "categoriesCount" to categoriesCount,
                    "subcategoriesCount" to subcategoriesCount,
                    "budgetsCount" to budgetsCount,
                    "debtsCount" to debtsCount,
                    "savingsGoalsCount" to savingsGoalsCount,
                    "fileSizeBytes" to rawSizeBytes,
                    "compressedSizeBytes" to compressedSizeBytes,
                    "uploadedAt" to FieldValue.serverTimestamp(),
                    "uploadedAtMillis" to now
                )
                manifestRef.set(payload).await()
                cleanupStaleBackupChunks(backupsCol, startIndex = 0, previousChunkCount = previousChunkCount)
            } else {
                val chunks = compressedBase64.chunked(CHUNK_SIZE_CHARS)
                if (chunks.size > MAX_CHUNKS) {
                    return Result.failure(IllegalStateException("Backup exceeds maximum cloud backup size limit."))
                }
                savedChunkCount = chunks.size
                val revisionId = "${now}_${chunks.size}"

                // Write all chunks before updating the latest manifest so concurrent readers never see partial chunks
                for ((index, chunkData) in chunks.withIndex()) {
                    val chunkPayload = hashMapOf<String, Any>(
                        "userId" to uid,
                        "docType" to "chunk",
                        "revisionId" to revisionId,
                        "chunkIndex" to index,
                        "totalChunks" to chunks.size,
                        "data" to chunkData,
                        "uploadedAt" to FieldValue.serverTimestamp()
                    )
                    backupsCol.document("$BACKUP_CHUNK_PREFIX$index").set(chunkPayload).await()
                }

                val manifestPayload = hashMapOf<String, Any>(
                    "userId" to uid,
                    "docType" to "manifest",
                    "schemaVersion" to schemaVersion,
                    "payloadEncoding" to PAYLOAD_ENCODING_GZIP_BASE64_CHUNKED,
                    "revisionId" to revisionId,
                    "chunkCount" to chunks.size,
                    "backupDate" to effectiveBackupDate,
                    "accountsCount" to accountsCount,
                    "transactionsCount" to transactionsCount,
                    "categoriesCount" to categoriesCount,
                    "subcategoriesCount" to subcategoriesCount,
                    "budgetsCount" to budgetsCount,
                    "debtsCount" to debtsCount,
                    "savingsGoalsCount" to savingsGoalsCount,
                    "fileSizeBytes" to rawSizeBytes,
                    "compressedSizeBytes" to compressedSizeBytes,
                    "uploadedAt" to FieldValue.serverTimestamp(),
                    "uploadedAtMillis" to now
                )
                manifestRef.set(manifestPayload).await()
                cleanupStaleBackupChunks(backupsCol, startIndex = chunks.size, previousChunkCount = previousChunkCount)
            }

            Result.success(
                CloudBackupInfo(
                    backupDate = effectiveBackupDate,
                    uploadedAtTimestamp = now,
                    accountsCount = accountsCount,
                    transactionsCount = transactionsCount,
                    categoriesCount = categoriesCount,
                    debtsCount = debtsCount,
                    fileSizeBytes = rawSizeBytes,
                    subcategoriesCount = subcategoriesCount,
                    budgetsCount = budgetsCount,
                    savingsGoalsCount = savingsGoalsCount,
                    compressedSizeBytes = compressedSizeBytes,
                    chunkCount = savedChunkCount,
                    schemaVersion = schemaVersion
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, manifestRef.path)
            Result.failure(e)
        }
    }

    /**
     * Fetches lightweight metadata for the user's latest cloud backup directly from the server
     * (falling back to cache if offline) so fresh installs and second devices immediately see backup status.
     */
    suspend fun fetchLatestCloudBackupMetadata(): Result<CloudBackupInfo?> {
        val uid = currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: return Result.success(null)
        val firestore = try {
            requireDb()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val manifestRef = firestore.collection("users").document(uid)
            .collection("backups").document(BACKUP_MANIFEST_DOC_ID)

        return try {
            val snapshot = getDocumentServerFirst(manifestRef)
            if (!snapshot.exists()) {
                Result.success(null)
            } else {
                Result.success(parseCloudBackupInfo(snapshot))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, manifestRef.path)
            Result.failure(e)
        }
    }

    /**
     * Downloads and reassembles the latest cloud backup JSON for the signed-in Google user.
     * Supports chunked GZIP+Base64, inline GZIP+Base64, and legacy uncompressed backupJson payloads.
     */
    suspend fun fetchLatestCloudBackup(): Result<Pair<String, CloudBackupInfo>> {
        val uid = currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Please sign in with Google to access Cloud Backups."))
        val firestore = try {
            requireDb()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val backupsCol = firestore.collection("users").document(uid).collection("backups")
        val manifestRef = backupsCol.document(BACKUP_MANIFEST_DOC_ID)

        return try {
            val snapshot = getDocumentServerFirst(manifestRef)
            if (!snapshot.exists()) {
                return Result.failure(NoSuchElementException("No cloud backup found for this Google account."))
            }

            val payloadEncoding = snapshot.getString("payloadEncoding")
            val chunkCount = (snapshot.get("chunkCount") as? Number)?.toInt() ?: 0

            val json: String = when {
                payloadEncoding == PAYLOAD_ENCODING_GZIP_BASE64_CHUNKED || chunkCount > 0 -> {
                    if (chunkCount <= 0 || chunkCount > MAX_CHUNKS) {
                        return Result.failure(IllegalStateException("Cloud backup chunk manifest is invalid."))
                    }
                    val expectedRevision = snapshot.getString("revisionId")
                    val combined = StringBuilder()
                    for (index in 0 until chunkCount) {
                        val chunkSnap = getDocumentServerFirst(backupsCol.document("$BACKUP_CHUNK_PREFIX$index"))
                        if (!chunkSnap.exists()) {
                            return Result.failure(IllegalStateException("Cloud backup chunk ${index + 1} of $chunkCount is missing."))
                        }
                        if (!expectedRevision.isNullOrBlank()) {
                            val chunkRevision = chunkSnap.getString("revisionId")
                            if (chunkRevision != expectedRevision) {
                                return Result.failure(IllegalStateException("Cloud backup was updated while downloading. Please try again."))
                            }
                        }
                        val chunkData = chunkSnap.getString("data")
                            ?: return Result.failure(IllegalStateException("Cloud backup chunk ${index + 1} is empty."))
                        combined.append(chunkData)
                    }
                    decompressFromBase64(combined.toString())
                }
                payloadEncoding == PAYLOAD_ENCODING_GZIP_BASE64 || !snapshot.getString("backupJsonCompressed").isNullOrBlank() -> {
                    val compressed = snapshot.getString("backupJsonCompressed")
                        ?: return Result.failure(IllegalStateException("Compressed cloud backup payload is missing."))
                    decompressFromBase64(compressed)
                }
                else -> {
                    snapshot.getString("backupJson")
                        ?: return Result.failure(IllegalStateException("Cloud backup payload is missing."))
                }
            }

            val info = parseCloudBackupInfo(
                snapshot = snapshot,
                fallbackFileSizeBytes = json.toByteArray(Charsets.UTF_8).size.toLong()
            )
            Result.success(Pair(json, info))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, manifestRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchCloudAccounts(): List<AccountEntity> {
        val uid = currentUser?.uid ?: return emptyList()
        return try {
            val snap = requireDb().collection("users").document(uid).collection("accounts").get().await()
            snap.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val typeStr = data["type"] as? String ?: "CASH"
                    val type = try { AccountType.valueOf(typeStr) } catch (_: Exception) { AccountType.CASH }
                    AccountEntity(
                        id = doc.id,
                        name = data["name"] as? String ?: "Account",
                        type = type,
                        institution = data["institution"] as? String ?: "",
                        balance = (data["balance"] as? Number)?.toDouble() ?: 0.0,
                        openingBalance = (data["openingBalance"] as? Number)?.toDouble() ?: 0.0,
                        accountNumberLast4 = data["accountNumberLast4"] as? String ?: "",
                        colorHex = data["colorHex"] as? String ?: "#4F46E5",
                        iconName = data["iconName"] as? String ?: "account_balance",
                        isHidden = data["isHidden"] as? Boolean ?: false,
                        sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
                        creditLimit = (data["creditLimit"] as? Number)?.toDouble() ?: 0.0,
                        billingDate = data["billingDate"] as? String ?: "",
                        paymentDueDate = data["paymentDueDate"] as? String ?: "",
                        minPayment = (data["minPayment"] as? Number)?.toDouble() ?: 0.0,
                        notes = data["notes"] as? String ?: "",
                        includeInTotal = data["includeInTotal"] as? Boolean ?: true
                    )
                } catch (_: Exception) { null }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchCloudTransactions(): List<TransactionEntity> {
        val uid = currentUser?.uid ?: return emptyList()
        return try {
            val snap = requireDb().collection("users").document(uid).collection("transactions").get().await()
            snap.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val typeStr = data["type"] as? String ?: "EXPENSE"
                    val type = try { TransactionType.valueOf(typeStr) } catch (_: Exception) { TransactionType.EXPENSE }
                    TransactionEntity(
                        id = doc.id,
                        type = type,
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        title = data["title"] as? String ?: "Transaction",
                        category = data["category"] as? String ?: "Miscellaneous",
                        subcategory = data["subcategory"] as? String ?: "",
                        accountId = data["accountId"] as? String ?: "",
                        toAccountId = data["toAccountId"] as? String,
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        notes = data["notes"] as? String ?: "",
                        isRecurring = data["isRecurring"] as? Boolean ?: false,
                        payeeOrPayer = data["payeeOrPayer"] as? String ?: ""
                    )
                } catch (_: Exception) { null }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun deleteCloudAllData(): Result<Unit> {
        val uid = currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val firestore = try {
            requireDb()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val userDoc = firestore.collection("users").document(uid)
        return try {
            // Delete manifest and all sibling backup chunks
            val backupsSnap = userDoc.collection("backups").get().await()
            for (doc in backupsSnap.documents) {
                doc.reference.delete().await()
            }
            val accountsSnap = userDoc.collection("accounts").get().await()
            for (doc in accountsSnap.documents) {
                doc.reference.delete().await()
            }
            val txSnap = userDoc.collection("transactions").get().await()
            for (doc in txSnap.documents) {
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userDoc.path)
            Result.failure(e)
        }
    }
}

data class CloudBackupInfo(
    val backupDate: String,
    val uploadedAtTimestamp: Long,
    val accountsCount: Int,
    val transactionsCount: Int,
    val categoriesCount: Int,
    val debtsCount: Int,
    val fileSizeBytes: Long,
    val subcategoriesCount: Int = 0,
    val budgetsCount: Int = 0,
    val savingsGoalsCount: Int = 0,
    val compressedSizeBytes: Long = 0L,
    val chunkCount: Int = 0,
    val schemaVersion: Int = 2
)

