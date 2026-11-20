package com.example.ui

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import com.example.ui.util.FinancialReportAnalytics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FinancialReportAnalyticsTest {

    @Test
    fun testFreshInstall_ProducesEmptyZeroStatesOnAllFourScreens() {
        val emptyTransactions = emptyList<TransactionEntity>()
        val emptyBudgets = emptyList<BudgetEntity>()
        val emptyDebts = emptyList<DebtEntity>()
        val emptyAccounts = emptyList<AccountEntity>()

        val year = 2026
        val month = Calendar.OCTOBER
        val day = 25

        // 1. Monthly Recap Screen
        val recap = FinancialReportAnalytics.computeMonthlyRecap(
            transactions = emptyTransactions,
            budgets = emptyBudgets,
            year = year,
            month = month
        )
        assertFalse("Monthly Recap should report hasData=false on fresh install", recap.hasData)
        assertEquals(0.0, recap.totalIncome, 0.001)
        assertEquals(0.0, recap.totalExpense, 0.001)
        assertEquals(0.0, recap.netSavings, 0.001)
        assertEquals(0, recap.savingsRate)
        assertEquals(0, recap.transactionCount)
        assertEquals("–", recap.grade)
        assertEquals("–", recap.incomeGrade)
        assertEquals("–", recap.expenseGrade)
        assertEquals("–", recap.savingsGrade)
        assertEquals("–", recap.budgetOkText)
        assertEquals("No budget set", recap.budgetOkSubtitle)
        assertNull("Top category must be null on clean install", recap.topCategory)
        assertEquals(0.0, recap.topCategoryAmount, 0.001)
        assertNull("Top merchant must be null on clean install", recap.topMerchant)
        assertEquals(0.0, recap.topMerchantAmount, 0.001)
        assertEquals(1, recap.insights.size)
        assertEquals("No transactions logged yet", recap.insights.first().title)

        // 2. Income Tracker Screen
        val incomeTracker = FinancialReportAnalytics.computeIncomeTracker(
            transactions = emptyTransactions,
            period = "6M"
        )
        assertEquals(0.0, incomeTracker.totalIncome, 0.001)
        assertEquals(0.0, incomeTracker.avgMonthly, 0.001)
        assertNull("Delta percentage should be null when there is no data", incomeTracker.deltaPct)
        assertEquals(6, incomeTracker.monthsCount)
        assertFalse(incomeTracker.hasTrendData)
        assertTrue("Income streams list must be empty on clean install", incomeTracker.incomeStreams.isEmpty())
        assertTrue("Sparkline points must all be zero", incomeTracker.trendPoints.all { it == 0f })

        // 3. Calendar Screen
        val calendar = FinancialReportAnalytics.computeCalendar(
            transactions = emptyTransactions,
            debts = emptyDebts,
            accounts = emptyAccounts,
            year = year,
            month = month,
            selectedDay = day
        )
        assertEquals(0.0, calendar.monthIncome, 0.001)
        assertEquals(0.0, calendar.monthExpense, 0.001)
        assertEquals(0.0, calendar.pendingDebtsTotal, 0.001)
        assertEquals(0.0, calendar.selectedDayIncome, 0.001)
        assertEquals(0.0, calendar.selectedDayExpense, 0.001)
        assertEquals(0.0, calendar.selectedDayNet, 0.001)
        assertTrue("Selected day transactions must be empty on clean install", calendar.selectedDayTransactions.isEmpty())
        // Day dots must have no activity and no hardcoded credit card dates
        for (d in 1..31) {
            val dots = calendar.dayDots[d]!!
            assertFalse(dots.hasExpense)
            assertFalse(dots.hasIncome)
            assertFalse(dots.hasTransfer)
            assertFalse("Card date must NOT be hardcoded for day $d", dots.hasCardDate)
        }

        // 4. Spending Comparison Screen
        val comparison = FinancialReportAnalytics.computeSpendingComparison(
            transactions = emptyTransactions,
            currentYear = year,
            currentMonth = month
        )
        assertEquals(0.0, comparison.currentMonthExpense, 0.001)
        assertEquals(0.0, comparison.previousMonthExpense, 0.001)
        assertEquals(0.0, comparison.expenseDiff, 0.001)
        assertEquals(0.0, comparison.expenseDeltaPct, 0.001)
        assertEquals(0.0, comparison.currentMonthIncome, 0.001)
        assertEquals(0.0, comparison.previousMonthIncome, 0.001)
        assertEquals(0.0, comparison.incomeDiff, 0.001)
        assertEquals(0.0, comparison.incomeDeltaPct, 0.001)
        assertTrue("Comparison categories must be empty on clean install", comparison.categories.isEmpty())
        assertNull("Biggest increase must be null when there are no records", comparison.biggestIncrease)
        assertNull("Biggest decrease must be null when there are no records", comparison.biggestDecrease)
        assertEquals("No spending comparison available. Add expenses to track monthly changes.", comparison.insightText)
    }

    @Test
    fun testPopulatedRecords_ComputeAccurateMetricsAcrossAllFourScreens() {
        val oct2026Cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 12, 0)
        }
        val octTime = oct2026Cal.timeInMillis

        val oct25Cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 25, 14, 0)
        }
        val oct25Time = oct25Cal.timeInMillis

        val sep2026Cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 10, 0)
        }
        val sepTime = sep2026Cal.timeInMillis

        val testTransactions = listOf(
            TransactionEntity(
                id = "tx1",
                title = "Monthly Salary",
                amount = 50000.0,
                type = TransactionType.INCOME,
                category = "Salary",
                accountId = "acc1",
                timestamp = octTime
            ),
            TransactionEntity(
                id = "tx2",
                title = "Grocery at Bazaar",
                amount = 12000.0,
                type = TransactionType.EXPENSE,
                category = "Groceries",
                accountId = "acc1",
                timestamp = octTime
            ),
            TransactionEntity(
                id = "tx3",
                title = "Internet Bill",
                amount = 1500.0,
                type = TransactionType.EXPENSE,
                category = "Utilities",
                accountId = "acc1",
                timestamp = oct25Time
            ),
            TransactionEntity(
                id = "tx4",
                title = "Freelance Project",
                amount = 2000.0,
                type = TransactionType.INCOME,
                category = "Freelance",
                accountId = "acc1",
                timestamp = oct25Time
            ),
            // Sep transaction
            TransactionEntity(
                id = "tx5",
                title = "Old Groceries",
                amount = 10000.0,
                type = TransactionType.EXPENSE,
                category = "Groceries",
                accountId = "acc1",
                timestamp = sepTime
            )
        )

        val testBudgets = listOf(
            BudgetEntity(
                id = "b1",
                categoryName = "Groceries",
                monthlyLimit = 15000.0,
                monthYear = "2026-10"
            )
        )

        val testDebts = listOf(
            DebtEntity(
                id = "d1",
                personName = "Rahim",
                amount = 3000.0,
                totalAmount = 5000.0,
                paidAmount = 2000.0,
                type = DebtType.BORROWED,
                isSettled = false,
                dueDate = "2026-10-25"
            )
        )

        val testAccounts = listOf(
            AccountEntity(
                id = "acc1",
                name = "Bank Account",
                type = AccountType.BANK,
                balance = 38500.0
            ),
            AccountEntity(
                id = "card1",
                name = "Credit Card",
                type = AccountType.CREDIT_CARD,
                billingDate = "25"
            )
        )

        // 1. Monthly Recap for Oct 2026
        val recap = FinancialReportAnalytics.computeMonthlyRecap(
            transactions = testTransactions,
            budgets = testBudgets,
            year = 2026,
            month = Calendar.OCTOBER
        )
        assertTrue(recap.hasData)
        assertEquals(52000.0, recap.totalIncome, 0.001) // 50000 + 2000
        assertEquals(13500.0, recap.totalExpense, 0.001) // 12000 + 1500
        assertEquals(38500.0, recap.netSavings, 0.001)
        val expectedSavingsRate = ((38500.0 / 52000.0) * 100).toInt()
        assertEquals(expectedSavingsRate, recap.savingsRate)
        assertEquals("A", recap.grade)
        assertEquals("Groceries", recap.topCategory)
        assertEquals(12000.0, recap.topCategoryAmount, 0.001)
        assertEquals("100%", recap.budgetOkText)
        assertEquals("1 of 1 in budget", recap.budgetOkSubtitle)

        // 2. Income Tracker
        val incomeTracker = FinancialReportAnalytics.computeIncomeTracker(
            transactions = testTransactions,
            period = "3M",
            referenceTimeMs = oct25Time + 1000
        )
        assertEquals(52000.0, incomeTracker.totalIncome, 0.001)
        assertEquals(2, incomeTracker.incomeStreams.size)
        assertEquals("Salary", incomeTracker.incomeStreams.first().title)
        assertEquals(50000.0, incomeTracker.incomeStreams.first().totalAmount, 0.001)
        assertTrue(incomeTracker.hasTrendData)

        // 3. Calendar
        val calendar = FinancialReportAnalytics.computeCalendar(
            transactions = testTransactions,
            debts = testDebts,
            accounts = testAccounts,
            year = 2026,
            month = Calendar.OCTOBER,
            selectedDay = 25
        )
        assertEquals(52000.0, calendar.monthIncome, 0.001)
        assertEquals(13500.0, calendar.monthExpense, 0.001)
        assertEquals(3000.0, calendar.pendingDebtsTotal, 0.001) // 5000 - 2000
        assertEquals(2000.0, calendar.selectedDayIncome, 0.001)
        assertEquals(1500.0, calendar.selectedDayExpense, 0.001)
        assertEquals(500.0, calendar.selectedDayNet, 0.001)
        assertEquals(2, calendar.selectedDayTransactions.size)
        // Day 25 has credit card billing date from testAccounts
        assertTrue(calendar.dayDots[25]!!.hasCardDate)
        // Day 5 does not
        assertFalse(calendar.dayDots[5]!!.hasCardDate)

        // 4. Spending Comparison (Oct 2026 vs Sep 2026)
        val comparison = FinancialReportAnalytics.computeSpendingComparison(
            transactions = testTransactions,
            currentYear = 2026,
            currentMonth = Calendar.OCTOBER
        )
        assertEquals(13500.0, comparison.currentMonthExpense, 0.001)
        assertEquals(10000.0, comparison.previousMonthExpense, 0.001)
        assertEquals(3500.0, comparison.expenseDiff, 0.001)
        assertEquals(35.0, comparison.expenseDeltaPct, 0.001)
        assertEquals(2, comparison.categories.size)
        val groceriesComp = comparison.categories.first { it.name == "Groceries" }
        assertEquals(12000.0, groceriesComp.currentMonthAmount, 0.001)
        assertEquals(10000.0, groceriesComp.previousMonthAmount, 0.001)
        assertEquals(20.0, groceriesComp.percentageChange, 0.001)
    }

    @Test
    fun `computeIncomeTracker handles Last Year, Custom ranges, and AppStrings has localized keys`() {
        val refCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val prevYearFeb = Calendar.getInstance().apply {
            set(2025, Calendar.FEBRUARY, 10, 12, 0, 0)
        }.timeInMillis
        val prevYearAug = Calendar.getInstance().apply {
            set(2025, Calendar.AUGUST, 15, 12, 0, 0)
        }.timeInMillis
        val twoYearsAgoMay = Calendar.getInstance().apply {
            set(2024, Calendar.MAY, 15, 12, 0, 0)
        }.timeInMillis
        val customStart = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val customEnd = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        val oct12 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 12, 10, 0, 0)
        }.timeInMillis
        val nov5 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 5, 10, 0, 0)
        }.timeInMillis

        val txs = listOf(
            TransactionEntity(
                id = "y24", title = "Bonus", amount = 12000.0, type = TransactionType.INCOME,
                category = "Bonus", accountId = "a1", timestamp = twoYearsAgoMay
            ),
            TransactionEntity(
                id = "y25_1", title = "Salary", amount = 18000.0, type = TransactionType.INCOME,
                category = "Salary", accountId = "a1", timestamp = prevYearFeb
            ),
            TransactionEntity(
                id = "y25_2", title = "Freelance", amount = 6000.0, type = TransactionType.INCOME,
                category = "Freelance", accountId = "a1", timestamp = prevYearAug
            ),
            TransactionEntity(
                id = "c1", title = "Salary", amount = 30000.0, type = TransactionType.INCOME,
                category = "Salary", accountId = "a1", timestamp = oct12
            ),
            TransactionEntity(
                id = "c2", title = "Dividend", amount = 10000.0, type = TransactionType.INCOME,
                category = "Investment", accountId = "a1", timestamp = nov5
            )
        )

        // 1. Last Year (2025): 18000 + 6000 = 24000 -> 2000/mo, 12 months, +100% vs 12000 in 2024
        val lastYearSummary = FinancialReportAnalytics.computeIncomeTracker(
            transactions = txs,
            period = "Last Year",
            referenceTimeMs = refCal.timeInMillis
        )
        assertEquals(24000.0, lastYearSummary.totalIncome, 0.001)
        assertEquals(2000.0, lastYearSummary.avgMonthly, 0.001)
        assertEquals(12, lastYearSummary.monthsCount)
        assertEquals(100.0, lastYearSummary.deltaPct ?: 0.0, 0.001)
        assertEquals(2, lastYearSummary.incomeStreams.size)
        assertTrue(lastYearSummary.hasTrendData)

        // 2. Custom range (Oct 1 - Nov 18, 2026): 30000 + 10000 = 40000 across 49 days
        val customSummary = FinancialReportAnalytics.computeIncomeTracker(
            transactions = txs,
            period = "Custom",
            referenceTimeMs = refCal.timeInMillis,
            customStartMs = customStart,
            customEndMs = customEnd
        )
        assertEquals(40000.0, customSummary.totalIncome, 0.001)
        val expectedCustomAvg = 40000.0 / (49.0 / 30.4375)
        assertEquals(expectedCustomAvg, customSummary.avgMonthly, 1.0)
        assertEquals(2, customSummary.monthsCount)
        assertEquals(listOf("Oct", "Nov"), customSummary.trendMonths)
        assertEquals(2, customSummary.incomeStreams.size)
        assertTrue(customSummary.hasTrendData)

        // 3. AppStrings localized text checks (no raw snake_case keys)
        assertEquals("Total Income", com.example.ui.util.AppStrings.t("total_income", com.example.data.model.AppLanguage.ENGLISH))
        assertEquals("Monthly Average", com.example.ui.util.AppStrings.t("avg_monthly", com.example.data.model.AppLanguage.ENGLISH))
        assertEquals("Income Streams", com.example.ui.util.AppStrings.t("income_streams", com.example.data.model.AppLanguage.ENGLISH))
        assertEquals("মোট আয়", com.example.ui.util.AppStrings.t("total_income", com.example.data.model.AppLanguage.BANGLA))
        assertEquals("মাসিক গড়", com.example.ui.util.AppStrings.t("avg_monthly", com.example.data.model.AppLanguage.BANGLA))
        assertEquals("আয়ের উৎসসমূহ", com.example.ui.util.AppStrings.t("income_streams", com.example.data.model.AppLanguage.BANGLA))
    }
}
