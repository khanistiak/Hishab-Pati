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

enum class RecapInsightType {
    SAVINGS_POSITIVE,
    SAVINGS_BALANCED,
    SAVINGS_NEGATIVE,
    TOP_CATEGORY,
    INCOME_RECORDED,
    EMPTY
}

data class ReportInsight(
    val title: String,
    val description: String,
    val type: RecapInsightType = RecapInsightType.EMPTY
)

data class RecapCategorySpend(
    val category: String,
    val amount: Double,
    val sharePct: Int,
    val txCount: Int
)

data class RecapWalletUsage(
    val accountId: String,
    val accountName: String,
    val accountType: AccountType?,
    val iconName: String,
    val colorHex: String,
    val spendTotal: Double,
    val incomeTotal: Double,
    val transactionCount: Int
)

data class RecapSummary(
    val hasData: Boolean,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val savingsRate: Int,
    val transactionCount: Int,
    val expenseCount: Int,
    val incomeCount: Int = 0,
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
    val budgetOkCount: Int = 0,
    val budgetTotalCount: Int = 0,
    val budgetOkPct: Int? = null,
    val topCategory: String?,
    val topCategoryAmount: Double,
    val topCategorySharePct: Int = 0,
    val topMerchant: String?,
    val topMerchantAmount: Double,
    val topCategories: List<RecapCategorySpend> = emptyList(),
    val walletUsages: List<RecapWalletUsage> = emptyList(),
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
        currencySymbol: String = "৳",
        accounts: List<AccountEntity> = emptyList()
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
                incomeCount = 0,
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
                budgetOkCount = 0,
                budgetTotalCount = 0,
                budgetOkPct = null,
                topCategory = null,
                topCategoryAmount = 0.0,
                topCategorySharePct = 0,
                topMerchant = null,
                topMerchantAmount = 0.0,
                topCategories = emptyList(),
                walletUsages = emptyList(),
                insights = listOf(
                    ReportInsight(
                        title = "No transactions logged yet",
                        description = "Start logging transactions for ${getMonthName(month, full = true)} $year to unlock automated spending analysis, trends, and budget health.",
                        type = RecapInsightType.EMPTY
                    )
                )
            )
        }

        val incomeTxs = monthTxs.filter { it.type == TransactionType.INCOME }
        val expenseTxs = monthTxs.filter { it.type == TransactionType.EXPENSE }
        val totalIncome = incomeTxs.sumOf { it.amount }
        val totalExpense = expenseTxs.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) {
            ((netSavings / totalIncome) * 100).toInt().coerceIn(0, 100)
        } else 0

        val expenseCount = expenseTxs.size
        val incomeCount = incomeTxs.size
        val avgExpenseSize = if (expenseCount > 0) totalExpense / expenseCount else 0.0
        val dailyAverage = if (totalExpense > 0) totalExpense / daysInMonth else 0.0

        // Financial health grade
        val (grade, healthTitle, healthDescription) = when {
            totalIncome > 0 && savingsRate >= 30 -> Triple(
                "A",
                "Excellent Financial Health",
                "You saved $savingsRate% of your income"
            )
            totalIncome > 0 && savingsRate >= 15 -> Triple(
                "B",
                "Good Financial Health",
                "You saved $savingsRate% of your income"
            )
            totalIncome > 0 && netSavings >= 0 -> Triple(
                "C",
                "Fair Financial Health",
                "You saved $savingsRate% of your income"
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
        val budgetTotalCount = activeBudgets.size
        val budgetOkCount = if (activeBudgets.isNotEmpty()) {
            activeBudgets.count { b ->
                val spent = expenseTxs.filter { it.category.equals(b.categoryName, ignoreCase = true) }.sumOf { it.amount }
                spent <= b.monthlyLimit
            }
        } else 0
        val budgetOkPct: Int? = if (budgetTotalCount > 0) (budgetOkCount * 100) / budgetTotalCount else null
        val (budgetOkText, budgetOkSubtitle) = if (budgetOkPct != null) {
            Pair("$budgetOkPct%", "$budgetOkCount of $budgetTotalCount in budget")
        } else {
            Pair("–", "No budget set")
        }

        // Top 3 spending categories
        val groupedCategories = expenseTxs
            .filter { it.category.isNotBlank() }
            .groupBy { it.category }
            .map { (catName, txs) ->
                val sum = txs.sumOf { it.amount }
                val pct = if (totalExpense > 0.0) ((sum / totalExpense) * 100.0).toInt().coerceIn(0, 100) else 0
                RecapCategorySpend(
                    category = catName,
                    amount = sum,
                    sharePct = pct,
                    txCount = txs.size
                )
            }
            .sortedByDescending { it.amount }

        val topCategories = groupedCategories.take(3)
        val topCat = topCategories.firstOrNull()?.category
        val topCatAmt = topCategories.firstOrNull()?.amount ?: 0.0
        val topCatSharePct = topCategories.firstOrNull()?.sharePct ?: 0

        // Top merchant
        val topMerchantEntry = expenseTxs.filter { it.title.isNotBlank() }
            .groupBy { it.title }
            .maxByOrNull { it.value.sumOf { tx -> tx.amount } }
        val topMerchant = topMerchantEntry?.key
        val topMerchantAmt = topMerchantEntry?.value?.sumOf { it.amount } ?: 0.0

        // Wallet / Payment Method Usage
        val accountMap = accounts.associateBy { it.id }
        val walletUsages = monthTxs
            .filter { it.accountId.isNotBlank() }
            .groupBy { it.accountId }
            .map { (accId, txs) ->
                val account = accountMap[accId]
                val spend = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val inc = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                RecapWalletUsage(
                    accountId = accId,
                    accountName = account?.name?.ifBlank { "Wallet" } ?: "Wallet",
                    accountType = account?.type,
                    iconName = account?.iconName?.ifBlank { "account_balance_wallet" } ?: "account_balance_wallet",
                    colorHex = account?.colorHex?.ifBlank { "#2563EB" } ?: "#2563EB",
                    spendTotal = spend,
                    incomeTotal = inc,
                    transactionCount = txs.size
                )
            }
            .sortedWith(
                compareByDescending<RecapWalletUsage> { it.spendTotal }
                    .thenByDescending { it.transactionCount }
                    .thenByDescending { it.incomeTotal }
            )
            .take(4)

        // Real typed insights
        val insights = mutableListOf<ReportInsight>()
        if (savingsRate >= 30) {
            insights.add(
                ReportInsight(
                    title = "Healthy savings rate",
                    description = "$savingsRate% • $currencySymbol${String.format(Locale.US, "%,.2f", netSavings)}",
                    type = RecapInsightType.SAVINGS_POSITIVE
                )
            )
        } else if (netSavings < 0) {
            insights.add(
                ReportInsight(
                    title = "Negative monthly balance",
                    description = "Expenses exceeded income by $currencySymbol${String.format(Locale.US, "%,.2f", kotlin.math.abs(netSavings))}.",
                    type = RecapInsightType.SAVINGS_NEGATIVE
                )
            )
        } else if (totalIncome > 0) {
            insights.add(
                ReportInsight(
                    title = "Balanced monthly cash flow",
                    description = "$savingsRate% • $currencySymbol${String.format(Locale.US, "%,.2f", netSavings)}",
                    type = RecapInsightType.SAVINGS_BALANCED
                )
            )
        }

        if (topCat != null) {
            insights.add(
                ReportInsight(
                    title = "Top spending: $topCat",
                    description = "$currencySymbol${String.format(Locale.US, "%,.2f", topCatAmt)} • $topCatSharePct% of expenses",
                    type = RecapInsightType.TOP_CATEGORY
                )
            )
        }

        if (totalIncome > 0) {
            val receiptWord = if (incomeCount == 1) "receipt" else "receipts"
            insights.add(
                ReportInsight(
                    title = "Income recorded",
                    description = "$currencySymbol${String.format(Locale.US, "%,.2f", totalIncome)} across $incomeCount $receiptWord",
                    type = RecapInsightType.INCOME_RECORDED
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
            incomeCount = incomeCount,
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
            budgetOkCount = budgetOkCount,
            budgetTotalCount = budgetTotalCount,
            budgetOkPct = budgetOkPct,
            topCategory = topCat,
            topCategoryAmount = topCatAmt,
            topCategorySharePct = topCatSharePct,
            topMerchant = topMerchant,
            topMerchantAmount = topMerchantAmt,
            topCategories = topCategories,
            walletUsages = walletUsages,
            insights = insights
        )
    }

    /**
     * Computes the Income Tracker summary.
     * Supports "3M", "6M", "1Y", "Last Year", and "Custom" periods.
     * Starts with pure 0.0 values and empty list on a fresh database.
     */
    fun computeIncomeTracker(
        transactions: List<TransactionEntity>,
        period: String, // "3M", "6M", "1Y", "Last Year", "Custom"
        referenceTimeMs: Long = System.currentTimeMillis(),
        customStartMs: Long? = null,
        customEndMs: Long? = null
    ): IncomeTrackerSummary {
        val normalizedPeriod = period.trim().uppercase(Locale.US)
        val refCal = Calendar.getInstance().apply { timeInMillis = referenceTimeMs }

        val periodStartTime: Long
        val periodEndTime: Long
        val prevPeriodStartTime: Long
        val prevPeriodEndTime: Long
        val monthBuckets: List<Pair<Int, Int>>
        val avgDivisorMonths: Double

        when (normalizedPeriod) {
            "LAST YEAR", "LAST_YEAR" -> {
                val lastYear = refCal.get(Calendar.YEAR) - 1
                val startCal = Calendar.getInstance().apply {
                    set(lastYear, Calendar.JANUARY, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    set(lastYear, Calendar.DECEMBER, 31, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val prevStartCal = Calendar.getInstance().apply {
                    set(lastYear - 1, Calendar.JANUARY, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val prevEndCal = Calendar.getInstance().apply {
                    set(lastYear - 1, Calendar.DECEMBER, 31, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                periodStartTime = startCal.timeInMillis
                periodEndTime = endCal.timeInMillis
                prevPeriodStartTime = prevStartCal.timeInMillis
                prevPeriodEndTime = prevEndCal.timeInMillis
                monthBuckets = (0..11).map { m -> lastYear to m }
                avgDivisorMonths = 12.0
            }
            "CUSTOM" -> {
                val fallbackStart = (refCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val rawStart = customStartMs ?: fallbackStart
                val rawEnd = customEndMs ?: referenceTimeMs
                val minMs = minOf(rawStart, rawEnd)
                val maxMs = maxOf(rawStart, rawEnd)

                val startCal = Calendar.getInstance().apply {
                    timeInMillis = minMs
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = maxMs
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                periodStartTime = startCal.timeInMillis
                periodEndTime = endCal.timeInMillis

                val spanMs = (periodEndTime - periodStartTime + 1L).coerceAtLeast(86_400_000L)
                prevPeriodEndTime = periodStartTime - 1L
                prevPeriodStartTime = periodStartTime - spanMs

                // Enumerate (year, month) buckets from startCal to endCal inclusive
                val buckets = mutableListOf<Pair<Int, Int>>()
                val cursor = Calendar.getInstance().apply {
                    set(startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endYm = endCal.get(Calendar.YEAR) * 12 + endCal.get(Calendar.MONTH)
                while (cursor.get(Calendar.YEAR) * 12 + cursor.get(Calendar.MONTH) <= endYm) {
                    buckets.add(cursor.get(Calendar.YEAR) to cursor.get(Calendar.MONTH))
                    cursor.add(Calendar.MONTH, 1)
                }
                monthBuckets = buckets.ifEmpty {
                    listOf(startCal.get(Calendar.YEAR) to startCal.get(Calendar.MONTH))
                }

                // Proportional months/days calculation (minimum 1.0 month to avoid exaggerating short windows)
                val endDayStart = Calendar.getInstance().apply {
                    timeInMillis = maxMs
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val totalDays = (((endDayStart - startCal.timeInMillis) / 86_400_000L).toInt() + 1).coerceAtLeast(1)
                avgDivisorMonths = (totalDays / 30.4375).coerceAtLeast(1.0)
            }
            else -> {
                val monthsCount = when (normalizedPeriod) {
                    "3M" -> 3
                    "1Y" -> 12
                    else -> 6
                }
                val periodStartCal = (refCal.clone() as Calendar).apply {
                    when (normalizedPeriod) {
                        "3M" -> add(Calendar.MONTH, -3)
                        "1Y" -> add(Calendar.YEAR, -1)
                        else -> add(Calendar.MONTH, -6)
                    }
                }
                val prevPeriodStartCal = (periodStartCal.clone() as Calendar).apply {
                    when (normalizedPeriod) {
                        "3M" -> add(Calendar.MONTH, -3)
                        "1Y" -> add(Calendar.YEAR, -1)
                        else -> add(Calendar.MONTH, -6)
                    }
                }
                periodStartTime = periodStartCal.timeInMillis
                periodEndTime = referenceTimeMs
                prevPeriodStartTime = prevPeriodStartCal.timeInMillis
                prevPeriodEndTime = periodStartTime - 1L
                monthBuckets = (0 until monthsCount).map { i ->
                    val mCal = (refCal.clone() as Calendar).apply {
                        add(Calendar.MONTH, -(monthsCount - 1 - i))
                    }
                    mCal.get(Calendar.YEAR) to mCal.get(Calendar.MONTH)
                }
                avgDivisorMonths = monthsCount.toDouble()
            }
        }

        val monthsCount = monthBuckets.size.coerceAtLeast(1)

        val incomeTxs = transactions.filter {
            it.type == TransactionType.INCOME &&
                it.timestamp >= periodStartTime &&
                it.timestamp <= periodEndTime
        }.sortedByDescending { it.timestamp }

        val totalIncome = incomeTxs.sumOf { it.amount }
        val avgMonthly = if (avgDivisorMonths > 0.0) totalIncome / avgDivisorMonths else 0.0

        val prevIncome = transactions.filter {
            it.type == TransactionType.INCOME &&
                it.timestamp >= prevPeriodStartTime &&
                it.timestamp <= prevPeriodEndTime
        }.sumOf { it.amount }

        val deltaPct: Double? = when {
            prevIncome > 0 -> ((totalIncome - prevIncome) / prevIncome) * 100
            totalIncome > 0 -> 100.0
            else -> null
        }

        // Generate trend buckets & labels
        val trendMonths: List<String>
        val monthlyAmounts: List<Double>
        if (normalizedPeriod == "CUSTOM" && monthBuckets.size == 1) {
            // Single-month custom range: slice into up to 5 day-window buckets so chart shows intra-month trend
            val spanMs = (periodEndTime - periodStartTime + 1L).coerceAtLeast(86_400_000L)
            val totalDays = (spanMs / 86_400_000L).toInt().coerceAtLeast(1)
            val bucketCount = minOf(5, totalDays).coerceAtLeast(2)
            val bucketSizeMs = spanMs.toDouble() / bucketCount.toDouble()
            val dayFmt = SimpleDateFormat("MMM d", Locale.US)

            trendMonths = (0 until bucketCount).map { i ->
                val bStart = periodStartTime + (i * bucketSizeMs).toLong()
                dayFmt.format(bStart)
            }
            monthlyAmounts = (0 until bucketCount).map { i ->
                val bStart = periodStartTime + (i * bucketSizeMs).toLong()
                val bEnd = if (i == bucketCount - 1) periodEndTime else (periodStartTime + ((i + 1) * bucketSizeMs).toLong() - 1L)
                incomeTxs.filter { it.timestamp in bStart..bEnd }.sumOf { it.amount }
            }
        } else {
            val multiYear = monthBuckets.map { it.first }.distinct().size > 1
            trendMonths = monthBuckets.map { (y, m) ->
                if (multiYear && monthBuckets.size > 6) {
                    "${getMonthName(m, full = false)} '${y % 100}"
                } else {
                    getMonthName(m, full = false)
                }
            }
            monthlyAmounts = monthBuckets.map { (y, m) ->
                incomeTxs.filter { tx ->
                    val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
                }.sumOf { it.amount }
            }
        }

        val maxAmount = monthlyAmounts.maxOrNull() ?: 0.0
        val trendPoints = if (maxAmount > 0.0) {
            monthlyAmounts.map { (it / maxAmount).toFloat().coerceIn(0.05f, 1f) }
        } else {
            List(monthlyAmounts.size) { 0f }
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

                val sparkAmounts = monthBuckets.map { (y, m) ->
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
