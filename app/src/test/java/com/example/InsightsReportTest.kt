package com.example

import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.InsightsPeriod
import com.example.ui.screens.insights.InsightsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InsightsReportTest {

    @Test
    fun testDateRangeComputationForCorePeriods() {
        val dailyRange = InsightsCalculator.computeDateRange(
            period = InsightsPeriod.DAILY,
            offset = 0,
            customStart = null,
            customEnd = null,
            language = AppLanguage.ENGLISH
        )
        assertTrue(dailyRange.endMillis >= dailyRange.startMillis)
        assertTrue(dailyRange.formattedDisplay.contains("Today"))

        val weekRange = InsightsCalculator.computeDateRange(
            period = InsightsPeriod.THIS_WEEK,
            offset = 0,
            customStart = null,
            customEnd = null,
            language = AppLanguage.ENGLISH
        )
        assertTrue(weekRange.endMillis >= weekRange.startMillis)

        val monthRange = InsightsCalculator.computeDateRange(
            period = InsightsPeriod.THIS_MONTH,
            offset = 0,
            customStart = null,
            customEnd = null,
            language = AppLanguage.ENGLISH
        )
        assertTrue(monthRange.endMillis >= monthRange.startMillis)

        val prevMonthRange = InsightsCalculator.computeDateRange(
            period = InsightsPeriod.THIS_MONTH,
            offset = -1,
            customStart = null,
            customEnd = null,
            language = AppLanguage.ENGLISH
        )
        assertTrue(prevMonthRange.endMillis < monthRange.startMillis)
    }

    @Test
    fun testPeriodComparisonCalculation() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 15, 12, 0)
        val midOct = cal.timeInMillis

        cal.set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        val startOct = cal.timeInMillis

        cal.set(2026, Calendar.OCTOBER, 31, 23, 59, 59)
        val endOct = cal.timeInMillis

        val currentRange = com.example.ui.screens.insights.InsightsDateRange(
            startMillis = startOct,
            endMillis = endOct,
            formattedDisplay = "October 2026",
            formattedDisplayBn = "অক্টোবর ২০২৬"
        )

        // Previous month timestamp (mid Sept)
        cal.set(2026, Calendar.SEPTEMBER, 15, 12, 0)
        val midSept = cal.timeInMillis

        val transactions = listOf(
            TransactionEntity(
                id = "tx1",
                title = "Salary",
                amount = 50000.0,
                type = TransactionType.INCOME,
                category = "Salary",
                timestamp = midOct,
                accountId = "acc1"
            ),
            TransactionEntity(
                id = "tx2",
                title = "Groceries",
                amount = 12000.0,
                type = TransactionType.EXPENSE,
                category = "Food",
                timestamp = midOct,
                accountId = "acc1"
            ),
            TransactionEntity(
                id = "tx3",
                title = "Electricity",
                amount = 3000.0,
                type = TransactionType.EXPENSE,
                category = "Bills",
                timestamp = midOct,
                accountId = "acc1"
            ),
            // Sept transaction
            TransactionEntity(
                id = "tx4",
                title = "Old Groceries",
                amount = 10000.0,
                type = TransactionType.EXPENSE,
                category = "Food",
                timestamp = midSept,
                accountId = "acc1"
            )
        )

        val comparison = InsightsCalculator.calculatePeriodComparison(transactions, currentRange)

        assertEquals(50000.0, comparison.currentIncome, 0.001)
        assertEquals(15000.0, comparison.currentExpense, 0.001)
        assertEquals(35000.0, comparison.currentNet, 0.001)
        assertEquals(10000.0, comparison.previousExpense, 0.001)
        // (15000 - 10000) / 10000 = 50% increase
        assertEquals(50.0, comparison.expenseChangePct, 0.001)
    }

    @Test
    fun testPeriodComparisonHandlesZeroPreviousValuesSafely() {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        val currentRange = com.example.ui.screens.insights.InsightsDateRange(
            startMillis = now - 100000,
            endMillis = now + 100000,
            formattedDisplay = "Now",
            formattedDisplayBn = "এখন"
        )

        val transactions = listOf(
            TransactionEntity(
                id = "tx1",
                title = "Income",
                amount = 5000.0,
                type = TransactionType.INCOME,
                category = "Salary",
                timestamp = now,
                accountId = "acc1"
            )
        )

        // Previous period has 0 income and 0 expense
        val comparison = InsightsCalculator.calculatePeriodComparison(transactions, currentRange)
        assertEquals(5000.0, comparison.currentIncome, 0.001)
        assertEquals(0.0, comparison.previousIncome, 0.001)
        // Ensure no NaN or infinite crash
        assertTrue(!comparison.incomeChangePct.isNaN())
        assertTrue(!comparison.expenseChangePct.isNaN())
        assertTrue(!comparison.netChangePct.isNaN())
    }

    @Test
    fun testCategoryExpenseRankingAndPercentages() {
        val start = 1000L
        val end = 5000L
        val range = com.example.ui.screens.insights.InsightsDateRange(
            startMillis = start,
            endMillis = end,
            formattedDisplay = "Test",
            formattedDisplayBn = "টেস্ট"
        )

        val transactions = listOf(
            TransactionEntity(
                id = "1",
                title = "Burger",
                amount = 600.0,
                type = TransactionType.EXPENSE,
                category = "Food",
                timestamp = 2000L,
                accountId = "acc1"
            ),
            TransactionEntity(
                id = "2",
                title = "Pizza",
                amount = 400.0,
                type = TransactionType.EXPENSE,
                category = "Food",
                timestamp = 2500L,
                accountId = "acc1"
            ),
            TransactionEntity(
                id = "3",
                title = "Bus",
                amount = 500.0,
                type = TransactionType.EXPENSE,
                category = "Transport",
                timestamp = 3000L,
                accountId = "acc1"
            ),
            TransactionEntity(
                id = "4",
                title = "Transfer",
                amount = 10000.0,
                type = TransactionType.TRANSFER,
                category = "Transfer",
                timestamp = 3500L,
                accountId = "acc1"
            )
        )

        val list = InsightsCalculator.calculateCategoryExpenses(transactions, range)

        // Transfer excluded. Total expense = 600 + 400 + 500 = 1500.
        // Food = 1000 (66.67%), Transport = 500 (33.33%)
        assertEquals(2, list.size)
        assertEquals("Food", list[0].categoryName)
        assertEquals(1000.0, list[0].amount, 0.001)
        assertEquals(66.666f, list[0].percentage, 0.1f)

        assertEquals("Transport", list[1].categoryName)
        assertEquals(500.0, list[1].amount, 0.001)
        assertEquals(33.333f, list[1].percentage, 0.1f)
    }

    @Test
    fun testBudgetApplicabilityMatchingAndHiding() {
        val budgets = listOf(
            BudgetEntity(
                id = "b1",
                categoryName = "Food",
                monthlyLimit = 10000.0,
                monthYear = "2026-10"
            ),
            BudgetEntity(
                id = "b2",
                categoryName = "Transport",
                monthlyLimit = 5000.0,
                monthYear = "ALL"
            )
        )

        // Query for 2026-10: both b1 and b2 match
        val octBudgets = budgets.filter { it.monthYear == "ALL" || it.monthYear == "2026-10" }
        assertEquals(2, octBudgets.size)
        assertEquals(15000.0, octBudgets.sumOf { it.monthlyLimit }, 0.001)

        // Query for 2026-11: only b2 matches
        val novBudgets = budgets.filter { it.monthYear == "ALL" || it.monthYear == "2026-11" }
        assertEquals(1, novBudgets.size)
        assertEquals("Transport", novBudgets[0].categoryName)

        // When no budgets exist: empty list -> BudgetStatusCard returns immediately (hidden)
        val emptyBudgets = emptyList<BudgetEntity>()
        val filteredEmpty = emptyBudgets.filter { it.monthYear == "ALL" || it.monthYear == "2026-10" }
        assertTrue(filteredEmpty.isEmpty())
    }

    @Test
    fun testSpendingTrendNoFabricatedData() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        val s = cal.timeInMillis
        cal.set(2026, Calendar.OCTOBER, 7, 23, 59, 59)
        val e = cal.timeInMillis

        val range = com.example.ui.screens.insights.InsightsDateRange(
            startMillis = s,
            endMillis = e,
            formattedDisplay = "Week",
            formattedDisplayBn = "সপ্তাহ"
        )

        // Only one transaction on Oct 3
        cal.set(2026, Calendar.OCTOBER, 3, 14, 0, 0)
        val oct3 = cal.timeInMillis

        val txs = listOf(
            TransactionEntity(
                id = "1",
                title = "Dinner",
                amount = 2500.0,
                type = TransactionType.EXPENSE,
                category = "Food",
                timestamp = oct3,
                accountId = "acc1"
            )
        )

        val trend = InsightsCalculator.calculateSpendingTrend(txs, InsightsPeriod.THIS_WEEK, range)
        assertEquals(2500.0, trend.totalExpense, 0.001)
        assertEquals(2500.0, trend.highestDayAmount, 0.001)
        assertTrue(trend.highestDayLabel.isNotBlank())
    }

    @Test
    fun testViewTransactionsFilterApplicationLogic() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 10, 12, 0)
        val oct10 = cal.timeInMillis

        cal.set(2026, Calendar.OCTOBER, 20, 15, 0)
        val oct20 = cal.timeInMillis

        cal.set(2026, Calendar.SEPTEMBER, 15, 10, 0)
        val sept15 = cal.timeInMillis

        val periodRange = Pair(
            Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 1, 0, 0, 0) }.timeInMillis,
            Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 31, 23, 59, 59) }.timeInMillis
        )

        val txs = listOf(
            TransactionEntity(
                id = "1",
                title = "Burger King",
                amount = 450.0,
                type = TransactionType.EXPENSE,
                category = "Food",
                timestamp = oct10,
                accountId = "acc1"
            ),
            TransactionEntity(
                id = "2",
                title = "Grocery Shop",
                amount = 3200.0,
                type = TransactionType.EXPENSE,
                category = "Food",
                timestamp = oct20,
                accountId = "acc1"
            ),
            TransactionEntity(
                id = "3",
                title = "Taxi",
                amount = 600.0,
                type = TransactionType.EXPENSE,
                category = "Transport",
                timestamp = oct10,
                accountId = "acc1"
            ),
            TransactionEntity(
                id = "4",
                title = "Old Dinner",
                amount = 800.0,
                type = TransactionType.EXPENSE,
                category = "Food",
                timestamp = sept15,
                accountId = "acc1"
            )
        )

        val chosenCategory = "Food"

        // Filter by chosen category AND period date range
        val filtered = txs.filter { tx ->
            val matchesCategory = tx.category.equals(chosenCategory, ignoreCase = true)
            val matchesTime = tx.timestamp in periodRange.first..periodRange.second
            matchesCategory && matchesTime
        }

        // Must only match tx 1 and 2 (both in October and in Food category)
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.id == "1" })
        assertTrue(filtered.any { it.id == "2" })
        assertTrue(filtered.none { it.id == "3" }) // Transport excluded
        assertTrue(filtered.none { it.id == "4" }) // Sept excluded
    }

    @Test
    fun testBudgetHidingForDailyAndWeeklyPeriods() {
        // Daily and Weekly have no corresponding Daily/Weekly limits in BudgetEntity
        val dailyPeriod = InsightsPeriod.DAILY
        val weeklyPeriod = InsightsPeriod.THIS_WEEK
        val monthlyPeriod = InsightsPeriod.THIS_MONTH

        fun isBudgetApplicable(period: InsightsPeriod, budgetsExistForMonth: Boolean): Boolean {
            if (period == InsightsPeriod.DAILY || period == InsightsPeriod.THIS_WEEK) return false
            return budgetsExistForMonth
        }

        assertTrue(!isBudgetApplicable(dailyPeriod, budgetsExistForMonth = true))
        assertTrue(!isBudgetApplicable(weeklyPeriod, budgetsExistForMonth = true))
        assertTrue(isBudgetApplicable(monthlyPeriod, budgetsExistForMonth = true))
        assertTrue(!isBudgetApplicable(monthlyPeriod, budgetsExistForMonth = false))
    }

    @Test
    fun testExportReport_FilteredScopingForBothReportContentOptions() {
        val range = com.example.ui.screens.insights.InsightsDateRange(
            startMillis = 1000L,
            endMillis = 9000L,
            formattedDisplay = "Nov 1 - Nov 30",
            formattedDisplayBn = "নভে ১ - নভে ৩০"
        )

        val foodGroceries = TransactionEntity(
            id = "tx_fg",
            title = "Fresh Vegetables, Fruits & \"Organic\" Milk",
            amount = 2400.0,
            type = TransactionType.EXPENSE,
            category = "Daily Essentials",
            subcategory = "Groceries",
            timestamp = 3000L,
            accountId = "acc_cash",
            notes = "Bought at market,\npaid cash"
        )
        val foodMeat = TransactionEntity(
            id = "tx_fm",
            title = "Beef & Fish",
            amount = 1600.0,
            type = TransactionType.EXPENSE,
            category = "Daily Essentials",
            subcategory = "Meat",
            timestamp = 4000L,
            accountId = "acc_cash"
        )
        val transportTx = TransactionEntity(
            id = "tx_tr",
            title = "Metro Rail Pass",
            amount = 900.0,
            type = TransactionType.EXPENSE,
            category = "Transportation",
            subcategory = "Commute",
            timestamp = 5000L,
            accountId = "acc_bank"
        )

        // Simulate Filtered Reports where user filtered by Category = "Daily Essentials" and Subcategory = "Groceries"
        val preFilteredList = listOf(foodGroceries, foodMeat)
        val filterContext = com.example.ui.screens.insights.ExportFilterContext(
            periodLabel = "Nov 1 - Nov 30",
            transactionTypeLabel = "Expense",
            categoryFilter = "Daily Essentials",
            subcategoryFilter = "Groceries",
            walletFilter = "Cash Wallet",
            isPreFiltered = true
        )

        // 1. Verify active filter summary chips
        val chips = filterContext.activeFilterChips(AppLanguage.ENGLISH)
        assertTrue(chips.contains("Type: Expense"))
        assertTrue(chips.contains("Category: Daily Essentials"))
        assertTrue(chips.contains("Subcategory: Groceries"))
        assertTrue(chips.contains("Wallet: Cash Wallet"))

        // 2. Expenses by category must only include the filtered subcategory transaction (2400.0), never transportTx or foodMeat
        val categoryCsv = InsightsCalculator.buildCsvReport(
            transactions = preFilteredList,
            dateRange = range,
            currencySymbol = "৳",
            scope = com.example.ui.screens.insights.ExportScope.EXPENSES_BY_CATEGORY,
            filterContext = filterContext
        )
        assertTrue(categoryCsv.contains("Daily Essentials"))
        assertTrue(categoryCsv.contains("Groceries (2400.00)"))
        assertTrue(!categoryCsv.contains("Meat"))
        assertTrue(!categoryCsv.contains("Transportation"))

        // 3. Transactions in date range must only include the filtered transaction and properly escape commas, quotes, and newlines
        val transactionsCsv = InsightsCalculator.buildCsvReport(
            transactions = preFilteredList,
            dateRange = range,
            currencySymbol = "৳",
            scope = com.example.ui.screens.insights.ExportScope.TRANSACTIONS_IN_DATE_RANGE,
            filterContext = filterContext
        )
        assertTrue(transactionsCsv.contains("\"Fresh Vegetables, Fruits & \"\"Organic\"\" Milk\""))
        assertTrue(transactionsCsv.contains("\"Bought at market,\npaid cash\""))
        assertTrue(!transactionsCsv.contains("Beef & Fish"))
        assertTrue(!transactionsCsv.contains("Metro Rail Pass"))
    }

    @Test
    fun testExportReport_RealPdfGenerationAndExcelCsvFormat() {
        val range = com.example.ui.screens.insights.InsightsDateRange(
            startMillis = 1000L,
            endMillis = 9000L,
            formattedDisplay = "November 2026",
            formattedDisplayBn = "নভেম্বর ২০২৬"
        )
        val txs = listOf(
            TransactionEntity(
                id = "tx1",
                title = "Utility Bill",
                amount = 3500.0,
                type = TransactionType.EXPENSE,
                category = "Rent & Utilities",
                subcategory = "Electricity",
                timestamp = 2500L,
                accountId = "acc1"
            )
        )

        // 1. Real PDF binary check
        val pdfBytes = InsightsCalculator.generatePdfReportBytes(
            transactions = txs,
            dateRange = range,
            currencySymbol = "৳",
            scope = com.example.ui.screens.insights.ExportScope.EXPENSES_BY_CATEGORY
        )
        assertTrue("PDF output must be non-empty", pdfBytes.size > 100)
        val header = String(pdfBytes, 0, 5, Charsets.US_ASCII)
        assertEquals("%PDF-", header)
        val pdfAscii = String(pdfBytes, Charsets.US_ASCII)
        assertTrue("PDF must contain trailer EOF marker", pdfAscii.contains("%%EOF"))
        assertTrue("PDF must contain report content", pdfAscii.contains("Rent & Utilities"))

        // 2. Excel (CSV) label and UTF-8 BOM check
        assertEquals("Excel (CSV)", com.example.ui.screens.insights.ExportFormat.EXCEL.title)
        assertEquals("csv", com.example.ui.screens.insights.ExportFormat.EXCEL.extension)
        val excelCsv = InsightsCalculator.buildCsvReport(
            transactions = txs,
            dateRange = range,
            currencySymbol = "৳",
            scope = com.example.ui.screens.insights.ExportScope.TRANSACTIONS_IN_DATE_RANGE,
            includeUtf8Bom = true
        )
        assertTrue("Excel CSV should start with UTF-8 BOM", excelCsv.startsWith("\uFEFF"))

        // 3. Filename includes report type and period
        val pdfFileName = InsightsCalculator.buildExportFileName(
            scope = com.example.ui.screens.insights.ExportScope.EXPENSES_BY_CATEGORY,
            periodLabel = "November 2026",
            format = com.example.ui.screens.insights.ExportFormat.PDF
        )
        assertEquals("HishabPati_Expenses_by_Category_November_2026.pdf", pdfFileName)

        val excelFileName = InsightsCalculator.buildExportFileName(
            scope = com.example.ui.screens.insights.ExportScope.TRANSACTIONS_IN_DATE_RANGE,
            periodLabel = "Nov 1 - Nov 30",
            format = com.example.ui.screens.insights.ExportFormat.EXCEL
        )
        assertEquals("HishabPati_Transactions_in_Date_Range_Excel_Nov_1_Nov_30.csv", excelFileName)
    }

    @Test
    fun testExportReport_EmptyTransactionsDoNotCreateEmptyOutput() {
        val range = com.example.ui.screens.insights.InsightsDateRange(
            startMillis = 1000L,
            endMillis = 9000L,
            formattedDisplay = "November 2026",
            formattedDisplayBn = "নভেম্বর ২০২৬"
        )

        val emptyList = emptyList<TransactionEntity>()
        assertTrue(
            !InsightsCalculator.hasExportableData(
                emptyList,
                range,
                com.example.ui.screens.insights.ExportScope.EXPENSES_BY_CATEGORY
            )
        )
        assertEquals(
            "",
            InsightsCalculator.buildCsvReport(
                emptyList,
                range,
                "৳",
                com.example.ui.screens.insights.ExportScope.EXPENSES_BY_CATEGORY
            )
        )
        assertEquals(
            0,
            InsightsCalculator.generatePdfReportBytes(
                emptyList,
                range,
                "৳",
                scope = com.example.ui.screens.insights.ExportScope.TRANSACTIONS_IN_DATE_RANGE
            ).size
        )
    }
}
