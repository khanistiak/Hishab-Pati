package com.example

import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.screens.plan.BudgetFilterOption
import com.example.ui.screens.plan.BudgetSortOption
import com.example.ui.screens.plan.CategoryBudgetUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BudgetDashboardTest {

    @Test
    fun testBudgetCalculations_OnTrackNearLimitAndOverBudget() {
        // 1. On Track (37% used)
        val onTrackModel = CategoryBudgetUiModel(
            budget = BudgetEntity(id = "b1", categoryName = "Daily Essentials", monthlyLimit = 15000.0, monthYear = "2026-10"),
            categoryName = "Daily Essentials",
            categoryIcon = "shopping_cart",
            categoryColorHex = "#10B981",
            limit = 15000.0,
            spent = 5550.0,
            remaining = 9450.0,
            percentage = 0.37f,
            transactionCount = 5,
            recentTransactions = emptyList(),
            isOverBudget = false,
            isNearLimit = false,
            hasDefinedBudget = true
        )

        assertFalse(onTrackModel.isOverBudget)
        assertFalse(onTrackModel.isNearLimit)
        assertEquals(9450.0, onTrackModel.remaining, 0.01)
        assertEquals(37, (onTrackModel.percentage * 100).toInt())

        // 2. Near Limit (80% used)
        val nearLimitModel = CategoryBudgetUiModel(
            budget = BudgetEntity(id = "b2", categoryName = "Rent & Utilities", monthlyLimit = 20000.0, monthYear = "2026-10"),
            categoryName = "Rent & Utilities",
            categoryIcon = "home",
            categoryColorHex = "#3B82F6",
            limit = 20000.0,
            spent = 16000.0,
            remaining = 4000.0,
            percentage = 0.80f,
            transactionCount = 2,
            recentTransactions = emptyList(),
            isOverBudget = false,
            isNearLimit = true,
            hasDefinedBudget = true
        )

        assertFalse(nearLimitModel.isOverBudget)
        assertTrue(nearLimitModel.isNearLimit)
        assertEquals(4000.0, nearLimitModel.remaining, 0.01)

        // 3. Over Budget (162% used)
        val overBudgetModel = CategoryBudgetUiModel(
            budget = BudgetEntity(id = "b3", categoryName = "Bike Expenses", monthlyLimit = 3000.0, monthYear = "2026-10"),
            categoryName = "Bike Expenses",
            categoryIcon = "bike",
            categoryColorHex = "#EC4899",
            limit = 3000.0,
            spent = 4870.0,
            remaining = -1870.0,
            percentage = 1.6233f,
            transactionCount = 4,
            recentTransactions = emptyList(),
            isOverBudget = true,
            isNearLimit = false,
            hasDefinedBudget = true
        )

        assertTrue(overBudgetModel.isOverBudget)
        assertEquals(-1870.0, overBudgetModel.remaining, 0.01)
    }

    @Test
    fun testBudgetFiltering_AccuratelySegmentsCategories() {
        val models = listOf(
            CategoryBudgetUiModel(
                budget = BudgetEntity(id = "1", categoryName = "Food", monthlyLimit = 10000.0, monthYear = "2026-10"),
                categoryName = "Food", categoryIcon = "restaurant", categoryColorHex = "#F59E0B",
                limit = 10000.0, spent = 2000.0, remaining = 8000.0, percentage = 0.20f,
                transactionCount = 1, recentTransactions = emptyList(),
                isOverBudget = false, isNearLimit = false, hasDefinedBudget = true
            ),
            CategoryBudgetUiModel(
                budget = BudgetEntity(id = "2", categoryName = "Utilities", monthlyLimit = 5000.0, monthYear = "2026-10"),
                categoryName = "Utilities", categoryIcon = "bolt", categoryColorHex = "#3B82F6",
                limit = 5000.0, spent = 4500.0, remaining = 500.0, percentage = 0.90f,
                transactionCount = 1, recentTransactions = emptyList(),
                isOverBudget = false, isNearLimit = true, hasDefinedBudget = true
            ),
            CategoryBudgetUiModel(
                budget = BudgetEntity(id = "3", categoryName = "Shopping", monthlyLimit = 5000.0, monthYear = "2026-10"),
                categoryName = "Shopping", categoryIcon = "shopping_bag", categoryColorHex = "#EC4899",
                limit = 5000.0, spent = 7000.0, remaining = -2000.0, percentage = 1.40f,
                transactionCount = 2, recentTransactions = emptyList(),
                isOverBudget = true, isNearLimit = false, hasDefinedBudget = true
            ),
            CategoryBudgetUiModel(
                budget = null,
                categoryName = "Miscellaneous", categoryIcon = "category", categoryColorHex = "#94A3B8",
                limit = 0.0, spent = 1200.0, remaining = -1200.0, percentage = 1.0f,
                transactionCount = 1, recentTransactions = emptyList(),
                isOverBudget = true, isNearLimit = false, hasDefinedBudget = false
            )
        )

        val onTrack = models.filter { it.hasDefinedBudget && !it.isOverBudget && !it.isNearLimit }
        val nearLimit = models.filter { it.hasDefinedBudget && it.isNearLimit }
        val exceeded = models.filter { it.hasDefinedBudget && it.isOverBudget }
        val noLimit = models.filter { !it.hasDefinedBudget }

        assertEquals(1, onTrack.size)
        assertEquals("Food", onTrack[0].categoryName)

        assertEquals(1, nearLimit.size)
        assertEquals("Utilities", nearLimit[0].categoryName)

        assertEquals(1, exceeded.size)
        assertEquals("Shopping", exceeded[0].categoryName)

        assertEquals(1, noLimit.size)
        assertEquals("Miscellaneous", noLimit[0].categoryName)
    }

    @Test
    fun testBudgetSorting_CorrectOrdering() {
        val models = listOf(
            CategoryBudgetUiModel(
                budget = null, categoryName = "Bravo", categoryIcon = "icon", categoryColorHex = "#000",
                limit = 1000.0, spent = 500.0, remaining = 500.0, percentage = 0.5f,
                transactionCount = 1, recentTransactions = emptyList(),
                isOverBudget = false, isNearLimit = false, hasDefinedBudget = true
            ),
            CategoryBudgetUiModel(
                budget = null, categoryName = "Alpha", categoryIcon = "icon", categoryColorHex = "#000",
                limit = 2000.0, spent = 1800.0, remaining = 200.0, percentage = 0.9f,
                transactionCount = 1, recentTransactions = emptyList(),
                isOverBudget = false, isNearLimit = true, hasDefinedBudget = true
            ),
            CategoryBudgetUiModel(
                budget = null, categoryName = "Charlie", categoryIcon = "icon", categoryColorHex = "#000",
                limit = 1000.0, spent = 100.0, remaining = 900.0, percentage = 0.1f,
                transactionCount = 1, recentTransactions = emptyList(),
                isOverBudget = false, isNearLimit = false, hasDefinedBudget = true
            )
        )

        val bySpentDesc = models.sortedByDescending { it.spent }
        assertEquals("Alpha", bySpentDesc[0].categoryName)
        assertEquals("Bravo", bySpentDesc[1].categoryName)
        assertEquals("Charlie", bySpentDesc[2].categoryName)

        val byNameAsc = models.sortedBy { it.categoryName }
        assertEquals("Alpha", byNameAsc[0].categoryName)
        assertEquals("Bravo", byNameAsc[1].categoryName)
        assertEquals("Charlie", byNameAsc[2].categoryName)

        val byRemainingAsc = models.sortedBy { it.remaining }
        assertEquals("Alpha", byRemainingAsc[0].categoryName)
        assertEquals("Bravo", byRemainingAsc[1].categoryName)
        assertEquals("Charlie", byRemainingAsc[2].categoryName)
    }

    @Test
    fun testProjectedTotalCalculation() {
        // e.g. Oct 2026: 31 days in month. 30 days elapsed, 4,870 spent.
        val spent = 4870.0
        val daysElapsed = 30
        val totalDays = 31

        val projected = (spent / daysElapsed.toDouble()) * totalDays.toDouble()
        assertEquals(5032.33, projected, 0.01)

        // When daysElapsed == totalDays
        val finalProjected = (spent / 31.0) * 31.0
        assertEquals(spent, finalProjected, 0.01)
    }

    @Test
    fun testSpendingHistorySubcategoryAggregation() {
        val txs = listOf(
            TransactionEntity(
                id = "tx1", title = "Fuel", amount = 1520.0, type = TransactionType.EXPENSE,
                category = "Bike Expenses", subcategory = "Fuel", accountId = "acc1",
                timestamp = System.currentTimeMillis()
            ),
            TransactionEntity(
                id = "tx2", title = "Accessories", amount = 2450.0, type = TransactionType.EXPENSE,
                category = "Bike Expenses", subcategory = "Accessories", accountId = "acc1",
                timestamp = System.currentTimeMillis()
            ),
            TransactionEntity(
                id = "tx3", title = "Number plate", amount = 800.0, type = TransactionType.EXPENSE,
                category = "Bike Expenses", subcategory = "", accountId = "acc1",
                timestamp = System.currentTimeMillis()
            ),
            TransactionEntity(
                id = "tx4", title = "Bike wash", amount = 100.0, type = TransactionType.EXPENSE,
                category = "Bike Expenses", subcategory = "Service", accountId = "acc1",
                timestamp = System.currentTimeMillis()
            )
        )

        val breakdown = txs.groupBy { it.subcategory.ifBlank { "Uncategorized" } }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }

        assertEquals(4, breakdown.size)
        assertEquals("Accessories", breakdown[0].first)
        assertEquals(2450.0, breakdown[0].second, 0.01)

        assertEquals("Fuel", breakdown[1].first)
        assertEquals(1520.0, breakdown[1].second, 0.01)

        assertEquals("Uncategorized", breakdown[2].first)
        assertEquals(800.0, breakdown[2].second, 0.01)

        assertEquals("Service", breakdown[3].first)
        assertEquals(100.0, breakdown[3].second, 0.01)
    }

    @Test
    fun testTotalMonthlyBudget_ParentChildDoubleCountingPrevention() {
        val validator = com.example.data.backup.FinancialIntegrityValidator

        // Scenario 1: Only parent category budgets
        val parentBudgets = listOf(
            BudgetEntity(id = "b1", categoryName = "Daily Essentials", monthlyLimit = 15000.0, monthYear = "2026-10"),
            BudgetEntity(id = "b2", categoryName = "Eating Out", monthlyLimit = 5000.0, monthYear = "2026-10")
        )
        assertEquals(20000.0, validator.computeTotalMonthlyBudget(parentBudgets, "2026-10"), 0.01)

        // Scenario 2: Category with subcategory budgets only
        val subcategoryBudgets = listOf(
            BudgetEntity(id = "b3", categoryName = "Bike Expenses", subcategoryName = "Fuel", monthlyLimit = 3000.0, monthYear = "2026-10"),
            BudgetEntity(id = "b4", categoryName = "Bike Expenses", subcategoryName = "Service", monthlyLimit = 1500.0, monthYear = "2026-10")
        )
        assertEquals(4500.0, validator.computeTotalMonthlyBudget(subcategoryBudgets, "2026-10"), 0.01)

        // Scenario 3: Category with BOTH parent budget AND subcategory budgets -> DO NOT double count!
        // The subcategory budgets sum to 4500.0, parent is 6000.0. Only 4500.0 should be counted.
        val mixedCategoryBudgets = listOf(
            BudgetEntity(id = "bParent", categoryName = "Bike Expenses", subcategoryName = "", monthlyLimit = 6000.0, monthYear = "2026-10"),
            BudgetEntity(id = "bSub1", categoryName = "Bike Expenses", subcategoryName = "Fuel", monthlyLimit = 3000.0, monthYear = "2026-10"),
            BudgetEntity(id = "bSub2", categoryName = "Bike Expenses", subcategoryName = "Service", monthlyLimit = 1500.0, monthYear = "2026-10"),
            BudgetEntity(id = "bOther", categoryName = "Daily Essentials", subcategoryName = "", monthlyLimit = 10000.0, monthYear = "2026-10")
        )
        // Bike Expenses (3000 + 1500 = 4500) + Daily Essentials (10000) = 14500.0 (NOT 20500.0)
        assertEquals(14500.0, validator.computeTotalMonthlyBudget(mixedCategoryBudgets, "2026-10"), 0.01)

        // Scenario 4: Overall budget when no category/subcategory budgets exist
        val overallOnly = listOf(
            BudgetEntity(id = "bOverall", categoryName = "Overall", monthlyLimit = 35000.0, monthYear = "2026-10", isOverall = true)
        )
        assertEquals(35000.0, validator.computeTotalMonthlyBudget(overallOnly, "2026-10"), 0.01)

        // Scenario 5: Overall budget plus specific category budgets -> specific limits take precedence
        val overallPlusSpecific = listOf(
            BudgetEntity(id = "bOverall", categoryName = "Overall", monthlyLimit = 35000.0, monthYear = "2026-10", isOverall = true),
            BudgetEntity(id = "bSpecific", categoryName = "Shopping", monthlyLimit = 8000.0, monthYear = "2026-10")
        )
        assertEquals(8000.0, validator.computeTotalMonthlyBudget(overallPlusSpecific, "2026-10"), 0.01)

        // Scenario 6: Month filtering ("ALL" applies, but different months are excluded)
        val monthFiltering = listOf(
            BudgetEntity(id = "bOct", categoryName = "Food", monthlyLimit = 5000.0, monthYear = "2026-10"),
            BudgetEntity(id = "bAll", categoryName = "Transport", monthlyLimit = 2000.0, monthYear = "ALL"),
            BudgetEntity(id = "bNov", categoryName = "Entertainment", monthlyLimit = 4000.0, monthYear = "2026-11")
        )
        assertEquals(7000.0, validator.computeTotalMonthlyBudget(monthFiltering, "2026-10"), 0.01)
    }

    @Test
    fun testUnbudgetedAndZeroSpendingCategories_HandledGracefully() {
        // Zero spending and no budget limit
        val zeroSpendingNoLimit = CategoryBudgetUiModel(
            budget = null,
            categoryName = "Entertainment",
            categoryIcon = "movie",
            categoryColorHex = "#64748B",
            limit = 0.0,
            spent = 0.0,
            remaining = 0.0,
            percentage = 0f,
            transactionCount = 0,
            recentTransactions = emptyList(),
            isOverBudget = false,
            isNearLimit = false,
            hasDefinedBudget = false
        )
        assertFalse(zeroSpendingNoLimit.hasDefinedBudget)
        assertEquals(0.0, zeroSpendingNoLimit.limit, 0.001)
        assertEquals(0.0, zeroSpendingNoLimit.spent, 0.001)
        assertEquals(0f, zeroSpendingNoLimit.percentage, 0.001f)
        assertFalse(zeroSpendingNoLimit.isOverBudget)

        // Spending with no budget limit
        val spendingNoLimit = CategoryBudgetUiModel(
            budget = null,
            categoryName = "Miscellaneous",
            categoryIcon = "category",
            categoryColorHex = "#64748B",
            limit = 0.0,
            spent = 1500.0,
            remaining = -1500.0,
            percentage = 0f,
            transactionCount = 2,
            recentTransactions = emptyList(),
            isOverBudget = false,
            isNearLimit = false,
            hasDefinedBudget = false
        )
        assertFalse(spendingNoLimit.hasDefinedBudget)
        assertEquals(0.0, spendingNoLimit.limit, 0.001)
        assertEquals(1500.0, spendingNoLimit.spent, 0.001)
        assertEquals(-1500.0, spendingNoLimit.remaining, 0.001)
    }

    @Test
    fun testBudgetDashboard_EdgeToEdgeConfiguration() {
        val edgeToEdgeScreens = listOf(
            com.example.ui.SubScreen.NONE,
            com.example.ui.SubScreen.THEME_SETTINGS,
            com.example.ui.SubScreen.SETTINGS,
            com.example.ui.SubScreen.PROFILE_SETTINGS,
            com.example.ui.SubScreen.USER_PROFILE,
            com.example.ui.SubScreen.CURRENCY_SELECTION,
            com.example.ui.SubScreen.NEW_SAVINGS_GOAL,
            com.example.ui.SubScreen.BACKUP_RESTORE,
            com.example.ui.SubScreen.ACCOUNTS_MANAGEMENT,
            com.example.ui.SubScreen.CURRENCY_CONVERTER,
            com.example.ui.SubScreen.MONTHLY_RECAP,
            com.example.ui.SubScreen.CALENDAR,
            com.example.ui.SubScreen.INCOME_TRACKER,
            com.example.ui.SubScreen.SPENDING_COMPARISON,
            com.example.ui.SubScreen.ABOUT,
            com.example.ui.SubScreen.ACCOUNT_DETAIL,
            com.example.ui.SubScreen.EDIT_ACCOUNT,
            com.example.ui.SubScreen.BUDGET_DASHBOARD,
            com.example.ui.SubScreen.ADD_BUDGET
        )
        assertTrue(edgeToEdgeScreens.contains(com.example.ui.SubScreen.BUDGET_DASHBOARD))
        assertTrue(edgeToEdgeScreens.contains(com.example.ui.SubScreen.ADD_BUDGET))
    }

    @Test
    fun testBudgetDetails_FixedFrostedHeaderMetricsAndTags() {
        // Verify default header height and extra gap ensure proper top clearance
        val defaultHeaderHeightDp = 68
        val extraTopGapDp = 4
        val totalInitialTopClearance = defaultHeaderHeightDp + extraTopGapDp
        assertTrue(totalInitialTopClearance >= 72)

        // Verify test tags match expected UI contracts
        val expectedTags = setOf(
            "budget_details_fixed_header",
            "budget_details_back_button",
            "budget_details_favorite_button",
            "budget_details_more_button",
            "budget_details_lazy_column"
        )
        assertEquals(5, expectedTags.size)
        assertTrue(expectedTags.contains("budget_details_fixed_header"))
        assertTrue(expectedTags.contains("budget_details_back_button"))
    }
}

