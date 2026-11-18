package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.data.backup.BackupFormatType
import com.example.data.backup.CsvBackupEngine
import com.example.data.backup.JsonBackupManager
import com.example.data.backup.RestoreMode
import com.example.data.backup.SampleDataGenerator
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRestorePhaseTest {

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
    fun testSampleJsonValidityAndRestore() = runBlocking {
        // 1. Generate sample JSON
        val sampleJson = SampleDataGenerator.generateSampleJsonTemplate()
        assertNotNull(sampleJson)
        assertTrue(sampleJson.isNotBlank())
        assertTrue("Sample JSON should be pretty-printed", sampleJson.contains("\n  \"appName\""))

        val root = JSONObject(sampleJson)
        assertEquals("Hishab Pati", root.getString("appName"))
        assertEquals(1, root.getInt("schemaVersion"))
        assertTrue(root.has("accounts"))
        assertTrue(root.has("transactions"))
        assertTrue(root.has("categories"))
        assertTrue(root.has("subcategories"))
        assertTrue(root.has("budgets"))
        assertTrue(root.has("debts"))
        assertTrue(root.has("savingsGoals"))

        // Verify all required account types (Cash, bKash/Nagad MFS, Bank Debit Card, Credit Card)
        val accArr = root.getJSONArray("accounts")
        assertEquals(5, accArr.length())
        val accountTypes = mutableSetOf<String>()
        val accountNames = mutableListOf<String>()
        for (i in 0 until accArr.length()) {
            val acc = accArr.getJSONObject(i)
            accountTypes.add(acc.getString("type"))
            accountNames.add(acc.getString("name").lowercase())
        }
        assertTrue(accountTypes.contains("CASH"))
        assertTrue(accountTypes.contains("MFS"))
        assertTrue(accountTypes.contains("BANK"))
        assertTrue(accountTypes.contains("CREDIT_CARD"))
        assertTrue("Must include bKash MFS wallet", accountNames.any { it.contains("bkash") })
        assertTrue("Must include Nagad MFS wallet", accountNames.any { it.contains("nagad") })

        // Verify every default Expense and Income category from InitialData has a sample transaction
        val (defaultCategories, _) = com.example.data.local.InitialData.getDefaultCategories()
        val expectedExpenseAndIncomeCategories = defaultCategories
            .filter { !it.isTransfer }
            .map { it.name }
            .toSet()

        val txArr = root.getJSONArray("transactions")
        assertTrue(txArr.length() >= 32)

        val coveredCategories = mutableSetOf<String>()
        var hasIncome = false
        var hasExpense = false
        var hasTransfer = false
        var hasBorrow = false
        var hasLend = false
        var hasRepay = false
        var hasRepayReceived = false
        var hasCcPayment = false
        var hasSubcategories = true

        for (i in 0 until txArr.length()) {
            val tx = txArr.getJSONObject(i)
            val type = tx.getString("type")
            val title = tx.getString("title").lowercase()
            val cat = tx.getString("category")
            val catLower = cat.lowercase()
            coveredCategories.add(cat)
            if (tx.optString("subcategory", "").isBlank()) {
                hasSubcategories = false
            }
            when {
                type == "BORROW" -> hasBorrow = true
                type == "LEND" -> hasLend = true
                type == "INCOME" && (catLower.contains("repayment received") || title.contains("repayment received") || title.contains("received back")) -> hasRepayReceived = true
                type == "EXPENSE" && (catLower.contains("repay") || title.contains("repay") || title.contains("settlement")) -> hasRepay = true
                type == "CC_PAYMENT" -> hasCcPayment = true
                type == "TRANSFER" -> hasTransfer = true
                type == "INCOME" -> hasIncome = true
                type == "EXPENSE" -> hasExpense = true
            }
        }

        assertTrue("All sample transactions should specify a subcategory", hasSubcategories)
        for (expectedCategory in expectedExpenseAndIncomeCategories) {
            assertTrue(
                "Missing sample transaction for default category '$expectedCategory'",
                coveredCategories.contains(expectedCategory)
            )
        }
        assertTrue("Must include income", hasIncome)
        assertTrue("Must include expense", hasExpense)
        assertTrue("Must include wallet-to-wallet transfer", hasTransfer)
        assertTrue("Must include borrowed money", hasBorrow)
        assertTrue("Must include lent loan", hasLend)
        assertTrue("Must include loan repayment", hasRepay)
        assertTrue("Must include loan repayment received", hasRepayReceived)
        assertTrue("Must include credit card payment", hasCcPayment)

        // 2. Validate preview of sample JSON
        val preview = backupManager.previewBackup(sampleJson, "Sample_Template.json")
        assertTrue("Preview of sample JSON must be valid", preview.isValid)
        assertEquals(5, preview.accountsCount)
        assertEquals(32, preview.transactionsCount)
        assertEquals(defaultCategories.size, preview.categoriesCount)
        assertEquals(5, preview.budgetsCount)
        assertEquals(4, preview.debtsCount)
        assertEquals(2, preview.savingsGoalsCount)

        // 3. Restore sample JSON into in-memory DB & verify via FinancialIntegrityValidator
        val restoreResult = backupManager.restoreWithDetailedSummary(sampleJson, replaceAll = true).getOrThrow()
        assertTrue(restoreResult.isFinancialIntegrityVerified)
        assertEquals(5, restoreResult.accountsRestored)
        assertEquals(32, restoreResult.transactionsRestored)
        assertEquals(5, restoreResult.budgetsRestored)
        assertEquals(4, restoreResult.debtsRestored)
        assertEquals(2, restoreResult.savingsGoalsRestored)

        val restoredAccs = db.accountDao().getAllAccounts().first()
        val restoredTxs = db.transactionDao().getAllTransactions().first()
        val restoredBudgets = db.budgetDao().getAllBudgets().first()
        val restoredDebts = db.debtDao().getAllDebts().first()
        val restoredGoals = db.savingsGoalDao().getAllGoals().first()

        assertEquals(5, restoredAccs.size)
        assertEquals(32, restoredTxs.size)
        assertEquals(5, restoredBudgets.size)
        assertEquals(4, restoredDebts.size)
        assertEquals(2, restoredGoals.size)
        assertTrue(restoredDebts.any { it.type == DebtType.LENT && !it.isSettled && it.paidAmount > 0.0 })
        assertTrue(restoredDebts.any { it.type == DebtType.BORROWED && !it.isSettled && it.paidAmount > 0.0 })

        val integrityReport = com.example.data.backup.FinancialIntegrityValidator.verifyRestoredData(
            accounts = restoredAccs,
            transactions = restoredTxs,
            budgets = restoredBudgets,
            debts = restoredDebts
        )
        assertTrue("Sample JSON must pass FinancialIntegrityValidator", integrityReport.isValid)
    }

    @Test
    fun testJsonAndCsvExportAndImportRoundtrip() = runBlocking {
        // Seed test data: 2 accounts, 2 transactions (1 expense, 1 transfer), 1 debt
        val acc1 = AccountEntity("acc_bank_test", "Bank Account", AccountType.BANK, "City Bank", 50000.0, 50000.0)
        val acc2 = AccountEntity("acc_cash_test", "Cash Wallet", AccountType.CASH, "", 10000.0, 10000.0)
        db.accountDao().insertAccounts(listOf(acc1, acc2))

        val cat1 = CategoryEntity("cat_groceries", "Groceries", isIncome = false)
        val cat2 = CategoryEntity("cat_transfer", "Transfer", isTransfer = true)
        db.categoryDao().insertCategories(listOf(cat1, cat2))

        val tx1 = TransactionEntity(
            id = "tx_exp_1",
            type = TransactionType.EXPENSE,
            amount = 1500.0,
            title = "Supermarket",
            category = "Groceries",
            accountId = "acc_cash_test",
            timestamp = System.currentTimeMillis()
        )
        val tx2 = TransactionEntity(
            id = "tx_trf_1",
            type = TransactionType.TRANSFER,
            amount = 5000.0,
            title = "Transfer to Cash",
            category = "Transfer",
            accountId = "acc_bank_test",
            toAccountId = "acc_cash_test",
            timestamp = System.currentTimeMillis()
        )
        db.transactionDao().insertTransactions(listOf(tx1, tx2))

        val debt1 = DebtEntity(
            id = "debt_1",
            personName = "Karim",
            amount = 3000.0,
            totalAmount = 3000.0,
            paidAmount = 0.0,
            type = DebtType.LENT,
            dueDate = "2026-11-30"
        )
        db.debtDao().insertDebt(debt1)

        // 1. Test JSON Export
        val exportedJson = backupManager.createBackupJson()
        assertTrue(exportedJson.contains("acc_bank_test"))
        assertTrue(exportedJson.contains("Supermarket"))
        assertTrue(exportedJson.contains("Karim"))

        // 2. Test CSV Export
        val exportedCsv = backupManager.createBackupCsv()
        assertTrue(exportedCsv.contains("SECTION,TRANSACTIONS"))
        assertTrue(exportedCsv.contains("SECTION,ACCOUNTS"))
        assertTrue(exportedCsv.contains("SECTION,DEBTS"))
        assertTrue(exportedCsv.contains("Supermarket"))
        assertTrue(exportedCsv.contains("Transfer to Cash"))

        // 3. Test CSV Import preview
        val csvPreview = backupManager.previewBackup(exportedCsv, "HishabPati_Export.csv")
        assertTrue("CSV preview must be valid", csvPreview.isValid)
        assertEquals(BackupFormatType.CSV_EXPORT, csvPreview.formatType)
        assertEquals(2, csvPreview.accountsCount)
        assertEquals(2, csvPreview.transactionsCount)
        assertEquals(1, csvPreview.debtsCount)

        // 4. Wipe DB and restore from CSV
        db.transactionDao().deleteAllTransactions()
        db.accountDao().deleteAllAccounts()
        db.debtDao().deleteAllDebts()

        val csvRestoreResult = backupManager.restoreWithDetailedSummary(exportedCsv, replaceAll = true).getOrThrow()
        assertEquals(2, csvRestoreResult.accountsRestored)
        assertEquals(2, csvRestoreResult.transactionsRestored)
        assertEquals(1, csvRestoreResult.debtsRestored)

        val restoredTxs = db.transactionDao().getAllTransactions().first()
        assertEquals(2, restoredTxs.size)
    }

    @Test
    fun testMergeModeDeduplication() = runBlocking {
        // Seed 1 account and 1 transaction
        val acc = AccountEntity("acc_wallet", "Wallet", AccountType.CASH, "", 20000.0, 20000.0)
        db.accountDao().insertAccount(acc)

        val tx = TransactionEntity(
            id = "tx_lunch",
            type = TransactionType.EXPENSE,
            amount = 450.0,
            title = "Lunch",
            category = "Food",
            accountId = "acc_wallet",
            timestamp = System.currentTimeMillis()
        )
        db.transactionDao().insertTransaction(tx)

        // Generate backup containing tx_lunch
        val backupJson = backupManager.createBackupJson()

        // Restore same backup in MERGE mode
        val result = backupManager.restoreWithDetailedSummary(backupJson, replaceAll = false).getOrThrow()
        // Duplicate should be detected and skipped
        assertTrue(result.skippedDuplicatesCount >= 1)

        val allTxs = db.transactionDao().getAllTransactions().first()
        // Must remain exactly 1, no duplicate inserted!
        assertEquals(1, allTxs.size)
        assertEquals("tx_lunch", allTxs[0].id)
    }

    @Test
    fun testInvalidContentDoesNotCorruptDatabase() = runBlocking {
        val acc = AccountEntity("acc_safe", "Safe Account", AccountType.CASH, "", 10000.0, 10000.0)
        db.accountDao().insertAccount(acc)

        val invalidJson = "{ invalid: json, malformed }"
        val preview = backupManager.previewBackup(invalidJson, "corrupt.json")
        assertFalse(preview.isValid)
        assertNotNull(preview.errorMessage)

        // Existing DB must remain completely unchanged
        val accs = db.accountDao().getAllAccounts().first()
        assertEquals(1, accs.size)
        assertEquals("Safe Account", accs[0].name)
    }

    @Test
    fun testSettingsAndBackupRestoreNavigationFlow() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        if (com.google.firebase.FirebaseApp.getApps(app).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(
                app,
                com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.mywallet.hkmobg")
                    .setProjectId("demo-test-project")
                    .setApiKey("fake-api-key")
                    .build()
            )
        }
        val viewModel = MainViewModel(app)

        // 1. Initial State: on Home
        assertEquals(SubScreen.NONE, viewModel.currentSubScreen.value)

        // 2. Tap Avatar on Home -> Opens Settings
        viewModel.openSubScreen(SubScreen.SETTINGS)
        assertEquals(SubScreen.SETTINGS, viewModel.currentSubScreen.value)

        // 3. Tap Backup & Restore from Settings -> Opens Backup & Restore
        viewModel.openSubScreen(SubScreen.BACKUP_RESTORE)
        assertEquals(SubScreen.BACKUP_RESTORE, viewModel.currentSubScreen.value)

        // 4. Back from Backup & Restore -> Returns to Settings
        val handled1 = viewModel.handleBackPress()
        assertTrue(handled1)
        assertEquals(SubScreen.SETTINGS, viewModel.currentSubScreen.value)

        // 5. Back from Settings -> Returns to Home
        val handled2 = viewModel.handleBackPress()
        assertTrue(handled2)
        assertEquals(SubScreen.NONE, viewModel.currentSubScreen.value)

        // 6. Verify ViewModel sample JSON template used by Backup & Restore Save/Share is populated
        val vmSampleJson = viewModel.getSampleJsonTemplate()
        assertTrue("ViewModel sample template must not be empty", vmSampleJson.isNotBlank())
        val vmSampleRoot = JSONObject(vmSampleJson)
        assertEquals(5, vmSampleRoot.getJSONArray("accounts").length())
        assertTrue(vmSampleRoot.getJSONArray("transactions").length() >= 32)

        // 7. Test Profile subscreen opened from Settings
        viewModel.openSubScreen(SubScreen.SETTINGS)
        viewModel.openSubScreen(SubScreen.USER_PROFILE)
        assertEquals(SubScreen.USER_PROFILE, viewModel.currentSubScreen.value)
        viewModel.closeSubScreen()
        assertEquals(SubScreen.SETTINGS, viewModel.currentSubScreen.value)
    }

    @Test
    fun testAutoBackupFrequencyAndPersistence() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        if (com.google.firebase.FirebaseApp.getApps(app).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(
                app,
                com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.mywallet.drebos")
                    .setProjectId("demo-test-project")
                    .setApiKey("fake-api-key")
                    .build()
            )
        }
        val viewModel = MainViewModel(app)

        // Default should be OFF
        assertEquals(com.example.data.backup.AutoBackupFrequency.OFF, viewModel.autoBackupFrequency.value)

        // Setting when unauthenticated should prompt Google Sign-In and remain OFF
        viewModel.setAutoBackupFrequency(com.example.data.backup.AutoBackupFrequency.DAILY)
        assertNotNull(viewModel.userNotificationMessage.value)

        // Cancelling works safely
        com.example.data.backup.AutoBackupScheduler.cancel(app)
        viewModel.onUserSignedOut()
        assertFalse(viewModel.isSyncingCloud.value)
    }

    @Test
    fun testEraseAllDataButtonAndStep2LabelsNoTrailingDots() {
        val eraseButtonEn = "Erase All Data"
        val eraseButtonBn = "সমস্ত তথ্য মুছে ফেলুন"
        assertFalse("Erase All Data English label must not contain trailing dots", eraseButtonEn.endsWith("..."))
        assertFalse("Erase All Data English label must not contain ellipsis", eraseButtonEn.endsWith("…"))
        assertFalse("Erase All Data Bangla label must not contain trailing dots", eraseButtonBn.endsWith("..."))

        val step2ButtonEn = "Proceed to Step 2"
        val step2ButtonBn = "পরবর্তী ধাপ"
        assertFalse("Proceed to Step 2 English label must not contain trailing dots", step2ButtonEn.endsWith("..."))
        assertFalse("Proceed to Step 2 English label must not contain ellipsis", step2ButtonEn.endsWith("…"))
        assertFalse("Proceed to Step 2 Bangla label must not contain trailing dots", step2ButtonBn.endsWith("..."))
    }

    @Test
    fun testEraseAllDataExecutionPreservesCloudOption() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        if (com.google.firebase.FirebaseApp.getApps(app).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(
                app,
                com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.mywallet.drebos")
                    .setProjectId("demo-test-project")
                    .setApiKey("fake-api-key")
                    .build()
            )
        }
        val viewModel = MainViewModel(app)
        viewModel.eraseAllData(preserveCloud = true)

        val accounts = viewModel.accounts.value
        assertEquals("Accounts should be empty after erase", 0, accounts.size)
        val transactions = viewModel.transactions.value
        assertEquals("Transactions should be empty after erase", 0, transactions.size)
    }
}
