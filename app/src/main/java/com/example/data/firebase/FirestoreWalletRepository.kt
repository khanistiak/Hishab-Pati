package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class FirestoreWalletRepository(private val db: FirebaseFirestore? = null) {

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
            } catch (e: Exception) {
                // Ignore individual sync errors so remaining items sync
            }
        }
        for (tx in transactions) {
            try {
                saveTransaction(tx)
            } catch (e: Exception) {
                // Ignore individual sync errors so remaining items sync
            }
        }
    }

    suspend fun saveCloudBackup(
        jsonString: String,
        accountsCount: Int,
        transactionsCount: Int,
        categoriesCount: Int,
        debtsCount: Int
    ): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.failure(IllegalStateException("Please sign in with Google to use Cloud Backup."))
        val docRef = requireDb().collection("users").document(uid).collection("backups").document("latest")
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "backupJson" to jsonString,
            "accountsCount" to accountsCount,
            "transactionsCount" to transactionsCount,
            "categoriesCount" to categoriesCount,
            "debtsCount" to debtsCount,
            "fileSizeBytes" to jsonString.toByteArray(Charsets.UTF_8).size.toLong(),
            "uploadedAt" to FieldValue.serverTimestamp(),
            "uploadedAtMillis" to System.currentTimeMillis()
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLatestCloudBackup(): Result<Pair<String, CloudBackupInfo>> {
        val uid = currentUser?.uid ?: return Result.failure(IllegalStateException("Please sign in with Google to access Cloud Backups."))
        val docRef = requireDb().collection("users").document(uid).collection("backups").document("latest")
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(NoSuchElementException("No cloud backup found for this Google account."))
            }
            val json = snapshot.getString("backupJson")
                ?: return Result.failure(IllegalStateException("Cloud backup payload is missing."))
            val info = CloudBackupInfo(
                backupDate = snapshot.getString("backupDate") ?: "Latest Cloud Backup",
                uploadedAtTimestamp = (snapshot.get("uploadedAtMillis") as? Number)?.toLong() ?: System.currentTimeMillis(),
                accountsCount = (snapshot.get("accountsCount") as? Number)?.toInt() ?: 0,
                transactionsCount = (snapshot.get("transactionsCount") as? Number)?.toInt() ?: 0,
                categoriesCount = (snapshot.get("categoriesCount") as? Number)?.toInt() ?: 0,
                debtsCount = (snapshot.get("debtsCount") as? Number)?.toInt() ?: 0,
                fileSizeBytes = (snapshot.get("fileSizeBytes") as? Number)?.toLong() ?: json.toByteArray(Charsets.UTF_8).size.toLong()
            )
            Result.success(Pair(json, info))
        } catch (e: Exception) {
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
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun deleteCloudAllData(): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return try {
            requireDb().collection("users").document(uid).collection("backups").document("latest").delete().await()
            val accountsSnap = requireDb().collection("users").document(uid).collection("accounts").get().await()
            for (doc in accountsSnap.documents) {
                doc.reference.delete().await()
            }
            val txSnap = requireDb().collection("users").document(uid).collection("transactions").get().await()
            for (doc in txSnap.documents) {
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
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
    val fileSizeBytes: Long
)

