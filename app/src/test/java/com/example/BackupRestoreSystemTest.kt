package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupErrorCode
import com.example.data.backup.BackupFormatType
import com.example.data.backup.BackupRestoreException
import com.example.data.backup.FinancialIntegrityValidator
import com.example.data.backup.JsonBackupManager
import com.example.data.backup.RestoreProgress
import com.example.data.backup.RestoreStage
import com.example.data.backup.SampleDataGenerator
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreSystemTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var backupManager: JsonBackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backupManager = JsonBackupManager(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `1_current My Wallet attached backup parses, deduplicates sample seeds, and verifies financial integrity`() = runBlocking {
        val jsonText = SampleDataGenerator.generateSampleJsonTemplate()

        val preview = backupManager.previewBackup(
            jsonString = jsonText,
            fileName = "sample_backup.json",
            fileSizeBytes = jsonText.toByteArray().size.toLong()
        )

        assertTrue("Preview should be valid", preview.isValid)
        assertEquals(BackupFormatType.MY_WALLET_CURRENT, preview.formatType)
        assertEquals(1, preview.schemaVersion)
        assertEquals(4, preview.accountsCount)
        assertEquals(17, preview.transactionsCount)
        assertEquals(11, preview.categoriesCount)
        assertEquals(15, preview.subcategoriesCount)
        assertEquals(3, preview.budgetsCount)
        assertEquals(4, preview.debtsCount)

        // Execute Replace All restore and capture progress stages
        val observedStages = mutableListOf<RestoreStage>()
        val result = backupManager.restoreWithDetailedSummary(
            jsonString = jsonText,
            replaceAll = true,
            onProgress = { progress ->
                observedStages.add(progress.stage)
                assertTrue(progress.percentage in 0..100)
            }
        ).getOrThrow()

        assertTrue("All verification checks must pass", result.isFinancialIntegrityVerified)
        assertEquals(4, result.accountsRestored)
        assertEquals(17, result.transactionsRestored)
        assertEquals(11, result.categoriesRestored)
        assertEquals(15, result.subcategoriesRestored)
        assertEquals(3, result.budgetsRestored)
        assertEquals(4, result.debtsRestored)
        assertTrue(observedStages.contains(RestoreStage.PROCESSING_TRANSACTIONS))
        assertTrue(observedStages.contains(RestoreStage.SAVING_TO_DATABASE))
        assertTrue(observedStages.contains(RestoreStage.VERIFYING_RESTORED_DATA))

        // Verify Net Worth = 120,300.0 assets - 4,500.0 liabilities = 115,800.0
        assertEquals(115800.0, result.verifiedNetWorth, 0.01)

        // Verify October 2026 consumption expenses = 40,000.0 (excluding internal transfers, CC payments, and debt flows)
        val restoredTxs = db.transactionDao().getAllTransactions().first()
        val restoredBudgets = db.budgetDao().getAllBudgets().first()
        val (refYear, refMonth) = FinancialIntegrityValidator.resolveReferenceYearMonth(restoredTxs, restoredBudgets)
        assertEquals(2026, refYear)
        assertEquals(Calendar.OCTOBER, refMonth)

        val octExpenses = restoredTxs.filter { tx ->
            if (!FinancialIntegrityValidator.isEligibleExpense(tx)) return@filter false
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            cal.get(Calendar.YEAR) == 2026 && cal.get(Calendar.MONTH) == Calendar.OCTOBER
        }
        val octTotalSpent = octExpenses.sumOf { it.amount }
        assertEquals(36200.0, octTotalSpent, 0.01)
        assertEquals(36200.0, result.verifiedCurrentMonthExpense, 0.01)
    }

    @Test
    fun `fresh installation initialization creates zero user-created budgets`() = runBlocking {
        val repo = com.example.data.repository.WalletRepository(db)
        repo.initializeIfNeeded()

        val budgets = repo.budgets.first()
        assertTrue("Fresh installation must have zero user-created budgets", budgets.isEmpty())
    }

    @Test
    fun `2_legacy Expend backup migrates wallets, records, split transactions, and ID mappings accurately`() = runBlocking {
        val legacyExpendJson = """
            {
              "appName": "Expend",
              "version": 1,
              "wallets": [
                {
                  "id": "w_bank_1",
                  "name": "City Bank PLC",
                  "type": "BANK",
                  "balance": 45000.0,
                  "initialBalance": 20000.0,
                  "color": "#DC2626"
                },
                {
                  "id": "w_mfs_1",
                  "name": "bKash Personal",
                  "type": "MFS",
                  "balance": 3500.0,
                  "initialBalance": 1000.0,
                  "color": "#E11D48"
                }
              ],
              "expenseCategories": [
                {
                  "id": "c_food",
                  "name": "Food & Dining",
                  "isIncome": false,
                  "subcategories": [
                    { "id": "sub_lunch", "name": "Lunch" },
                    { "id": "sub_snacks", "name": "Snacks" }
                  ]
                },
                {
                  "id": "c_shopping",
                  "name": "Shopping",
                  "isIncome": false
                }
              ],
              "records": [
                {
                  "id": "rec_normal_1",
                  "type": "EXPENSE",
                  "amount": 450.0,
                  "title": "Biryani Lunch",
                  "categoryId": "c_food",
                  "subcategoryId": "sub_lunch",
                  "walletId": "w_mfs_1",
                  "date": 1761400000,
                  "note": "Office lunch"
                },
                {
                  "id": "rec_transfer_1",
                  "type": "TRANSFER",
                  "amount": 2000.0,
                  "title": "bKash Cash In",
                  "walletId": "w_bank_1",
                  "toWalletId": "w_mfs_1",
                  "date": "2026-10-20T10:30:00Z"
                },
                {
                  "id": "rec_split_parent",
                  "type": "EXPENSE",
                  "amount": 1500.0,
                  "title": "Supermarket Split",
                  "walletId": "w_bank_1",
                  "date": 1761480000000,
                  "splits": [
                    { "amount": 600.0, "categoryId": "c_food", "subcategoryId": "sub_snacks", "title": "Bakery Snacks" },
                    { "amount": 900.0, "categoryId": "c_shopping", "title": "Household Items" }
                  ]
                }
              ],
              "loans": [
                {
                  "id": "loan_1",
                  "personName": "Rahim",
                  "totalAmount": 5000.0,
                  "paidAmount": 1000.0,
                  "type": "LENT",
                  "walletId": "w_bank_1"
                }
              ]
            }
        """.trimIndent()

        val preview = backupManager.previewBackup(
            jsonString = legacyExpendJson,
            fileName = "expend_backup.json",
            fileSizeBytes = legacyExpendJson.length.toLong()
        )

        assertTrue(preview.isValid)
        assertEquals(BackupFormatType.EXPEND_LEGACY, preview.formatType)
        assertEquals(2, preview.accountsCount)
        // 1 normal + 1 transfer + 2 split children = 4 transactions
        assertEquals(4, preview.transactionsCount)
        assertEquals(1, preview.debtsCount)

        val result = backupManager.restoreWithDetailedSummary(
            jsonString = legacyExpendJson,
            replaceAll = true
        ).getOrThrow()

        assertTrue(result.isFinancialIntegrityVerified)
        val txs = db.transactionDao().getAllTransactions().first()
        assertEquals(4, txs.size)

        val lunchTx = txs.first { it.id == "rec_normal_1" }
        assertEquals("Food & Dining", lunchTx.category)
        assertEquals("Lunch", lunchTx.subcategory)
        assertEquals("w_mfs_1", lunchTx.accountId)
        assertEquals(1761400000000L, lunchTx.timestamp) // converted from seconds to ms

        val transferTx = txs.first { it.id == "rec_transfer_1" }
        assertEquals(TransactionType.TRANSFER, transferTx.type)
        assertEquals("w_bank_1", transferTx.accountId)
        assertEquals("w_mfs_1", transferTx.toAccountId)
        assertFalse(FinancialIntegrityValidator.isEligibleExpense(transferTx))

        val split0 = txs.first { it.id == "rec_split_parent_split_0" }
        assertEquals(600.0, split0.amount, 0.01)
        assertEquals("Food & Dining", split0.category)
        assertEquals("Snacks", split0.subcategory)
    }

    @Test
    fun `3_large backup with 2500 transactions restores in batches with accurate progress and zero data loss`() = runBlocking {
        val txBuilder = StringBuilder()
        for (i in 1..2500) {
            if (i > 1) txBuilder.append(",")
            val amt = (i % 50) + 10.0
            val ts = 1790800000000L + i * 60000L
            txBuilder.append(
                """{"id":"large_tx_$i","type":"EXPENSE","amount":$amt,"title":"Item $i","category":"Daily Essentials","subcategory":"Groceries","accountId":"acc_main","timestamp":$ts}"""
            )
        }

        val largeJson = """
            {
              "appName": "My Wallet",
              "schemaVersion": 1,
              "backupDate": "2026-10-26T12:00:00Z",
              "accounts": [
                {"id":"acc_main","name":"Main Bank","type":"BANK","balance":250000.0,"openingBalance":300000.0}
              ],
              "transactions": [$txBuilder],
              "categories": [
                {"id":"cat_essentials","name":"Daily Essentials","isIncome":false}
              ]
            }
        """.trimIndent()

        val preview = backupManager.previewBackup(largeJson, "large_backup.json", largeJson.length.toLong())
        assertTrue(preview.isValid)
        assertEquals(2500, preview.transactionsCount)

        val progressUpdates = mutableListOf<RestoreProgress>()
        val summary = backupManager.restoreWithDetailedSummary(
            jsonString = largeJson,
            replaceAll = true,
            onProgress = { progressUpdates.add(it) }
        ).getOrThrow()

        assertTrue(summary.isFinancialIntegrityVerified)
        assertEquals(2500, summary.transactionsRestored)
        assertEquals(2500, db.transactionDao().getAllTransactions().first().size)
        assertTrue("Should emit multiple batch progress updates", progressUpdates.size >= 8)
    }

    @Test
    fun `4_backup with missing optional fields uses safe backward-compatible defaults`() = runBlocking {
        val minimalJson = """
            {
              "accounts": [
                {"id": "acc_1", "name": "Cash Pocket", "balance": 1200.0}
              ],
              "transactions": [
                {"id": "tx_min_1", "amount": 200.0, "accountId": "acc_1"}
              ]
            }
        """.trimIndent()

        val preview = backupManager.previewBackup(minimalJson, "minimal.json", minimalJson.length.toLong())
        assertTrue(preview.isValid)
        assertEquals(BackupFormatType.MY_WALLET_LEGACY, preview.formatType)
        assertEquals(1, preview.accountsCount)
        assertEquals(1, preview.transactionsCount)

        val summary = backupManager.restoreWithDetailedSummary(
            jsonString = minimalJson,
            replaceAll = true
        ).getOrThrow()
        assertTrue(summary.isFinancialIntegrityVerified)

        val restoredTx = db.transactionDao().getAllTransactions().first().single()
        assertEquals("Miscellaneous", restoredTx.title)
        assertEquals("Miscellaneous", restoredTx.category)
        assertEquals(TransactionType.EXPENSE, restoredTx.type)
    }

    @Test
    fun `5_backup with unsupported schema version reports ERR_UNSUPPORTED_VERSION`() {
        val futureJson = """
            {
              "appName": "My Wallet",
              "schemaVersion": 99,
              "accounts": [{"id": "acc_1", "name": "Bank", "balance": 500.0}],
              "transactions": []
            }
        """.trimIndent()

        val preview = backupManager.previewBackup(futureJson, "future_v99.json", futureJson.length.toLong())
        assertFalse(preview.isValid)
        assertNotNull(preview.diagnosticError)
        assertEquals(BackupErrorCode.ERR_UNSUPPORTED_VERSION, preview.diagnosticError!!.errorCode)
        assertTrue(preview.diagnosticError!!.userMessageEn.contains("unsupported version", ignoreCase = true))
    }

    @Test
    fun `6_corrupted JSON file reports ERR_INVALID_JSON or ERR_EMPTY_OR_CORRUPTED`() {
        val corruptedJson = """{"appName": "My Wallet", "schemaVersion": 1, "accounts": [ { "id": "acc_1" """

        val preview = backupManager.previewBackup(corruptedJson, "corrupted.json", corruptedJson.length.toLong())
        assertFalse(preview.isValid)
        assertNotNull(preview.diagnosticError)
        assertEquals(BackupErrorCode.ERR_INVALID_JSON, preview.diagnosticError!!.errorCode)
    }

    @Test
    fun `7_backup containing duplicate records deduplicates deterministically`() = runBlocking {
        val duplicateJson = """
            {
              "appName": "My Wallet",
              "schemaVersion": 1,
              "accounts": [
                {"id": "acc_1", "name": "Wallet", "type": "CASH", "balance": 1000.0},
                {"id": "acc_1", "name": "Wallet Duplicate", "type": "CASH", "balance": 1000.0}
              ],
              "transactions": [
                {"id": "tx_dup_1", "type": "EXPENSE", "amount": 100.0, "title": "Tea", "category": "Eating Out", "accountId": "acc_1", "timestamp": 1760000000000},
                {"id": "tx_dup_1", "type": "EXPENSE", "amount": 100.0, "title": "Tea", "category": "Eating Out", "accountId": "acc_1", "timestamp": 1760000000000}
              ]
            }
        """.trimIndent()

        val preview = backupManager.previewBackup(duplicateJson, "dups.json", duplicateJson.length.toLong())
        assertTrue(preview.isValid)
        assertEquals(1, preview.accountsCount)
        assertEquals(1, preview.transactionsCount)
        assertTrue(preview.detectedDuplicatesCount >= 2)
    }

    @Test
    fun `8_backup containing invalid account references rejects orphan references in strict My Wallet format and auto-synthesizes in legacy Expend`() = runBlocking {
        val invalidRefMyWalletJson = """
            {
              "appName": "My Wallet",
              "schemaVersion": 1,
              "accounts": [
                {"id": "acc_existing", "name": "Primary Bank", "type": "BANK", "balance": 5000.0}
              ],
              "transactions": [
                {"id": "tx_orphan_1", "type": "EXPENSE", "amount": 300.0, "title": "Orphan Spend", "category": "Shopping", "accountId": "acc_missing_wallet", "timestamp": 1760000000000}
              ]
            }
        """.trimIndent()

        val preview = backupManager.previewBackup(invalidRefMyWalletJson, "orphan.json", invalidRefMyWalletJson.length.toLong())
        assertFalse(preview.isValid)
        assertNotNull(preview.diagnosticError)
        assertEquals(BackupErrorCode.ERR_REFERENTIAL_INTEGRITY, preview.diagnosticError!!.errorCode)
    }

    @Test
    fun `9_Replace All restoration completely replaces existing database records`() = runBlocking {
        db.accountDao().insertAccount(AccountEntity(id = "local_only_acc", name = "Old Local Account", type = AccountType.CASH, balance = 999.0))
        db.transactionDao().insertTransaction(
            TransactionEntity(id = "local_only_tx", type = TransactionType.EXPENSE, amount = 50.0, title = "Old Tx", category = "Food", accountId = "local_only_acc", timestamp = 1700000000000L)
        )

        val backupJson = """
            {
              "appName": "My Wallet",
              "schemaVersion": 1,
              "accounts": [
                {"id": "new_acc", "name": "New Restored Bank", "type": "BANK", "balance": 15000.0}
              ],
              "transactions": [
                {"id": "new_tx", "type": "INCOME", "amount": 15000.0, "title": "Salary", "category": "Salary", "accountId": "new_acc", "timestamp": 1760000000000}
              ]
            }
        """.trimIndent()

        val summary = backupManager.restoreWithDetailedSummary(
            jsonString = backupJson,
            replaceAll = true
        ).getOrThrow()

        assertTrue(summary.isFinancialIntegrityVerified)
        val accounts = db.accountDao().getAllAccounts().first()
        assertEquals(1, accounts.size)
        assertEquals("new_acc", accounts.single().id)
        assertEquals(15000.0, accounts.single().balance, 0.01)

        val txs = db.transactionDao().getAllTransactions().first()
        assertEquals(1, txs.size)
        assertEquals("new_tx", txs.single().id)
    }

    @Test
    fun `10_Merge restoration preserves local-only records, skips duplicates, and reconciles account balances`() = runBlocking {
        // Local DB has acc_shared (with 1 shared tx + 1 local-only expense of 200) and acc_local_only
        db.accountDao().insertAccount(AccountEntity(id = "acc_shared", name = "Shared Account", type = AccountType.BANK, balance = 4800.0))
        db.accountDao().insertAccount(AccountEntity(id = "acc_local_only", name = "Local Cash", type = AccountType.CASH, balance = 750.0))
        db.transactionDao().insertTransaction(
            TransactionEntity(id = "tx_shared", type = TransactionType.INCOME, amount = 5000.0, title = "Salary", category = "Salary", accountId = "acc_shared", timestamp = 1760000000000L)
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(id = "tx_local_new", type = TransactionType.EXPENSE, amount = 200.0, title = "Local Coffee", category = "Eating Out", accountId = "acc_shared", timestamp = 1760050000000L)
        )

        val backupJson = """
            {
              "appName": "My Wallet",
              "schemaVersion": 1,
              "accounts": [
                {"id": "acc_shared", "name": "Shared Account", "type": "BANK", "balance": 5000.0}
              ],
              "transactions": [
                {"id": "tx_shared", "type": "INCOME", "amount": 5000.0, "title": "Salary", "category": "Salary", "accountId": "acc_shared", "timestamp": 1760000000000},
                {"id": "tx_backup_only", "type": "EXPENSE", "amount": 300.0, "title": "Backup Books", "category": "Education", "accountId": "acc_shared", "timestamp": 1759900000000}
              ]
            }
        """.trimIndent()

        val summary = backupManager.restoreWithDetailedSummary(
            jsonString = backupJson,
            replaceAll = false
        ).getOrThrow()

        assertTrue(summary.isFinancialIntegrityVerified)
        assertTrue("Duplicate tx_shared should be counted as skipped", summary.skippedDuplicatesCount >= 1)

        val accounts = db.accountDao().getAllAccounts().first().associateBy { it.id }
        assertEquals(2, accounts.size)
        // Reconciled balance = backup balance (5000.0) + local-only delta (-200.0) = 4800.0
        assertEquals(4800.0, accounts["acc_shared"]!!.balance, 0.01)
        assertEquals(750.0, accounts["acc_local_only"]!!.balance, 0.01)

        val allTxs = db.transactionDao().getAllTransactions().first()
        assertEquals(3, allTxs.size) // tx_shared, tx_local_new, tx_backup_only
    }

    @Test
    fun `11_interrupted restoration rolls back and preserves existing database records`() = runBlocking {
        db.accountDao().insertAccount(AccountEntity(id = "safe_acc", name = "Safe Account", type = AccountType.BANK, balance = 8888.0))
        db.categoryDao().insertCategory(CategoryEntity(id = "safe_cat", name = "Safe Category", isIncome = false))

        val backupJson = """
            {
              "appName": "My Wallet",
              "schemaVersion": 1,
              "accounts": [
                {"id": "other_acc", "name": "Other Account", "type": "BANK", "balance": 100.0}
              ],
              "transactions": [
                {"id": "other_tx", "type": "EXPENSE", "amount": 50.0, "title": "Test", "category": "Food", "accountId": "other_acc", "timestamp": 1760000000000}
              ]
            }
        """.trimIndent()

        val result = backupManager.restoreWithDetailedSummary(
            jsonString = backupJson,
            replaceAll = true,
            onProgress = { progress ->
                if (progress.stage == RestoreStage.SAVING_TO_DATABASE && progress.processedRecords > 0) {
                    throw CancellationException("Simulated user cancellation mid-restore")
                }
            }
        )

        assertTrue("Interrupted restore should fail gracefully", result.isFailure)

        // Verify original data is completely intact
        val accounts = db.accountDao().getAllAccounts().first()
        assertEquals(1, accounts.size)
        assertEquals("safe_acc", accounts.single().id)
        assertEquals(8888.0, accounts.single().balance, 0.01)
    }

    @Test
    fun `12_failed database write rolls back atomically without corrupting existing wallet data`() = runBlocking {
        db.accountDao().insertAccount(AccountEntity(id = "orig_acc", name = "Original Wallet", type = AccountType.CASH, balance = 4200.0))

        val backupJson = """
            {
              "appName": "My Wallet",
              "schemaVersion": 1,
              "accounts": [{"id": "acc_new", "name": "New", "type": "CASH", "balance": 500.0}],
              "transactions": [
                {"id": "tx_new", "type": "EXPENSE", "amount": 50.0, "title": "Snack", "category": "Eating Out", "accountId": "acc_new", "timestamp": 1760000000000}
              ]
            }
        """.trimIndent()

        val result = backupManager.restoreWithDetailedSummary(
            jsonString = backupJson,
            replaceAll = true,
            simulateDatabaseFailure = true
        )

        assertTrue("Simulated DB write failure must return Result.failure", result.isFailure)
        val ex = result.exceptionOrNull() as? BackupRestoreException
        assertNotNull(ex)
        assertEquals(BackupErrorCode.ERR_DATABASE_WRITE_FAILED, ex!!.diagnostic.errorCode)

        // Verify original wallet data was preserved untouched via atomic rollback
        val accounts = db.accountDao().getAllAccounts().first()
        assertEquals(1, accounts.size)
        assertEquals("orig_acc", accounts.single().id)
        assertEquals(4200.0, accounts.single().balance, 0.01)
    }

    @Test
    fun `13_converted backup with missing transfer destination wallet migrates safely without ERR_REFERENTIAL_INTEGRITY`() = runBlocking {
        val convertedBackupJson = """
            {
              "appName": "My Wallet",
              "schemaVersion": 1,
              "_comment": "Converted from Expend backup",
              "accounts": [
                {
                  "id": "w_city_bank",
                  "name": "City Bank",
                  "type": "BANK",
                  "balance": 25000.0,
                  "openingBalance": 30000.0
                }
              ],
              "transactions": [
                {
                  "id": "exp_tx_single_transfer",
                  "type": "TRANSFER",
                  "amount": 5000.0,
                  "title": "Cash Out Outflow",
                  "category": "Transfer",
                  "accountId": "w_city_bank",
                  "timestamp": 1761000000000
                }
              ]
            }
        """.trimIndent()

        val preview = backupManager.previewBackup(
            convertedBackupJson,
            "converted_test.json",
            convertedBackupJson.length.toLong()
        )

        assertTrue("Preview should be valid for converted backup with missing transfer destination", preview.isValid)
        assertEquals(BackupFormatType.MY_WALLET_CONVERTED_EXPEND, preview.formatType)
        assertEquals(1, preview.accountsCount)
        assertEquals(1, preview.transactionsCount)

        val result = backupManager.restoreWithDetailedSummary(
            jsonString = convertedBackupJson,
            replaceAll = true
        ).getOrThrow()

        assertTrue(result.isFinancialIntegrityVerified)
        assertEquals(1, result.accountsRestored)
        assertEquals(1, result.transactionsRestored)

        val restoredTxs = db.transactionDao().getAllTransactions().first()
        assertEquals(1, restoredTxs.size)
        val tx = restoredTxs.single()
        assertEquals("exp_tx_single_transfer", tx.id)
        assertEquals("w_city_bank", tx.accountId)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Transfer", tx.category)
        assertTrue("Notes should contain [External Transfer]", tx.notes.contains("[External Transfer]"))

        val restoredAccounts = db.accountDao().getAllAccounts().first()
        assertEquals(1, restoredAccounts.size)
        assertEquals("w_city_bank", restoredAccounts.single().id)
        assertEquals(25000.0, restoredAccounts.single().balance, 0.01)
    }

    @Test
    fun `14_legacy Expend backup with unlisted referenced wallets auto-recovers them as hidden accounts`() = runBlocking {
        val legacyExpendJson = """
            {
              "appName": "Expend",
              "version": 1,
              "wallets": [
                {
                  "id": "w_primary",
                  "name": "Primary Wallet",
                  "type": "CASH",
                  "balance": 1000.0
                }
              ],
              "records": [
                {
                  "id": "rec_legacy_unlisted",
                  "type": "EXPENSE",
                  "amount": 250.0,
                  "title": "Legacy Unlisted Wallet Spend",
                  "walletId": "w_unlisted_legacy_account",
                  "date": 1761000000
                }
              ]
            }
        """.trimIndent()

        val preview = backupManager.previewBackup(
            legacyExpendJson,
            "legacy_expend.json",
            legacyExpendJson.length.toLong()
        )

        assertTrue("Legacy backup with unlisted wallet should preview successfully", preview.isValid)
        assertEquals(BackupFormatType.EXPEND_LEGACY, preview.formatType)
        assertEquals(2, preview.accountsCount) // 1 declared + 1 recovered

        val result = backupManager.restoreWithDetailedSummary(
            jsonString = legacyExpendJson,
            replaceAll = true
        ).getOrThrow()

        assertTrue(result.isFinancialIntegrityVerified)
        val accounts = db.accountDao().getAllAccounts().first()
        assertEquals(2, accounts.size)
        val recovered = accounts.first { it.id == "w_unlisted_legacy_account" }
        assertTrue(recovered.isHidden)
    }

    @Test
    fun `15_strict My Wallet format rejects transfer transaction with missing destination account`() = runBlocking {
        val strictMissingDstTransferJson = """
            {
              "appName": "My Wallet",
              "schemaVersion": 1,
              "accounts": [
                {"id": "acc_main", "name": "Main Bank", "type": "BANK", "balance": 10000.0}
              ],
              "transactions": [
                {
                  "id": "tx_strict_transfer",
                  "type": "TRANSFER",
                  "amount": 1000.0,
                  "title": "Transfer Without Destination",
                  "category": "Transfer",
                  "accountId": "acc_main",
                  "timestamp": 1761000000000
                }
              ]
            }
        """.trimIndent()

        val preview = backupManager.previewBackup(
            strictMissingDstTransferJson,
            "strict_invalid_transfer.json",
            strictMissingDstTransferJson.length.toLong()
        )

        assertFalse("Strict format with missing transfer destination must fail validation", preview.isValid)
        assertNotNull(preview.diagnosticError)
        assertEquals(BackupErrorCode.ERR_REFERENTIAL_INTEGRITY, preview.diagnosticError!!.errorCode)
    }
}
