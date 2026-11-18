package com.example.ui.screens.insights

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class InsightsTabMode(val label: String, val labelBn: String) {
    INCOME("Income", "আয়"),
    EXPENSE("Expense", "ব্যয়")
}

data class InsightsDateRange(
    val startMillis: Long,
    val endMillis: Long,
    val formattedDisplay: String,
    val formattedDisplayBn: String
)

data class CategoryExpenseItem(
    val categoryName: String,
    val amount: Double,
    val percentage: Float, // 0..100
    val count: Int,
    val color: Color
)

data class SubcategoryExpenseItem(
    val subcategoryName: String,
    val amount: Double,
    val percentageOfCategory: Float, // 0..100
    val count: Int,
    val color: Color
)

data class SpendingTrendPoint(
    val label: String,
    val amount: Double,
    val timestamp: Long
)

data class SpendingTrendSummary(
    val points: List<SpendingTrendPoint>,
    val totalExpense: Double,
    val highestDayLabel: String,
    val highestDayAmount: Double,
    val averageDailySpending: Double
)

data class IncomeSourceItem(
    val name: String,
    val amount: Double,
    val percentage: Float,
    val count: Int,
    val color: Color
)

data class CashFlowMonthData(
    val monthLabel: String,
    val fullMonthLabel: String,
    val year: Int,
    val income: Double,
    val expense: Double,
    val net: Double,
    val savingsRate: Double, // 0..100
    val highestExpenseCategory: String? = null,
    val highestExpenseAmount: Double = 0.0,
    val highestIncomeCategory: String? = null,
    val highestIncomeAmount: Double = 0.0
)

data class CashFlowSummary(
    val monthlyData: List<CashFlowMonthData>,
    val totalIncome: Double,
    val totalExpense: Double,
    val netCashFlow: Double,
    val savingsAmount: Double,
    val overallSavingsRate: Double
)

data class MerchantExpenseItem(
    val merchantName: String,
    val amount: Double,
    val count: Int
)

data class PeriodComparisonData(
    val currentIncome: Double,
    val previousIncome: Double,
    val incomeDiff: Double,
    val incomeChangePct: Double,
    val currentExpense: Double,
    val previousExpense: Double,
    val expenseDiff: Double,
    val expenseChangePct: Double,
    val currentNet: Double,
    val previousNet: Double,
    val netDiff: Double,
    val netChangePct: Double
)

data class FinancialHealthData(
    val score: Int, // 0..100
    val rating: String,
    val ratingBn: String,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netSavings: Double,
    val savingsRate: Double?, // null if income is 0
    val expenseToIncomeRatio: Double?, // null if income is 0
    val budgetAdherencePct: Double?, // null if no budgets
    val explanation: String,
    val explanationBn: String
)

data class SmartInsightItem(
    val title: String,
    val titleBn: String,
    val description: String,
    val descriptionBn: String,
    val isPositive: Boolean
)
