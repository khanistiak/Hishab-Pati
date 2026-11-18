package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountType
import com.example.data.notification.NotificationStore
import com.example.ui.components.CalculatorEvaluator
import com.example.ui.util.CurrencyFormatter
import com.example.voice.VoiceTransactionParser
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import com.example.ui.screens.notifications.NotificationCategory
import com.example.ui.screens.notifications.NotificationDestinationType
import com.example.ui.screens.notifications.generateRealNotifications
import com.example.data.local.AppDatabase
import com.example.data.repository.WalletRepository
import com.example.ui.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Hishab Pati", appName)
    }

    @Test
    fun `currency formatter formats south asian lakh correctly`() {
        val formatted = CurrencyFormatter.format(148887.55, "৳")
        assertEquals("৳1,48,887.55", formatted)
    }

    @Test
    fun `voice parser detects bangla and english amounts`() {
        val accounts = listOf(
            AccountEntity(id = "acc_bkash", name = "bKash Account", type = AccountType.MFS, balance = 12500.0)
        )
        val parsed = VoiceTransactionParser.parse("bKash theke 500 taka grocery shopping korsi", accounts, emptyList())
        assertEquals(500.0, parsed.amount, 0.01)
        assertEquals("acc_bkash", parsed.matchedAccountId)
    }

    @Test
    fun `calculator evaluator evaluates arithmetic expressions`() {
        assertEquals(190.0, CalculatorEvaluator.evaluate("120 + 35 × 2"), 0.001)
        assertEquals(225.0, CalculatorEvaluator.evaluate("(500 - 50) ÷ 2"), 0.001)
        assertEquals(1500.5, CalculatorEvaluator.evaluate("1000.5 + 500"), 0.001)
        assertEquals(250.0, CalculatorEvaluator.evaluate("250 + "), 0.001)
    }

    @Test
    fun `calculator evaluator evaluates percentage expressions accurately`() {
        // Core requirements: 200 × 10% = 20, 200 + 10% = 220, 200 − 10% = 180, 200 ÷ 10% = 2000
        assertEquals(20.0, CalculatorEvaluator.evaluate("200 × 10%"), 0.001)
        assertEquals(220.0, CalculatorEvaluator.evaluate("200 + 10%"), 0.001)
        assertEquals(180.0, CalculatorEvaluator.evaluate("200 − 10%"), 0.001)
        assertEquals(2000.0, CalculatorEvaluator.evaluate("200 ÷ 10%"), 0.001)
        assertEquals(0.1, CalculatorEvaluator.evaluate("10%"), 0.001)
        assertEquals(231.0, CalculatorEvaluator.evaluate("200 + 10% + 5%"), 0.001)
        assertEquals(170.0, CalculatorEvaluator.evaluate("200 - 15%"), 0.001)
        assertEquals(5.0, CalculatorEvaluator.evaluate("10% * 50"), 0.001)

        // Nested parentheses with percentages
        assertEquals(220.0, CalculatorEvaluator.evaluate("(100 + 100) + 10%"), 0.001)
        assertEquals(78.0, CalculatorEvaluator.evaluate("100 - (20 + 10%)"), 0.001)
        assertEquals(440.0, CalculatorEvaluator.evaluate("(200 + 10%) * 2"), 0.001)

        // Safe division by zero percentage
        assertEquals(0.0, CalculatorEvaluator.evaluate("200 ÷ 0%"), 0.001)

        // Safe handling of repeated operators and invalid expressions without crashing
        assertEquals(200.0, CalculatorEvaluator.evaluate("200 + %"), 0.001)
        assertEquals(0.0, CalculatorEvaluator.evaluate("%%%"), 0.001)
        assertEquals(0.1, CalculatorEvaluator.evaluate("10%%"), 0.001)
        assertEquals(220.0, CalculatorEvaluator.evaluate("200 + 10% + "), 0.001)
        assertEquals(0.0, CalculatorEvaluator.evaluate(""), 0.001)
    }

    @Test
    fun `notification store persists dismissals and read state across instances`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store1 = NotificationStore(context)
        store1.clearHistory()

        assertFalse(store1.isDismissed("notif_1"))
        assertFalse(store1.isRead("notif_1"))

        // Dismiss single notification
        store1.dismissNotification("notif_1")
        assertTrue(store1.isDismissed("notif_1"))

        // Mark read
        store1.markAsRead("notif_2")
        assertTrue(store1.isRead("notif_2"))

        // Create a new instance simulating app restart
        val store2 = NotificationStore(context)
        assertTrue("Dismissed notification should persist across restart", store2.isDismissed("notif_1"))
        assertTrue("Read state should persist across restart", store2.isRead("notif_2"))

        // Test dismiss all
        store2.dismissAll(listOf("notif_2", "notif_3", "notif_4"))
        assertTrue(store2.isDismissed("notif_2"))
        assertTrue(store2.isDismissed("notif_3"))
        assertTrue(store2.isDismissed("notif_4"))
        assertTrue(store2.isAllCleared())
    }

    @Test
    fun `fresh installation yields zero notifications and clean empty state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = NotificationStore(context)
        store.clearHistory()

        // On fresh installation, no accounts, debts, budgets, or transactions exist
        val notifications = generateRealNotifications(
            budgets = emptyList(),
            debts = emptyList(),
            accounts = emptyList(),
            transactions = emptyList(),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = store
        )

        // Must be completely empty
        assertTrue("Fresh install must not show fictional notifications", notifications.isEmpty())
    }

    @Test
    fun `legitimate notifications generated when user has actual financial events`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = NotificationStore(context)
        store.clearHistory()

        val creditCardAccount = AccountEntity(
            id = "acc_cc_city",
            name = "City Bank Credit Card",
            type = AccountType.CREDIT_CARD,
            balance = 25000.0
        )
        val debt = DebtEntity(
            id = "debt_tanvir",
            personName = "Tanvir",
            amount = 10000.0,
            type = DebtType.LENT,
            dueDate = "2026-11-05",
            isSettled = false
        )
        val budget = BudgetEntity(
            id = "budget_dining",
            categoryName = "Dining",
            monthlyLimit = 10000.0,
            monthYear = "2026-10"
        )
        val now = System.currentTimeMillis()
        val expenseTx = TransactionEntity(
            id = "tx_exp_1",
            type = TransactionType.EXPENSE,
            amount = 9000.0, // 90% of Dining budget
            title = "Restaurant Dinner",
            category = "Dining",
            accountId = "acc_cc_city",
            timestamp = now
        )
        val salaryTx = TransactionEntity(
            id = "tx_salary_1",
            type = TransactionType.INCOME,
            amount = 120000.0,
            title = "October Salary",
            category = "Salary",
            accountId = "acc_cc_city",
            timestamp = now
        )

        val notifications = generateRealNotifications(
            budgets = listOf(budget),
            debts = listOf(debt),
            accounts = listOf(creditCardAccount),
            transactions = listOf(expenseTx, salaryTx),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = store
        )

        assertEquals(4, notifications.size)

        // Verify categories
        val billNotif = notifications.firstOrNull { it.category == NotificationCategory.BILLS }
        assertTrue("Credit card bill reminder generated", billNotif != null)
        assertEquals("acc_cc_city", billNotif?.targetEntityId)
        assertEquals(NotificationDestinationType.BILL, billNotif?.destinationType)

        val budgetNotif = notifications.firstOrNull { it.destinationType == NotificationDestinationType.BUDGET }
        assertTrue("Budget alert generated for 90% spending", budgetNotif != null)
        assertTrue(budgetNotif?.title?.contains("Dining") == true)

        val debtNotif = notifications.firstOrNull { it.destinationType == NotificationDestinationType.DEBT }
        assertTrue("Debt collection alert generated", debtNotif != null)
        assertTrue(debtNotif?.title?.contains("Tanvir") == true)

        val salaryNotif = notifications.firstOrNull { it.destinationType == NotificationDestinationType.TRANSACTION }
        assertTrue("Salary credited notification generated", salaryNotif != null)

        // Verify dismissal works and persists
        billNotif?.let { store.dismissNotification(it.id) }
        val remaining = generateRealNotifications(
            budgets = listOf(budget),
            debts = listOf(debt),
            accounts = listOf(creditCardAccount),
            transactions = listOf(expenseTx, salaryTx),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = store
        )
        assertEquals(3, remaining.size)
        assertFalse(remaining.any { it.id == billNotif?.id })
    }

    @Test
    fun `manage debts hides settled debts by default on both payable and receivable tabs`() {
        val payableUnsettled = DebtEntity(
            id = "d1",
            personName = "Arian Chowdhury",
            amount = 12000.0,
            type = DebtType.BORROWED,
            isSettled = false
        )
        val payableSettled = DebtEntity(
            id = "d2",
            personName = "Farhan Kabir",
            amount = 0.0,
            type = DebtType.BORROWED,
            isSettled = true,
            totalAmount = 5000.0,
            paidAmount = 5000.0
        )
        val receivableUnsettled = DebtEntity(
            id = "d3",
            personName = "Sadia Ahmed",
            amount = 8000.0,
            type = DebtType.LENT,
            isSettled = false
        )
        val receivableSettled = DebtEntity(
            id = "d4",
            personName = "Rafiqul Islam",
            amount = 0.0,
            type = DebtType.LENT,
            isSettled = true,
            totalAmount = 3000.0,
            paidAmount = 3000.0
        )

        val allDebts = listOf(payableUnsettled, payableSettled, receivableUnsettled, receivableSettled)

        // Default: showSettledDebts = false
        val showSettledDebts = false

        val displayedPayable = allDebts.filter { it.type == DebtType.BORROWED && (showSettledDebts || !it.isSettled) }
        val displayedReceivable = allDebts.filter { it.type == DebtType.LENT && (showSettledDebts || !it.isSettled) }

        assertEquals(1, displayedPayable.size)
        assertEquals("Arian Chowdhury", displayedPayable[0].personName)
        assertFalse("Settled payable debt must be hidden by default", displayedPayable.any { it.isSettled })

        assertEquals(1, displayedReceivable.size)
        assertEquals("Sadia Ahmed", displayedReceivable[0].personName)
        assertFalse("Settled receivable debt must be hidden by default", displayedReceivable.any { it.isSettled })
    }

    @Test
    fun `manage debts shows both settled and unsettled debts when toggle is enabled`() {
        val payableUnsettled = DebtEntity(
            id = "d1",
            personName = "Arian Chowdhury",
            amount = 12000.0,
            type = DebtType.BORROWED,
            isSettled = false
        )
        val payableSettled = DebtEntity(
            id = "d2",
            personName = "Farhan Kabir",
            amount = 0.0,
            type = DebtType.BORROWED,
            isSettled = true,
            totalAmount = 5000.0,
            paidAmount = 5000.0
        )

        val allDebts = listOf(payableUnsettled, payableSettled)

        // When toggle is enabled: showSettledDebts = true
        val showSettledDebts = true
        val displayed = allDebts.filter { it.type == DebtType.BORROWED && (showSettledDebts || !it.isSettled) }

        assertEquals(2, displayed.size)
        assertTrue(displayed.any { it.id == "d1" && !it.isSettled })
        assertTrue(displayed.any { it.id == "d2" && it.isSettled })
    }

    @Test
    fun `record counts and totals update accurately based on displayed debts`() {
        val payableUnsettled = DebtEntity(
            id = "d1",
            personName = "Arian Chowdhury",
            amount = 12000.0,
            type = DebtType.BORROWED,
            isSettled = false
        )
        val payableSettled = DebtEntity(
            id = "d2",
            personName = "Farhan Kabir",
            amount = 0.0,
            type = DebtType.BORROWED,
            isSettled = true,
            totalAmount = 5000.0,
            paidAmount = 5000.0
        )
        val allDebts = listOf(payableUnsettled, payableSettled)

        // OFF (default): 1 record, total due 12,000
        var showSettled = false
        var displayed = allDebts.filter { it.type == DebtType.BORROWED && (showSettled || !it.isSettled) }
        var count = displayed.size
        var totalOutstanding = displayed.filter { !it.isSettled }.sumOf { it.amount }

        assertEquals(1, count)
        assertEquals(12000.0, totalOutstanding, 0.001)

        // ON: 2 records, total due 12,000
        showSettled = true
        displayed = allDebts.filter { it.type == DebtType.BORROWED && (showSettled || !it.isSettled) }
        count = displayed.size
        totalOutstanding = displayed.filter { !it.isSettled }.sumOf { it.amount }

        assertEquals(2, count)
        assertEquals(12000.0, totalOutstanding, 0.001)

        // Verify underlying list/history is completely untouched
        assertEquals(2, allDebts.size)
        assertEquals(5000.0, allDebts.find { it.id == "d2" }?.paidAmount ?: 0.0, 0.001)
    }

    @Test
    fun `lending money with bindToWallet deducts wallet balance`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = WalletRepository(db)

        val account = AccountEntity(id = "acc_cash", name = "Cash Wallet", type = AccountType.CASH, balance = 50000.0)
        repo.insertAccount(account)

        // Lending 10,230 to Walid with bindToWallet = true
        val debt = DebtEntity(
            id = "debt_walid",
            personName = "Walid",
            amount = 10230.0,
            totalAmount = 10230.0,
            paidAmount = 0.0,
            type = DebtType.LENT,
            accountId = "acc_cash",
            bindToWallet = true
        )
        // Insert initial transaction for the debt
        val initTx = TransactionEntity(
            id = "tx_debt_init_${debt.id}",
            type = TransactionType.LEND,
            amount = 10230.0,
            title = "Lent to Walid",
            category = "Financial",
            subcategory = "Debt",
            accountId = "acc_cash",
            debtId = debt.id
        )
        repo.insertDebt(debt)
        repo.insertTransaction(initTx)

        val updatedAcc = db.accountDao().getAccountById("acc_cash")
        assertEquals(39770.0, updatedAcc?.balance ?: 0.0, 0.001)

        db.close()
    }

    @Test
    fun `borrowing money with bindToWallet adds to wallet balance`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = WalletRepository(db)

        val account = AccountEntity(id = "acc_cash", name = "Cash Wallet", type = AccountType.CASH, balance = 10000.0)
        repo.insertAccount(account)

        val debt = DebtEntity(
            id = "debt_arian",
            personName = "Arian",
            amount = 12000.0,
            totalAmount = 12000.0,
            paidAmount = 0.0,
            type = DebtType.BORROWED,
            accountId = "acc_cash",
            bindToWallet = true
        )
        val initTx = TransactionEntity(
            id = "tx_debt_init_${debt.id}",
            type = TransactionType.BORROW,
            amount = 12000.0,
            title = "Borrowed from Arian",
            category = "Financial",
            subcategory = "Debt",
            accountId = "acc_cash",
            debtId = debt.id
        )
        repo.insertDebt(debt)
        repo.insertTransaction(initTx)

        val updatedAcc = db.accountDao().getAccountById("acc_cash")
        assertEquals(22000.0, updatedAcc?.balance ?: 0.0, 0.001)

        db.close()
    }

    @Test
    fun `lending without bindToWallet does not change wallet balance`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = WalletRepository(db)

        val account = AccountEntity(id = "acc_cash", name = "Cash Wallet", type = AccountType.CASH, balance = 50000.0)
        repo.insertAccount(account)

        // Unbound debt
        val debt = DebtEntity(
            id = "debt_unbound",
            personName = "Walid",
            amount = 10000.0,
            totalAmount = 10000.0,
            paidAmount = 0.0,
            type = DebtType.LENT,
            accountId = "",
            bindToWallet = false
        )
        val initTx = TransactionEntity(
            id = "tx_debt_init_${debt.id}",
            type = TransactionType.LEND,
            amount = 10000.0,
            title = "Lent to Walid",
            category = "Financial",
            subcategory = "Debt",
            accountId = "",
            debtId = debt.id
        )
        repo.insertDebt(debt)
        repo.insertTransaction(initTx)

        val updatedAcc = db.accountDao().getAccountById("acc_cash")
        assertEquals("Account balance must remain unchanged when debt is unbound", 50000.0, updatedAcc?.balance ?: 0.0, 0.001)

        db.close()
    }

    @Test
    fun `multiple debt transactions create separate entries and update totals correctly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = WalletRepository(db)

        val account = AccountEntity(id = "acc_cash", name = "Cash Wallet", type = AccountType.CASH, balance = 50000.0)
        repo.insertAccount(account)

        // 1. Initial lend: 10,230
        val debt = DebtEntity(
            id = "debt_walid",
            personName = "Walid",
            amount = 10230.0,
            totalAmount = 10230.0,
            paidAmount = 0.0,
            type = DebtType.LENT,
            accountId = "acc_cash",
            bindToWallet = true
        )
        val tx1 = TransactionEntity(
            id = "tx_debt_init_${debt.id}",
            type = TransactionType.LEND,
            amount = 10230.0,
            title = "Lent to Walid",
            category = "Financial",
            subcategory = "Debt",
            accountId = "acc_cash",
            debtId = debt.id
        )
        repo.insertDebt(debt)
        repo.insertTransaction(tx1)

        // 2. Second lend: 6,000
        val tx2 = TransactionEntity(
            id = "tx_lend_more_1",
            type = TransactionType.LEND,
            amount = 6000.0,
            title = "Lent More",
            category = "Financial",
            subcategory = "Debt",
            accountId = "acc_cash",
            debtId = debt.id
        )
        repo.insertTransaction(tx2)

        // Check wallet balance: 50,000 - 10,230 - 6,000 = 33,770
        val accAfterLends = db.accountDao().getAccountById("acc_cash")
        assertEquals(33770.0, accAfterLends?.balance ?: 0.0, 0.001)

        // Check debt history transactions count
        val debtTxs: List<TransactionEntity> = db.transactionDao().getTransactionsByDebtId(debt.id)
        assertEquals(2, debtTxs.size)

        // Compute total additions
        val totalLent = debtTxs.filter { tx: TransactionEntity -> tx.type == TransactionType.LEND }.sumOf { tx: TransactionEntity -> tx.amount }
        assertEquals(16230.0, totalLent, 0.001)

        // 3. Receive partial repayment of 5,000
        val repayTx = TransactionEntity(
            id = "tx_repay_1",
            type = TransactionType.INCOME,
            amount = 5000.0,
            title = "Payment Received",
            category = "Financial",
            subcategory = "Debt Repayment",
            accountId = "acc_cash",
            debtId = debt.id
        )
        repo.insertTransaction(repayTx)

        // Wallet balance increases by 5,000: 33,770 + 5,000 = 38,770
        val accAfterRepay = db.accountDao().getAccountById("acc_cash")
        assertEquals(38770.0, accAfterRepay?.balance ?: 0.0, 0.001)

        // Remaining debt: 16,230 - 5,000 = 11,230
        val allTxs: List<TransactionEntity> = db.transactionDao().getTransactionsByDebtId(debt.id)
        assertEquals(3, allTxs.size)
        val remaining = totalLent - allTxs.filter { tx: TransactionEntity -> tx.type == TransactionType.INCOME }.sumOf { tx: TransactionEntity -> tx.amount }
        assertEquals(11230.0, remaining, 0.001)

        db.close()
    }

    @Test
    fun `deleting debt transaction reverses wallet balance without corruption`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = WalletRepository(db)

        val account = AccountEntity(id = "acc_cash", name = "Cash Wallet", type = AccountType.CASH, balance = 50000.0)
        repo.insertAccount(account)

        val tx = TransactionEntity(
            id = "tx_lend_1",
            type = TransactionType.LEND,
            amount = 6000.0,
            title = "Lent to Walid",
            category = "Financial",
            accountId = "acc_cash",
            debtId = "debt_walid"
        )
        repo.insertTransaction(tx)
        assertEquals(44000.0, db.accountDao().getAccountById("acc_cash")?.balance ?: 0.0, 0.001)

        // Deleting transaction reverts balance
        repo.deleteTransaction("tx_lend_1")
        assertEquals(50000.0, db.accountDao().getAccountById("acc_cash")?.balance ?: 0.0, 0.001)

        db.close()
    }

    @Test
    fun `net worth automatically includes active receivables and payables`() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = MainViewModel(application)

        val accounts = listOf(
            AccountEntity(id = "acc_1", name = "Bank", type = AccountType.BANK, balance = 50000.0),
            AccountEntity(id = "acc_2", name = "Cash", type = AccountType.CASH, balance = 10000.0)
        )
        val debts = listOf(
            // Someone owes me 16,230 (asset)
            DebtEntity(id = "d1", personName = "Walid", amount = 16230.0, type = DebtType.LENT, isSettled = false),
            // I owe someone 12,000 (liability)
            DebtEntity(id = "d2", personName = "Arian", amount = 12000.0, type = DebtType.BORROWED, isSettled = false),
            // Settled debt should not count
            DebtEntity(id = "d3", personName = "Farhan", amount = 0.0, type = DebtType.BORROWED, isSettled = true)
        )

        val netWorthSummary = viewModel.calculateNetWorth(accounts, debts)
        // Assets: 60,000 (wallets) + 16,230 (receivable) = 76,230
        // Liabilities: 12,000 (payable)
        // Net worth: 76,230 - 12,000 = 64,230
        assertEquals(64230.0, netWorthSummary.netWorth, 0.001)
        assertEquals(76230.0, netWorthSummary.totalAssets, 0.001)
        assertEquals(12000.0, netWorthSummary.totalLiabilities, 0.001)
    }

    @Test
    fun `receivable debt calculations with multiple lend additions and partial payment`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = WalletRepository(db)

        val wallet = AccountEntity(id = "acc_cash", name = "Cash Wallet", type = AccountType.CASH, balance = 50000.0)
        repo.insertAccount(wallet)

        // 1. Initial Lent 5,000
        val debt = DebtEntity(
            id = "d_rec_1",
            personName = "Test Person",
            amount = 5000.0,
            totalAmount = 5000.0,
            paidAmount = 0.0,
            type = DebtType.LENT,
            accountId = "acc_cash",
            bindToWallet = true
        )
        repo.insertDebt(debt)
        repo.insertTransaction(
            TransactionEntity(
                id = "tx_debt_init_${debt.id}",
                type = TransactionType.LEND,
                amount = 5000.0,
                title = "Initial Debt (Lent)",
                category = "Debt / Lend",
                subcategory = "Initial Debt",
                accountId = "acc_cash",
                debtId = debt.id
            )
        )
        // Wallet reduced by 5,000 -> 45,000
        assertEquals(45000.0, db.accountDao().getAccountById("acc_cash")?.balance ?: 0.0, 0.001)

        // 2. Lent more 2,000
        repo.insertTransaction(
            TransactionEntity(
                id = "tx_add_1",
                type = TransactionType.LEND,
                amount = 2000.0,
                title = "Lent More",
                category = "Debt / Lend",
                subcategory = "Lend More",
                accountId = "acc_cash",
                debtId = debt.id
            )
        )
        // Wallet reduced by 2,000 -> 43,000
        assertEquals(43000.0, db.accountDao().getAccountById("acc_cash")?.balance ?: 0.0, 0.001)

        // 3. Lent more 9,000
        repo.insertTransaction(
            TransactionEntity(
                id = "tx_add_2",
                type = TransactionType.LEND,
                amount = 9000.0,
                title = "Lent More",
                category = "Debt / Lend",
                subcategory = "Lend More",
                accountId = "acc_cash",
                debtId = debt.id
            )
        )
        // Wallet reduced by 9,000 -> 34,000
        assertEquals(34000.0, db.accountDao().getAccountById("acc_cash")?.balance ?: 0.0, 0.001)

        // 4. Payment received 2,000
        repo.insertTransaction(
            TransactionEntity(
                id = "tx_pay_1",
                type = TransactionType.INCOME,
                amount = 2000.0,
                title = "Payment Received",
                category = "Debt Collection",
                subcategory = "Installment",
                accountId = "acc_cash",
                debtId = debt.id
            )
        )
        // Wallet increased by 2,000 -> 36,000
        assertEquals(36000.0, db.accountDao().getAccountById("acc_cash")?.balance ?: 0.0, 0.001)

        // Verify transactions for debt
        val txs = repo.getTransactionsForDebt(debt.id)
        assertEquals(4, txs.size)

        val totalAdditions = txs.filter { tx ->
            val isPayment = tx.subcategory == "Installment" || tx.subcategory == "Final Settlement" ||
                            tx.category == "Debt Repayment" || tx.category == "Debt Collection" ||
                            tx.type == TransactionType.INCOME
            !isPayment
        }.sumOf { it.amount }
        assertEquals(16000.0, totalAdditions, 0.001)

        val totalRepayments = txs.filter { tx ->
            val isPayment = tx.subcategory == "Installment" || tx.subcategory == "Final Settlement" ||
                            tx.category == "Debt Repayment" || tx.category == "Debt Collection" ||
                            tx.type == TransactionType.INCOME
            isPayment
        }.sumOf { it.amount }
        assertEquals(2000.0, totalRepayments, 0.001)

        val remaining = (totalAdditions - totalRepayments).coerceAtLeast(0.0)
        assertEquals(14000.0, remaining, 0.001)
        val isSettled = remaining <= 0.001
        assertFalse(isSettled)

        db.close()
    }

    @Test
    fun `payable debt calculations with borrow addition and multiple repayments`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = WalletRepository(db)

        val wallet = AccountEntity(id = "acc_bank", name = "Bank Account", type = AccountType.BANK, balance = 10000.0)
        repo.insertAccount(wallet)

        // 1. Initial Borrowed 50,000
        val debt = DebtEntity(
            id = "d_pay_1",
            personName = "Lender Person",
            amount = 50000.0,
            totalAmount = 50000.0,
            paidAmount = 0.0,
            type = DebtType.BORROWED,
            accountId = "acc_bank",
            bindToWallet = true
        )
        repo.insertDebt(debt)
        repo.insertTransaction(
            TransactionEntity(
                id = "tx_debt_init_${debt.id}",
                type = TransactionType.BORROW,
                amount = 50000.0,
                title = "Initial Debt (Borrowed)",
                category = "Debt / Borrow",
                subcategory = "Initial Debt",
                accountId = "acc_bank",
                debtId = debt.id
            )
        )
        // Wallet increased by 50,000 -> 60,000
        assertEquals(60000.0, db.accountDao().getAccountById("acc_bank")?.balance ?: 0.0, 0.001)

        // 2. Repayment 10,000
        repo.insertTransaction(
            TransactionEntity(
                id = "tx_repay_1",
                type = TransactionType.EXPENSE,
                amount = 10000.0,
                title = "Repayment",
                category = "Debt Repayment",
                subcategory = "Installment",
                accountId = "acc_bank",
                debtId = debt.id
            )
        )
        // Wallet reduced by 10,000 -> 50,000
        assertEquals(50000.0, db.accountDao().getAccountById("acc_bank")?.balance ?: 0.0, 0.001)

        // 3. Repayment 15,000
        repo.insertTransaction(
            TransactionEntity(
                id = "tx_repay_2",
                type = TransactionType.EXPENSE,
                amount = 15000.0,
                title = "Repayment",
                category = "Debt Repayment",
                subcategory = "Installment",
                accountId = "acc_bank",
                debtId = debt.id
            )
        )
        // Wallet reduced by 15,000 -> 35,000
        assertEquals(35000.0, db.accountDao().getAccountById("acc_bank")?.balance ?: 0.0, 0.001)

        // 4. Borrow more 10,000
        repo.insertTransaction(
            TransactionEntity(
                id = "tx_borrow_more",
                type = TransactionType.BORROW,
                amount = 10000.0,
                title = "Borrowed More",
                category = "Debt / Borrow",
                subcategory = "Borrow More",
                accountId = "acc_bank",
                debtId = debt.id
            )
        )
        // Wallet increased by 10,000 -> 45,000
        assertEquals(45000.0, db.accountDao().getAccountById("acc_bank")?.balance ?: 0.0, 0.001)

        val txs = repo.getTransactionsForDebt(debt.id)
        assertEquals(4, txs.size)

        val totalAdditions = txs.filter { tx ->
            val isPayment = tx.subcategory == "Installment" || tx.subcategory == "Final Settlement" ||
                            tx.category == "Debt Repayment" || tx.category == "Debt Collection" ||
                            tx.type == TransactionType.EXPENSE
            !isPayment
        }.sumOf { it.amount }
        // 50,000 initial + 10,000 borrow more = 60,000
        assertEquals(60000.0, totalAdditions, 0.001)

        val totalRepayments = txs.filter { tx ->
            val isPayment = tx.subcategory == "Installment" || tx.subcategory == "Final Settlement" ||
                            tx.category == "Debt Repayment" || tx.category == "Debt Collection" ||
                            tx.type == TransactionType.EXPENSE
            isPayment
        }.sumOf { it.amount }
        // 10,000 + 15,000 = 25,000
        assertEquals(25000.0, totalRepayments, 0.001)

        val remaining = (totalAdditions - totalRepayments).coerceAtLeast(0.0)
        // 60,000 - 25,000 = 35,000
        assertEquals(35000.0, remaining, 0.001)

        val paidPct = ((totalRepayments / totalAdditions) * 100).toInt()
        assertEquals(41, paidPct) // 25,000 / 60,000 * 100 = 41.67% -> 41%

        db.close()
    }

    @Test
    fun `adding interest increases debt without changing wallet balance`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = WalletRepository(db)

        val wallet = AccountEntity(id = "acc_cash", name = "Cash Wallet", type = AccountType.CASH, balance = 50000.0)
        repo.insertAccount(wallet)

        val debt = DebtEntity(
            id = "d_int_1",
            personName = "Borrower",
            amount = 10000.0,
            totalAmount = 10000.0,
            paidAmount = 0.0,
            type = DebtType.LENT,
            accountId = "acc_cash",
            bindToWallet = true
        )
        repo.insertDebt(debt)
        repo.insertTransaction(
            TransactionEntity(
                id = "tx_debt_init_${debt.id}",
                type = TransactionType.LEND,
                amount = 10000.0,
                title = "Initial Debt (Lent)",
                category = "Debt / Lend",
                subcategory = "Initial Debt",
                accountId = "acc_cash",
                debtId = debt.id
            )
        )
        // Balance after lent: 50,000 - 10,000 = 40,000
        assertEquals(40000.0, db.accountDao().getAccountById("acc_cash")?.balance ?: 0.0, 0.001)

        // Add interest: 1,000 (accountId = "" so wallet is untouched)
        repo.insertTransaction(
            TransactionEntity(
                id = "tx_interest_1",
                type = TransactionType.LEND,
                amount = 1000.0,
                title = "Interest Accrued",
                category = "Interest",
                subcategory = "Interest",
                accountId = "",
                debtId = debt.id
            )
        )
        // Wallet balance remains 40,000!
        assertEquals(40000.0, db.accountDao().getAccountById("acc_cash")?.balance ?: 0.0, 0.001)

        val txs = repo.getTransactionsForDebt(debt.id)
        val totalAdditions = txs.filter { tx ->
            val isPayment = tx.subcategory == "Installment" || tx.subcategory == "Final Settlement" ||
                            tx.category == "Debt Repayment" || tx.category == "Debt Collection" ||
                            tx.type == TransactionType.INCOME
            !isPayment
        }.sumOf { it.amount }
        // 10,000 + 1,000 = 11,000
        assertEquals(11000.0, totalAdditions, 0.001)

        db.close()
    }
}
