package com.example.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.ui.InsightsPeriod
import com.example.ui.components.CategoryIcon
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 1. Period Controls: Chips for Daily, This Week, This Month, Last Month + Navigation arrows (< and >)
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
    val displayedPeriods = listOf(
        InsightsPeriod.DAILY,
        InsightsPeriod.THIS_WEEK,
        InsightsPeriod.THIS_MONTH,
        InsightsPeriod.LAST_MONTH
    )

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
                val isSelected = selectedPeriod == p
                val chipLabel = if (language == AppLanguage.BANGLA) p.bnLabel else p.label

                FilterChip(
                    selected = isSelected,
                    onClick = { onPeriodSelected(p) },
                    label = {
                        Text(
                            text = chipLabel,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("period_chip_${p.name.lowercase()}")
                )
            }
        }

        // Date range navigation bar (< Date Badge >)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousPeriod,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("period_prev_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Period",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BANGLA) dateRange.formattedDisplayBn else dateRange.formattedDisplay,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            val canGoForward = periodOffset < 0 && selectedPeriod != InsightsPeriod.CUSTOM
            IconButton(
                onClick = { if (canGoForward) onNextPeriod() },
                enabled = canGoForward,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("period_next_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Period",
                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            }
        }
    }
}

/**
 * 2. Financial Overview: 3 concise figures (Total Income, Total Expenses, Net Cash Flow)
 * with compact safe previous-period comparison badges.
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

    val isBn = language == AppLanguage.BANGLA

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("financial_overview_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (isBn) "আর্থিক সারসংক্ষেপ" else "Financial Overview",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Income Summary
                OverviewMetricItem(
                    title = if (isBn) "মোট আয়" else "Income",
                    amount = comparison.currentIncome,
                    color = IncomeGreen,
                    icon = Icons.Default.TrendingUp,
                    changePct = if (comparison.previousIncome > 0) comparison.incomeChangePct else null,
                    isExpenseMetric = false,
                    currencySymbol = currencySymbol,
                    language = language,
                    modifier = Modifier.weight(1f)
                )

                // Expense Summary
                OverviewMetricItem(
                    title = if (isBn) "মোট ব্যয়" else "Expenses",
                    amount = comparison.currentExpense,
                    color = ExpenseRed,
                    icon = Icons.Default.TrendingDown,
                    changePct = if (comparison.previousExpense > 0) comparison.expenseChangePct else null,
                    isExpenseMetric = true,
                    currencySymbol = currencySymbol,
                    language = language,
                    modifier = Modifier.weight(1f)
                )

                // Net Cash Flow Summary
                val netColor = if (comparison.currentNet >= 0) IncomeGreen else ExpenseRed
                OverviewMetricItem(
                    title = if (isBn) "নিট ক্যাশ ফ্লো" else "Net Flow",
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
private fun OverviewMetricItem(
    title: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    changePct: Double?,
    isExpenseMetric: Boolean,
    currencySymbol: String,
    language: AppLanguage,
    showSign: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = CurrencyFormatter.format(amount, currencySymbol, showSign = showSign, language = language),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = color
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Previous period comparison chip
            if (changePct != null) {
                val isFavorable = if (isExpenseMetric) changePct <= 0 else changePct >= 0
                val badgeColor = if (isFavorable) IncomeGreen else ExpenseRed
                val signStr = if (changePct > 0) "+" else ""
                val rounded = (abs(changePct)).roundToInt()
                Text(
                    text = "$signStr$rounded% vs prev",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

/**
 * 3. Budget Status: Shown only if applicable budgets exist for the period.
 * Hides completely when no budgets exist (per requirements).
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
    // If the period is Daily or Weekly, no daily or weekly budget entity exists in the app.
    // Hiding this section prevents displaying misleading monthly budget targets against 1-day or 1-week expenses.
    if (period == InsightsPeriod.DAILY || period == InsightsPeriod.THIS_WEEK) {
        return
    }

    val monthYearStr = remember(dateRange) {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = dateRange.startMillis
        SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
    }

    // Filter budgets that apply to this period
    val applicableBudgets = remember(budgets, monthYearStr) {
        budgets.filter { it.monthYear == "ALL" || it.monthYear == monthYearStr }
    }

    // Per requirement: If no applicable budget exists, hide this section rather than showing an empty state or zero values.
    if (applicableBudgets.isEmpty()) {
        return
    }

    // Calculate budget amount and spent amount
    val (budgetLimit, amountSpent) = remember(applicableBudgets, transactions, dateRange) {
        val overallBudget = applicableBudgets.find { it.isOverall }
        val limit = overallBudget?.monthlyLimit ?: applicableBudgets.sumOf { it.monthlyLimit }

        val periodExpenses = transactions.filter {
            it.type == com.example.data.model.TransactionType.EXPENSE &&
                    it.timestamp in dateRange.startMillis..dateRange.endMillis
        }

        val spent = if (overallBudget != null) {
            periodExpenses.sumOf { it.amount }
        } else {
            val budgetedCatNames = applicableBudgets.map { it.categoryName.trim().lowercase() }.toSet()
            periodExpenses.filter { it.category.trim().lowercase() in budgetedCatNames }.sumOf { it.amount }
        }

        Pair(limit, spent)
    }

    if (budgetLimit <= 0) return

    val remaining = (budgetLimit - amountSpent).coerceAtLeast(0.0)
    val percentageUsed = ((amountSpent / budgetLimit) * 100).toFloat()
    val progressFraction = (amountSpent / budgetLimit).toFloat().coerceIn(0f, 1f)
    val isOverBudget = amountSpent > budgetLimit

    val progressColor = when {
        isOverBudget -> ExpenseRed
        percentageUsed > 80f -> Color(0xFFF59E0B) // Amber warning
        else -> MaterialTheme.colorScheme.primary
    }

    val isBn = language == AppLanguage.BANGLA

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("budget_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "বাজেট অগ্রগতি" else "Budget Status",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = progressColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isOverBudget) {
                            if (isBn) "সীমা অতিক্রান্ত" else "Over Budget"
                        } else {
                            "${percentageUsed.roundToInt()}% used"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = progressColor,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Key Figures: Budget, Spent, Remaining, Percentage Used
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isBn) "মোট বাজেট" else "Budget",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                    Text(
                        text = CurrencyFormatter.format(budgetLimit, currencySymbol, language = language),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isBn) "ব্যয় হয়েছে" else "Spent",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                    Text(
                        text = CurrencyFormatter.format(amountSpent, currencySymbol, language = language),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = progressColor)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isBn) "অবশিষ্ট" else "Remaining",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                    Text(
                        text = CurrencyFormatter.format(remaining, currencySymbol, language = language),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) ExpenseRed else IncomeGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(progressColor)
                )
            }
        }
    }
}

/**
 * 4. Spending Trend: Readable chart across the selected period with date and amount inspection.
 * Uses real calculated interval points, never fabricates data for days with no transactions.
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

    var selectedIndex by remember(summary) {
        mutableStateOf<Int?>(if (summary.points.isNotEmpty()) summary.points.lastIndex else null)
    }

    val isBn = language == AppLanguage.BANGLA
    val points = summary.points
    val maxAmount = points.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    val primaryColor = MaterialTheme.colorScheme.primary

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("spending_trend_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isBn) "ব্যয় প্রবণতা" else "Spending Trend",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isBn) "নির্দিষ্ট দিনের খরচ দেখতে ট্যাপ করুন" else "Tap along the chart to inspect amounts",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = primaryColor.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Peak Day & Daily Average Highlights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isBn) "দৈনিক গড়" else "Daily Average",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                    Text(
                        text = CurrencyFormatter.format(summary.averageDailySpending, currencySymbol, language = language),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isBn) "সর্বোচ্চ দিন" else "Peak Spending",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                    Text(
                        text = if (summary.highestDayAmount > 0) {
                            "${summary.highestDayLabel} (${CurrencyFormatter.format(summary.highestDayAmount, currencySymbol, language = language)})"
                        } else "—",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ExpenseRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart area
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .pointerInput(points) {
                            detectTapGestures { tapOffset ->
                                val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                                val index = ((tapOffset.x + stepX / 2) / stepX).toInt()
                                selectedIndex = index.coerceIn(0, points.lastIndex)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val availableH = height - 20.dp.toPx()
                        val stepX = width / (points.size - 1)

                        val path = Path()
                        val fillPath = Path()

                        points.forEachIndexed { i, p ->
                            val x = i * stepX
                            val y = availableH - ((p.amount / maxAmount) * (availableH - 12.dp.toPx())).toFloat()

                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, availableH)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (i - 1) * stepX
                                val prevY = availableH - ((points[i - 1].amount / maxAmount) * (availableH - 12.dp.toPx())).toFloat()
                                val cx = (prevX + x) / 2f
                                path.cubicTo(cx, prevY, cx, y, x, y)
                                fillPath.cubicTo(cx, prevY, cx, y, x, y)
                            }

                            if (i == points.lastIndex) {
                                fillPath.lineTo(x, availableH)
                                fillPath.close()
                            }
                        }

                        // Gradient fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.22f), primaryColor.copy(alpha = 0.02f)),
                                startY = 0f,
                                endY = availableH
                            )
                        )

                        // Line
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Inspection point
                        selectedIndex?.let { selIdx ->
                            if (selIdx in points.indices) {
                                val pt = points[selIdx]
                                val ptX = selIdx * stepX
                                val ptY = availableH - ((pt.amount / maxAmount) * (availableH - 12.dp.toPx())).toFloat()

                                // Vertical dashed line
                                drawLine(
                                    color = primaryColor.copy(alpha = 0.4f),
                                    start = Offset(ptX, 0f),
                                    end = Offset(ptX, availableH),
                                    strokeWidth = 1.5.dp.toPx()
                                )

                                // Outer dot
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.25f),
                                    radius = 7.dp.toPx(),
                                    center = Offset(ptX, ptY)
                                )
                                // Inner dot
                                drawCircle(
                                    color = primaryColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(ptX, ptY)
                                )
                            }
                        }
                    }
                }

                // Bottom axis interval indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val firstLabel = points.firstOrNull()?.label ?: ""
                    val midLabel = points.getOrNull(points.size / 2)?.label ?: ""
                    val lastLabel = points.lastOrNull()?.label ?: ""

                    Text(
                        text = firstLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                    if (points.size > 2) {
                        Text(
                            text = midLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                    }
                    Text(
                        text = lastLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }

                // Selected point details banner
                selectedIndex?.let { selIdx ->
                    if (selIdx in points.indices) {
                        val selPt = points[selIdx]
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selPt.label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = CurrencyFormatter.format(selPt.amount, currencySymbol, language = language),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Spending by Category:
 * Single clear category breakdown ranked by amount spent (name, icon/color, total spent, % of total).
 * Eliminates duplicate Top Categories section.
 * Underneath: "View Transactions" button that opens Activity filtered by both selected period and chosen category.
 */
@Composable
fun CategorySpendingCard(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    dateRange: InsightsDateRange,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    onViewTransactions: (category: String?, dateRange: Pair<Long, Long>?, dateRangeLabel: String?) -> Unit,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val categoryExpenses = remember(transactions, dateRange, categories) {
        InsightsCalculator.calculateCategoryExpenses(transactions, dateRange, categories)
    }

    val totalExpense = remember(categoryExpenses) {
        categoryExpenses.sumOf { it.amount }
    }

    val isBn = language == AppLanguage.BANGLA

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("category_spending_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isBn) "খাতভিত্তিক ব্যয়" else "Spending by Category",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isBn) "সর্বোচ্চ খরচ অনুযায়ী তালিকাভুক্ত" else "Ranked breakdown of expenses",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Text(
                    text = "${categoryExpenses.size} ${if (isBn) "টি খাত" else "categories"}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (categoryExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBn) "এই সময়ের মধ্যে কোনো ব্যয়ের তথ্য নেই" else "No expense records for this period",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
            } else {
                // Top Visual: Donut proportion chart + Center total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            categoryExpenses.forEach { item ->
                                val sweep = (item.percentage / 100f) * 360f
                                val isSelected = item.categoryName == selectedCategory
                                val strokeW = if (isSelected) 20.dp.toPx() else 14.dp.toPx()

                                drawArc(
                                    color = item.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep.coerceAtLeast(1f),
                                    useCenter = false,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                                )
                                startAngle += sweep
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isBn) "মোট ব্যয়" else "Total",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                text = CurrencyFormatter.format(totalExpense, currencySymbol, language = language),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Top 3 quick highlights
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryExpenses.take(3).forEach { item ->
                            val isSel = item.categoryName == selectedCategory
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) item.color.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        onSelectCategory(if (isSel) null else item.categoryName)
                                    }
                                    .padding(vertical = 3.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(item.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.categoryName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "${item.percentage.roundToInt()}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = item.color
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ranked List of all categories
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryExpenses.forEachIndexed { index, item ->
                        val isSelected = item.categoryName == selectedCategory
                        val categoryEntity = categories.find { it.name.equals(item.categoryName, ignoreCase = true) }
                        val iconName = categoryEntity?.iconName ?: "category"
                        val colorHex = categoryEntity?.colorHex ?: "#4F46E5"

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelectCategory(if (isSelected) null else item.categoryName)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Rank number
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        ),
                                        modifier = Modifier.width(24.dp)
                                    )

                                    // Category icon
                                    CategoryIcon(
                                        iconName = iconName,
                                        colorHex = colorHex,
                                        size = 36.dp
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = item.categoryName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.count} ${if (isBn) "টি লেনদেন" else "transactions"}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.format(item.amount, currencySymbol, language = language),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "${item.percentage.roundToInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = item.color,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Prominent "View Transactions" Button (passes chosen category AND selected period)
                val activeTargetCat = selectedCategory ?: categoryExpenses.firstOrNull()?.categoryName
                Button(
                    onClick = {
                        val datePair = Pair(dateRange.startMillis, dateRange.endMillis)
                        val dateLabel = if (isBn) dateRange.formattedDisplayBn else dateRange.formattedDisplay
                        onViewTransactions(activeTargetCat, datePair, dateLabel)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("view_transactions_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (activeTargetCat != null) {
                                if (isBn) "লেনদেন দেখুন ($activeTargetCat)" else "View Transactions ($activeTargetCat)"
                            } else {
                                if (isBn) "সব লেনদেন দেখুন" else "View Transactions"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
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
 * Only show when supported by real data.
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
    val primaryColor = MaterialTheme.colorScheme.primary

    val highlights = remember(transactions, dateRange, categories, primaryColor) {
        val list = mutableListOf<HighlightItem>()
        val comparison = InsightsCalculator.calculatePeriodComparison(transactions, dateRange)
        val catExpenses = InsightsCalculator.calculateCategoryExpenses(transactions, dateRange, categories)
        val trend = InsightsCalculator.calculateSpendingTrend(transactions, InsightsPeriod.THIS_MONTH, dateRange)

        // 1. Highest-spending category
        if (catExpenses.isNotEmpty()) {
            val top = catExpenses.first()
            if (top.amount > 0) {
                list.add(
                    HighlightItem(
                        icon = Icons.Default.Star,
                        color = Color(0xFFF59E0B),
                        title = if (language == AppLanguage.BANGLA) "শীর্ষ খরচের খাত" else "Top Expense Category",
                        description = if (language == AppLanguage.BANGLA) {
                            "${top.categoryName} খাতে সবচেয়ে বেশি ব্যয় হয়েছে (${top.percentage.roundToInt()}%)"
                        } else {
                            "${top.categoryName} accounted for ${top.percentage.roundToInt()}% of your total spending"
                        }
                    )
                )
            }
        }

        // 2. Change vs previous period (if previous spending was > 0)
        if (comparison.previousExpense > 0 && comparison.currentExpense > 0) {
            val isDecrease = comparison.expenseChangePct < 0
            val pctAbs = abs(comparison.expenseChangePct).roundToInt()
            list.add(
                HighlightItem(
                    icon = if (isDecrease) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                    color = if (isDecrease) IncomeGreen else ExpenseRed,
                    title = if (language == AppLanguage.BANGLA) "পূর্ববর্তী সময়ের তুলনা" else "Period Comparison",
                    description = if (language == AppLanguage.BANGLA) {
                        if (isDecrease) "পূর্ববর্তী সময়ের তুলনায় খরচ $pctAbs% হ্রাস পেয়েছে"
                        else "পূর্ববর্তী সময়ের তুলনায় খরচ $pctAbs% বৃদ্ধি পেয়েছে"
                    } else {
                        if (isDecrease) "Spending is $pctAbs% lower than the previous period"
                        else "Spending is $pctAbs% higher than the previous period"
                    }
                )
            )
        }

        // 3. Peak spending day
        if (trend.highestDayAmount > 0) {
            list.add(
                HighlightItem(
                    icon = Icons.Default.ShowChart,
                    color = primaryColor,
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

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("spending_highlights_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(h.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = h.icon,
                                    contentDescription = null,
                                    tint = h.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = h.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = h.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class HighlightItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val title: String,
    val description: String
)

/**
 * 7. Cash Flow:
 * Simple comparison chart of income vs expenses over the selected period and net cash flow total.
 * Avoids repeating overview figures in another large card.
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
    val isBn = language == AppLanguage.BANGLA
    val netColor = if (net >= 0) IncomeGreen else ExpenseRed

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("cash_flow_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

                // Compact Net Flow badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = netColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${if (isBn) "নিট: " else "Net: "}${CurrencyFormatter.format(net, currencySymbol, showSign = true, language = language)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = netColor,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Proportional Bar Comparison (Income vs Expense)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Income Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isBn) "মোট আয়" else "Income",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        )
                        Text(
                            text = CurrencyFormatter.format(income, currencySymbol, language = language),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = IncomeGreen)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (income / maxVal).toFloat().coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(IncomeGreen)
                        )
                    }
                }

                // Expense Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isBn) "মোট ব্যয়" else "Expenses",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        )
                        Text(
                            text = CurrencyFormatter.format(expense, currencySymbol, language = language),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ExpenseRed)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (expense / maxVal).toFloat().coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(ExpenseRed)
                        )
                    }
                }
            }
        }
    }
}
