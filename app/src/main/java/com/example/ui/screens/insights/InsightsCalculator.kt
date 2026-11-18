package com.example.ui.screens.insights

import androidx.compose.ui.graphics.Color
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.InsightsPeriod
import com.example.ui.util.CurrencyFormatter
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object InsightsCalculator {

    private val categoryPalette = listOf(
        Color(0xFF0EA5E9),
        Color(0xFF8B5CF6),
        Color(0xFFF59E0B),
        Color(0xFF10B981),
        Color(0xFFEC4899),
        Color(0xFF6366F1),
        Color(0xFFF97316),
        Color(0xFF14B8A6),
        Color(0xFFEF4444),
        Color(0xFF3B82F6)
    )

    private val subcategoryPalette = listOf(
        Color(0xFF38BDF8),
        Color(0xFFA78BFA),
        Color(0xFFFBBF24),
        Color(0xFF34D399),
        Color(0xFFF472B6),
        Color(0xFF818CF8),
        Color(0xFFFB923C),
        Color(0xFF2DD4BF)
    )

    fun computeDateRange(
        period: InsightsPeriod,
        offset: Int = 0,
        customStart: Long? = null,
        customEnd: Long? = null,
        language: AppLanguage = AppLanguage.ENGLISH
    ): InsightsDateRange {
        val cal = Calendar.getInstance()
        val startCal = Calendar.getInstance()
        val endCal = Calendar.getInstance()

        when (period) {
            InsightsPeriod.DAILY -> {
                startCal.add(Calendar.DAY_OF_YEAR, offset)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.timeInMillis = startCal.timeInMillis
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)

                val fmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(startCal.time)
                return InsightsDateRange(
                    startMillis = startCal.timeInMillis,
                    endMillis = endCal.timeInMillis,
                    formattedDisplay = fmt,
                    formattedDisplayBn = CurrencyFormatter.localizeDigits(fmt, AppLanguage.BANGLA)
                )
            }
            InsightsPeriod.THIS_WEEK -> {
                startCal.add(Calendar.WEEK_OF_YEAR, offset)
                startCal.set(Calendar.DAY_OF_WEEK, startCal.firstDayOfWeek)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.DAY_OF_YEAR, 6)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)

                val df = SimpleDateFormat("dd MMM", Locale.ENGLISH)
                val yr = SimpleDateFormat("yyyy", Locale.ENGLISH).format(endCal.time)
                val disp = "${df.format(startCal.time)} - ${df.format(endCal.time)}, $yr"
                return InsightsDateRange(
                    startMillis = startCal.timeInMillis,
                    endMillis = endCal.timeInMillis,
                    formattedDisplay = disp,
                    formattedDisplayBn = CurrencyFormatter.localizeDigits(disp, AppLanguage.BANGLA)
                )
            }
            InsightsPeriod.THIS_MONTH, InsightsPeriod.LAST_MONTH -> {
                val monthShift = if (period == InsightsPeriod.LAST_MONTH) offset - 1 else offset
                startCal.add(Calendar.MONTH, monthShift)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.timeInMillis = startCal.timeInMillis
                endCal.set(Calendar.DAY_OF_MONTH, startCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)

                val disp = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(startCal.time)
                val dispBn = translateMonthYearBn(startCal)
                return InsightsDateRange(
                    startMillis = startCal.timeInMillis,
                    endMillis = endCal.timeInMillis,
                    formattedDisplay = disp,
                    formattedDisplayBn = dispBn
                )
            }
            InsightsPeriod.THIS_YEAR, InsightsPeriod.LAST_YEAR -> {
                val yearShift = if (period == InsightsPeriod.LAST_YEAR) offset - 1 else offset
                startCal.add(Calendar.YEAR, yearShift)
                startCal.set(Calendar.MONTH, Calendar.JANUARY)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.timeInMillis = startCal.timeInMillis
                endCal.set(Calendar.MONTH, Calendar.DECEMBER)
                endCal.set(Calendar.DAY_OF_MONTH, 31)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)

                val yrStr = startCal.get(Calendar.YEAR).toString()
                return InsightsDateRange(
                    startMillis = startCal.timeInMillis,
                    endMillis = endCal.timeInMillis,
                    formattedDisplay = yrStr,
                    formattedDisplayBn = CurrencyFormatter.localizeDigits(yrStr, AppLanguage.BANGLA)
                )
            }
            InsightsPeriod.CUSTOM -> {
                val s = customStart ?: (cal.timeInMillis - 30L * 24 * 3600 * 1000)
                val e = customEnd ?: cal.timeInMillis
                startCal.timeInMillis = minOf(s, e)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.timeInMillis = maxOf(s, e)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)

                val df = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                val disp = "${df.format(startCal.time)} - ${df.format(endCal.time)}"
                return InsightsDateRange(
                    startMillis = startCal.timeInMillis,
                    endMillis = endCal.timeInMillis,
                    formattedDisplay = disp,
                    formattedDisplayBn = CurrencyFormatter.localizeDigits(disp, AppLanguage.BANGLA)
                )
            }
        }
    }

    private fun translateMonthYearBn(cal: Calendar): String {
        val bnMonths = arrayOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val y = CurrencyFormatter.localizeDigits(cal.get(Calendar.YEAR).toString(), AppLanguage.BANGLA)
        return "${bnMonths[m]} $y"
    }

    fun isExpense(tx: TransactionEntity): Boolean =
        tx.type == TransactionType.EXPENSE

    fun isIncome(tx: TransactionEntity): Boolean =
        tx.type == TransactionType.INCOME

    fun isTransfer(tx: TransactionEntity): Boolean =
        tx.type == TransactionType.TRANSFER || tx.category.equals("Transfer", ignoreCase = true)

    fun getCategoryColor(
        categoryName: String,
        categories: List<CategoryEntity>,
        indexFallback: Int = 0
    ): Color {
        val matched = categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
        if (matched != null && matched.colorHex.isNotBlank()) {
            try {
                return Color(android.graphics.Color.parseColor(matched.colorHex))
            } catch (_: Exception) {
            }
        }
        return categoryPalette[abs(indexFallback) % categoryPalette.size]
    }

    fun calculateCategoryExpenses(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        categories: List<CategoryEntity> = emptyList()
    ): List<CategoryExpenseItem> {
        val expenses = transactions.filter {
            !isTransfer(it) && isExpense(it) && it.timestamp in dateRange.startMillis..dateRange.endMillis
        }
        val total = expenses.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        return expenses
            .groupBy { it.category.ifBlank { "Uncategorized" } }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { it.amount } }
            .mapIndexed { idx, (catName, txs) ->
                val sum = txs.sumOf { it.amount }
                val pct = ((sum / total) * 100.0).toFloat()
                CategoryExpenseItem(
                    categoryName = catName,
                    amount = sum,
                    percentage = pct,
                    count = txs.size,
                    color = getCategoryColor(catName, categories, idx)
                )
            }
    }

    fun calculateSubcategoryExpenses(
        transactions: List<TransactionEntity>,
        categoryName: String,
        dateRange: InsightsDateRange,
        subcategories: List<SubcategoryEntity> = emptyList()
    ): List<SubcategoryExpenseItem> {
        val matching = transactions.filter {
            !isTransfer(it) &&
                isExpense(it) &&
                it.timestamp in dateRange.startMillis..dateRange.endMillis &&
                it.category.equals(categoryName, ignoreCase = true)
        }
        val total = matching.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        return matching
            .groupBy { it.subcategory?.takeIf { s -> s.isNotBlank() } ?: "General Expense" }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { it.amount } }
            .mapIndexed { idx, (subName, txs) ->
                val sum = txs.sumOf { it.amount }
                val pct = ((sum / total) * 100.0).toFloat()
                SubcategoryExpenseItem(
                    subcategoryName = subName,
                    amount = sum,
                    percentageOfCategory = pct,
                    count = txs.size,
                    color = subcategoryPalette[idx % subcategoryPalette.size]
                )
            }
    }

    fun calculateSpendingTrend(
        transactions: List<TransactionEntity>,
        period: InsightsPeriod,
        dateRange: InsightsDateRange
    ): SpendingTrendSummary {
        val expenses = transactions.filter {
            !isTransfer(it) && isExpense(it) && it.timestamp in dateRange.startMillis..dateRange.endMillis
        }
        val totalExpense = expenses.sumOf { it.amount }
        val points = mutableListOf<SpendingTrendPoint>()

        when (period) {
            InsightsPeriod.DAILY -> {
                val hourFormat = SimpleDateFormat("h a", Locale.ENGLISH)
                val stepMillis = 4L * 3600L * 1000L
                for (i in 0 until 6) {
                    val s = dateRange.startMillis + i * stepMillis
                    val e = if (i == 5) dateRange.endMillis else (s + stepMillis - 1)
                    val sum = expenses.filter { it.timestamp in s..e }.sumOf { it.amount }
                    val label = hourFormat.format(Date(s))
                    points.add(SpendingTrendPoint(label = label, amount = sum, timestamp = s))
                }
            }
            InsightsPeriod.THIS_YEAR, InsightsPeriod.LAST_YEAR -> {
                val cal = Calendar.getInstance().apply { timeInMillis = dateRange.startMillis }
                val monthFmt = SimpleDateFormat("MMM", Locale.ENGLISH)
                for (m in 0 until 12) {
                    cal.set(Calendar.MONTH, m)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val s = cal.timeInMillis
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val e = cal.timeInMillis
                    val sum = expenses.filter { it.timestamp in s..e }.sumOf { it.amount }
                    points.add(SpendingTrendPoint(label = monthFmt.format(Date(s)), amount = sum, timestamp = s))
                }
            }
            else -> {
                val dayFmt = if (period == InsightsPeriod.THIS_WEEK) {
                    SimpleDateFormat("EEE", Locale.ENGLISH)
                } else {
                    SimpleDateFormat("dd/MM", Locale.ENGLISH)
                }
                val cal = Calendar.getInstance().apply { timeInMillis = dateRange.startMillis }
                while (cal.timeInMillis <= dateRange.endMillis) {
                    val s = cal.timeInMillis
                    val endOfDay = Calendar.getInstance().apply {
                        timeInMillis = s
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    val sum = expenses.filter { it.timestamp in s..endOfDay }.sumOf { it.amount }
                    points.add(SpendingTrendPoint(label = dayFmt.format(Date(s)), amount = sum, timestamp = s))
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        val maxPt = points.maxByOrNull { it.amount }
        val dayCount = (((dateRange.endMillis - dateRange.startMillis) / (24L * 3600L * 1000L)).toInt() + 1).coerceAtLeast(1)
        return SpendingTrendSummary(
            points = points,
            totalExpense = totalExpense,
            highestDayLabel = if ((maxPt?.amount ?: 0.0) > 0.0) maxPt?.label ?: "-" else "-",
            highestDayAmount = maxPt?.amount ?: 0.0,
            averageDailySpending = totalExpense / dayCount
        )
    }

    fun calculateIncomeSources(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange
    ): List<IncomeSourceItem> {
        val incomes = transactions.filter {
            !isTransfer(it) && isIncome(it) && it.timestamp in dateRange.startMillis..dateRange.endMillis
        }
        val total = incomes.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        return incomes
            .groupBy { it.category.ifBlank { "Income" } }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { it.amount } }
            .mapIndexed { idx, (name, txs) ->
                val sum = txs.sumOf { it.amount }
                IncomeSourceItem(
                    name = name,
                    amount = sum,
                    percentage = ((sum / total) * 100.0).toFloat(),
                    count = txs.size,
                    color = categoryPalette[idx % categoryPalette.size]
                )
            }
    }

    fun calculateCashFlowHistory(
        transactions: List<TransactionEntity>,
        numMonths: Int = 6
    ): CashFlowSummary {
        val monthFmt = SimpleDateFormat("MMM", Locale.ENGLISH)
        val fullMonthFmt = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        val monthlyList = mutableListOf<CashFlowMonthData>()

        for (i in (numMonths - 1) downTo 0) {
            val startCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = startCal.timeInMillis
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val monthTxs = transactions.filter {
                !isTransfer(it) && it.timestamp in startCal.timeInMillis..endCal.timeInMillis
            }
            val expTxs = monthTxs.filter { isExpense(it) }
            val incTxs = monthTxs.filter { isIncome(it) }
            val inc = incTxs.sumOf { it.amount }
            val exp = expTxs.sumOf { it.amount }
            val net = inc - exp
            val savingsRate = if (inc > 0.0) ((net / inc) * 100.0) else 0.0

            val topExp = expTxs.groupBy { it.category.ifBlank { "Uncategorized" } }
                .mapValues { e -> e.value.sumOf { it.amount } }
                .maxByOrNull { it.value }
            val topInc = incTxs.groupBy { it.category.ifBlank { "Income" } }
                .mapValues { e -> e.value.sumOf { it.amount } }
                .maxByOrNull { it.value }

            monthlyList.add(
                CashFlowMonthData(
                    monthLabel = monthFmt.format(startCal.time),
                    fullMonthLabel = fullMonthFmt.format(startCal.time),
                    year = startCal.get(Calendar.YEAR),
                    income = inc,
                    expense = exp,
                    net = net,
                    savingsRate = savingsRate,
                    highestExpenseCategory = topExp?.key,
                    highestExpenseAmount = topExp?.value ?: 0.0,
                    highestIncomeCategory = topInc?.key,
                    highestIncomeAmount = topInc?.value ?: 0.0
                )
            )
        }

        val totalInc = monthlyList.sumOf { it.income }
        val totalExp = monthlyList.sumOf { it.expense }
        val netFlow = totalInc - totalExp
        val overallRate = if (totalInc > 0.0) ((netFlow / totalInc) * 100.0) else 0.0

        return CashFlowSummary(
            monthlyData = monthlyList,
            totalIncome = totalInc,
            totalExpense = totalExp,
            netCashFlow = netFlow,
            savingsAmount = netFlow.coerceAtLeast(0.0),
            overallSavingsRate = overallRate
        )
    }

    fun calculateTopMerchants(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        limit: Int = 5
    ): List<MerchantExpenseItem> {
        val expenses = transactions.filter {
            !isTransfer(it) && isExpense(it) && it.timestamp in dateRange.startMillis..dateRange.endMillis
        }
        return expenses
            .groupBy { it.title.trim().ifBlank { it.category.ifBlank { "General Expense" } } }
            .map { (name, txs) ->
                MerchantExpenseItem(
                    merchantName = name,
                    amount = txs.sumOf { it.amount },
                    count = txs.size
                )
            }
            .sortedByDescending { it.amount }
            .take(limit)
    }

    fun calculatePeriodComparison(
        transactions: List<TransactionEntity>,
        currentRange: InsightsDateRange
    ): PeriodComparisonData {
        val duration = (currentRange.endMillis - currentRange.startMillis + 1L).coerceAtLeast(1L)
        val prevEnd = currentRange.startMillis - 1L
        val prevStart = prevEnd - duration + 1L

        val currTxs = transactions.filter {
            !isTransfer(it) && it.timestamp in currentRange.startMillis..currentRange.endMillis
        }
        val prevTxs = transactions.filter {
            !isTransfer(it) && it.timestamp in prevStart..prevEnd
        }

        val currInc = currTxs.filter { isIncome(it) }.sumOf { it.amount }
        val prevInc = prevTxs.filter { isIncome(it) }.sumOf { it.amount }
        val currExp = currTxs.filter { isExpense(it) }.sumOf { it.amount }
        val prevExp = prevTxs.filter { isExpense(it) }.sumOf { it.amount }

        val incDiff = currInc - prevInc
        val incPct = if (prevInc > 0.0) ((incDiff / prevInc) * 100.0) else if (currInc > 0.0) 100.0 else 0.0

        val expDiff = currExp - prevExp
        val expPct = if (prevExp > 0.0) ((expDiff / prevExp) * 100.0) else if (currExp > 0.0) 100.0 else 0.0

        val currNet = currInc - currExp
        val prevNet = prevInc - prevExp
        val netDiff = currNet - prevNet
        val netPct = if (abs(prevNet) > 0.0) ((netDiff / abs(prevNet)) * 100.0) else if (currNet != 0.0) 100.0 else 0.0

        return PeriodComparisonData(
            currentIncome = currInc,
            previousIncome = prevInc,
            incomeDiff = incDiff,
            incomeChangePct = incPct,
            currentExpense = currExp,
            previousExpense = prevExp,
            expenseDiff = expDiff,
            expenseChangePct = expPct,
            currentNet = currNet,
            previousNet = prevNet,
            netDiff = netDiff,
            netChangePct = netPct
        )
    }

    fun calculateFinancialHealth(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        dateRange: InsightsDateRange,
        language: AppLanguage = AppLanguage.ENGLISH
    ): FinancialHealthData {
        val periodTxs = transactions.filter {
            !isTransfer(it) && it.timestamp in dateRange.startMillis..dateRange.endMillis
        }
        val inc = periodTxs.filter { isIncome(it) }.sumOf { it.amount }
        val exp = periodTxs.filter { isExpense(it) }.sumOf { it.amount }
        val net = inc - exp

        if (inc <= 0.0 && exp <= 0.0) {
            return FinancialHealthData(
                score = 50,
                rating = "Good",
                ratingBn = "ভালো",
                totalIncome = 0.0,
                totalExpenses = 0.0,
                netSavings = 0.0,
                savingsRate = null,
                expenseToIncomeRatio = null,
                budgetAdherencePct = null,
                explanation = "Log income and expense transactions in this period to evaluate your financial health score.",
                explanationBn = "নির্দিষ্ট সময়ের আর্থিক স্বাস্থ্য স্কোর দেখতে আয় ও ব্যয়ের তথ্য যোগ করুন।"
            )
        }

        val savingsRate = if (inc > 0.0) ((net / inc) * 100.0) else null
        val expToInc = if (inc > 0.0) ((exp / inc) * 100.0) else null

        val adherencePct: Double? = if (budgets.isNotEmpty()) {
            val withinCount = budgets.count { b ->
                val catSpent = periodTxs.filter {
                    isExpense(it) && it.category.equals(b.categoryName, ignoreCase = true)
                }.sumOf { it.amount }
                catSpent <= b.monthlyLimit
            }
            (withinCount.toDouble() / budgets.size.toDouble()) * 100.0
        } else {
            null
        }

        val rawScore = when {
            inc <= 0.0 && exp > 0.0 -> 25
            savingsRate != null -> {
                val base = (50.0 + savingsRate * 0.9).roundToInt()
                val bonus = if ((adherencePct ?: 100.0) >= 80.0) 8 else 0
                (base + bonus).coerceIn(10, 100)
            }
            else -> 50
        }

        val (rating, ratingBn) = when {
            rawScore >= 80 -> "Excellent" to "চমৎকার"
            rawScore >= 60 -> "Good" to "ভালো"
            rawScore >= 40 -> "Fair" to "মোটামুটি"
            else -> "Needs Improvement" to "উন্নতি প্রয়োজন"
        }

        val (explanation, explanationBn) = when {
            net < 0 -> (
                "Expenses exceed income for this period (negative net savings). Consider cutting back on discretionary spending." to
                    "এই মেয়াদে ব্যয় আয়ের চেয়ে বেশি (ঋণাত্মক সঞ্চয়)। অপ্রয়োজনীয় ব্যয় কমানোর পরামর্শ দেওয়া হচ্ছে।"
                )
            rawScore >= 80 -> (
                "Strong savings rate and spending comfortably within limits." to
                    "উচ্চ সঞ্চয় হার এবং খরচের চমৎকার নিয়ন্ত্রণ রয়েছে।"
                )
            rawScore >= 60 -> (
                "Balanced cash flow with healthy budget control." to
                    "স্থিতিশীল ক্যাশ ফ্লো এবং স্বাস্থ্যকর বাজেট অনুপাত।"
                )
            rawScore >= 40 -> (
                "Spending is close to earnings; watch discretionary expenses." to
                    "আয়ের বড় অংশ ব্যয় হচ্ছে; খরচের খাতগুলো পর্যালোচনা করুন।"
                )
            else -> (
                "Outflows exceed or closely match inflows. Consider reviewing your top categories." to
                    "ব্যয় আয়ের সমতুল্য বা বেশি হচ্ছে। শীর্ষ খরচ কমানোর চেষ্টা করুন।"
                )
        }

        return FinancialHealthData(
            score = rawScore,
            rating = rating,
            ratingBn = ratingBn,
            totalIncome = inc,
            totalExpenses = exp,
            netSavings = net,
            savingsRate = savingsRate,
            expenseToIncomeRatio = expToInc,
            budgetAdherencePct = adherencePct,
            explanation = explanation,
            explanationBn = explanationBn
        )
    }

    fun generateSmartInsights(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        categories: List<CategoryEntity>,
        currencySymbol: String,
        language: AppLanguage = AppLanguage.ENGLISH
    ): List<SmartInsightItem> {
        val comparison = calculatePeriodComparison(transactions, dateRange)
        val catExpenses = calculateCategoryExpenses(transactions, dateRange, categories)
        val items = mutableListOf<SmartInsightItem>()

        if (comparison.currentExpense <= 0.0 && comparison.currentIncome <= 0.0) {
            items.add(
                SmartInsightItem(
                    title = "No Data Yet",
                    titleBn = "কোন তথ্য নেই",
                    description = "Add income and expense transactions in this period to see automated smart insights.",
                    descriptionBn = "এই সময়ের জন্য স্বয়ংক্রিয় তথ্য দেখতে লেনদেন যোগ করুন।",
                    isPositive = true
                )
            )
            return items
        }

        if (comparison.previousExpense > 0.0) {
            val expChange = comparison.expenseChangePct
            val pct = String.format(Locale.US, "%.1f", abs(expChange))
            if (expChange <= -1.0) {
                items.add(
                    SmartInsightItem(
                        title = "Spending Decreased",
                        titleBn = "ব্যয় হ্রাস পেয়েছে",
                        description = "Your spending decreased by $pct% compared with the previous period.",
                        descriptionBn = "আগের সময়ের তুলনায় আপনার খরচ ${CurrencyFormatter.localizeDigits(pct, AppLanguage.BANGLA)}% কমেছে।",
                        isPositive = true
                    )
                )
            } else if (expChange >= 1.0) {
                items.add(
                    SmartInsightItem(
                        title = "Spending Increased",
                        titleBn = "ব্যয় বৃদ্ধি পেয়েছে",
                        description = "Your spending is up by $pct% compared with the previous period.",
                        descriptionBn = "আগের সময়ের চেয়ে আপনার ব্যয় ${CurrencyFormatter.localizeDigits(pct, AppLanguage.BANGLA)}% বৃদ্ধি পেয়েছে।",
                        isPositive = false
                    )
                )
            }
        }

        val top = catExpenses.firstOrNull()
        if (top != null) {
            val amtFmt = "$currencySymbol${String.format(Locale.US, "%.0f", top.amount)}"
            val pctFmt = String.format(Locale.US, "%.0f", top.percentage)
            items.add(
                SmartInsightItem(
                    title = "Highest Spending Category",
                    titleBn = "সর্বোচ্চ খরচের খাত",
                    description = "Your highest spending category is ${top.categoryName} at $amtFmt ($pctFmt% of total).",
                    descriptionBn = "আপনার সর্বোচ্চ খরচের খাত হলো ${top.categoryName} ($amtFmt, মোট ব্যয়ের ${CurrencyFormatter.localizeDigits(pctFmt, AppLanguage.BANGLA)}%)।",
                    isPositive = top.percentage < 50f
                )
            )
        }

        if (comparison.currentIncome > 0.0) {
            val savingsRate = ((comparison.currentNet / comparison.currentIncome) * 100.0)
            val sFmt = String.format(Locale.US, "%.1f", savingsRate)
            items.add(
                SmartInsightItem(
                    title = "Savings Performance",
                    titleBn = "সঞ্চয় অগ্রগতি",
                    description = "Your savings rate is $sFmt% for the selected period.",
                    descriptionBn = "নির্বাচিত সময়ের জন্য আপনার সঞ্চয় হার ${CurrencyFormatter.localizeDigits(sFmt, AppLanguage.BANGLA)}%।",
                    isPositive = savingsRate >= 20.0
                )
            )
        }

        return items
    }

    fun escapeCsvField(raw: String): String {
        val needsQuotes = raw.contains(',') || raw.contains('"') || raw.contains('\n') || raw.contains('\r')
        if (!needsQuotes) return raw
        return "\"${raw.replace("\"", "\"\"")}\""
    }

    fun filterTransactionsForExport(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        filterContext: ExportFilterContext? = null
    ): List<TransactionEntity> {
        val base = if (filterContext?.isPreFiltered == true) {
            transactions
        } else {
            transactions.filter { it.timestamp in dateRange.startMillis..dateRange.endMillis }
        }
        if (filterContext == null) return base.sortedByDescending { it.timestamp }

        return base.filter { tx ->
            val catOk = filterContext.categoryFilter.isNullOrBlank() ||
                tx.category.equals(filterContext.categoryFilter, ignoreCase = true)
            val subOk = filterContext.subcategoryFilter.isNullOrBlank() ||
                (tx.subcategory ?: "").equals(filterContext.subcategoryFilter, ignoreCase = true)
            val queryOk = filterContext.searchQuery.isNullOrBlank() ||
                tx.title.contains(filterContext.searchQuery, ignoreCase = true) ||
                tx.category.contains(filterContext.searchQuery, ignoreCase = true) ||
                (tx.notes ?: "").contains(filterContext.searchQuery, ignoreCase = true)
            catOk && subOk && queryOk
        }.sortedByDescending { it.timestamp }
    }

    fun getExportableTransactionsForScope(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        scope: ExportScope,
        filterContext: ExportFilterContext? = null
    ): List<TransactionEntity> {
        val filtered = filterTransactionsForExport(transactions, dateRange, filterContext)
        return when (scope) {
            ExportScope.EXPENSES_BY_CATEGORY -> filtered.filter { !isTransfer(it) && isExpense(it) }
            ExportScope.TRANSACTIONS_IN_DATE_RANGE -> filtered
        }
    }

    fun hasExportableData(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        scope: ExportScope,
        filterContext: ExportFilterContext? = null
    ): Boolean = getExportableTransactionsForScope(transactions, dateRange, scope, filterContext).isNotEmpty()

    fun buildExportFileName(
        scope: ExportScope,
        periodLabel: String,
        format: ExportFormat
    ): String {
        val cleanPeriod = periodLabel.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifBlank { "Report" }
        val formatTag = if (format == ExportFormat.EXCEL) "_Excel" else ""
        return "HishabPati_${scope.fileSlug}_${cleanPeriod}$formatTag.${format.extension}"
    }

    fun buildCsvReport(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        currencySymbol: String,
        scope: ExportScope,
        filterContext: ExportFilterContext? = null,
        accounts: List<AccountEntity> = emptyList(),
        includeUtf8Bom: Boolean = false
    ): String {
        val sb = StringBuilder()
        if (includeUtf8Bom) {
            sb.append('\uFEFF')
        }
        val scopedTxs = getExportableTransactionsForScope(transactions, dateRange, scope, filterContext)
        val accountMap = accounts.associateBy({ it.id }, { it.name })

        if (scope == ExportScope.EXPENSES_BY_CATEGORY) {
            sb.append("Category,Subcategories,Transactions,Amount ($currencySymbol),Percentage (%)\n")
            val total = scopedTxs.sumOf { it.amount }
            val grouped = scopedTxs.groupBy { it.category.ifBlank { "Uncategorized" } }
                .entries
                .sortedByDescending { e -> e.value.sumOf { it.amount } }
            for ((cat, list) in grouped) {
                val sum = list.sumOf { it.amount }
                val pct = if (total > 0.0) (sum / total) * 100.0 else 0.0
                val subs = list.mapNotNull { it.subcategory?.takeIf { s -> s.isNotBlank() } }
                    .distinct()
                    .joinToString("; ")
                sb.append(
                    listOf(
                        escapeCsvField(cat),
                        escapeCsvField(subs),
                        list.size.toString(),
                        String.format(Locale.US, "%.2f", sum),
                        String.format(Locale.US, "%.2f%%", pct)
                    ).joinToString(",")
                ).append('\n')
            }
            sb.append(
                listOf(
                    "Total",
                    "All matching expense categories",
                    scopedTxs.size.toString(),
                    String.format(Locale.US, "%.2f", total),
                    "100.00%"
                ).joinToString(",")
            ).append('\n')
        } else {
            sb.append("Date,Type,Title,Category,Subcategory,Wallet,Amount ($currencySymbol),Notes\n")
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
            for (tx in scopedTxs) {
                val walletName = accountMap[tx.accountId] ?: tx.accountId
                sb.append(
                    listOf(
                        escapeCsvField(df.format(Date(tx.timestamp))),
                        escapeCsvField(tx.type.name),
                        escapeCsvField(tx.title),
                        escapeCsvField(tx.category),
                        escapeCsvField(tx.subcategory ?: ""),
                        escapeCsvField(walletName),
                        String.format(Locale.US, "%.2f", tx.amount),
                        escapeCsvField(tx.notes ?: "")
                    ).joinToString(",")
                ).append('\n')
            }
        }
        return sb.toString()
    }

    fun buildSummaryTextReport(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        currencySymbol: String,
        categories: List<CategoryEntity> = emptyList(),
        scope: ExportScope = ExportScope.EXPENSES_BY_CATEGORY,
        filterContext: ExportFilterContext? = null,
        accounts: List<AccountEntity> = emptyList()
    ): String {
        val scopedTxs = getExportableTransactionsForScope(transactions, dateRange, scope, filterContext)
        val sb = StringBuilder()
        val periodTitle = filterContext?.periodLabel ?: dateRange.formattedDisplay
        sb.appendLine("Hishab Pati — ${scope.title}")
        sb.appendLine("Period: $periodTitle")
        sb.appendLine("Total Matching Records: ${scopedTxs.size}")
        sb.appendLine("----------------------------------------")

        if (scope == ExportScope.EXPENSES_BY_CATEGORY) {
            val total = scopedTxs.sumOf { it.amount }
            sb.appendLine("Total Expenses: $currencySymbol${String.format(Locale.US, "%.2f", total)}")
            sb.appendLine()
            val grouped = scopedTxs.groupBy { it.category.ifBlank { "Uncategorized" } }
                .entries
                .sortedByDescending { e -> e.value.sumOf { it.amount } }
            grouped.forEachIndexed { idx, (cat, list) ->
                val sum = list.sumOf { it.amount }
                val pct = if (total > 0.0) (sum / total) * 100.0 else 0.0
                sb.appendLine("${idx + 1}. $cat: $currencySymbol${String.format(Locale.US, "%.2f", sum)} (${String.format(Locale.US, "%.1f", pct)}%, ${list.size} tx)")
            }
        } else {
            val accountMap = accounts.associateBy({ it.id }, { it.name })
            val df = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            for (tx in scopedTxs) {
                val walletName = accountMap[tx.accountId] ?: "Wallet"
                sb.appendLine("${df.format(Date(tx.timestamp))} | ${tx.type.name} | ${tx.title.ifBlank { tx.category }} | $walletName | $currencySymbol${String.format(Locale.US, "%.2f", tx.amount)}")
            }
        }
        return sb.toString().trimEnd()
    }

    fun generatePdfReportBytes(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        currencySymbol: String,
        categories: List<CategoryEntity> = emptyList(),
        scope: ExportScope = ExportScope.EXPENSES_BY_CATEGORY,
        filterContext: ExportFilterContext? = null,
        accounts: List<AccountEntity> = emptyList()
    ): ByteArray {
        val summary = buildSummaryTextReport(
            transactions = transactions,
            dateRange = dateRange,
            currencySymbol = currencySymbol,
            categories = categories,
            scope = scope,
            filterContext = filterContext,
            accounts = accounts
        )
        return buildStandardPdf14Bytes(summary.lines())
    }

    private fun escapePdfLiteral(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            when {
                ch == '\\' || ch == '(' || ch == ')' -> {
                    sb.append('\\').append(ch)
                }
                ch.code in 32..126 -> sb.append(ch)
                else -> sb.append('?')
            }
        }
        return sb.toString()
    }

    private fun buildStandardPdf14Bytes(lines: List<String>): ByteArray {
        val streamBuilder = StringBuilder()
        streamBuilder.append("BT\n/F1 10 Tf\n50 760 Td\n14 TL\n")
        for (line in lines.take(48)) {
            streamBuilder.append("(${escapePdfLiteral(line)}) Tj T*\n")
        }
        streamBuilder.append("ET\n")
        val contentBytes = streamBuilder.toString().toByteArray(Charsets.US_ASCII)

        val out = ByteArrayOutputStream()
        fun writeAscii(s: String) {
            out.write(s.toByteArray(Charsets.US_ASCII))
        }

        val offsets = IntArray(6)
        writeAscii("%PDF-1.4\n")
        offsets[1] = out.size()
        writeAscii("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n")
        offsets[2] = out.size()
        writeAscii("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n")
        offsets[3] = out.size()
        writeAscii("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n")
        offsets[4] = out.size()
        writeAscii("4 0 obj << /Length ${contentBytes.size} >> stream\n")
        out.write(contentBytes)
        writeAscii("endstream endobj\n")
        offsets[5] = out.size()
        writeAscii("5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n")

        val xrefOffset = out.size()
        writeAscii("xref\n0 6\n0000000000 65535 f \n")
        for (i in 1..5) {
            writeAscii(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
        }
        writeAscii("trailer << /Size 6 /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")
        return out.toByteArray()
    }

    fun calculateCategoryIncomes(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        categories: List<CategoryEntity> = emptyList()
    ): List<CategoryExpenseItem> {
        val incomes = transactions.filter {
            !isTransfer(it) && isIncome(it) && it.timestamp in dateRange.startMillis..dateRange.endMillis
        }
        val total = incomes.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        return incomes
            .groupBy { it.category.ifBlank { "Uncategorized Income" } }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { it.amount } }
            .mapIndexed { idx, (catName, txs) ->
                val sum = txs.sumOf { it.amount }
                val pct = ((sum / total) * 100.0).toFloat()
                CategoryExpenseItem(
                    categoryName = catName,
                    amount = sum,
                    percentage = pct,
                    count = txs.size,
                    color = getCategoryColor(catName, categories, idx)
                )
            }
    }

    fun calculateSubcategoryIncomes(
        transactions: List<TransactionEntity>,
        categoryName: String,
        dateRange: InsightsDateRange,
        subcategories: List<SubcategoryEntity> = emptyList()
    ): List<SubcategoryExpenseItem> {
        val matching = transactions.filter {
            !isTransfer(it) &&
                isIncome(it) &&
                it.timestamp in dateRange.startMillis..dateRange.endMillis &&
                it.category.equals(categoryName, ignoreCase = true)
        }
        val total = matching.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        return matching
            .groupBy { it.subcategory?.takeIf { s -> s.isNotBlank() } ?: "Income" }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { it.amount } }
            .mapIndexed { idx, (subName, txs) ->
                val sum = txs.sumOf { it.amount }
                val pct = ((sum / total) * 100.0).toFloat()
                SubcategoryExpenseItem(
                    subcategoryName = subName,
                    amount = sum,
                    percentageOfCategory = pct,
                    count = txs.size,
                    color = subcategoryPalette[idx % subcategoryPalette.size]
                )
            }
    }

    fun calculateIncomeTrend(
        transactions: List<TransactionEntity>,
        period: InsightsPeriod,
        dateRange: InsightsDateRange
    ): SpendingTrendSummary {
        val incomes = transactions.filter {
            !isTransfer(it) && isIncome(it) && it.timestamp in dateRange.startMillis..dateRange.endMillis
        }
        val totalIncome = incomes.sumOf { it.amount }
        val points = mutableListOf<SpendingTrendPoint>()

        when (period) {
            InsightsPeriod.DAILY -> {
                val hourFormat = SimpleDateFormat("h a", Locale.ENGLISH)
                val stepMillis = 4L * 3600L * 1000L
                for (i in 0 until 6) {
                    val s = dateRange.startMillis + i * stepMillis
                    val e = if (i == 5) dateRange.endMillis else (s + stepMillis - 1)
                    val blockIncomes = incomes.filter { it.timestamp in s..e }.sumOf { it.amount }
                    val label = hourFormat.format(Date(s))
                    points.add(SpendingTrendPoint(label = label, amount = blockIncomes, timestamp = s))
                }
            }
            InsightsPeriod.THIS_YEAR, InsightsPeriod.LAST_YEAR -> {
                val cal = Calendar.getInstance().apply { timeInMillis = dateRange.startMillis }
                val monthFmt = SimpleDateFormat("MMM", Locale.ENGLISH)
                for (m in 0 until 12) {
                    cal.set(Calendar.MONTH, m)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val s = cal.timeInMillis
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val e = cal.timeInMillis
                    val sum = incomes.filter { it.timestamp in s..e }.sumOf { it.amount }
                    points.add(SpendingTrendPoint(label = monthFmt.format(Date(s)), amount = sum, timestamp = s))
                }
            }
            else -> {
                val dayFmt = if (period == InsightsPeriod.THIS_WEEK) {
                    SimpleDateFormat("EEE", Locale.ENGLISH)
                } else {
                    SimpleDateFormat("dd/MM", Locale.ENGLISH)
                }
                val cal = Calendar.getInstance().apply { timeInMillis = dateRange.startMillis }
                while (cal.timeInMillis <= dateRange.endMillis) {
                    val s = cal.timeInMillis
                    val endOfDay = Calendar.getInstance().apply {
                        timeInMillis = s
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    val sum = incomes.filter { it.timestamp in s..endOfDay }.sumOf { it.amount }
                    points.add(SpendingTrendPoint(label = dayFmt.format(Date(s)), amount = sum, timestamp = s))
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        val maxPt = points.maxByOrNull { it.amount }
        val dayCount = (((dateRange.endMillis - dateRange.startMillis) / (24L * 3600L * 1000L)).toInt() + 1).coerceAtLeast(1)
        return SpendingTrendSummary(
            points = points,
            totalExpense = totalIncome,
            highestDayLabel = if ((maxPt?.amount ?: 0.0) > 0.0) maxPt?.label ?: "-" else "-",
            highestDayAmount = maxPt?.amount ?: 0.0,
            averageDailySpending = totalIncome / dayCount
        )
    }

    fun calculateTopIncomePayers(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        limit: Int = 5
    ): List<MerchantExpenseItem> {
        val incomes = transactions.filter {
            !isTransfer(it) && isIncome(it) && it.timestamp in dateRange.startMillis..dateRange.endMillis
        }
        return incomes
            .groupBy { it.title.trim().ifBlank { it.category.ifBlank { "Income" } } }
            .map { (name, txs) ->
                MerchantExpenseItem(
                    merchantName = name,
                    amount = txs.sumOf { it.amount },
                    count = txs.size
                )
            }
            .sortedByDescending { it.amount }
            .take(limit)
    }

    fun generateSmartIncomeInsights(
        transactions: List<TransactionEntity>,
        dateRange: InsightsDateRange,
        categories: List<CategoryEntity>,
        currencySymbol: String,
        language: AppLanguage = AppLanguage.ENGLISH
    ): List<SmartInsightItem> {
        val comparison = calculatePeriodComparison(transactions, dateRange)
        val catIncomes = calculateCategoryIncomes(transactions, dateRange, categories)
        val items = mutableListOf<SmartInsightItem>()

        if (comparison.currentIncome <= 0.0) {
            items.add(
                SmartInsightItem(
                    title = "No Data Yet",
                    titleBn = "কোন তথ্য নেই",
                    description = "Add income transactions in this period to see automated income insights.",
                    descriptionBn = "এই সময়ের জন্য স্বয়ংক্রিয় আয়ের তথ্য দেখতে লেনদেন যোগ করুন।",
                    isPositive = true
                )
            )
            return items
        }

        if (comparison.previousIncome > 0.0) {
            val incChange = comparison.incomeChangePct
            val pct = String.format(Locale.US, "%.1f", abs(incChange))
            if (incChange >= 1.0) {
                items.add(
                    SmartInsightItem(
                        title = "Income Increased",
                        titleBn = "আয় বৃদ্ধি পেয়েছে",
                        description = "Your income increased by $pct% compared with the previous period.",
                        descriptionBn = "আগের সময়ের তুলনায় আপনার আয় ${CurrencyFormatter.localizeDigits(pct, AppLanguage.BANGLA)}% বৃদ্ধি পেয়েছে।",
                        isPositive = true
                    )
                )
            } else if (incChange <= -1.0) {
                items.add(
                    SmartInsightItem(
                        title = "Income Decreased",
                        titleBn = "আয় হ্রাস পেয়েছে",
                        description = "Your income decreased by $pct% compared with the previous period.",
                        descriptionBn = "আগের সময়ের তুলনায় আপনার আয় ${CurrencyFormatter.localizeDigits(pct, AppLanguage.BANGLA)}% হ্রাস পেয়েছে।",
                        isPositive = false
                    )
                )
            }
        }

        val top = catIncomes.firstOrNull()
        if (top != null) {
            val amtFmt = "$currencySymbol${String.format(Locale.US, "%.0f", top.amount)}"
            val pctFmt = String.format(Locale.US, "%.0f", top.percentage)
            items.add(
                SmartInsightItem(
                    title = "Top Income Source",
                    titleBn = "প্রধান আয়ের উৎস",
                    description = "Your primary income source is ${top.categoryName} at $amtFmt ($pctFmt% of total).",
                    descriptionBn = "আপনার প্রধান আয়ের উৎস হলো ${top.categoryName} ($amtFmt, মোট আয়ের ${CurrencyFormatter.localizeDigits(pctFmt, AppLanguage.BANGLA)}%)।",
                    isPositive = true
                )
            )
        }

        return items
    }
}
