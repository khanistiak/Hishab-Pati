package com.example.ui.screens.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.FinancialIntegrityValidator
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.ActivityFilterType
import com.example.ui.InsightsPeriod
import com.example.ui.components.TranslucentCard
import com.example.ui.components.getIconForName
import com.example.ui.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

private val RoyalIndigo = Color(0xFF4F46E5)
private val ElectricViolet = Color(0xFF7C3AED)
private val SoftViolet = Color(0xFF6366F1)
private val CashFlowIncomeEmerald = Color(0xFF10B981)
private val CashFlowExpenseRose = Color(0xFFF43F5E)

private fun indigoVioletGradient(): Brush = Brush.linearGradient(
    colors = listOf(RoyalIndigo, ElectricViolet)
)

private data class DonutSliceArc(
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color,
    val strokeWidth: Float,
    val isSelected: Boolean
)

/**
 * 1. Period Controls: Royal Indigo-to-Electric Violet gradient active chips + Frosted Glass date capsule (< Month >).
 * Highlights a quick-filter chip ONLY when it accurately matches the active calendar range (Last Month, This Month, etc.).
 */
@Composable
fun ReportPeriodControls(
    selectedPeriod: InsightsPeriod,
    periodOffset: Int,
    dateRange: InsightsDateRange,
    language: AppLanguage,
    onPeriodSelected: (InsightsPeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val displayedPeriods = listOf(
        InsightsPeriod.DAILY,
        InsightsPeriod.THIS_WEEK,
        InsightsPeriod.THIS_MONTH,
        InsightsPeriod.LAST_MONTH
    )

    // Determine whether the user has stepped away from a quick preset into a custom navigated month/period
    val isCustomNavigatedPeriod = selectedPeriod == InsightsPeriod.CUSTOM ||
        (periodOffset != 0 && !(selectedPeriod == InsightsPeriod.THIS_MONTH && periodOffset == -1))

    val glassFill = if (isDark) Color(0xFF1E293B).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f)
    val glassBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.82f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("report_period_controls")
    ) {
        // Period filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayedPeriods) { p ->
                val isSelected = when (p) {
                    InsightsPeriod.LAST_MONTH ->
                        (selectedPeriod == InsightsPeriod.LAST_MONTH && periodOffset == 0) ||
                            (selectedPeriod == InsightsPeriod.THIS_MONTH && periodOffset == -1)
                    InsightsPeriod.THIS_MONTH ->
                        selectedPeriod == InsightsPeriod.THIS_MONTH && periodOffset == 0
                    else ->
                        selectedPeriod == p && periodOffset == 0
                }
                val chipLabel = if (language == AppLanguage.BANGLA) p.bnLabel else p.label
                val chipShape = CircleShape

                Box(
                    modifier = Modifier
                        .then(
                            if (isSelected) {
                                Modifier.shadow(
                                    elevation = 6.dp,
                                    shape = chipShape,
                                    ambientColor = ElectricViolet.copy(alpha = 0.28f),
                                    spotColor = ElectricViolet.copy(alpha = 0.42f)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clip(chipShape)
                        .then(
                            if (isSelected) {
                                Modifier.background(indigoVioletGradient(), chipShape)
                            } else {
                                Modifier
                                    .background(glassFill, chipShape)
                                    .border(1.dp, glassBorder, chipShape)
                            }
                        )
                        .clickable { onPeriodSelected(p) }
                        .padding(horizontal = 15.dp, vertical = 8.dp)
                        .testTag("period_chip_${p.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chipLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Floating Frosted Glass Date Selector (< November 2026 >)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(glassFill, CircleShape)
                    .border(1.dp, glassBorder, CircleShape)
                    .clickable(onClick = onPreviousPeriod)
                    .testTag("period_prev_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Period",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            val dateBorderColor = if (isCustomNavigatedPeriod) {
                SoftViolet.copy(alpha = if (isDark) 0.55f else 0.45f)
            } else {
                glassBorder
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(glassFill, CircleShape)
                    .border(1.dp, dateBorderColor, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 7.dp)
                    .testTag("period_date_capsule")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFA5B4FC) else RoyalIndigo,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (language == AppLanguage.BANGLA) dateRange.formattedDisplayBn else dateRange.formattedDisplay,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            val canGoForward = (periodOffset < 0 || selectedPeriod == InsightsPeriod.LAST_MONTH) &&
                selectedPeriod != InsightsPeriod.CUSTOM
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (canGoForward) glassFill else glassFill.copy(alpha = 0.35f),
                        CircleShape
                    )
                    .border(1.dp, glassBorder, CircleShape)
                    .clickable(enabled = canGoForward, onClick = onNextPeriod)
                    .testTag("period_next_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Period",
                    tint = if (canGoForward) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 2. Financial Overview Card: 20.dp Bento with subtle glass rim, vertical hairline dividers,
 * FontWeight.ExtraBold tabular figures, and compact period comparison badge.
 */
@Composable
fun FinancialOverviewCard(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val comparison = remember(transactions, dateRange) {
        InsightsCalculator.calculatePeriodComparison(transactions, dateRange)
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isBn = language == AppLanguage.BANGLA
    val hairlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.28f else 0.42f)

    // Summary comparison badge prioritizing net cash flow delta (fallback to expense delta)
    val headlineDelta: Pair<Double, Boolean>? = when {
        abs(comparison.previousNet) > 0.0 -> comparison.netChangePct to (comparison.netChangePct >= 0)
        comparison.previousExpense > 0.0 -> comparison.expenseChangePct to (comparison.expenseChangePct <= 0)
        else -> null
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("financial_overview_card"),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "আর্থিক সারসংক্ষেপ" else "Financial Overview",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                if (headlineDelta != null) {
                    val (pctVal, isFavorable) = headlineDelta
                    val badgeColor = if (isFavorable) CashFlowIncomeEmerald else CashFlowExpenseRose
                    val sign = if (pctVal > 0) "+" else if (pctVal < 0) "-" else ""
                    val roundedPct = abs(pctVal).roundToInt()
                    val localizedPct = if (isBn) {
                        CurrencyFormatter.convertToBanglaDigits(roundedPct.toString())
                    } else {
                        roundedPct.toString()
                    }
                    val badgeLabel = if (isBn) {
                        "$sign$localizedPct% পূর্বের তুলনায়"
                    } else {
                        "$sign$localizedPct% vs prev"
                    }
                    Surface(
                        shape = CircleShape,
                        color = badgeColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                        border = BorderStroke(0.75.dp, badgeColor.copy(alpha = 0.28f))
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                fontSize = 10.5.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverviewMetricColumn(
                    title = if (isBn) "মোট আয়" else "Income",
                    amount = comparison.currentIncome,
                    color = CashFlowIncomeEmerald,
                    icon = Icons.Default.TrendingUp,
                    changePct = if (comparison.previousIncome > 0) comparison.incomeChangePct else null,
                    isExpenseMetric = false,
                    currencySymbol = currencySymbol,
                    language = language,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .width(1.dp)
                        .height(44.dp)
                        .background(hairlineColor)
                )

                OverviewMetricColumn(
                    title = if (isBn) "মোট ব্যয়" else "Expenses",
                    amount = comparison.currentExpense,
                    color = CashFlowExpenseRose,
                    icon = Icons.Default.TrendingDown,
                    changePct = if (comparison.previousExpense > 0) comparison.expenseChangePct else null,
                    isExpenseMetric = true,
                    currencySymbol = currencySymbol,
                    language = language,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .width(1.dp)
                        .height(44.dp)
                        .background(hairlineColor)
                )

                val netColor = if (comparison.currentNet >= 0) CashFlowIncomeEmerald else CashFlowExpenseRose
                OverviewMetricColumn(
                    title = if (isBn) "নিট ফ্লো" else "Net Flow",
                    amount = comparison.currentNet,
                    color = netColor,
                    icon = Icons.Default.AccountBalanceWallet,
                    changePct = if (abs(comparison.previousNet) > 0) comparison.netChangePct else null,
                    isExpenseMetric = false,
                    showSign = true,
                    currencySymbol = currencySymbol,
                    language = language,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OverviewMetricColumn(
    title: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    changePct: Double?,
    isExpenseMetric: Boolean,
    currencySymbol: String,
    language: AppLanguage,
    showSign: Boolean = false,
    modifier: Modifier = Modifier
) {
    val formatted = CurrencyFormatter.format(amount, currencySymbol, showSign = showSign, language = language)
    var fontSizeSp by remember(formatted) { mutableFloatStateOf(15.5f) }
    val isBn = language == AppLanguage.BANGLA

    Column(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = formatted,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp + 3f).sp,
                color = color,
                letterSpacing = (-0.3).sp,
                fontFeatureSettings = "tnum"
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (result.didOverflowWidth && fontSizeSp > 11.5f) {
                    fontSizeSp = (fontSizeSp - 0.5f).coerceAtLeast(11.5f)
                }
            }
        )

        Spacer(modifier = Modifier.height(3.dp))

        if (changePct != null) {
            val isFavorable = if (isExpenseMetric) changePct <= 0 else changePct >= 0
            val deltaColor = if (isFavorable) CashFlowIncomeEmerald else CashFlowExpenseRose
            val signStr = if (changePct > 0) "+" else if (changePct < 0) "-" else ""
            val rounded = abs(changePct).roundToInt()
            val numStr = if (isBn) CurrencyFormatter.convertToBanglaDigits(rounded.toString()) else rounded.toString()
            Text(
                text = if (isBn) "$signStr$numStr% পূর্বে" else "$signStr$numStr% vs prev",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = deltaColor,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = "—",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
                )
            )
        }
    }
}

/**
 * 3. Budget Status: Shown only if applicable budgets exist for the period.
 * Clearly displays a micro-subtitle ("Tracked budget categories only") when tracking specific category budgets
 * so the user immediately sees why budgeted spend differs from total expenses (e.g., Debt Repayment).
 */
@Composable
fun BudgetStatusCard(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    period: InsightsPeriod = InsightsPeriod.THIS_MONTH,
    dateRange: InsightsDateRange,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    if (period == InsightsPeriod.DAILY || period == InsightsPeriod.THIS_WEEK) {
        return
    }

    val monthYearStr = remember(dateRange) {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = dateRange.startMillis
        SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
    }

    val applicableBudgets = remember(budgets, monthYearStr) {
        budgets.filter { it.monthYear == "ALL" || it.monthYear == monthYearStr }
    }

    if (applicableBudgets.isEmpty()) {
        return
    }

    val overallBudget = remember(applicableBudgets) {
        applicableBudgets.find { it.isOverall }
    }

    val (budgetLimit, amountSpent) = remember(applicableBudgets, overallBudget, transactions, dateRange, monthYearStr) {
        val limit = FinancialIntegrityValidator.computeTotalMonthlyBudget(applicableBudgets, monthYearStr)

        val periodExpenses = transactions.filter {
            FinancialIntegrityValidator.isEligibleExpense(it) &&
                it.timestamp in dateRange.startMillis..dateRange.endMillis
        }

        val spent = if (overallBudget != null) {
            periodExpenses.sumOf { it.amount }
        } else {
            val categoryBudgets = applicableBudgets.filter { !it.isOverall }
            periodExpenses.filter { tx ->
                categoryBudgets.any { budget ->
                    FinancialIntegrityValidator.isMatchingBudgetTransaction(tx, budget)
                }
            }.sumOf { it.amount }
        }

        Pair(limit, spent)
    }

    if (budgetLimit <= 0) return

    val remaining = (budgetLimit - amountSpent).coerceAtLeast(0.0)
    val percentageUsed = ((amountSpent / budgetLimit) * 100).toFloat()
    val targetProgress = (amountSpent / budgetLimit).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "budget_status_progress"
    )
    val isOverBudget = amountSpent > budgetLimit

    val progressColor = when {
        isOverBudget -> CashFlowExpenseRose
        percentageUsed > 80f -> Color(0xFFF59E0B)
        else -> RoyalIndigo
    }
    val progressBrush = when {
        isOverBudget -> Brush.horizontalGradient(listOf(CashFlowExpenseRose, Color(0xFFFB7185)))
        percentageUsed > 80f -> Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)))
        else -> indigoVioletGradient()
    }

    val isBn = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pctStr = if (isBn) {
        "${CurrencyFormatter.convertToBanglaDigits(percentageUsed.roundToInt().toString())}% ব্যবহৃত"
    } else {
        "${percentageUsed.roundToInt()}% used"
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("budget_status_card"),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) "বাজেট অগ্রগতি" else "Budget Status",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (overallBudget == null) {
                            if (isBn) "শুধুমাত্র বাজেটভুক্ত খাতসমূহ" else "Tracked budget categories only"
                        } else {
                            if (isBn) "মাসিক সকল ব্যয় অন্তর্ভুক্ত" else "All monthly expenses tracked"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.testTag("budget_status_scope_subtitle")
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = progressColor.copy(alpha = if (isDark) 0.20f else 0.13f)
                ) {
                    Text(
                        text = if (isOverBudget) {
                            if (isBn) "সীমা অতিক্রান্ত" else "Over Budget"
                        } else {
                            pctStr
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark && !isOverBudget && percentageUsed <= 80f) Color(0xFFA5B4FC) else progressColor,
                            fontSize = 11.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isBn) "মোট বাজেট" else "Budget",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                        )
                    )
                    Text(
                        text = CurrencyFormatter.format(budgetLimit, currencySymbol, language = language),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isBn) "ব্যয় হয়েছে" else "Spent",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                        )
                    )
                    Text(
                        text = CurrencyFormatter.format(amountSpent, currencySymbol, language = language),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark && !isOverBudget && percentageUsed <= 80f) Color(0xFFA5B4FC) else progressColor,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isBn) "অবশিষ্ট" else "Remaining",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                        )
                    )
                    Text(
                        text = CurrencyFormatter.format(remaining, currencySymbol, language = language),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isOverBudget) CashFlowExpenseRose else CashFlowIncomeEmerald,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(4.5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedProgress)
                        .height(9.dp)
                        .clip(RoundedCornerShape(4.5.dp))
                        .background(progressBrush)
                )
            }
        }
    }
}

/**
 * 4. Spending Trend: Smooth cubic-bezier curve in Royal Indigo-to-Electric Violet gradient stroke (2.5.dp)
 * with soft vertical area fill, interactive touch/drag scrubber tooltip ("09/11 • ৳18,000"),
 * and Daily Average / Peak Spending metrics.
 */
@Composable
fun SpendingTrendCard(
    transactions: List<TransactionEntity>,
    period: InsightsPeriod,
    dateRange: InsightsDateRange,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val summary = remember(transactions, period, dateRange) {
        InsightsCalculator.calculateSpendingTrend(transactions, period, dateRange)
    }

    val points = summary.points
    val defaultIndex = remember(points) {
        if (points.isEmpty()) null
        else {
            val peakIdx = points.indices.maxByOrNull { points[it].amount }
            if (peakIdx != null && points[peakIdx].amount > 0) peakIdx else points.lastIndex
        }
    }

    var selectedIndex by remember(summary) {
        mutableStateOf(defaultIndex)
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isBn = language == AppLanguage.BANGLA
    val maxAmount = points.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    val accentTint = if (isDark) Color(0xFFA5B4FC) else RoyalIndigo

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("spending_trend_card"),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) "ব্যয় প্রবণতা" else "Spending Trend",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isBn) "খরচ দেখতে চার্টে ট্যাপ বা স্লাইড করুন" else "Tap or scrub along the curve to inspect",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                            fontSize = 11.sp
                        )
                    )
                }

                // Active scrubber tooltip pill ("09/11 • ৳18,000")
                val activePoint = selectedIndex?.let { points.getOrNull(it) }
                if (activePoint != null && summary.totalExpense > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(indigoVioletGradient(), CircleShape)
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                            .testTag("spending_trend_tooltip")
                    ) {
                        Text(
                            text = "${activePoint.label} • ${CurrencyFormatter.format(activePoint.amount, currencySymbol, language = language)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SoftViolet.copy(alpha = 0.14f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = accentTint,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Daily Average & Peak Spending Tiles
            val metricTileBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.52f) else Color.White.copy(alpha = 0.56f)
            val metricTileBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.80f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .background(metricTileBg)
                        .border(1.dp, metricTileBorder, RoundedCornerShape(13.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = if (isBn) "দৈনিক গড়" else "Daily Average",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(summary.averageDailySpending, currencySymbol, language = language),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .background(metricTileBg)
                        .border(1.dp, metricTileBorder, RoundedCornerShape(13.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = if (isBn) "সর্বোচ্চ ব্যয়" else "Peak Spending",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (summary.highestDayAmount > 0) {
                            "${summary.highestDayLabel} • ${CurrencyFormatter.format(summary.highestDayAmount, currencySymbol, language = language)}"
                        } else "—",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = CashFlowExpenseRose,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (points.size < 2 || summary.totalExpense <= 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBn) "এই সময়ের মধ্যে ব্যয়ের পর্যাপ্ত তথ্য নেই" else "No spending recorded for this period",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
            } else {
                val guideLineColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF94A3B8).copy(alpha = 0.22f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(138.dp)
                        .pointerInput(points) {
                            detectTapGestures { tapOffset ->
                                val stepX = size.width.toFloat() / (points.size - 1).coerceAtLeast(1)
                                val index = ((tapOffset.x + stepX / 2f) / stepX).toInt()
                                selectedIndex = index.coerceIn(0, points.lastIndex)
                            }
                        }
                        .pointerInput(points) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val stepX = size.width.toFloat() / (points.size - 1).coerceAtLeast(1)
                                val index = ((change.position.x + stepX / 2f) / stepX).toInt()
                                selectedIndex = index.coerceIn(0, points.lastIndex)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val topPad = 14.dp.toPx()
                        val bottomPad = 10.dp.toPx()
                        val availableH = (height - bottomPad).coerceAtLeast(24f)
                        val plotH = (availableH - topPad).coerceAtLeast(16f)
                        val stepX = width / (points.size - 1).coerceAtLeast(1)

                        // Subtle horizontal reference grid lines
                        for (g in 0..2) {
                            val gy = topPad + (plotH * g / 2f)
                            drawLine(
                                color = guideLineColor,
                                start = Offset(0f, gy),
                                end = Offset(width, gy),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val path = Path()
                        val fillPath = Path()

                        points.forEachIndexed { i, p ->
                            val x = i * stepX
                            val y = availableH - ((p.amount / maxAmount).toFloat() * plotH)

                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, availableH)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (i - 1) * stepX
                                val prevY = availableH - ((points[i - 1].amount / maxAmount).toFloat() * plotH)
                                val cx = (prevX + x) / 2f
                                path.cubicTo(cx, prevY, cx, y, x, y)
                                fillPath.cubicTo(cx, prevY, cx, y, x, y)
                            }

                            if (i == points.lastIndex) {
                                fillPath.lineTo(x, availableH)
                                fillPath.close()
                            }
                        }

                        // Soft vertical Indigo-to-Violet area fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SoftViolet.copy(alpha = if (isDark) 0.30f else 0.24f),
                                    ElectricViolet.copy(alpha = if (isDark) 0.10f else 0.07f),
                                    Color.Transparent
                                ),
                                startY = topPad,
                                endY = availableH
                            )
                        )

                        // Smooth 2.5.dp Royal Indigo-to-Electric Violet gradient curve
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(
                                colors = listOf(RoyalIndigo, ElectricViolet)
                            ),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Active touch scrubber indicator
                        selectedIndex?.let { selIdx ->
                            if (selIdx in points.indices) {
                                val pt = points[selIdx]
                                val ptX = selIdx * stepX
                                val ptY = availableH - ((pt.amount / maxAmount).toFloat() * plotH)

                                drawLine(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            ElectricViolet.copy(alpha = 0.55f),
                                            RoyalIndigo.copy(alpha = 0.12f)
                                        ),
                                        startY = ptY,
                                        endY = availableH
                                    ),
                                    start = Offset(ptX, ptY),
                                    end = Offset(ptX, availableH),
                                    strokeWidth = 1.5.dp.toPx()
                                )

                                drawCircle(
                                    color = ElectricViolet.copy(alpha = 0.26f),
                                    radius = 8.5.dp.toPx(),
                                    center = Offset(ptX, ptY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = Offset(ptX, ptY)
                                )
                                drawCircle(
                                    color = RoyalIndigo,
                                    radius = 3.2.dp.toPx(),
                                    center = Offset(ptX, ptY)
                                )
                            }
                        }
                    }
                }

                // Bottom axis labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val firstLabel = points.firstOrNull()?.label ?: ""
                    val midLabel = points.getOrNull(points.size / 2)?.label ?: ""
                    val lastLabel = points.lastOrNull()?.label ?: ""

                    Text(
                        text = firstLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontFeatureSettings = "tnum"
                        )
                    )
                    if (points.size > 2) {
                        Text(
                            text = midLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                    Text(
                        text = lastLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }
        }
    }
}

/**
 * 5. Category Breakdown (Expend-style Interactive Category-Wise Donut & Subcategory Breakdown):
 * - Top `↓ Expense` / `↑ Income` segmented pill toggle to switch cleanly between expense & income breakdowns
 * - Interactive Segmented Donut Ring (22.dp stroke, rounded caps) with direct slice tap hit-testing
 *   (tapping a slice dims unselected slices to alpha = 0.25f and updates center hub to Category Name • Share % + Amount)
 * - Horizontal scrollable category icon strip (`All Categories` + category icons) synced with the Donut
 * - Smoothly expanding Subcategory Breakdown list directly below the Donut when a category is selected
 * - Sleek `Open Filtered Report →` button opening FilteredReportsScreen with active type, date range, and category
 * - Clean category rows (no #1/#2/#3 rank numbers) tapping directly into FilteredReportsScreen
 */
@Composable
fun CategorySpendingCard(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    dateRange: InsightsDateRange,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    onViewTransactions: (
        category: String?,
        dateRange: Pair<Long, Long>?,
        dateRangeLabel: String?,
        filterType: ActivityFilterType
    ) -> Unit,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isBn = language == AppLanguage.BANGLA

    var activeType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var isExpandedAll by remember(dateRange, activeType) { mutableStateOf(false) }

    val activeFilterType = if (activeType == TransactionType.INCOME) {
        ActivityFilterType.INCOME
    } else {
        ActivityFilterType.EXPENSES
    }

    // Compute category breakdown for either Expense or Income (strictly excluding transfers)
    val categoryItems = remember(transactions, dateRange, categories, activeType) {
        if (activeType == TransactionType.EXPENSE) {
            InsightsCalculator.calculateCategoryExpenses(transactions, dateRange, categories)
        } else {
            val incomes = transactions.filter {
                !InsightsCalculator.isTransfer(it) &&
                    it.type == TransactionType.INCOME &&
                    it.timestamp in dateRange.startMillis..dateRange.endMillis
            }
            val total = incomes.sumOf { it.amount }
            if (total <= 0.0) {
                emptyList()
            } else {
                val incomePalette = listOf(
                    Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6),
                    Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFF59E0B),
                    Color(0xFF14B8A6), Color(0xFFEC4899)
                )
                incomes
                    .groupBy { it.category.ifBlank { "Income" } }
                    .entries
                    .sortedByDescending { entry -> entry.value.sumOf { it.amount } }
                    .mapIndexed { idx, (catName, txs) ->
                        val sum = txs.sumOf { it.amount }
                        val catEntity = categories.find { it.name.equals(catName, ignoreCase = true) }
                        val parsedColor = catEntity?.colorHex?.let { hex ->
                            try {
                                Color(android.graphics.Color.parseColor(hex))
                            } catch (_: Exception) {
                                null
                            }
                        } ?: incomePalette[idx % incomePalette.size]
                        CategoryExpenseItem(
                            categoryName = catName,
                            amount = sum,
                            percentage = ((sum / total) * 100.0).toFloat(),
                            count = txs.size,
                            color = parsedColor
                        )
                    }
            }
        }
    }

    val totalAmount = remember(categoryItems) {
        categoryItems.sumOf { it.amount }
    }

    // Keep selected category valid for current list
    val activeSelectedItem = remember(categoryItems, selectedCategory) {
        selectedCategory?.let { sel ->
            categoryItems.find { it.categoryName.equals(sel, ignoreCase = true) }
        }
    }

    // Subcategory breakdown for the selected category (for both Expense and Income)
    val selectedSubcategories = remember(transactions, dateRange, activeType, activeSelectedItem, isBn) {
        val sel = activeSelectedItem ?: return@remember emptyList()
        val matching = transactions.filter { tx ->
            !InsightsCalculator.isTransfer(tx) &&
                tx.timestamp in dateRange.startMillis..dateRange.endMillis &&
                tx.category.equals(sel.categoryName, ignoreCase = true) &&
                if (activeType == TransactionType.EXPENSE) {
                    tx.type == TransactionType.EXPENSE
                } else {
                    tx.type == TransactionType.INCOME
                }
        }
        val catTotal = matching.sumOf { it.amount }
        if (catTotal <= 0.0) return@remember emptyList()

        val generalLabel = if (isBn) "সাধারণ" else "General"
        val subPalette = listOf(
            Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF06B6D4),
            Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEC4899)
        )
        matching
            .groupBy { it.subcategory.ifBlank { generalLabel } }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { it.amount } }
            .mapIndexed { idx, (subName, txs) ->
                val sum = txs.sumOf { it.amount }
                SubcategoryExpenseItem(
                    subcategoryName = subName,
                    amount = sum,
                    percentageOfCategory = ((sum / catTotal) * 100.0).toFloat(),
                    count = txs.size,
                    color = subPalette[idx % subPalette.size]
                )
            }
    }

    // Smooth continuous sweep animation for Donut Ring
    val sweepAnim = remember(dateRange, activeType, categoryItems.size, totalAmount) { Animatable(0f) }
    LaunchedEffect(dateRange, activeType, categoryItems.size, totalAmount) {
        sweepAnim.snapTo(0f)
        sweepAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 660, easing = FastOutSlowInEasing)
        )
    }
    val selectedStrokeBoost by animateFloatAsState(
        targetValue = if (activeSelectedItem != null) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "donut_selected_stroke_boost"
    )

    val centerDisplayAmount = activeSelectedItem?.amount ?: totalAmount
    val centerFormatted = CurrencyFormatter.format(centerDisplayAmount, currencySymbol, language = language)
    var centerTotalFontSizeSp by remember(centerFormatted) { mutableFloatStateOf(15f) }

    val openFilteredReport: (String?) -> Unit = { categoryName ->
        val datePair = Pair(dateRange.startMillis, dateRange.endMillis)
        val dateLabel = if (isBn) dateRange.formattedDisplayBn else dateRange.formattedDisplay
        onViewTransactions(categoryName, datePair, dateLabel, activeFilterType)
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("category_spending_card"),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
        ) {
            // Card Header + Category Count Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeType == TransactionType.EXPENSE) {
                            if (isBn) "খাতভিত্তিক ব্যয়" else "Spending by Category"
                        } else {
                            if (isBn) "খাতভিত্তিক আয়" else "Income by Category"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isBn) "উপ-খাত দেখতে চার্ট বা আইকনে ট্যাপ করুন" else "Tap a slice or icon to inspect subcategories",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                            fontSize = 11.sp
                        )
                    )
                }

                val countLabel = if (isBn) {
                    "${CurrencyFormatter.convertToBanglaDigits(categoryItems.size.toString())}টি খাত"
                } else {
                    if (categoryItems.size == 1) "1 category" else "${categoryItems.size} categories"
                }
                Surface(
                    shape = CircleShape,
                    color = SoftViolet.copy(alpha = if (isDark) 0.18f else 0.10f)
                ) {
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFA5B4FC) else RoyalIndigo,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Top ↓ Expense / ↑ Income Segmented Pill Toggle
            val toggleTrackBg = if (isDark) Color(0xFF0F172A).copy(alpha = 0.72f) else Color(0xFFF1F5F9)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(toggleTrackBg)
                    .padding(4.dp)
                    .testTag("category_type_segmented_toggle"),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isExpenseSelected = activeType == TransactionType.EXPENSE
                val isIncomeSelected = activeType == TransactionType.INCOME

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            brush = if (isExpenseSelected) {
                                indigoVioletGradient()
                            } else {
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .clickable {
                            if (activeType != TransactionType.EXPENSE) {
                                activeType = TransactionType.EXPENSE
                                onSelectCategory(null)
                            }
                        }
                        .testTag("category_toggle_expense"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isExpenseSelected) Color.White else CashFlowExpenseRose,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isBn) "ব্যয়" else "Expense",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isExpenseSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isExpenseSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                fontSize = 12.5.sp
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            brush = if (isIncomeSelected) {
                                Brush.linearGradient(listOf(CashFlowIncomeEmerald, Color(0xFF059669)))
                            } else {
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .clickable {
                            if (activeType != TransactionType.INCOME) {
                                activeType = TransactionType.INCOME
                                onSelectCategory(null)
                            }
                        }
                        .testTag("category_toggle_income"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isIncomeSelected) Color.White else CashFlowIncomeEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isBn) "আয়" else "Income",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isIncomeSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isIncomeSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                fontSize = 12.5.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (activeType == TransactionType.EXPENSE) {
                            if (isBn) "এই সময়ের মধ্যে কোনো ব্যয়ের তথ্য নেই" else "No expense records for this period"
                        } else {
                            if (isBn) "এই সময়ের মধ্যে কোনো আয়ের তথ্য নেই" else "No income records for this period"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
            } else {
                // Top Visual: Interactive Segmented Donut Ring + Top 3 Legend Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .pointerInput(categoryItems, activeSelectedItem) {
                                detectTapGestures { tapOffset ->
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val dx = tapOffset.x - cx
                                    val dy = tapOffset.y - cy
                                    val dist = hypot(dx, dy)
                                    val outerR = minOf(cx, cy)
                                    val innerR = outerR * 0.45f
                                    if (dist < innerR) {
                                        // Center tap clears selection
                                        onSelectCategory(null)
                                    } else if (dist <= outerR && categoryItems.isNotEmpty()) {
                                        var angleDeg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
                                        // Convert so -90° (top 12 o'clock) maps to 0°..360° clockwise
                                        angleDeg = (angleDeg + 90f + 360f) % 360f
                                        val totalPct = categoryItems.sumOf { it.percentage.toDouble() }.toFloat().coerceAtLeast(1f)
                                        var cumulative = 0f
                                        var tappedCat: String? = null
                                        for (item in categoryItems) {
                                            val sweep = (item.percentage / totalPct) * 360f
                                            if (angleDeg in cumulative..(cumulative + sweep)) {
                                                tappedCat = item.categoryName
                                                break
                                            }
                                            cumulative += sweep
                                        }
                                        if (tappedCat != null) {
                                            val alreadySelected = activeSelectedItem?.categoryName.equals(tappedCat, ignoreCase = true)
                                            onSelectCategory(if (alreadySelected) null else tappedCat)
                                        }
                                    }
                                }
                            }
                            .testTag("category_segmented_donut"),
                        contentAlignment = Alignment.Center
                    ) {
                        val trackColor = if (isDark) {
                            Color.White.copy(alpha = 0.06f)
                        } else {
                            Color(0xFFE2E8F0).copy(alpha = 0.55f)
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokePx = 22.dp.toPx()
                            val selectedExtraPx = 3.dp.toPx() * selectedStrokeBoost
                            val maxStrokePx = strokePx + 3.dp.toPx()
                            val inset = maxStrokePx / 2f + 2.dp.toPx()
                            val arcSize = Size(
                                width = (size.width - inset * 2f).coerceAtLeast(10f),
                                height = (size.height - inset * 2f).coerceAtLeast(10f)
                            )
                            val topLeft = Offset(inset, inset)

                            // Subtle continuous base track
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                            )

                            val gapDegrees = if (categoryItems.size > 1) 1.5f else 0f
                            val totalPct = categoryItems.sumOf { it.percentage.toDouble() }.toFloat().coerceAtLeast(1f)
                            val totalAnimatedAngle = 360f * sweepAnim.value
                            val hasSelection = activeSelectedItem != null

                            val slicesToDraw = ArrayList<DonutSliceArc>(categoryItems.size)
                            var cumulativeAngle = 0f
                            categoryItems.forEach { item ->
                                val rawSweep = (item.percentage / totalPct) * 360f
                                val effectiveGap = if (categoryItems.size > 1) {
                                    minOf(gapDegrees, rawSweep * 0.35f)
                                } else {
                                    0f
                                }
                                val sliceStart = cumulativeAngle + (effectiveGap / 2f)
                                val sliceEnd = cumulativeAngle + rawSweep - (effectiveGap / 2f)
                                val visibleEnd = minOf(sliceEnd, totalAnimatedAngle)
                                val drawnSweep = (visibleEnd - sliceStart).coerceAtLeast(0f)

                                val isSelected = activeSelectedItem?.categoryName.equals(item.categoryName, ignoreCase = true)
                                val segStroke = if (isSelected) strokePx + selectedExtraPx else strokePx
                                val sliceColor = if (hasSelection && !isSelected) {
                                    item.color.copy(alpha = 0.25f)
                                } else {
                                    item.color
                                }

                                if (drawnSweep > 0.05f) {
                                    slicesToDraw.add(
                                        DonutSliceArc(
                                            startAngle = -90f + sliceStart,
                                            sweepAngle = drawnSweep,
                                            color = sliceColor,
                                            strokeWidth = segStroke,
                                            isSelected = isSelected
                                        )
                                    )
                                }
                                cumulativeAngle += rawSweep
                            }

                            // Draw unselected slices first, then selected slice on top for clean radial edges
                            slicesToDraw.forEach { slice ->
                                if (!slice.isSelected) {
                                    drawArc(
                                        color = slice.color,
                                        startAngle = slice.startAngle,
                                        sweepAngle = slice.sweepAngle,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = slice.strokeWidth, cap = StrokeCap.Butt)
                                    )
                                }
                            }
                            slicesToDraw.forEach { slice ->
                                if (slice.isSelected) {
                                    drawArc(
                                        color = slice.color,
                                        startAngle = slice.startAngle,
                                        sweepAngle = slice.sweepAngle,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = slice.strokeWidth, cap = StrokeCap.Butt)
                                    )
                                }
                            }
                        }

                        // Clean luminous center hub (shows Category Name • % + Amount when selected)
                        val selItem = activeSelectedItem
                        val selCatEntity = selItem?.let { s ->
                            categories.find { it.name.equals(s.categoryName, ignoreCase = true) }
                        }
                        val hubTopLabel = if (selItem != null) {
                            val name = selCatEntity?.getDisplayName(language) ?: selItem.categoryName
                            val pct = if (isBn) {
                                "${CurrencyFormatter.convertToBanglaDigits(selItem.percentage.roundToInt().toString())}%"
                            } else {
                                "${selItem.percentage.roundToInt()}%"
                            }
                            "$name • $pct"
                        } else {
                            if (activeType == TransactionType.EXPENSE) {
                                if (isBn) "মোট ব্যয়" else "TOTAL SPENT"
                            } else {
                                if (isBn) "মোট আয়" else "TOTAL INCOME"
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0xFF0F172A).copy(alpha = 0.88f)
                                    else Color.White.copy(alpha = 0.92f),
                                    CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selItem != null) selItem.color.copy(alpha = 0.45f)
                                    else if (isDark) Color.White.copy(alpha = 0.10f) else Color.White,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = hubTopLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = if (selItem != null) 9.sp else 8.5.sp,
                                        letterSpacing = if (isBn || selItem != null) 0.sp else 0.55.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = selItem?.color ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = centerFormatted,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = centerTotalFontSizeSp.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = (-0.3).sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { result ->
                                        if (result.didOverflowWidth && centerTotalFontSizeSp > 10.5f) {
                                            centerTotalFontSizeSp = (centerTotalFontSizeSp - 0.5f).coerceAtLeast(10.5f)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Top 3 Category Legend Pills (tap toggles Donut inspection & subcategory expansion)
                    val legendPillBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.56f) else Color.White.copy(alpha = 0.68f)
                    val legendPillBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.85f)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryItems.take(3).forEach { item ->
                            val isSel = activeSelectedItem?.categoryName.equals(item.categoryName, ignoreCase = true)
                            val catEntity = categories.find { it.name.equals(item.categoryName, ignoreCase = true) }
                            val displayName = catEntity?.getDisplayName(language) ?: item.categoryName
                            val pctText = if (isBn) {
                                "${CurrencyFormatter.convertToBanglaDigits(item.percentage.roundToInt().toString())}%"
                            } else {
                                "${item.percentage.roundToInt()}%"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSel) item.color.copy(alpha = if (isDark) 0.22f else 0.14f)
                                        else legendPillBg
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) item.color.copy(alpha = 0.5f) else legendPillBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        onSelectCategory(if (isSel) null else item.categoryName)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(item.color)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = pctText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = item.color,
                                        fontFeatureSettings = "tnum"
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Horizontal Scrollable Category Icon Strip (All Categories + Category Icons)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("insights_category_icon_strip"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "all_categories_strip_chip") {
                        val isAllSelected = activeSelectedItem == null
                        val chipBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.85f)
                        val chipBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFE2E8F0)
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    brush = if (isAllSelected) {
                                        indigoVioletGradient()
                                    } else {
                                        Brush.linearGradient(listOf(chipBg, chipBg))
                                    }
                                )
                                .then(
                                    if (!isAllSelected) Modifier.border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                    else Modifier
                                )
                                .clickable { onSelectCategory(null) }
                                .padding(horizontal = 12.dp)
                                .testTag("insights_strip_all_categories"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = if (isBn) "সব খাত" else "All Categories",
                                    tint = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isBn) "সব খাত" else "All Categories",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    items(categoryItems, key = { "strip_${it.categoryName}" }) { item ->
                        val isSelected = activeSelectedItem?.categoryName.equals(item.categoryName, ignoreCase = true)
                        val catEntity = categories.find { it.name.equals(item.categoryName, ignoreCase = true) }
                        val displayName = catEntity?.getDisplayName(language) ?: item.categoryName
                        val iconName = catEntity?.iconName ?: "category"

                        Surface(
                            onClick = {
                                onSelectCategory(if (isSelected) null else item.categoryName)
                            },
                            shape = CircleShape,
                            color = if (isSelected) item.color
                            else if (isDark) Color(0xFF1E293B).copy(alpha = 0.72f) else Color.White,
                            border = if (isSelected) null else BorderStroke(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFE2E8F0)
                            ),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .testTag("insights_strip_cat_${item.categoryName}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getIconForName(iconName),
                                    contentDescription = displayName,
                                    tint = if (isSelected) Color.White else item.color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Smoothly Expanding Subcategory Breakdown Card when a slice/icon is selected
                AnimatedVisibility(
                    visible = activeSelectedItem != null && selectedSubcategories.isNotEmpty(),
                    enter = expandVertically(animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(200)),
                    exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(140))
                ) {
                    val selItem = activeSelectedItem
                    if (selItem != null) {
                        val catEntity = categories.find { it.name.equals(selItem.categoryName, ignoreCase = true) }
                        val selDisplayName = catEntity?.getDisplayName(language) ?: selItem.categoryName
                        val subPanelBg = if (isDark) Color(0xFF0F172A).copy(alpha = 0.65f) else Color(0xFFF8FAFC)
                        val subPanelBorder = selItem.color.copy(alpha = if (isDark) 0.35f else 0.25f)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(subPanelBg)
                                .border(1.dp, subPanelBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                                .testTag("insights_subcategory_breakdown_panel"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "$selDisplayName • উপ-খাতসমূহ" else "$selDisplayName • Subcategories",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = selItem.color,
                                        fontSize = 12.sp
                                    )
                                )
                                Text(
                                    text = CurrencyFormatter.format(selItem.amount, currencySymbol, language = language),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFeatureSettings = "tnum",
                                        fontSize = 12.5.sp
                                    )
                                )
                            }

                            selectedSubcategories.forEach { sub ->
                                val subPctStr = if (isBn) {
                                    "${CurrencyFormatter.convertToBanglaDigits(sub.percentageOfCategory.roundToInt().toString())}%"
                                } else {
                                    "${sub.percentageOfCategory.roundToInt()}%"
                                }
                                val subTxStr = if (isBn) {
                                    "${CurrencyFormatter.convertToBanglaDigits(sub.count.toString())}টি"
                                } else {
                                    "${sub.count} tx"
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(sub.color)
                                            )
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Text(
                                                text = sub.subcategoryName,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 12.5.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "($subTxStr • $subPctStr)",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
                                                    fontSize = 10.5.sp,
                                                    fontFeatureSettings = "tnum"
                                                )
                                            )
                                        }
                                        Text(
                                            text = CurrencyFormatter.format(sub.amount, currencySymbol, language = language),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontFeatureSettings = "tnum",
                                                fontSize = 12.5.sp
                                            )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (isDark) Color.White.copy(alpha = 0.08f)
                                                else Color(0xFFE2E8F0)
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((sub.percentageOfCategory / 100f).coerceIn(0.03f, 1f))
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(sub.color)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sleek "Open Filtered Report →" / "View Transactions" CTA button
                val ctaLabel = if (activeSelectedItem != null) {
                    val catNameDisp = categories.find {
                        it.name.equals(activeSelectedItem.categoryName, ignoreCase = true)
                    }?.getDisplayName(language) ?: activeSelectedItem.categoryName
                    if (isBn) "$catNameDisp এর লেনদেন দেখুন" else "View $catNameDisp Report"
                } else {
                    if (isBn) "বিস্তারিত ফিল্টার রিপোর্ট দেখুন" else "Open Filtered Report"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(indigoVioletGradient())
                        .clickable { openFilteredReport(activeSelectedItem?.categoryName) }
                        .testTag("open_filtered_report_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = ctaLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = ctaLabel,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clean Category Breakdown Rows (NO #1/#2/#3 rank badges; starts cleanly with 38.dp squircle icon badge)
                val visibleItems = if (isExpandedAll || categoryItems.size <= 5) {
                    categoryItems
                } else {
                    categoryItems.take(5)
                }

                val rowShape = RoundedCornerShape(14.dp)
                val tileBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                val tileBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.85f)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    visibleItems.forEach { item ->
                        val isHighlighted = activeSelectedItem?.categoryName.equals(item.categoryName, ignoreCase = true)
                        val categoryEntity = categories.find { it.name.equals(item.categoryName, ignoreCase = true) }
                        val displayName = categoryEntity?.getDisplayName(language) ?: item.categoryName
                        val iconName = categoryEntity?.iconName ?: "category"
                        val txCountLabel = if (isBn) {
                            "${CurrencyFormatter.convertToBanglaDigits(item.count.toString())}টি লেনদেন"
                        } else {
                            if (item.count == 1) "1 transaction" else "${item.count} transactions"
                        }
                        val pctLabel = if (isBn) {
                            "${CurrencyFormatter.convertToBanglaDigits(item.percentage.roundToInt().toString())}%"
                        } else {
                            "${item.percentage.roundToInt()}%"
                        }
                        val barFraction = (item.percentage / 100f).coerceIn(0.03f, 1f)

                        Surface(
                            onClick = { openFilteredReport(item.categoryName) },
                            shape = rowShape,
                            color = if (isHighlighted) {
                                item.color.copy(alpha = if (isDark) 0.16f else 0.10f)
                            } else {
                                tileBg
                            },
                            border = BorderStroke(
                                width = if (isHighlighted) 1.25.dp else 1.dp,
                                color = if (isHighlighted) item.color.copy(alpha = 0.45f) else tileBorder
                            ),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(rowShape)
                                .testTag("category_spending_row_${item.categoryName}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 38.dp squircle category icon badge (clean leading element, zero rank badges)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(item.color.copy(alpha = if (isDark) 0.22f else 0.13f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconForName(iconName),
                                        contentDescription = displayName,
                                        tint = item.color,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Category name, transaction count, and colored 5.dp progress bar
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = txCountLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(2.5.dp))
                                            .background(
                                                if (isDark) Color.White.copy(alpha = 0.08f)
                                                else Color(0xFFE2E8F0).copy(alpha = 0.85f)
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = barFraction * sweepAnim.value)
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(2.5.dp))
                                                .background(item.color)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Amount + percentage pill
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.format(item.amount, currencySymbol, language = language),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.5.sp,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(item.color.copy(alpha = if (isDark) 0.20f else 0.12f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = pctLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = item.color,
                                                fontSize = 10.sp,
                                                fontFeatureSettings = "tnum"
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = if (isBn) "$displayName লেনদেন দেখুন" else "View $displayName transactions",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                // Smooth expandable "Show all N categories" / "Show less" toggle when > 5 categories
                if (categoryItems.size > 5) {
                    Spacer(modifier = Modifier.height(12.dp))

                    val toggleText = if (isExpandedAll) {
                        if (isBn) "কম দেখুন" else "Show less"
                    } else {
                        val totalCats = if (isBn) {
                            CurrencyFormatter.convertToBanglaDigits(categoryItems.size.toString())
                        } else {
                            categoryItems.size.toString()
                        }
                        if (isBn) "সব ${totalCats}টি খাত দেখুন" else "Show all $totalCats categories"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftViolet.copy(alpha = if (isDark) 0.16f else 0.08f))
                            .border(
                                width = 1.dp,
                                color = SoftViolet.copy(alpha = if (isDark) 0.28f else 0.18f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { isExpandedAll = !isExpandedAll }
                            .padding(vertical = 9.dp)
                            .testTag("toggle_all_categories_btn"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = toggleText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFA5B4FC) else RoyalIndigo,
                                fontSize = 12.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpandedAll) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = toggleText,
                            tint = if (isDark) Color(0xFFA5B4FC) else RoyalIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 6. Key Spending Highlights:
 * At most 2–3 short, data-backed highlights (highest-spending category, period change, highest-spending day).
 */
@Composable
fun SpendingHighlightsCard(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity> = emptyList(),
    dateRange: InsightsDateRange,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = if (isDark) Color(0xFFA5B4FC) else RoyalIndigo

    val highlights = remember(transactions, dateRange, categories, accentColor, language) {
        val list = mutableListOf<HighlightItem>()
        val comparison = InsightsCalculator.calculatePeriodComparison(transactions, dateRange)
        val catExpenses = InsightsCalculator.calculateCategoryExpenses(transactions, dateRange, categories)
        val trend = InsightsCalculator.calculateSpendingTrend(transactions, InsightsPeriod.THIS_MONTH, dateRange)

        // 1. Highest-spending category
        if (catExpenses.isNotEmpty()) {
            val top = catExpenses.first()
            if (top.amount > 0) {
                val catEntity = categories.find { it.name.equals(top.categoryName, ignoreCase = true) }
                val dispName = catEntity?.getDisplayName(language) ?: top.categoryName
                val pctStr = if (language == AppLanguage.BANGLA) {
                    CurrencyFormatter.convertToBanglaDigits(top.percentage.roundToInt().toString())
                } else {
                    top.percentage.roundToInt().toString()
                }
                list.add(
                    HighlightItem(
                        icon = Icons.Default.Star,
                        color = Color(0xFFF59E0B),
                        title = if (language == AppLanguage.BANGLA) "শীর্ষ খরচের খাত" else "Top Expense Category",
                        description = if (language == AppLanguage.BANGLA) {
                            "$dispName খাতে সবচেয়ে বেশি ব্যয় হয়েছে ($pctStr%)"
                        } else {
                            "$dispName accounted for $pctStr% of your total spending"
                        }
                    )
                )
            }
        }

        // 2. Change vs previous period
        if (comparison.previousExpense > 0 && comparison.currentExpense > 0) {
            val isDecrease = comparison.expenseChangePct < 0
            val pctAbs = abs(comparison.expenseChangePct).roundToInt()
            val pctStr = if (language == AppLanguage.BANGLA) {
                CurrencyFormatter.convertToBanglaDigits(pctAbs.toString())
            } else {
                pctAbs.toString()
            }
            list.add(
                HighlightItem(
                    icon = if (isDecrease) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                    color = if (isDecrease) CashFlowIncomeEmerald else CashFlowExpenseRose,
                    title = if (language == AppLanguage.BANGLA) "পূর্ববর্তী সময়ের তুলনা" else "Period Comparison",
                    description = if (language == AppLanguage.BANGLA) {
                        if (isDecrease) "পূর্ববর্তী সময়ের তুলনায় খরচ $pctStr% হ্রাস পেয়েছে"
                        else "পূর্ববর্তী সময়ের তুলনায় খরচ $pctStr% বৃদ্ধি পেয়েছে"
                    } else {
                        if (isDecrease) "Spending is $pctStr% lower than the previous period"
                        else "Spending is $pctStr% higher than the previous period"
                    }
                )
            )
        }

        // 3. Peak spending day
        if (trend.highestDayAmount > 0) {
            list.add(
                HighlightItem(
                    icon = Icons.Default.ShowChart,
                    color = accentColor,
                    title = if (language == AppLanguage.BANGLA) "সর্বোচ্চ খরচের দিন" else "Peak Spending Day",
                    description = if (language == AppLanguage.BANGLA) {
                        "${trend.highestDayLabel}-এ সর্বোচ্চ খরচ হয়েছে (${CurrencyFormatter.format(trend.highestDayAmount, currencySymbol, language = language)})"
                    } else {
                        "Highest single day was on ${trend.highestDayLabel} (${CurrencyFormatter.format(trend.highestDayAmount, currencySymbol, language = language)})"
                    }
                )
            )
        }

        list.take(3)
    }

    if (highlights.isEmpty()) return

    val isBn = language == AppLanguage.BANGLA
    val itemBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.52f) else Color.White.copy(alpha = 0.62f)
    val itemBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.84f)

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("spending_highlights_card"),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isBn) "গুরুত্বপূর্ণ অন্তর্দৃষ্টি" else "Key Highlights",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                highlights.forEach { h ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(itemBg)
                            .border(1.dp, itemBorder, RoundedCornerShape(13.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(h.color.copy(alpha = if (isDark) 0.22f else 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = h.icon,
                                contentDescription = null,
                                tint = h.color,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = h.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = h.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class HighlightItem(
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val description: String
)

/**
 * 7. Cash Flow Comparison:
 * Clean dual rounded progress bars (10.dp height) comparing Income (#10B981) vs Expenses (#F43F5E)
 * with a crisp "Net: +৳49,670" pill badge.
 */
@Composable
fun CashFlowCard(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val (income, expense, net) = remember(transactions, dateRange) {
        val periodTxs = transactions.filter {
            !InsightsCalculator.isTransfer(it) && it.timestamp in dateRange.startMillis..dateRange.endMillis
        }
        val inc = periodTxs.filter { InsightsCalculator.isIncome(it) }.sumOf { it.amount }
        val exp = periodTxs.filter { InsightsCalculator.isExpense(it) }.sumOf { it.amount }
        Triple(inc, exp, inc - exp)
    }

    val maxVal = maxOf(income, expense).coerceAtLeast(1.0)
    val incomeFraction by animateFloatAsState(
        targetValue = (income / maxVal).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "cash_flow_income_bar"
    )
    val expenseFraction by animateFloatAsState(
        targetValue = (expense / maxVal).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "cash_flow_expense_bar"
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isBn = language == AppLanguage.BANGLA
    val netColor = if (net >= 0) CashFlowIncomeEmerald else CashFlowExpenseRose

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("cash_flow_card"),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "ক্যাশ ফ্লো তুলনা" else "Cash Flow Comparison",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Surface(
                    shape = CircleShape,
                    color = netColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                    border = BorderStroke(0.75.dp, netColor.copy(alpha = 0.28f))
                ) {
                    Text(
                        text = "${if (isBn) "নিট: " else "Net: "}${CurrencyFormatter.format(net, currencySymbol, showSign = true, language = language)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = netColor,
                            fontSize = 11.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val trackBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0).copy(alpha = 0.75f)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Income Bar (#10B981)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CashFlowIncomeEmerald)
                            )
                            Text(
                                text = if (isBn) "মোট আয়" else "Income",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                                )
                            )
                        }
                        Text(
                            text = CurrencyFormatter.format(income, currencySymbol, language = language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = CashFlowIncomeEmerald,
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(trackBg)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = incomeFraction)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(CashFlowIncomeEmerald, Color(0xFF34D399))
                                    )
                                )
                        )
                    }
                }

                // Expense Bar (#F43F5E)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CashFlowExpenseRose)
                            )
                            Text(
                                text = if (isBn) "মোট ব্যয়" else "Expenses",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                                )
                            )
                        }
                        Text(
                            text = CurrencyFormatter.format(expense, currencySymbol, language = language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = CashFlowExpenseRose,
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(trackBg)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = expenseFraction)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(CashFlowExpenseRose, Color(0xFFFB7185))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
