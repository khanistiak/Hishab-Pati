package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

class JsonBackupManager(private val db: AppDatabase) {

    private val isRestoreInProgress = AtomicBoolean(false)

    @Volatile
    var lastPreRestoreBackupJson: String? = null
        private set

    fun isRestoring(): Boolean = isRestoreInProgress.get()

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("appName", "Hishab Pati")
        root.put("schemaVersion", BackupMigrationEngine.CURRENT_SCHEMA_VERSION)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        root.put("backupDate", isoFormat.format(Date()))

        val settings = db.settingsDao().getSettings() ?: AppSettingsEntity()
        root.put("currency", settings.currencyCode)

        val settingsObj = JSONObject().apply {
            put("userName", settings.userName)
            put("country", settings.country)
            put("currencyCode", settings.currencyCode)
            put("currencySymbol", settings.currencySymbol)
            put("language", settings.language.name)
            put("themeMode", settings.themeMode.name)
            put("themeColor", settings.themeColor.name)
            put("backgroundTexture", settings.backgroundTexture.name)
            put("appLockEnabled", settings.appLockEnabled)
            put("screenshotProtectionEnabled", settings.screenshotProtectionEnabled)
            put("notificationsEnabled", settings.notificationsEnabled)
        }
        root.put("settings", settingsObj)

        // Accounts
        val accounts = db.accountDao().getAllAccounts().first()
        val accountsArr = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("type", acc.type.name)
                put("institution", acc.institution)
                put("balance", acc.balance)
                put("openingBalance", acc.openingBalance)
                put("accountNumberLast4", acc.accountNumberLast4)
                put("colorHex", acc.colorHex)
                put("iconName", acc.iconName)
                put("isHidden", acc.isHidden)
                put("sortOrder", acc.sortOrder)
                put("creditLimit", acc.creditLimit)
                put("billingDate", acc.billingDate)
                put("paymentDueDate", acc.paymentDueDate)
                put("minPayment", acc.minPayment)
                put("notes", acc.notes)
                put("includeInTotal", acc.includeInTotal)
            }
            accountsArr.put(obj)
        }
        root.put("accounts", accountsArr)

        // Transactions
        val transactions = db.transactionDao().getAllTransactions().first()
        val txArr = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("type", tx.type.name)
                put("amount", tx.amount)
                put("title", tx.title)
                put("category", tx.category)
                put("subcategory", tx.subcategory)
                put("accountId", tx.accountId)
                if (!tx.toAccountId.isNullOrBlank()) put("toAccountId", tx.toAccountId)
                put("timestamp", tx.timestamp)
                put("notes", tx.notes)
                put("isRecurring", tx.isRecurring)
                put("payeeOrPayer", tx.payeeOrPayer)
            }
            txArr.put(obj)
        }
        root.put("transactions", txArr)

        // Categories & Subcategories
        val categories = db.categoryDao().getAllCategories().first()
        val catArr = JSONArray()
        categories.forEach { cat ->
            catArr.put(
                JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("nameBn", cat.nameBn)
                    put("isIncome", cat.isIncome)
                    put("isTransfer", cat.isTransfer)
                    put("isActive", cat.isActive)
                    put("iconName", cat.iconName)
                    put("colorHex", cat.colorHex)
                    put("sortOrder", cat.sortOrder)
                }
            )
        }
        root.put("categories", catArr)

        val subcategories = db.categoryDao().getAllSubcategories().first()
        val subArr = JSONArray()
        subcategories.forEach { sub ->
            subArr.put(
                JSONObject().apply {
                    put("id", sub.id)
                    put("categoryId", sub.categoryId)
                    put("name", sub.name)
                    put("nameBn", sub.nameBn)
                    put("sortOrder", sub.sortOrder)
                }
            )
        }
        root.put("subcategories", subArr)

        // Budgets
        val budgets = db.budgetDao().getAllBudgets().first()
        val bgtArr = JSONArray()
        budgets.forEach { b ->
            bgtArr.put(
                JSONObject().apply {
                    put("id", b.id)
                    put("categoryName", b.categoryName)
                    put("monthlyLimit", b.monthlyLimit)
                    put("monthYear", b.monthYear)
                    put("isOverall", b.isOverall)
                    put("rolloverEnabled", b.rolloverEnabled)
                    put("name", b.name)
                    put("subcategoryName", b.subcategoryName)
                    put("iconName", b.iconName)
                    put("colorHex", b.colorHex)
                    put("alertPercentage", b.alertPercentage)
                }
            )
        }
        root.put("budgets", bgtArr)

        // Debts
        val debts = db.debtDao().getAllDebts().first()
        val debtArr = JSONArray()
        debts.forEach { d ->
            debtArr.put(
                JSONObject().apply {
                    put("id", d.id)
                    put("personName", d.personName)
                    put("amount", d.amount)
                    put("totalAmount", d.totalAmount)
                    put("paidAmount", d.paidAmount)
                    put("type", d.type.name)
                    put("dueDate", d.dueDate)
                    put("notes", d.notes)
                    put("isSettled", d.isSettled)
                    put("accountId", d.accountId)
                    put("reason", d.reason)
                    put("colorHex", d.colorHex)
                    put("tentativeCloseDateStr", d.tentativeCloseDateStr)
                    put("tags", d.tags)
                }
            )
        }
        root.put("debts", debtArr)

        // Savings Goals
        val goals = db.savingsGoalDao().getAllGoals().first()
        val goalArr = JSONArray()
        goals.forEach { g ->
            goalArr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("targetAmount", g.targetAmount)
                    put("savedAmount", g.savedAmount)
                    put("themeColorHex", g.themeColorHex)
                    put("iconName", g.iconName)
                    if (g.targetDeadline != null) put("targetDeadline", g.targetDeadline)
                    put("targetDeadlineStr", g.targetDeadlineStr)
                    put("createdAt", g.createdAt)
                    put("isCompleted", g.isCompleted)
                }
            )
        }
        root.put("savingsGoals", goalArr)

        root.toString(2)
    }

    suspend fun createBackupCsv(): String = withContext(Dispatchers.IO) {
        val accounts = db.accountDao().getAllAccounts().first()
        val transactions = db.transactionDao().getAllTransactions().first()
        val categories = db.categoryDao().getAllCategories().first()
        val debts = db.debtDao().getAllDebts().first()
        val settings = db.settingsDao().getSettings()
        CsvBackupEngine.exportToCsv(
            accounts = accounts,
            transactions = transactions,
            categories = categories,
            debts = debts,
            settings = settings
        )
    }

    fun previewBackup(
        jsonString: String,
        fileName: String = "wallet_backup.json",
        fileSizeBytes: Long = jsonString.toByteArray(Charsets.UTF_8).size.toLong()
    ): BackupPreview {
        val lowerName = fileName.lowercase(Locale.US)
        val isCsv = lowerName.endsWith(".csv") ||
            jsonString.trimStart().startsWith("#") ||
            jsonString.trimStart().startsWith("SECTION,") ||
            (!jsonString.trimStart().startsWith("{") && jsonString.contains(",") && (jsonString.contains("amount") || jsonString.contains("Amount")))

        if (fileName.isNotBlank() && !lowerName.endsWith(".json") && !lowerName.endsWith(".csv") && !isCsv) {
            val diag = BackupDiagnosticError(
                errorCode = BackupErrorCode.ERR_INVALID_EXTENSION,
                failedStage = RestoreStage.READING_FILE,
                technicalDetails = "Selected file '$fileName' does not have a supported .json or .csv extension."
            )
            return BackupPreview(
                appName = "",
                schemaVersion = 0,
                backupDate = "",
                accountsCount = 0,
                transactionsCount = 0,
                categoriesCount = 0,
                budgetsCount = 0,
                debtsCount = 0,
                isValid = false,
                errorMessage = diag.userMessageEn,
                fileName = fileName,
                fileSizeBytes = fileSizeBytes,
                diagnosticError = diag
            )
        }

        return try {
            val payload = if (isCsv) {
                CsvBackupEngine.parseCsv(jsonString)
            } else {
                BackupMigrationEngine.parseAndMigrate(jsonString)
            }
            BackupPreview(
                appName = payload.appName,
                schemaVersion = payload.schemaVersion,
                backupDate = payload.backupDate.ifBlank { "Unknown date" },
                accountsCount = payload.accounts.size,
                transactionsCount = payload.transactions.size,
                categoriesCount = payload.categories.size,
                subcategoriesCount = payload.subcategories.size,
                budgetsCount = payload.budgets.size,
                debtsCount = payload.debts.size,
                savingsGoalsCount = payload.savingsGoals.size,
                hasSettings = payload.settings != null,
                isValid = true,
                fileName = fileName,
                fileSizeBytes = fileSizeBytes,
                formatType = if (isCsv) BackupFormatType.CSV_EXPORT else payload.formatType,
                detectedDuplicatesCount = payload.skippedDuplicatesCount,
                requiresMigration = !isCsv && payload.formatType != BackupFormatType.MY_WALLET_CURRENT
            )
        } catch (e: BackupRestoreException) {
            BackupPreview(
                appName = "",
                schemaVersion = 0,
                backupDate = "",
                accountsCount = 0,
                transactionsCount = 0,
                categoriesCount = 0,
                budgetsCount = 0,
                debtsCount = 0,
                isValid = false,
                errorMessage = e.diagnostic.userMessageEn,
                fileName = fileName,
                fileSizeBytes = fileSizeBytes,
                diagnosticError = e.diagnostic
            )
        } catch (e: Exception) {
            val diag = BackupDiagnosticError(
                errorCode = BackupErrorCode.ERR_INVALID_JSON,
                failedStage = RestoreStage.VALIDATING_STRUCTURE,
                technicalDetails = e.message?.take(180) ?: "Unknown parsing exception"
            )
            BackupPreview(
                appName = "",
                schemaVersion = 0,
                backupDate = "",
                accountsCount = 0,
                transactionsCount = 0,
                categoriesCount = 0,
                budgetsCount = 0,
                debtsCount = 0,
                isValid = false,
                errorMessage = diag.userMessageEn,
                fileName = fileName,
                fileSizeBytes = fileSizeBytes,
                diagnosticError = diag
            )
        }
    }

    suspend fun restoreFromJson(
        jsonString: String,
        replaceAll: Boolean
    ): Result<Unit> {
        return restoreWithDetailedSummary(jsonString = jsonString, replaceAll = replaceAll).map { }
    }

    suspend fun restoreWithDetailedSummary(
        jsonString: String,
        replaceAll: Boolean,
        simulateDatabaseFailure: Boolean = false,
        onProgress: (RestoreProgress) -> Unit = {}
    ): Result<RestoreResultSummary> = withContext(Dispatchers.IO) {
        if (!isRestoreInProgress.compareAndSet(false, true)) {
            val diag = BackupDiagnosticError(
                errorCode = BackupErrorCode.ERR_UNEXPECTED,
                failedStage = RestoreStage.READING_FILE,
                technicalDetails = "Another restore operation is already in progress.",
                customMessageEn = "A restore operation is already running. Please wait for it to finish."
            )
            return@withContext Result.failure(BackupRestoreException(diag))
        }

        var currentStage = RestoreStage.READING_FILE
        try {
            coroutineContext.ensureActive()

            val existingAccounts = db.accountDao().getAllAccounts().first()
            val existingAccountIds = if (replaceAll) emptySet() else existingAccounts.map { it.id }.toSet()

            val isCsv = jsonString.trimStart().startsWith("#") ||
                jsonString.trimStart().startsWith("SECTION,") ||
                (!jsonString.trimStart().startsWith("{") && jsonString.contains(","))

            // Stages 1..8: Parse, validate schema, migrate legacy structures, and check referential integrity
            val payload = if (isCsv) {
                currentStage = RestoreStage.READING_FILE
                onProgress(
                    RestoreProgress(
                        stage = RestoreStage.READING_FILE,
                        percentage = 20,
                        isIndeterminate = false,
                        processedRecords = 0,
                        totalRecords = 0,
                        statusDetail = "Parsing CSV format..."
                    )
                )
                CsvBackupEngine.parseCsv(jsonString)
            } else {
                BackupMigrationEngine.parseAndMigrate(
                    jsonString = jsonString,
                    existingAccountIds = existingAccountIds
                ) { stage, pct, _, detail ->
                    currentStage = stage
                    onProgress(
                        RestoreProgress(
                            stage = stage,
                            percentage = pct,
                            isIndeterminate = false,
                            processedRecords = 0,
                            totalRecords = 0,
                            statusDetail = detail
                        )
                    )
                }
            }

            val totalRecordsToProcess = payload.accounts.size +
                payload.categories.size +
                payload.subcategories.size +
                payload.transactions.size +
                payload.budgets.size +
                payload.debts.size +
                payload.savingsGoals.size

            var processedRecords = 0
            fun reportStage(stage: RestoreStage, pct: Int, detail: String, indeterminate: Boolean = false) {
                currentStage = stage
                onProgress(
                    RestoreProgress(
                        stage = stage,
                        percentage = pct.coerceIn(0, 100),
                        isIndeterminate = indeterminate,
                        processedRecords = processedRecords.coerceAtMost(totalRecordsToProcess),
                        totalRecords = totalRecordsToProcess,
                        statusDetail = detail
                    )
                )
            }

            coroutineContext.ensureActive()

            // Create pre-restore safety backup snapshot in memory before modifying database
            val preRestoreCreated = try {
                lastPreRestoreBackupJson = createBackupJson()
                true
            } catch (_: Exception) {
                false
            }

            reportStage(
                RestoreStage.SAVING_TO_DATABASE,
                55,
                "Preparing atomic database commit..."
            )

            var additionalSkippedDuplicates = 0
            var insertedAccountsCount = 0
            var insertedCategoriesCount = 0
            var insertedSubcategoriesCount = 0
            var insertedTransactionsCount = 0
            var insertedBudgetsCount = 0
            var insertedDebtsCount = 0
            var insertedGoalsCount = 0
            var verificationReport: FinancialVerificationReport? = null

            // Stage 9: Atomic Database Transaction (rolls back automatically if any exception occurs)
            db.withTransaction {
                coroutineContext.ensureActive()

                if (replaceAll) {
                    db.transactionDao().deleteAllTransactions()
                    db.accountDao().deleteAllAccounts()
                    db.budgetDao().deleteAllBudgets()
                    db.debtDao().deleteAllDebts()
                    db.categoryDao().deleteAllCategories()
                    db.categoryDao().deleteAllSubcategories()
                    db.savingsGoalDao().deleteAllGoals()

                    // Save Settings
                    payload.settings?.let { s ->
                        val current = db.settingsDao().getSettings() ?: AppSettingsEntity()
                        db.settingsDao().saveSettings(
                            current.copy(
                                userName = s.userName,
                                country = s.country,
                                currencyCode = s.currencyCode,
                                currencySymbol = s.currencySymbol,
                                language = s.language,
                                themeMode = s.themeMode,
                                themeColor = s.themeColor,
                                backgroundTexture = s.backgroundTexture,
                                appLockEnabled = s.appLockEnabled,
                                screenshotProtectionEnabled = s.screenshotProtectionEnabled,
                                notificationsEnabled = s.notificationsEnabled
                            )
                        )
                    }

                    // Accounts
                    if (payload.accounts.isNotEmpty()) {
                        db.accountDao().insertAccounts(payload.accounts)
                        insertedAccountsCount = payload.accounts.size
                        processedRecords += payload.accounts.size
                        reportStage(
                            RestoreStage.SAVING_TO_DATABASE,
                            60,
                            "Saved ${payload.accounts.size} accounts..."
                        )
                    }

                    // Categories & Subcategories
                    if (payload.categories.isNotEmpty()) {
                        db.categoryDao().insertCategories(payload.categories)
                        insertedCategoriesCount = payload.categories.size
                        processedRecords += payload.categories.size
                    }
                    if (payload.subcategories.isNotEmpty()) {
                        db.categoryDao().insertSubcategories(payload.subcategories)
                        insertedSubcategoriesCount = payload.subcategories.size
                        processedRecords += payload.subcategories.size
                    }
                    reportStage(
                        RestoreStage.PROCESSING_CATEGORIES,
                        65,
                        "Saved $insertedCategoriesCount categories & $insertedSubcategoriesCount subcategories..."
                    )

                    // Transactions in safe batches of 250
                    if (payload.transactions.isNotEmpty()) {
                        val chunks = payload.transactions.chunked(250)
                        chunks.forEachIndexed { idx, batch ->
                            coroutineContext.ensureActive()
                            db.transactionDao().insertTransactions(batch)
                            insertedTransactionsCount += batch.size
                            processedRecords += batch.size
                            val txProgress = 65 + ((idx + 1) * 20 / chunks.size)
                            reportStage(
                                RestoreStage.PROCESSING_TRANSACTIONS,
                                txProgress,
                                "Processed $insertedTransactionsCount of ${payload.transactions.size} transactions..."
                            )
                        }
                    }

                    // Budgets, Debts, Savings Goals
                    if (payload.budgets.isNotEmpty()) {
                        db.budgetDao().insertBudgets(payload.budgets)
                        insertedBudgetsCount = payload.budgets.size
                        processedRecords += payload.budgets.size
                    }
                    if (payload.debts.isNotEmpty()) {
                        db.debtDao().insertDebts(payload.debts)
                        insertedDebtsCount = payload.debts.size
                        processedRecords += payload.debts.size
                    }
                    if (payload.savingsGoals.isNotEmpty()) {
                        db.savingsGoalDao().insertGoals(payload.savingsGoals)
                        insertedGoalsCount = payload.savingsGoals.size
                        processedRecords += payload.savingsGoals.size
                    }
                    reportStage(
                        RestoreStage.PROCESSING_BUDGETS_DEBTS,
                        90,
                        "Saved budgets and debts..."
                    )
                } else {
                    // MERGE MODE: Deterministic duplicate handling & accurate balance reconciliation
                    val localAccounts = db.accountDao().getAllAccounts().first()
                    val localCategories = db.categoryDao().getAllCategories().first()
                    val localSubcategories = db.categoryDao().getAllSubcategories().first()
                    val localTransactions = db.transactionDao().getAllTransactions().first().toMutableList()
                    val localBudgets = db.budgetDao().getAllBudgets().first()
                    val localDebts = db.debtDao().getAllDebts().first().toMutableList()
                    val localGoals = db.savingsGoalDao().getAllGoals().first()

                    // Clean up any un-migrated synthetic seed transactions (tx_1..tx_19)
                    // if the incoming backup contains real migrated records
                    val incomingHasRealExpend = payload.transactions.any { it.id.startsWith("exp_") || it.id.startsWith("rec_") }
                    if (incomingHasRealExpend) {
                        val sampleTxIdsToRemove = localTransactions
                            .filter { it.id.matches(Regex("^tx_\\d+$")) }
                            .map { it.id }
                        sampleTxIdsToRemove.forEach { id ->
                            db.transactionDao().deleteTransactionById(id)
                        }
                        localTransactions.removeAll { it.id in sampleTxIdsToRemove }
                    }

                    // 1. Merge Categories & Subcategories (avoid duplicates by ID or lowercase name)
                    val existingCatIds = localCategories.map { it.id }.toMutableSet()
                    val existingCatNames = localCategories.map { it.name.lowercase(Locale.US) }.toMutableSet()
                    val newCategories = mutableListOf<CategoryEntity>()
                    for (cat in payload.categories) {
                        if (existingCatIds.contains(cat.id) || existingCatNames.contains(cat.name.lowercase(Locale.US))) {
                            additionalSkippedDuplicates++
                        } else {
                            existingCatIds.add(cat.id)
                            existingCatNames.add(cat.name.lowercase(Locale.US))
                            newCategories.add(cat)
                        }
                        processedRecords++
                    }
                    if (newCategories.isNotEmpty()) {
                        db.categoryDao().insertCategories(newCategories)
                        insertedCategoriesCount = newCategories.size
                    }

                    val existingSubIds = localSubcategories.map { it.id }.toMutableSet()
                    val existingSubKeys = localSubcategories
                        .map { "${it.categoryId.lowercase(Locale.US)}::${it.name.lowercase(Locale.US)}" }
                        .toMutableSet()
                    val newSubcategories = mutableListOf<SubcategoryEntity>()
                    for (sub in payload.subcategories) {
                        val key = "${sub.categoryId.lowercase(Locale.US)}::${sub.name.lowercase(Locale.US)}"
                        if (existingSubIds.contains(sub.id) || existingSubKeys.contains(key)) {
                            additionalSkippedDuplicates++
                        } else {
                            existingSubIds.add(sub.id)
                            existingSubKeys.add(key)
                            newSubcategories.add(sub)
                        }
                        processedRecords++
                    }
                    if (newSubcategories.isNotEmpty()) {
                        db.categoryDao().insertSubcategories(newSubcategories)
                        insertedSubcategoriesCount = newSubcategories.size
                    }

                    // 2. Merge Transactions (detect duplicates by ID or composite signature)
                    val existingTxIds = localTransactions.map { it.id }.toMutableSet()
                    fun txSignature(tx: TransactionEntity): String {
                        val minuteBucket = tx.timestamp / 60000L
                        return "${tx.type}::${tx.amount}::${tx.accountId}::${tx.title.lowercase(Locale.US)}::$minuteBucket"
                    }
                    val existingTxSignatures = localTransactions.map { txSignature(it) }.toMutableSet()
                    val backupTxIds = payload.transactions.map { it.id }.toSet()
                    val backupTxSignatures = payload.transactions.map { txSignature(it) }.toSet()

                    val newTransactions = mutableListOf<TransactionEntity>()
                    for (tx in payload.transactions) {
                        val sig = txSignature(tx)
                        if (existingTxIds.contains(tx.id) || existingTxSignatures.contains(sig)) {
                            additionalSkippedDuplicates++
                        } else {
                            existingTxIds.add(tx.id)
                            existingTxSignatures.add(sig)
                            newTransactions.add(tx)
                        }
                        processedRecords++
                    }

                    if (newTransactions.isNotEmpty()) {
                        val chunks = newTransactions.chunked(250)
                        chunks.forEachIndexed { idx, batch ->
                            coroutineContext.ensureActive()
                            db.transactionDao().insertTransactions(batch)
                            insertedTransactionsCount += batch.size
                            val txProgress = 65 + ((idx + 1) * 15 / chunks.size)
                            reportStage(
                                RestoreStage.PROCESSING_TRANSACTIONS,
                                txProgress,
                                "Merged $insertedTransactionsCount new transactions..."
                            )
                        }
                    }

                    // 3. Merge Accounts & Reconcile Balances
                    // Local-only transactions = local transactions that were NOT in the incoming backup
                    val localOnlyTxs = localTransactions.filter { lt ->
                        !backupTxIds.contains(lt.id) && !backupTxSignatures.contains(txSignature(lt))
                    }
                    val localAccountMap = localAccounts.associateBy { it.id }
                    val backupAccountMap = payload.accounts.associateBy { it.id }
                    val accountsToUpsert = mutableListOf<AccountEntity>()

                    for (bAcc in payload.accounts) {
                        val existingAcc = localAccountMap[bAcc.id]
                        if (existingAcc == null) {
                            accountsToUpsert.add(bAcc)
                            insertedAccountsCount++
                        } else {
                            val reconciledBalance = if (payload.accountsHaveExplicitBalance.contains(bAcc.id)) {
                                val localOnlyDelta = localOnlyTxs.sumOf {
                                    FinancialIntegrityValidator.computeSingleTxBalanceDelta(it, bAcc.id)
                                }
                                bAcc.balance + localOnlyDelta
                            } else {
                                val newlyAddedDelta = newTransactions.sumOf {
                                    FinancialIntegrityValidator.computeSingleTxBalanceDelta(it, bAcc.id)
                                }
                                existingAcc.balance + newlyAddedDelta
                            }
                            accountsToUpsert.add(existingAcc.copy(balance = reconciledBalance))
                        }
                        processedRecords++
                    }

                    // Also update any local-only accounts affected by newly merged transactions
                    for (lAcc in localAccounts) {
                        if (!backupAccountMap.containsKey(lAcc.id)) {
                            val delta = newTransactions.sumOf {
                                FinancialIntegrityValidator.computeSingleTxBalanceDelta(it, lAcc.id)
                            }
                            if (delta != 0.0) {
                                accountsToUpsert.add(lAcc.copy(balance = lAcc.balance + delta))
                            }
                        }
                    }

                    if (accountsToUpsert.isNotEmpty()) {
                        db.accountDao().insertAccounts(accountsToUpsert)
                    }

                    // 4. Merge Budgets
                    val existingBudgetIds = localBudgets.map { it.id }.toMutableSet()
                    val existingBudgetKeys = localBudgets
                        .map { "${it.categoryName.lowercase(Locale.US)}::${it.monthYear}::${it.isOverall}" }
                        .toMutableSet()
                    val newBudgets = mutableListOf<BudgetEntity>()
                    for (bgt in payload.budgets) {
                        val key = "${bgt.categoryName.lowercase(Locale.US)}::${bgt.monthYear}::${bgt.isOverall}"
                        if (existingBudgetIds.contains(bgt.id) || existingBudgetKeys.contains(key)) {
                            additionalSkippedDuplicates++
                        } else {
                            existingBudgetIds.add(bgt.id)
                            existingBudgetKeys.add(key)
                            newBudgets.add(bgt)
                        }
                        processedRecords++
                    }
                    if (newBudgets.isNotEmpty()) {
                        db.budgetDao().insertBudgets(newBudgets)
                        insertedBudgetsCount = newBudgets.size
                    }

                    // 5. Merge Debts
                    val existingDebtIds = localDebts.map { it.id }.toMutableSet()
                    val existingActiveDebtKeys = localDebts
                        .filter { !it.isSettled }
                        .map { "${it.personName.lowercase(Locale.US)}::${it.type}::${it.amount}" }
                        .toMutableSet()
                    val newDebts = mutableListOf<DebtEntity>()
                    for (d in payload.debts) {
                        val activeKey = "${d.personName.lowercase(Locale.US)}::${d.type}::${d.amount}"
                        if (existingDebtIds.contains(d.id) || (!d.isSettled && existingActiveDebtKeys.contains(activeKey))) {
                            additionalSkippedDuplicates++
                        } else {
                            existingDebtIds.add(d.id)
                            if (!d.isSettled) existingActiveDebtKeys.add(activeKey)
                            newDebts.add(d)
                        }
                        processedRecords++
                    }
                    if (newDebts.isNotEmpty()) {
                        db.debtDao().insertDebts(newDebts)
                        insertedDebtsCount = newDebts.size
                    }

                    // 6. Merge Savings Goals
                    val existingGoalIds = localGoals.map { it.id }.toMutableSet()
                    val newGoals = payload.savingsGoals.filter { !existingGoalIds.contains(it.id) }
                    if (newGoals.isNotEmpty()) {
                        db.savingsGoalDao().insertGoals(newGoals)
                        insertedGoalsCount = newGoals.size
                    }
                }

                if (simulateDatabaseFailure) {
                    throw BackupRestoreException(
                        BackupDiagnosticError(
                            errorCode = BackupErrorCode.ERR_DATABASE_WRITE_FAILED,
                            failedStage = RestoreStage.SAVING_TO_DATABASE,
                            technicalDetails = "Simulated database write failure triggered rollback."
                        )
                    )
                }

                // Stage 11: Post-write verification inside transaction before commit
                reportStage(
                    RestoreStage.VERIFYING_RESTORED_DATA,
                    96,
                    "Verifying restored financial integrity..."
                )
                val finalAccounts = db.accountDao().getAllAccounts().first()
                val finalTransactions = db.transactionDao().getAllTransactions().first()
                val finalBudgets = db.budgetDao().getAllBudgets().first()
                val finalDebts = db.debtDao().getAllDebts().first()

                val report = FinancialIntegrityValidator.verifyRestoredData(
                    accounts = finalAccounts,
                    transactions = finalTransactions,
                    budgets = finalBudgets,
                    debts = finalDebts
                )
                if (!report.isValid) {
                    throw BackupRestoreException(
                        BackupDiagnosticError(
                            errorCode = BackupErrorCode.ERR_REFERENTIAL_INTEGRITY,
                            failedStage = RestoreStage.VERIFYING_RESTORED_DATA,
                            technicalDetails = report.errorDetails ?: "Post-restore financial verification failed."
                        )
                    )
                }
                verificationReport = report
            }

            reportStage(
                RestoreStage.REFRESHING_ANALYTICS,
                100,
                "Restore complete. Refreshing dashboard..."
            )

            val totalRestored = insertedAccountsCount +
                insertedCategoriesCount +
                insertedSubcategoriesCount +
                insertedTransactionsCount +
                insertedBudgetsCount +
                insertedDebtsCount +
                insertedGoalsCount

            val report = verificationReport!!
            val summary = RestoreResultSummary(
                mode = if (replaceAll) RestoreMode.REPLACE_ALL else RestoreMode.MERGE,
                formatType = payload.formatType,
                schemaVersion = payload.schemaVersion,
                totalRestoredRecords = totalRestored,
                accountsRestored = insertedAccountsCount,
                transactionsRestored = insertedTransactionsCount,
                categoriesRestored = insertedCategoriesCount,
                subcategoriesRestored = insertedSubcategoriesCount,
                budgetsRestored = insertedBudgetsCount,
                debtsRestored = insertedDebtsCount,
                savingsGoalsRestored = insertedGoalsCount,
                skippedDuplicatesCount = payload.skippedDuplicatesCount + additionalSkippedDuplicates,
                migratedRecordsCount = payload.migratedRecordsCount,
                verifiedTotalIncome = report.totalIncome,
                verifiedTotalExpense = report.totalExpense,
                verifiedCurrentMonthExpense = report.currentMonthExpense,
                verifiedNetWorth = report.netWorth,
                isFinancialIntegrityVerified = true,
                preRestoreBackupCreated = preRestoreCreated
            )

            Result.success(summary)
        } catch (e: CancellationException) {
            val diag = BackupDiagnosticError(
                errorCode = BackupErrorCode.ERR_INTERRUPTED,
                failedStage = currentStage,
                technicalDetails = "Coroutine was cancelled during stage ${currentStage.name}."
            )
            Result.failure(BackupRestoreException(diag, e))
        } catch (e: BackupRestoreException) {
            Result.failure(e)
        } catch (e: Exception) {
            val diag = BackupDiagnosticError(
                errorCode = BackupErrorCode.ERR_DATABASE_WRITE_FAILED,
                failedStage = currentStage,
                technicalDetails = "${e::class.simpleName}: ${e.message?.take(180) ?: "Unexpected failure"}"
            )
            Result.failure(BackupRestoreException(diag, e))
        } finally {
            isRestoreInProgress.set(false)
        }
    }
}
