package com.example.ui.util

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ReportInsight(
    val title: String,
    val description: String
)

data class RecapSummary(
    val hasData: Boolean,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val savingsRate: Int,
    val transactionCount: Int,
    val expenseCount: Int,
    val avgExpenseSize: Double,
    val dailyAverage: Double,
    val grade: String,
    val healthTitle: String,
    val healthDescription: String,
    val incomeGrade: String,
    val expenseGrade: String,
    val savingsGrade: String,
    val budgetOkText: String,
    val budgetOkSubtitle: String,
    val topCategory: String?,
    val topCategoryAmount: Double,
    val topMerchant: String?,
    val topMerchantAmount: Double,
    val insights: List<ReportInsight>
)

data class IncomeStreamItem(
    val title: String,
    val totalAmount: Double,
    val paymentCount: Int,
    val typeBadge: String,
    val consistencyPct: Int,
    val sharePct: Double,
    val lastReceivedDate: String,
    val sparklinePoints: List<Float>
)

data class IncomeTrackerSummary(
    val totalIncome: Double,
    val avgMonthly: Double,
    val deltaPct: Double?,
    val monthsCount: Int,
    val trendMonths: List<String>,
    val trendPoints: List<Float>,
    val hasTrendData: Boolean,
    val incomeStreams: List<IncomeStreamItem>
)

data class CalendarDayDots(
    val hasExpense: Boolean,
    val hasIncome: Boolean,
    val hasTransfer: Boolean,
    val hasCardDate: Boolean
)

data class CalendarSummary(
    val monthIncome: Double,
    val monthExpense: Double,
    val pendingDebtsTotal: Double,
    val selectedDayIncome: Double,
    val selectedDayExpense: Double,
    val selectedDayNet: Double,
    val selectedDayTransactions: List<TransactionEntity>,
    val dayDots: Map<Int, CalendarDayDots>
)

data class ComparisonCategoryItem(
    val name: String,
    val currentMonthAmount: Double,
    val previousMonthAmount: Double,
    val percentageChange: Double
)

data class SpendingComparisonSummary(
    val prevYear: Int,
    val prevMonth: Int,
    val currentMonthExpense: Double,
    val previousMonthExpense: Double,
    val expenseDiff: Double,
    val expenseDeltaPct: Double,
    val currentMonthIncome: Double,
    val previousMonthIncome: Double,
    val incomeDiff: Double,
    val incomeDeltaPct: Double,
    val categories: List<ComparisonCategoryItem>,
    val biggestIncrease: ComparisonCategoryItem?,
    val biggestDecrease: ComparisonCategoryItem?,
    val insightText: String
)

object FinancialReportAnalytics {

    private val MONTH_NAMES_SHORT = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val MONTH_NAMES_FULL = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun getMonthName(month: Int, full: Boolean = false): String {
        val safeMonth = month.coerceIn(0, 11)
        return if (full) MONTH_NAMES_FULL[safeMonth] else MONTH_NAMES_SHORT[safeMonth]
    }

    /**
     * Computes the Monthly Recap summary from actual records.
     * Returns zero/empty states without fabricated figures when transactions are empty.
     */
    fun computeMonthlyRecap(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        year: Int,
        month: Int,
        currencySymbol: String = "৳"
    ): RecapSummary {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthTxs = transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.YEAR) == year && txCal.get(Calendar.MONTH) == month
        }

        if (monthTxs.isEmpty()) {
            return RecapSummary(
                hasData = false,
                totalIncome = 0.0,
                totalExpense = 0.0,
                netSavings = 0.0,
                savingsRate = 0,
                transactionCount = 0,
                expenseCount = 0,
                avgExpenseSize = 0.0,
                dailyAverage = 0.0,
                grade = "–",
                healthTitle = "No Data For This Month",
                healthDescription = "No income or expense records found for this period.",
                incomeGrade = "–",
                expenseGrade = "–",
                savingsGrade = "–",
                budgetOkText = "–",
                budgetOkSubtitle = "No budget set",
                topCategory = null,
                topCategoryAmount = 0.0,
                topMerchant = null,
                topMerchantAmount = 0.0,
                insights = listOf(
                    ReportInsight(
                        title = "No transactions logged yet",
                        description = "Start logging transactions for ${getMonthName(month, full = true)} $year to unlock automated spending analysis, trends, and budget health."
                    )
                )
            )
        }

        val totalIncome = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) {
            ((netSavings / totalIncome) * 100).toInt().coerceIn(0, 100)
        } else 0

        val expenseTxs = monthTxs.filter { it.type == TransactionType.EXPENSE }
        val expenseCount = expenseTxs.size
        val avgExpenseSize = if (expenseCount > 0) totalExpense / expenseCount else 0.0
        val dailyAverage = if (totalExpense > 0) totalExpense / daysInMonth else 0.0

        // Financial health grade
        val (grade, healthTitle, healthDescription) = when {
            totalIncome > 0 && savingsRate >= 30 -> Triple(
                "A",
                "Excellent Financial Health",
                "You saved $savingsRate% of your total income this month!"
            )
            totalIncome > 0 && savingsRate >= 15 -> Triple(
                "B",
                "Good Financial Health",
                "You saved $savingsRate% of your total income this month."
            )
            totalIncome > 0 && netSavings >= 0 -> Triple(
                "C",
                "Fair Financial Health",
                "You saved $savingsRate% of your income. Watch discretionary expenses."
            )
            totalIncome == 0.0 && totalExpense == 0.0 -> Triple(
                "–",
                "No Data For This Month",
                "No income or expense records found for this period."
            )
            else -> Triple(
                "D",
                "Expenses Exceeded Income",
                "Expenses exceeded total income this month."
            )
        }

        // Sub-grades
        val incomeGrade = if (totalIncome > 0) "A" else "–"
        val expenseGrade = when {
            totalExpense == 0.0 -> "–"
            totalIncome > 0 && totalExpense <= totalIncome * 0.7 -> "A"
            totalIncome > 0 && totalExpense <= totalIncome -> "B"
            else -> "C"
        }
        val savingsGrade = when {
            netSavings > 0 -> "A"
            netSavings == 0.0 -> "–"
            else -> "D"
        }

        // Budget evaluation
        val monthKey = String.format(Locale.US, "%04d-%02d", year, month + 1)
        val activeBudgets = budgets.filter { it.monthYear == monthKey || it.monthYear.isBlank() }
        val (budgetOkText, budgetOkSubtitle) = if (activeBudgets.isNotEmpty()) {
            val okCount = activeBudgets.count { b ->
                val spent = expenseTxs.filter { it.category.equals(b.categoryName, ignoreCase = true) }.sumOf { it.amount }
                spent <= b.monthlyLimit
            }
            val pct = (okCount * 100) / activeBudgets.size
            Pair("$pct%", "$okCount of ${activeBudgets.size} in budget")
        } else {
            Pair("–", "No budget set")
        }

        // Top category & merchant
        val topCategoryEntry = expenseTxs.filter { it.category.isNotBlank() }
            .groupBy { it.category }
            .maxByOrNull { it.value.sumOf { tx -> tx.amount } }
        val topCat = topCategoryEntry?.key
        val topCatAmt = topCategoryEntry?.value?.sumOf { it.amount } ?: 0.0

        val topMerchantEntry = expenseTxs.filter { it.title.isNotBlank() }
            .groupBy { it.title }
            .maxByOrNull { it.value.sumOf { tx -> tx.amount } }
        val topMerchant = topMerchantEntry?.key
        val topMerchantAmt = topMerchantEntry?.value?.sumOf { it.amount } ?: 0.0

        // Real insights
        val insights = mutableListOf<ReportInsight>()
        if (savingsRate >= 30) {
            insights.add(
                ReportInsight(
                    title = "Healthy savings rate",
                    description = "You saved $savingsRate% of your income this month ($currencySymbol${String.format(Locale.US, "%,.2f", netSavings)})."
                )
            )
        } else if (netSavings < 0) {
            insights.add(
                ReportInsight(
                    title = "Negative monthly balance",
                    description = "Expenses exceeded total income by $currencySymbol${String.format(Locale.US, "%,.2f", kotlin.math.abs(netSavings))}."
                )
            )
        } else if (totalIncome > 0) {
            insights.add(
                ReportInsight(
                    title = "Balanced monthly cash flow",
                    description = "Net balance is $currencySymbol${String.format(Locale.US, "%,.2f", netSavings)} with a $savingsRate% savings rate."
                )
            )
        }

        if (topCat != null) {
            val pct = if (totalExpense > 0) ((topCatAmt / totalExpense) * 100).toInt() else 0
            insights.add(
                ReportInsight(
                    title = "Top spending: $topCat",
                    description = "Totaled $currencySymbol${String.format(Locale.US, "%,.2f", topCatAmt)} ($pct% of all expenses)."
                )
            )
        }

        if (totalIncome > 0) {
            val incCount = monthTxs.count { it.type == TransactionType.INCOME }
            insights.add(
                ReportInsight(
                    title = "Income recorded",
                    description = "Totaled $currencySymbol${String.format(Locale.US, "%,.2f", totalIncome)} across $incCount income receipt(s)."
                )
            )
        }

        return RecapSummary(
            hasData = true,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            savingsRate = savingsRate,
            transactionCount = monthTxs.size,
            expenseCount = expenseCount,
            avgExpenseSize = avgExpenseSize,
            dailyAverage = dailyAverage,
            grade = grade,
            healthTitle = healthTitle,
            healthDescription = healthDescription,
            incomeGrade = incomeGrade,
            expenseGrade = expenseGrade,
            savingsGrade = savingsGrade,
            budgetOkText = budgetOkText,
            budgetOkSubtitle = budgetOkSubtitle,
            topCategory = topCat,
            topCategoryAmount = topCatAmt,
            topMerchant = topMerchant,
            topMerchantAmount = topMerchantAmt,
            insights = insights
        )
    }

    /**
     * Computes the Income Tracker summary.
     * Starts with pure 0.0 values and empty list on a fresh database.
     */
    fun computeIncomeTracker(
        transactions: List<TransactionEntity>,
        period: String, // "3M", "6M", "1Y"
        referenceTimeMs: Long = System.currentTimeMillis()
    ): IncomeTrackerSummary {
        val monthsCount = when (period) {
            "3M" -> 3
            "1Y" -> 12
            else -> 6
        }

        val refCal = Calendar.getInstance().apply { timeInMillis = referenceTimeMs }
        val periodStartCal = (refCal.clone() as Calendar).apply {
            when (period) {
                "3M" -> add(Calendar.MONTH, -3)
                "1Y" -> add(Calendar.YEAR, -1)
                else -> add(Calendar.MONTH, -6)
            }
        }
        val periodStartTime = periodStartCal.timeInMillis

        val incomeTxs = transactions.filter {
            it.type == TransactionType.INCOME && it.timestamp >= periodStartTime && it.timestamp <= referenceTimeMs
        }.sortedByDescending { it.timestamp }

        val totalIncome = incomeTxs.sumOf { it.amount }
        val avgMonthly = if (monthsCount > 0) totalIncome / monthsCount else 0.0

        // Prior period income for delta calculation
        val prevPeriodStartCal = (periodStartCal.clone() as Calendar).apply {
            when (period) {
                "3M" -> add(Calendar.MONTH, -3)
                "1Y" -> add(Calendar.YEAR, -1)
                else -> add(Calendar.MONTH, -6)
            }
        }
        val prevIncome = transactions.filter {
            it.type == TransactionType.INCOME &&
                it.timestamp >= prevPeriodStartCal.timeInMillis &&
                it.timestamp < periodStartTime
        }.sumOf { it.amount }

        val deltaPct: Double? = when {
            prevIncome > 0 -> ((totalIncome - prevIncome) / prevIncome) * 100
            totalIncome > 0 -> 100.0
            else -> null
        }

        // Generate trend months ending at referenceTime
        val trendMonths = (0 until monthsCount).map { i ->
            val mCal = (refCal.clone() as Calendar).apply {
                add(Calendar.MONTH, -(monthsCount - 1 - i))
            }
            getMonthName(mCal.get(Calendar.MONTH), full = false)
        }

        // Monthly trend amounts
        val monthlyAmounts = (0 until monthsCount).map { i ->
            val mCal = (refCal.clone() as Calendar).apply {
                add(Calendar.MONTH, -(monthsCount - 1 - i))
            }
            val y = mCal.get(Calendar.YEAR)
            val m = mCal.get(Calendar.MONTH)
            incomeTxs.filter { tx ->
                val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
            }.sumOf { it.amount }
        }

        val maxAmount = monthlyAmounts.maxOrNull() ?: 0.0
        val trendPoints = if (maxAmount > 0.0) {
            monthlyAmounts.map { (it / maxAmount).toFloat().coerceIn(0.05f, 1f) }
        } else {
            List(monthsCount) { 0f }
        }

        // Streams grouped dynamically from real records
        val incomeStreams = if (incomeTxs.isEmpty()) {
            emptyList()
        } else {
            incomeTxs.groupBy {
                val name = it.category.ifBlank { it.title }
                if (name.isBlank()) "Other Income" else name
            }.map { (name, txs) ->
                val streamTotal = txs.sumOf { it.amount }
                val count = txs.size
                val share = if (totalIncome > 0) (streamTotal / totalIncome) * 100 else 0.0
                val badge = if (count >= monthsCount) "Monthly" else if (count > 1) "Regular" else "One-time"
                val latestTx = txs.maxByOrNull { it.timestamp }
                val lastDate = latestTx?.let { SimpleDateFormat("MMM d, yyyy", Locale.US).format(it.timestamp) } ?: "–"

                val sparkAmounts = (0 until monthsCount).map { i ->
                    val mCal = (refCal.clone() as Calendar).apply {
                        add(Calendar.MONTH, -(monthsCount - 1 - i))
                    }
                    val y = mCal.get(Calendar.YEAR)
                    val m = mCal.get(Calendar.MONTH)
                    txs.filter { tx ->
                        val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                        c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
                    }.sumOf { it.amount }
                }
                val maxSpark = sparkAmounts.maxOrNull() ?: 0.0
                val normSpark = if (maxSpark > 0.0) {
                    sparkAmounts.map { (it / maxSpark).toFloat().coerceIn(0.05f, 1f) }
                } else {
                    List(monthsCount) { 0f }
                }

                IncomeStreamItem(
                    title = name,
                    totalAmount = streamTotal,
                    paymentCount = count,
                    typeBadge = badge,
                    consistencyPct = (count * 100 / monthsCount).coerceIn(1, 100),
                    sharePct = share,
                    lastReceivedDate = lastDate,
                    sparklinePoints = normSpark
                )
            }.sortedByDescending { it.totalAmount }
        }

        return IncomeTrackerSummary(
            totalIncome = totalIncome,
            avgMonthly = avgMonthly,
            deltaPct = deltaPct,
            monthsCount = monthsCount,
            trendMonths = trendMonths,
            trendPoints = trendPoints,
            hasTrendData = maxAmount > 0.0,
            incomeStreams = incomeStreams
        )
    }

    /**
     * Computes the Calendar summary and day breakdown.
     * Ensures empty states when there is no financial activity.
     */
    fun computeCalendar(
        transactions: List<TransactionEntity>,
        debts: List<DebtEntity>,
        accounts: List<AccountEntity>,
        year: Int,
        month: Int,
        selectedDay: Int
    ): CalendarSummary {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        var monthIncome = 0.0
        var monthExpense = 0.0
        val monthTxs = transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.YEAR) == year && txCal.get(Calendar.MONTH) == month
        }

        for (tx in monthTxs) {
            when (tx.type) {
                TransactionType.INCOME -> monthIncome += tx.amount
                TransactionType.EXPENSE -> monthExpense += tx.amount
                else -> {}
            }
        }

        val pendingDebtsTotal = debts.filter { !it.isSettled }.sumOf { it.totalAmount - it.paidAmount }

        // Selected day transactions
        val selectedDayTxs = monthTxs.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.DAY_OF_MONTH) == selectedDay
        }.sortedByDescending { it.timestamp }

        val selectedDayIncome = selectedDayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val selectedDayExpense = selectedDayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val selectedDayNet = selectedDayIncome - selectedDayExpense

        // Day dots map across month
        val dayDots = (1..maxDays).associateWith { dayNum ->
            val dayTxs = monthTxs.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                txCal.get(Calendar.DAY_OF_MONTH) == dayNum
            }
            val hasCardDate = accounts.any {
                it.type == AccountType.CREDIT_CARD &&
                    (it.billingDate.toIntOrNull() == dayNum || it.paymentDueDate.toIntOrNull() == dayNum)
            } || debts.any { debt ->
                if (!debt.isSettled && debt.dueDate.isNotBlank()) {
                    val dateStr = debt.dueDate
                    val expectedDayStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNum)
                    dateStr.startsWith(expectedDayStr) || dateStr.toIntOrNull() == dayNum
                } else false
            }

            CalendarDayDots(
                hasExpense = dayTxs.any { it.type == TransactionType.EXPENSE },
                hasIncome = dayTxs.any { it.type == TransactionType.INCOME },
                hasTransfer = dayTxs.any { it.type == TransactionType.TRANSFER },
                hasCardDate = hasCardDate
            )
        }

        return CalendarSummary(
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            pendingDebtsTotal = pendingDebtsTotal,
            selectedDayIncome = selectedDayIncome,
            selectedDayExpense = selectedDayExpense,
            selectedDayNet = selectedDayNet,
            selectedDayTransactions = selectedDayTxs,
            dayDots = dayDots
        )
    }

    /**
     * Computes the Spending Comparison between current month and previous month.
     * Computes exclusively from actual saved records.
     */
    fun computeSpendingComparison(
        transactions: List<TransactionEntity>,
        currentYear: Int,
        currentMonth: Int
    ): SpendingComparisonSummary {
        val (prevYear, prevMonth) = if (currentMonth == 0) {
            Pair(currentYear - 1, 11)
        } else {
            Pair(currentYear, currentMonth - 1)
        }

        val currentMonthTxs = transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }

        val prevMonthTxs = transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            cal.get(Calendar.YEAR) == prevYear && cal.get(Calendar.MONTH) == prevMonth
        }

        val currentMonthExpense = currentMonthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val previousMonthExpense = prevMonthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val expenseDiff = currentMonthExpense - previousMonthExpense
        val expenseDeltaPct = if (previousMonthExpense > 0) {
            ((currentMonthExpense - previousMonthExpense) / previousMonthExpense) * 100
        } else if (currentMonthExpense > 0) {
            100.0
        } else {
            0.0
        }

        val currentMonthIncome = currentMonthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val previousMonthIncome = prevMonthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val incomeDiff = currentMonthIncome - previousMonthIncome
        val incomeDeltaPct = if (previousMonthIncome > 0) {
            ((currentMonthIncome - previousMonthIncome) / previousMonthIncome) * 100
        } else if (currentMonthIncome > 0) {
            100.0
        } else {
            0.0
        }

        // Distinct expense categories in either month
        val allCategories = (currentMonthTxs.map { it.category } + prevMonthTxs.map { it.category })
            .filter { it.isNotBlank() }
            .distinct()

        val categories = allCategories.map { cat ->
            val currAmt = currentMonthTxs.filter { it.type == TransactionType.EXPENSE && it.category == cat }.sumOf { it.amount }
            val prevAmt = prevMonthTxs.filter { it.type == TransactionType.EXPENSE && it.category == cat }.sumOf { it.amount }
            val pctChange = if (prevAmt > 0) {
                ((currAmt - prevAmt) / prevAmt) * 100
            } else if (currAmt > 0) {
                100.0
            } else {
                0.0
            }
            ComparisonCategoryItem(cat, currAmt, prevAmt, pctChange)
        }.filter { it.currentMonthAmount > 0 || it.previousMonthAmount > 0 }
            .sortedByDescending { kotlin.math.max(it.currentMonthAmount, it.previousMonthAmount) }

        val biggestIncrease = categories.filter { it.percentageChange > 0 }
            .maxByOrNull { it.currentMonthAmount - it.previousMonthAmount }

        val biggestDecrease = categories.filter { it.percentageChange < 0 }
            .minByOrNull { it.currentMonthAmount - it.previousMonthAmount }

        val curName = getMonthName(currentMonth)
        val prevName = getMonthName(prevMonth)

        val insightText = when {
            categories.isEmpty() -> "No spending comparison available. Add expenses to track monthly changes."
            expenseDiff < 0 -> "You spent ${String.format(Locale.US, "%.1f", kotlin.math.abs(expenseDeltaPct))}% less overall compared to $prevName $prevYear."
            expenseDiff > 0 -> "You spent ${String.format(Locale.US, "%.1f", expenseDeltaPct)}% more overall compared to $prevName $prevYear."
            else -> "Spending in $curName $currentYear matches $prevName $prevYear."
        }

        return SpendingComparisonSummary(
            prevYear = prevYear,
            prevMonth = prevMonth,
            currentMonthExpense = currentMonthExpense,
            previousMonthExpense = previousMonthExpense,
            expenseDiff = expenseDiff,
            expenseDeltaPct = expenseDeltaPct,
            currentMonthIncome = currentMonthIncome,
            previousMonthIncome = previousMonthIncome,
            incomeDiff = incomeDiff,
            incomeDeltaPct = incomeDeltaPct,
            categories = categories,
            biggestIncrease = biggestIncrease,
            biggestDecrease = biggestDecrease,
            insightText = insightText
        )
    }
}
