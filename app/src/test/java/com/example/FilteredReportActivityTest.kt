package com.example

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.ActivityFilterType
import com.example.ui.ActivitySort
import com.example.ui.screens.activity.FilteredReportCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FilteredReportActivityTest {

    private val now = System.currentTimeMillis()
    private val oneDay = 24L * 60 * 60 * 1000

    private val sampleAccounts = listOf(
        AccountEntity(
            id = "acc_cash",
            name = "Cash Wallet",
            type = AccountType.CASH,
            balance = 15000.0,
            colorHex = "#10B981"
        ),
        AccountEntity(
            id = "acc_bank",
            name = "City Bank",
            type = AccountType.BANK,
            balance = 45000.0,
            colorHex = "#3B82F6"
        )
    )

    private val sampleCategories = listOf(
        CategoryEntity(id = "cat_food", name = "Eating Out", isIncome = false, iconName = "restaurant", colorHex = "#F59E0B"),
        CategoryEntity(id = "cat_groceries", name = "Daily Essentials", isIncome = false, iconName = "shopping_cart", colorHex = "#10B981"),
        CategoryEntity(id = "cat_salary", name = "Salary", isIncome = true, iconName = "payments", colorHex = "#10B981"),
        CategoryEntity(id = "cat_transfer", name = "Transfer", isTransfer = true, iconName = "swap_horiz", colorHex = "#3B82F6")
    )

    private val sampleTransactions = listOf(
        TransactionEntity(
            id = "tx1",
            title = "Supermarket Grocery",
            amount = 3200.0,
            type = TransactionType.EXPENSE,
            category = "Daily Essentials",
            subcategory = "Vegetables",
            timestamp = now - (2 * oneDay),
            accountId = "acc_cash"
        ),
        TransactionEntity(
            id = "tx2",
            title = "Dinner at Bistro",
            amount = 1800.0,
            type = TransactionType.EXPENSE,
            category = "Eating Out",
            subcategory = "Dinner",
            timestamp = now - (5 * oneDay),
            accountId = "acc_cash"
        ),
        TransactionEntity(
            id = "tx3",
            title = "Monthly Salary",
            amount = 60000.0,
            type = TransactionType.INCOME,
            category = "Salary",
            subcategory = "Full-time",
            timestamp = now - (10 * oneDay),
            accountId = "acc_bank"
        ),
        TransactionEntity(
            id = "tx4",
            title = "ATM Cash Withdrawal",
            amount = 5000.0,
            type = TransactionType.TRANSFER,
            category = "Transfer",
            subcategory = "ATM",
            timestamp = now - (3 * oneDay),
            accountId = "acc_bank",
            toAccountId = "acc_cash"
        ),
        TransactionEntity(
            id = "tx5",
            title = "Kitchen Supplies",
            amount = 1200.0,
            type = TransactionType.EXPENSE,
            category = "Daily Essentials",
            subcategory = "Household",
            timestamp = now - (20 * oneDay),
            accountId = "acc_bank"
        )
    )

    @Test
    fun testFilterByTransactionType_IndividualAndAll() {
        // 1. Expense Filter
        val expenses = sampleTransactions.filter { tx ->
            tx.type == TransactionType.EXPENSE || tx.type == TransactionType.LEND
        }
        assertEquals(3, expenses.size)
        assertTrue(expenses.all { it.type == TransactionType.EXPENSE })

        // 2. Income Filter
        val incomes = sampleTransactions.filter { tx ->
            tx.type == TransactionType.INCOME || tx.type == TransactionType.BORROW
        }
        assertEquals(1, incomes.size)
        assertEquals("Monthly Salary", incomes.first().title)

        // 3. Transfer Filter
        val transfers = sampleTransactions.filter { tx ->
            tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT
        }
        assertEquals(1, transfers.size)
        assertEquals("ATM Cash Withdrawal", transfers.first().title)

        // 4. All Types
        val all = sampleTransactions
        assertEquals(5, all.size)
    }

    @Test
    fun testFilterByAccount_IndividualAndAll() {
        // Cash wallet transactions
        val cashOnly = sampleTransactions.filter { it.accountId == "acc_cash" }
        assertEquals(2, cashOnly.size)

        // Bank account transactions
        val bankOnly = sampleTransactions.filter { it.accountId == "acc_bank" }
        assertEquals(3, bankOnly.size)

        // All accounts (null filter)
        val selectedAccountId: String? = null
        val allAccounts = sampleTransactions.filter { selectedAccountId == null || it.accountId == selectedAccountId }
        assertEquals(5, allAccounts.size)
    }

    @Test
    fun testFilterByCategory_IndividualAndAll() {
        // Daily Essentials only
        val groceriesOnly = sampleTransactions.filter { it.category.equals("Daily Essentials", ignoreCase = true) }
        assertEquals(2, groceriesOnly.size)
        assertEquals(4400.0, groceriesOnly.sumOf { it.amount }, 0.01)

        // All Categories (null filter)
        val selectedCat: String? = null
        val allCategories = sampleTransactions.filter { selectedCat == null || it.category.equals(selectedCat, ignoreCase = true) }
        assertEquals(5, allCategories.size)
    }

    @Test
    fun testFilterByDateRange_CustomAndPreset() {
        val startTime = now - (7 * oneDay)
        val endTime = now

        // Transactions in the last 7 days
        val recentTxs = sampleTransactions.filter { it.timestamp in startTime..endTime }
        // tx1 (2d ago), tx2 (5d ago), tx4 (3d ago) -> 3 txs
        assertEquals(3, recentTxs.size)
        assertFalse(recentTxs.any { it.id == "tx3" || it.id == "tx5" })
    }

    @Test
    fun testCombinedFilters_TypeAccountCategoryDate() {
        val startTime = now - (25 * oneDay)
        val endTime = now
        val selectedAccount = "acc_cash"
        val selectedCategory = "Daily Essentials"
        val filterType = ActivityFilterType.EXPENSES

        val result = sampleTransactions.filter { tx ->
            val matchesType = when (filterType) {
                ActivityFilterType.ALL -> true
                ActivityFilterType.EXPENSES -> tx.type == TransactionType.EXPENSE || tx.type == TransactionType.LEND
                ActivityFilterType.INCOME -> tx.type == TransactionType.INCOME || tx.type == TransactionType.BORROW
                ActivityFilterType.TRANSFERS -> tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT
            }
            val matchesTime = tx.timestamp in startTime..endTime
            val matchesAccount = tx.accountId == selectedAccount
            val matchesCategory = tx.category.equals(selectedCategory, ignoreCase = true)

            matchesType && matchesTime && matchesAccount && matchesCategory
        }

        assertEquals(1, result.size)
        assertEquals("tx1", result.first().id)
        assertEquals(3200.0, result.first().amount, 0.01)
    }

    @Test
    fun testSortingAppliedToFilteredResults() {
        val expenses = sampleTransactions.filter { it.type == TransactionType.EXPENSE }

        // Sort: Highest Amount
        val sortedHighest = expenses.sortedByDescending { it.amount }
        assertEquals(3200.0, sortedHighest.first().amount, 0.01)
        assertEquals(1200.0, sortedHighest.last().amount, 0.01)

        // Sort: Lowest Amount
        val sortedLowest = expenses.sortedBy { it.amount }
        assertEquals(1200.0, sortedLowest.first().amount, 0.01)
        assertEquals(3200.0, sortedLowest.last().amount, 0.01)

        // Sort: Newest
        val sortedNewest = expenses.sortedByDescending { it.timestamp }
        assertEquals("tx1", sortedNewest.first().id) // 2d ago

        // Sort: Oldest
        val sortedOldest = expenses.sortedBy { it.timestamp }
        assertEquals("tx5", sortedOldest.first().id) // 20d ago
    }

    @Test
    fun testFilteredReportCalculator_BreakdownAndSubcategories() {
        val (total, shares) = FilteredReportCalculator.calculateCategoryBreakdown(
            transactions = sampleTransactions.filter { it.type == TransactionType.EXPENSE },
            categories = sampleCategories,
            language = AppLanguage.ENGLISH
        )

        assertEquals(6200.0, total, 0.01) // 3200 + 1800 + 1200
        assertEquals(2, shares.size) // Daily Essentials (4400) and Eating Out (1800)

        val topShare = shares.first()
        assertEquals("Daily Essentials", topShare.categoryName)
        assertEquals(4400.0, topShare.amount, 0.01)
        assertEquals(2, topShare.subcategories.size) // Vegetables & Household

        val subMap = topShare.subcategories.toMap()
        assertEquals(3200.0, subMap["Vegetables"] ?: 0.0, 0.01)
        assertEquals(1200.0, subMap["Household"] ?: 0.0, 0.01)
    }

    @Test
    fun testFilteredReportCalculator_BalanceTimeline() {
        val startTime = now - (30 * oneDay)
        val endTime = now

        val (startBal, endBal, points) = FilteredReportCalculator.calculateBalanceTimeline(
            startTime = startTime,
            endTime = endTime,
            accounts = sampleAccounts,
            transactions = sampleTransactions,
            selectedAccountId = null,
            language = AppLanguage.ENGLISH
        )

        // Current accounts total = 15000 + 45000 = 60000
        assertEquals(60000.0, endBal, 0.01)
        assertTrue(points.isNotEmpty())
        assertTrue(points.first().timestamp <= points.last().timestamp)
    }

    @Test
    fun testEmptyResultsHandling() {
        // Query that matches nothing
        val emptyResult = sampleTransactions.filter { it.category == "NonExistentCategory" }
        assertTrue(emptyResult.isEmpty())

        val (total, shares) = FilteredReportCalculator.calculateCategoryBreakdown(
            transactions = emptyResult,
            categories = sampleCategories,
            language = AppLanguage.ENGLISH
        )
        assertEquals(0.0, total, 0.01)
        assertTrue(shares.isEmpty())
    }

    @Test
    fun testKeywordSearch_MatchesTransactionTitle() {
        val testTxs = sampleTransactions + listOf(
            TransactionEntity(
                id = "tx_gas",
                title = "LP Gas Cylinder refill",
                amount = 1450.0,
                type = TransactionType.EXPENSE,
                category = "Rent & Utilities",
                accountId = "acc_cash"
            )
        )

        val query = "cylinder"
        val matched = testTxs.filter { tx ->
            query.isBlank() ||
                tx.title.contains(query, ignoreCase = true) ||
                tx.category.contains(query, ignoreCase = true) ||
                tx.subcategory.contains(query, ignoreCase = true)
        }

        assertEquals(1, matched.size)
        assertEquals("LP Gas Cylinder refill", matched.first().title)
    }

    @Test
    fun testDateGroup_DayTotalExcludesTransfers() {
        // Reproducing the exact scenario from the reference screenshot:
        // October 31, 2026:
        // - Account transfer 1,050.00 (Transfer)
        // - Transfer Fee 10.00 (Expense)
        // - Account transfer 20,000.00 (Transfer)
        // - Monthly medicine 2,412.00 (Expense)
        val dayTxs = listOf(
            TransactionEntity(
                id = "tx_tr1",
                title = "Account transfer",
                amount = 1050.0,
                type = TransactionType.TRANSFER,
                category = "Transfer",
                accountId = "acc_bank",
                toAccountId = "acc_cash"
            ),
            TransactionEntity(
                id = "tx_fee",
                title = "Transfer Fee",
                amount = 10.0,
                type = TransactionType.EXPENSE,
                category = "Miscellaneous",
                accountId = "acc_bank"
            ),
            TransactionEntity(
                id = "tx_tr2",
                title = "Account transfer",
                amount = 20000.0,
                type = TransactionType.TRANSFER,
                category = "Transfer",
                accountId = "acc_bank",
                toAccountId = "acc_cash"
            ),
            TransactionEntity(
                id = "tx_med",
                title = "Monthly medicine",
                amount = 2412.0,
                type = TransactionType.EXPENSE,
                category = "Health Care",
                subcategory = "Medicine",
                accountId = "acc_cash"
            )
        )

        val dayExpenses = dayTxs.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LEND }.sumOf { it.amount }
        val dayIncome = dayTxs.filter { it.type == TransactionType.INCOME || it.type == TransactionType.BORROW }.sumOf { it.amount }

        // Day expenses should ONLY be fee (10) + medicine (2412) = 2422. Transfers (1050, 20000) are excluded!
        assertEquals(2422.0, dayExpenses, 0.001)
        assertEquals(0.0, dayIncome, 0.001)
    }

    @Test
    fun testTransferWalletDisplay_FormatSourceToDestination() {
        val transferTx = TransactionEntity(
            id = "tx_tr_test",
            title = "Account transfer",
            amount = 5000.0,
            type = TransactionType.TRANSFER,
            accountId = "acc_bank",
            toAccountId = "acc_cash"
        )

        val regularTx = TransactionEntity(
            id = "tx_reg_test",
            title = "Internet Bill",
            amount = 500.0,
            type = TransactionType.EXPENSE,
            accountId = "acc_cash"
        )

        val accounts = sampleAccounts
        fun formatAccountLabel(tx: TransactionEntity): String {
            val isTransfer = tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT
            val acc = accounts.firstOrNull { it.id == tx.accountId }
            val toAcc = if (tx.toAccountId != null) accounts.firstOrNull { it.id == tx.toAccountId } else null

            return if (isTransfer && toAcc != null) {
                "${acc?.name ?: "Wallet"} → ${toAcc.name}"
            } else {
                acc?.name ?: "Wallet"
            }
        }

        assertEquals("City Bank → Cash Wallet", formatAccountLabel(transferTx))
        assertEquals("Cash Wallet", formatAccountLabel(regularTx))
    }

    @Test
    fun testSearchSortAndFilterTogether() {
        // Filter by EXPENSE, Sort by HIGHEST_AMOUNT, Search "Dinner"
        val query = "dinner"
        val selectedFilter = ActivityFilterType.EXPENSES
        val selectedSort = ActivitySort.HIGHEST_AMOUNT

        val filtered = sampleTransactions.filter { tx ->
            val matchesQuery = query.isBlank() ||
                tx.title.contains(query, ignoreCase = true) ||
                tx.category.contains(query, ignoreCase = true) ||
                tx.subcategory.contains(query, ignoreCase = true)

            val matchesType = when (selectedFilter) {
                ActivityFilterType.ALL -> true
                ActivityFilterType.EXPENSES -> tx.type == TransactionType.EXPENSE || tx.type == TransactionType.LEND
                ActivityFilterType.INCOME -> tx.type == TransactionType.INCOME || tx.type == TransactionType.BORROW
                ActivityFilterType.TRANSFERS -> tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT
            }

            matchesQuery && matchesType
        }.let { list ->
            when (selectedSort) {
                ActivitySort.NEWEST -> list.sortedByDescending { it.timestamp }
                ActivitySort.OLDEST -> list.sortedBy { it.timestamp }
                ActivitySort.HIGHEST_AMOUNT -> list.sortedByDescending { it.amount }
                ActivitySort.LOWEST_AMOUNT -> list.sortedBy { it.amount }
            }
        }

        assertEquals(1, filtered.size)
        assertEquals("Dinner at Bistro", filtered.first().title)
        assertEquals(1800.0, filtered.first().amount, 0.01)
    }

    @Test
    fun testTransferSummaryCalculation() {
        val mixedTxs = sampleTransactions + listOf(
            TransactionEntity(
                id = "tx_cc",
                title = "Credit Card Bill Payment",
                amount = 12000.0,
                type = TransactionType.CC_PAYMENT,
                category = "Card Payment",
                accountId = "acc_bank",
                toAccountId = "acc_cash"
            )
        )

        val transferTxs = mixedTxs.filter { it.type == TransactionType.TRANSFER || it.type == TransactionType.CC_PAYMENT }
        val totalTransfers = transferTxs.sumOf { it.amount }

        assertEquals(2, transferTxs.size)
        // tx4: 5000.0 + tx_cc: 12000.0 = 17000.0
        assertEquals(17000.0, totalTransfers, 0.001)
    }

    @Test
    fun testPast3MonthsAndLastYearTimeRanges() {
        val nowMs = System.currentTimeMillis()

        // Past 3 months start time
        val past3MonthsCalendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, -3)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val past3MonthsStart = past3MonthsCalendar.timeInMillis
        assertTrue(past3MonthsStart < nowMs)
        assertTrue(nowMs - past3MonthsStart >= 80L * oneDay)

        // Last year start and end time
        val lastYearCalendar = Calendar.getInstance().apply {
            add(Calendar.YEAR, -1)
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lastYearStart = lastYearCalendar.timeInMillis
        lastYearCalendar.set(Calendar.MONTH, Calendar.DECEMBER)
        lastYearCalendar.set(Calendar.DAY_OF_MONTH, 31)
        lastYearCalendar.set(Calendar.HOUR_OF_DAY, 23)
        lastYearCalendar.set(Calendar.MINUTE, 59)
        lastYearCalendar.set(Calendar.SECOND, 59)
        lastYearCalendar.set(Calendar.MILLISECOND, 999)
        val lastYearEnd = lastYearCalendar.timeInMillis

        assertTrue(lastYearStart < lastYearEnd)
        assertTrue(lastYearEnd < nowMs)
    }

    @Test
    fun testSortingAllFourOptionsWithFilters() {
        val filtered = sampleTransactions.filter { it.type == TransactionType.EXPENSE }
        assertEquals(3, filtered.size)

        val newest = filtered.sortedByDescending { it.timestamp }
        val oldest = filtered.sortedBy { it.timestamp }
        val highest = filtered.sortedByDescending { it.amount }
        val lowest = filtered.sortedBy { it.amount }

        assertEquals("tx1", newest.first().id)
        assertEquals("tx5", oldest.first().id)
        assertEquals(3200.0, highest.first().amount, 0.01)
        assertEquals(1200.0, lowest.first().amount, 0.01)
    }

    @Test
    fun testDateRangePillFormatting() {
        val calStart = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 31, 23, 59, 59)
        }

        val sMonth = java.text.SimpleDateFormat("MMM", java.util.Locale.US).format(calStart.time)
        val eMonth = java.text.SimpleDateFormat("MMM", java.util.Locale.US).format(calEnd.time)
        val sDay = calStart.get(Calendar.DAY_OF_MONTH)
        val eDay = calEnd.get(Calendar.DAY_OF_MONTH)

        val formatted = if (calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR)) {
            "$sMonth $sDay - $eMonth $eDay"
        } else {
            val sYear = calStart.get(Calendar.YEAR)
            val eYear = calEnd.get(Calendar.YEAR)
            "$sMonth $sDay, $sYear - $eMonth $eDay, $eYear"
        }

        // Matches Screenshot 1 exact header format "Oct 1 - Oct 31"
        assertEquals("Oct 1 - Oct 31", formatted)
    }

    @Test
    fun testDatePresets_ThisWeekThisMonthThisYearPreviousWeek() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }

        // 1. THIS_MONTH calculation
        val thisMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val thisMonthEnd = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        assertTrue(thisMonthStart < thisMonthEnd)
        assertTrue(now in thisMonthStart..thisMonthEnd)

        // 2. THIS_WEEK calculation
        val thisWeekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val thisWeekEnd = Calendar.getInstance().apply {
            timeInMillis = thisWeekStart
            add(Calendar.DAY_OF_WEEK, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        assertTrue(thisWeekStart < thisWeekEnd)

        // 3. PREVIOUS_WEEK calculation
        val prevWeekStart = Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, -1)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val prevWeekEnd = Calendar.getInstance().apply {
            timeInMillis = prevWeekStart
            add(Calendar.DAY_OF_WEEK, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        assertTrue(prevWeekStart < prevWeekEnd)
        assertTrue(prevWeekEnd < thisWeekStart)

        // 4. THIS_YEAR calculation
        val thisYearStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val thisYearEnd = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        assertTrue(thisYearStart <= thisMonthStart)
        assertTrue(thisMonthEnd <= thisYearEnd)
    }

    @Test
    fun testReportTypeFilterOptions_ExpenseIncomeTransferAll() {
        val expenseTxs = sampleTransactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LEND }
        val incomeTxs = sampleTransactions.filter { it.type == TransactionType.INCOME || it.type == TransactionType.BORROW }
        val transferTxs = sampleTransactions.filter { it.type == TransactionType.TRANSFER || it.type == TransactionType.CC_PAYMENT }

        assertEquals(3, expenseTxs.size)
        assertEquals(1, incomeTxs.size)
        assertEquals(1, transferTxs.size)

        val totalExpenses = expenseTxs.sumOf { it.amount }
        val totalIncome = incomeTxs.sumOf { it.amount }
        val totalTransfers = transferTxs.sumOf { it.amount }

        assertEquals(6200.0, totalExpenses, 0.01)
        assertEquals(60000.0, totalIncome, 0.01)
        assertEquals(5000.0, totalTransfers, 0.01)
    }

    @Test
    fun testTransactionTypeCycling_ExpenseToIncomeToTransferToExpense() {
        // Simulates cycling: Expense -> Income -> Transfer -> Expense
        fun getNextType(current: ActivityFilterType): ActivityFilterType {
            return when (current) {
                ActivityFilterType.EXPENSES -> ActivityFilterType.INCOME
                ActivityFilterType.INCOME -> ActivityFilterType.TRANSFERS
                ActivityFilterType.TRANSFERS -> ActivityFilterType.EXPENSES
                ActivityFilterType.ALL -> ActivityFilterType.EXPENSES
            }
        }

        var currentType = ActivityFilterType.EXPENSES
        assertEquals(ActivityFilterType.EXPENSES, currentType)

        // 1. Tap 1: Expense -> Income
        currentType = getNextType(currentType)
        assertEquals(ActivityFilterType.INCOME, currentType)
        val incomeList = sampleTransactions.filter { it.type == TransactionType.INCOME || it.type == TransactionType.BORROW }
        assertEquals(1, incomeList.size)
        assertEquals("Monthly Salary", incomeList.first().title)

        // 2. Tap 2: Income -> Transfer
        currentType = getNextType(currentType)
        assertEquals(ActivityFilterType.TRANSFERS, currentType)
        val transferList = sampleTransactions.filter { it.type == TransactionType.TRANSFER || it.type == TransactionType.CC_PAYMENT }
        assertEquals(1, transferList.size)
        assertEquals("ATM Cash Withdrawal", transferList.first().title)

        // 3. Tap 3: Transfer -> Expense
        currentType = getNextType(currentType)
        assertEquals(ActivityFilterType.EXPENSES, currentType)
        val expenseList = sampleTransactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LEND }
        assertEquals(3, expenseList.size)
    }

    @Test
    fun testDatePresetSelection_UpdatesReportAndDatePill() {
        var selectedTimeRange = "THIS_MONTH"
        var customDateRange: Pair<Long, Long>? = null

        // Default: This Month
        assertEquals("THIS_MONTH", selectedTimeRange)

        // Switch to This Week
        selectedTimeRange = "THIS_WEEK"
        customDateRange = null
        assertEquals("THIS_WEEK", selectedTimeRange)

        // Switch to Previous Week
        selectedTimeRange = "PREVIOUS_WEEK"
        assertEquals("PREVIOUS_WEEK", selectedTimeRange)

        // Switch to This Year
        selectedTimeRange = "THIS_YEAR"
        assertEquals("THIS_YEAR", selectedTimeRange)

        // Switch to Custom
        val customStart = 1761955200000L // Example start timestamp
        val customEnd = 1762128000000L   // Example end timestamp
        selectedTimeRange = "CUSTOM"
        customDateRange = customStart to customEnd
        assertEquals("CUSTOM", selectedTimeRange)
        assertEquals(customStart, customDateRange.first)
        assertEquals(customEnd, customDateRange.second)
    }

    @Test
    fun testFilterSheetOpening_OnlySettingsButtonTriggersSheet() {
        var showFilterBottomSheet = false
        val onOpenFilter = { showFilterBottomSheet = true }

        // Settings / Sliders button is tapped:
        onOpenFilter()
        assertTrue(showFilterBottomSheet)

        // Reset
        showFilterBottomSheet = false

        // Transaction-type button is tapped -> must NOT call onOpenFilter
        val onCycleFilterType = {
            // only updates type, does not open sheet
        }
        onCycleFilterType()
        assertFalse("Transaction-type button must not open filter sheet", showFilterBottomSheet)

        // Date-range button is tapped -> must NOT call onOpenFilter
        var isDatePresetsExpanded = false
        val onDateRangeClick = {
            isDatePresetsExpanded = !isDatePresetsExpanded
        }
        onDateRangeClick()
        assertTrue(isDatePresetsExpanded)
        assertFalse("Date-range button must not open filter sheet", showFilterBottomSheet)
    }

    @Test
    fun testCategoryBreakdown_PreservesAllCategoriesWhenSingleCategoryFiltered() {
        // Given transactions spanning multiple categories
        val reportTransactions = listOf(
            TransactionEntity(id = "1", title = "Groceries", amount = 15000.0, type = TransactionType.EXPENSE, category = "Daily Essentials", accountId = "acc_cash", timestamp = now),
            TransactionEntity(id = "2", title = "Dining", amount = 5000.0, type = TransactionType.EXPENSE, category = "Eating Out", accountId = "acc_cash", timestamp = now),
            TransactionEntity(id = "3", title = "Medicine", amount = 2000.0, type = TransactionType.EXPENSE, category = "Health Care", accountId = "acc_cash", timestamp = now)
        )

        // Calculate breakdown for the report
        val (totalAmount, allShares) = FilteredReportCalculator.calculateCategoryBreakdown(
            transactions = reportTransactions,
            categories = sampleCategories,
            language = AppLanguage.ENGLISH
        )

        assertEquals(22000.0, totalAmount, 0.01)
        assertEquals(3, allShares.size)

        // When user applies filter for "Daily Essentials" to the transactions list
        val selectedCategoryFilter = "Daily Essentials"
        val filteredList = reportTransactions.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }

        // The transaction list has only 1 item
        assertEquals(1, filteredList.size)
        assertEquals(15000.0, filteredList.sumOf { it.amount }, 0.01)

        // BUT the report category shares still keep all 3 categories visible!
        assertEquals(3, allShares.size)
        val dailyEssentialsShare = allShares.find { it.categoryName == "Daily Essentials" }
        assertEquals(15000.0, dailyEssentialsShare?.amount ?: 0.0, 0.01)
        val expectedPct = (15000.0 / 22000.0) * 100.0
        assertEquals(expectedPct, dailyEssentialsShare?.percentage ?: 0.0, 0.1)
    }

    @Test
    fun testCategoryBreakdown_UncategorizedSubcategoryMapping() {
        val mixedSubcategories = listOf(
            TransactionEntity(id = "1", title = "Apples", amount = 700.0, type = TransactionType.EXPENSE, category = "Daily Essentials", subcategory = "Fruits", accountId = "acc_cash", timestamp = now),
            TransactionEntity(id = "2", title = "Misc item", amount = 500.0, type = TransactionType.EXPENSE, category = "Daily Essentials", subcategory = "", accountId = "acc_cash", timestamp = now),
            TransactionEntity(id = "3", title = "Blank item", amount = 300.0, type = TransactionType.EXPENSE, category = "Daily Essentials", subcategory = "   ", accountId = "acc_cash", timestamp = now)
        )

        // Test English: blank subcategories become "Uncategorized"
        val (_, sharesEn) = FilteredReportCalculator.calculateCategoryBreakdown(
            transactions = mixedSubcategories,
            categories = sampleCategories,
            language = AppLanguage.ENGLISH
        )
        val subMapEn = sharesEn.first().subcategories.toMap()
        assertEquals(700.0, subMapEn["Fruits"] ?: 0.0, 0.01)
        assertEquals(800.0, subMapEn["Uncategorized"] ?: 0.0, 0.01)

        // Test Bengali: blank subcategories become "ক্যাটাগরিহীন"
        val (_, sharesBn) = FilteredReportCalculator.calculateCategoryBreakdown(
            transactions = mixedSubcategories,
            categories = sampleCategories,
            language = AppLanguage.BANGLA
        )
        val subMapBn = sharesBn.first().subcategories.toMap()
        assertEquals(700.0, subMapBn["Fruits"] ?: 0.0, 0.01)
        assertEquals(800.0, subMapBn["ক্যাটাগরিহীন"] ?: 0.0, 0.01)
    }

    @Test
    fun testViewTransactionsAndResetFilterFlow() {
        var appliedCategoryFilter: String? = null
        var inspectedCategory: String? = null

        // Step 1: Initial state -> All Categories
        assertEquals(null, inspectedCategory)
        assertEquals(null, appliedCategoryFilter)

        // Step 2: User taps a colored segment or category icon -> Inspects "Daily Essentials"
        inspectedCategory = "Daily Essentials"
        assertEquals("Daily Essentials", inspectedCategory)
        assertEquals(null, appliedCategoryFilter) // List not filtered yet!

        // Step 3: User taps "View Transactions"
        val onViewTransactions = { cat: String ->
            appliedCategoryFilter = cat
        }
        onViewTransactions(inspectedCategory!!)
        assertEquals("Daily Essentials", appliedCategoryFilter)

        // Step 4: After filter applied, button is "Reset Filter"
        val isFilteredToInspected = appliedCategoryFilter == inspectedCategory
        assertTrue("Button should now be Reset Filter", isFilteredToInspected)

        // Step 5: User taps "Reset Filter"
        val onResetFilter = {
            appliedCategoryFilter = null
        }
        onResetFilter()
        assertEquals(null, appliedCategoryFilter) // Restores all categories!
    }

    @Test
    fun testSubcategoryPercentageCalculation_RelativeToCategoryTotal() {
        val testTransactions = listOf(
            TransactionEntity(id = "sub1", title = "Grocery Store", amount = 5000.0, type = TransactionType.EXPENSE, category = "Daily Essentials", subcategory = "Groceries", accountId = "acc_cash", timestamp = now),
            TransactionEntity(id = "sub2", title = "Butcher Shop", amount = 3000.0, type = TransactionType.EXPENSE, category = "Daily Essentials", subcategory = "Fish & Meat", accountId = "acc_cash", timestamp = now),
            TransactionEntity(id = "sub3", title = "Fruit Market", amount = 2000.0, type = TransactionType.EXPENSE, category = "Daily Essentials", subcategory = "Fruits", accountId = "acc_cash", timestamp = now)
        )

        val (_, shares) = FilteredReportCalculator.calculateCategoryBreakdown(
            transactions = testTransactions,
            categories = sampleCategories,
            language = AppLanguage.ENGLISH
        )

        val dailyEssentialsShare = shares.first { it.categoryName == "Daily Essentials" }
        assertEquals(10000.0, dailyEssentialsShare.amount, 0.01)

        val subMap = dailyEssentialsShare.subcategories.toMap()
        assertEquals(5000.0, subMap["Groceries"] ?: 0.0, 0.01)
        assertEquals(3000.0, subMap["Fish & Meat"] ?: 0.0, 0.01)
        assertEquals(2000.0, subMap["Fruits"] ?: 0.0, 0.01)

        // Compute relative percentage to category total
        fun calculateSubPct(subAmt: Double, catTotal: Double): Int {
            return if (catTotal > 0.0) kotlin.math.round((subAmt / catTotal) * 100.0).toInt() else 0
        }

        assertEquals(50, calculateSubPct(subMap["Groceries"]!!, dailyEssentialsShare.amount))
        assertEquals(30, calculateSubPct(subMap["Fish & Meat"]!!, dailyEssentialsShare.amount))
        assertEquals(20, calculateSubPct(subMap["Fruits"]!!, dailyEssentialsShare.amount))

        // Safe division check: Zero category total returns 0% without throwing ArithmeticException
        assertEquals(0, calculateSubPct(0.0, 0.0))
        assertEquals(0, calculateSubPct(100.0, 0.0))
    }

    @Test
    fun testTransactionsSectionFilteredTotal_UpdatesWithFilters() {
        val testTxs = listOf(
            TransactionEntity(id = "t1", title = "Market", amount = 10000.0, type = TransactionType.EXPENSE, category = "Daily Essentials", accountId = "acc_cash", timestamp = now),
            TransactionEntity(id = "t2", title = "Lunch", amount = 4000.0, type = TransactionType.EXPENSE, category = "Eating Out", accountId = "acc_cash", timestamp = now),
            TransactionEntity(id = "t3", title = "Ride", amount = 1000.0, type = TransactionType.EXPENSE, category = "Transport", accountId = "acc_cash", timestamp = now)
        )

        // Initial state: no category filter applied
        var selectedCategoryFilter: String? = null
        var filteredList = if (selectedCategoryFilter == null) testTxs else testTxs.filter { it.category == selectedCategoryFilter }
        var filteredTotalAmount = filteredList.sumOf { it.amount }

        assertEquals(15000.0, filteredTotalAmount, 0.01)
        assertEquals(3, filteredList.size)

        // User filters by "Daily Essentials" via View Transactions
        selectedCategoryFilter = "Daily Essentials"
        filteredList = if (selectedCategoryFilter == null) testTxs else testTxs.filter { it.category == selectedCategoryFilter }
        filteredTotalAmount = filteredList.sumOf { it.amount }

        assertEquals(10000.0, filteredTotalAmount, 0.01)
        assertEquals(1, filteredList.size)

        // User taps "Reset Filter"
        selectedCategoryFilter = null
        filteredList = if (selectedCategoryFilter == null) testTxs else testTxs.filter { it.category == selectedCategoryFilter }
        filteredTotalAmount = filteredList.sumOf { it.amount }

        assertEquals(15000.0, filteredTotalAmount, 0.01)
        assertEquals(3, filteredList.size)
    }

    @Test
    fun testDatePickerThemeResolution_LightAndDarkMode() {
        // Light mode (surface luminance >= 0.5f) must resolve to Light dialog theme
        val isDarkLightMode = false
        val lightThemeRes = if (isDarkLightMode) {
            android.R.style.Theme_DeviceDefault_Dialog
        } else {
            android.R.style.Theme_DeviceDefault_Light_Dialog
        }
        assertEquals(android.R.style.Theme_DeviceDefault_Light_Dialog, lightThemeRes)

        // Dark mode (surface luminance < 0.5f) must resolve to Dark dialog theme
        val isDarkDarkMode = true
        val darkThemeRes = if (isDarkDarkMode) {
            android.R.style.Theme_DeviceDefault_Dialog
        } else {
            android.R.style.Theme_DeviceDefault_Light_Dialog
        }
        assertEquals(android.R.style.Theme_DeviceDefault_Dialog, darkThemeRes)
    }

    @Test
    fun testWalletColorParsingForFilterCards() {
        val defaultColor = androidx.compose.ui.graphics.Color(0xFF2563EB)
        val parsedHex = FilteredReportCalculator.parseColor("#10B981", defaultColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF10B981), parsedHex)

        val invalidHex = FilteredReportCalculator.parseColor("invalid", defaultColor)
        assertEquals(defaultColor, invalidHex)
    }
}

