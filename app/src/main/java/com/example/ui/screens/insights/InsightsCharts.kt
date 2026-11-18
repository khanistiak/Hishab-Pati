package com.example.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.ui.InsightsPeriod
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter

@Composable
fun SpendingTrendSection(
    transactions: List<TransactionEntity>,
    period: InsightsPeriod,
    dateRange: InsightsDateRange,
    currencySymbol: String,
    language: AppLanguage,
    isIncomeMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val summary = remember(transactions, period, dateRange, isIncomeMode) {
        if (isIncomeMode) {
            InsightsCalculator.calculateIncomeTrend(transactions, period, dateRange)
        } else {
            InsightsCalculator.calculateSpendingTrend(transactions, period, dateRange)
        }
    }

    var selectedPointIndex by remember(summary) {
        mutableStateOf<Int?>(if (summary.points.isNotEmpty()) summary.points.lastIndex else null)
    }

    val chartColor = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary

    val title = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "আয় প্রবণতা" else "Income Trend"
    } else {
        if (language == AppLanguage.BANGLA) "ব্যয় প্রবণতা" else "Spending Trend"
    }
    val subtitle = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "গ্রাফে ট্যাপ করে নির্দিষ্ট দিনের আয় দেখুন" else "Tap along the chart to inspect income amounts"
    } else {
        if (language == AppLanguage.BANGLA) "গ্রাফে ট্যাপ করে নির্দিষ্ট দিনের খরচ দেখুন" else "Tap along the chart to inspect amounts"
    }
    val totalLabel = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "মোট আয়" else "Total Inflow"
    } else {
        if (language == AppLanguage.BANGLA) "মোট ব্যয়" else "Total Expenses"
    }
    val peakLabel = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "সর্বোচ্চ আয়" else "Peak Inflow"
    } else {
        if (language == AppLanguage.BANGLA) "সর্বোচ্চ দিন" else "Peak Day"
    }
    val avgLabel = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "দৈনিক গড় আয়" else "Daily Avg Inflow"
    } else {
        if (language == AppLanguage.BANGLA) "দৈনিক গড়" else "Daily Avg"
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag(if (isIncomeMode) "income_trend_card" else "spending_trend_card")
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
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = chartColor.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = chartColor,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Stats Row: Total, Highest Day, Daily Average
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = totalLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = CurrencyFormatter.format(summary.totalExpense, currencySymbol, language = language),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.onSurface)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = peakLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = if (summary.highestDayAmount > 0) {
                            "${summary.highestDayLabel} (${CurrencyFormatter.formatCompact(summary.highestDayAmount, currencySymbol, language)})"
                        } else "-",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isIncomeMode) IncomeGreen else ExpenseRed
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = avgLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = CurrencyFormatter.format(summary.averageDailySpending, currencySymbol, language = language),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected point inspection banner
            val selectedPoint = selectedPointIndex?.let { summary.points.getOrNull(it) }
            if (selectedPoint != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (language == AppLanguage.BANGLA) "তারিখ/পর্যায়:" else "Point:"} ${selectedPoint.label}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = CurrencyFormatter.format(selectedPoint.amount, currencySymbol, language = language),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = chartColor
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Interactive Canvas Line Chart
            val points = summary.points
            val maxAmount = (points.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(10.0)

            val primaryColor = chartColor

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isNotEmpty()) {
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                val index = (fraction * (points.size - 1)).toInt()
                                selectedPointIndex = index.coerceIn(0, points.lastIndex)
                            }
                        }
                    }
            ) {
                if (points.size < 2) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "প্রবণতা দেখার জন্য পর্যাপ্ত তথ্য নেই" else "Not enough data for trend analysis",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                    if (points.size >= 2) {
                        val width = size.width
                        val height = size.height
                        val paddingBottom = 16.dp.toPx()
                        val availableH = height - paddingBottom

                        val path = Path()
                        val fillPath = Path()

                        val stepX = width / (points.size - 1)

                        points.forEachIndexed { i, p ->
                            val x = i * stepX
                            val y = availableH - ((p.amount / maxAmount) * (availableH - 20.dp.toPx())).toFloat()

                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, availableH)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (i - 1) * stepX
                                val prevY = availableH - ((points[i - 1].amount / maxAmount) * (availableH - 20.dp.toPx())).toFloat()
                                val cx = (prevX + x) / 2f
                                path.cubicTo(cx, prevY, cx, y, x, y)
                                fillPath.cubicTo(cx, prevY, cx, y, x, y)
                            }

                            if (i == points.lastIndex) {
                                fillPath.lineTo(x, availableH)
                                fillPath.close()
                            }
                        }

                        // Gradient fill under curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.02f)),
                                startY = 0f,
                                endY = availableH
                            )
                        )

                        // Smooth line
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw inspection point indicator
                        selectedPointIndex?.let { selIdx ->
                            if (selIdx in points.indices) {
                                val selX = selIdx * stepX
                                val selY = availableH - ((points[selIdx].amount / maxAmount) * (availableH - 20.dp.toPx())).toFloat()

                                // Vertical dashed line
                                drawLine(
                                    color = primaryColor.copy(alpha = 0.5f),
                                    start = Offset(selX, 0f),
                                    end = Offset(selX, availableH),
                                    strokeWidth = 1.5.dp.toPx()
                                )

                                // Highlight circle
                                drawCircle(
                                    color = Color.White,
                                    radius = 6.dp.toPx(),
                                    center = Offset(selX, selY)
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(selX, selY)
                                )
                            }
                        }
                    }
                }
            }
            }

            // Labels along X-Axis
            if (points.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val labelStride = (points.size / 6).coerceAtLeast(1)
                    points.forEachIndexed { i, p ->
                        if (i == 0 || i == points.lastIndex || i % labelStride == 0) {
                            Text(
                                text = p.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = if (i == selectedPointIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontWeight = if (i == selectedPointIndex) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhereYourMoneyWentSection(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage,
    isIncomeMode: Boolean = false,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryExpenses = remember(transactions, dateRange, categories, isIncomeMode) {
        if (isIncomeMode) {
            InsightsCalculator.calculateCategoryIncomes(transactions, dateRange, categories)
        } else {
            InsightsCalculator.calculateCategoryExpenses(transactions, dateRange, categories)
        }
    }

    val totalExpense = categoryExpenses.sumOf { it.amount }
    var selectedCatName by remember(categoryExpenses) {
        mutableStateOf<String?>(categoryExpenses.firstOrNull()?.categoryName)
    }

    val title = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "কোথা থেকে আয় এলো" else "Where Your Money Came From"
    } else {
        if (language == AppLanguage.BANGLA) "কোথায় খরচ হলো" else "Where Your Money Went"
    }
    val subtitle = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "সামগ্রিক আয়ের অনুপাত ও বিভিন্ন উৎস" else "Income distribution across sources"
    } else {
        if (language == AppLanguage.BANGLA) "সামগ্রিক ব্যয়ের অনুপাত ও বিতরণ" else "Spending distribution across categories"
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag(if (isIncomeMode) "where_your_money_came_from_card" else "where_your_money_went_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryExpenses.isEmpty()) {
                Text(
                    text = if (isIncomeMode) {
                        if (language == AppLanguage.BANGLA) "কোন আয়ের তথ্য পাওয়া যায়নি" else "No income records available for this period."
                    } else {
                        if (language == AppLanguage.BANGLA) "কোন তথ্য পাওয়া যায়নি" else "No expense records available for this period."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart
                    Box(
                        modifier = Modifier.size(135.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            categoryExpenses.forEach { item ->
                                val sweep = (item.percentage / 100f) * 360f
                                val isSelected = item.categoryName == selectedCatName
                                val strokeW = if (isSelected) 26.dp.toPx() else 20.dp.toPx()

                                drawArc(
                                    color = item.color,
                                    startAngle = startAngle,
                                    sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                    useCenter = false,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                                startAngle += sweep
                            }
                        }

                        val activeItem = categoryExpenses.firstOrNull { it.categoryName == selectedCatName } ?: categoryExpenses.firstOrNull()

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = activeItem?.categoryName ?: (if (language == AppLanguage.BANGLA) "মোট" else "Total"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = CurrencyFormatter.format(activeItem?.amount ?: totalExpense, currencySymbol, language = language),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryExpenses.take(5).forEach { item ->
                            val isSel = item.categoryName == selectedCatName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedCatName = item.categoryName
                                        onCategoryClick(item.categoryName)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
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
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "${CurrencyFormatter.format(item.percentage.toDouble(), "", language = language)}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

@Composable
fun CashFlowSection(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var numMonths by remember { mutableStateOf(6) }

    val summary = remember(transactions, numMonths) {
        InsightsCalculator.calculateCashFlowHistory(transactions, numMonths = numMonths)
    }

    var selectedMonth by remember(summary) {
        mutableStateOf(summary.monthlyData.lastOrNull())
    }

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
            // Header Row with 6M / 12M Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "ক্যাশ ফ্লো বিশ্লেষণ" else "Cash Flow Analysis",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (language == AppLanguage.BANGLA) "মাসিক আয় ও ব্যয়ের পাশাপাশি তুলনা" else "Monthly income & expense side-by-side",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                // 6M / 12M Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (numMonths == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (numMonths == 6) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { numMonths = 6 }
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "৬ মাস" else "6 Months",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (numMonths == 6) FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (numMonths == 12) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (numMonths == 12) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { numMonths = 12 }
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "১২ মাস" else "12 Months",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (numMonths == 12) FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Overall Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "মোট আয়" else "Total Income",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        )
                    }
                    Text(
                        text = CurrencyFormatter.format(summary.totalIncome, currencySymbol, language = language),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = IncomeGreen)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseRed))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "মোট ব্যয়" else "Total Expenses",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        )
                    }
                    Text(
                        text = CurrencyFormatter.format(summary.totalExpense, currencySymbol, language = language),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ExpenseRed)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "নিট ক্যাশ ফ্লো" else "Net Cash Flow",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                    val netColor = if (summary.netCashFlow >= 0) IncomeGreen else ExpenseRed
                    Text(
                        text = CurrencyFormatter.format(summary.netCashFlow, currencySymbol, showSign = true, language = language),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = netColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bar Chart side-by-side
            val maxBar = summary.monthlyData.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(10.0) ?: 10.0

            if (summary.monthlyData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "ক্যাশ ফ্লোর কোনো তথ্য নেই" else "No cash flow history available",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    summary.monthlyData.forEach { month ->
                        val isSelected = selectedMonth?.monthLabel == month.monthLabel && selectedMonth?.year == month.year
                        val incH = ((month.income / maxBar) * 85).dp.coerceAtLeast(4.dp)
                        val expH = ((month.expense / maxBar) * 85).dp.coerceAtLeast(4.dp)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { selectedMonth = month }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Income bar
                                Box(
                                    modifier = Modifier
                                        .width(if (numMonths == 6) 12.dp else 7.dp)
                                        .height(incH)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(IncomeGreen)
                                )
                                // Expense bar
                                Box(
                                    modifier = Modifier
                                        .width(if (numMonths == 6) 12.dp else 7.dp)
                                        .height(expH)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(ExpenseRed)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = month.monthLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = if (numMonths == 6) 10.sp else 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }

            // Tapped Month Detailed Breakdown Card
            selectedMonth?.let { month ->
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = month.fullMonthLabel,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            val netColor = if (month.net >= 0) IncomeGreen else ExpenseRed
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = netColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${if (language == AppLanguage.BANGLA) "নিট: " else "Net: "}${CurrencyFormatter.format(month.net, currencySymbol, showSign = true, language = language)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = netColor,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "মাসিক আয়" else "Income",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                                Text(
                                    text = CurrencyFormatter.format(month.income, currencySymbol, language = language),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "মাসিক ব্যয়" else "Expenses",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                                Text(
                                    text = CurrencyFormatter.format(month.expense, currencySymbol, language = language),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                )
                            }
                        }

                        // Highest Categories in this month
                        if (!month.highestExpenseCategory.isNullOrBlank() || !month.highestIncomeCategory.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (!month.highestExpenseCategory.isNullOrBlank()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (language == AppLanguage.BANGLA) "শীর্ষ খরচের খাত" else "Top Expense Category",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                            )
                                        )
                                        Text(
                                            text = "${month.highestExpenseCategory} (${CurrencyFormatter.format(month.highestExpenseAmount, currencySymbol, language = language)})",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (!month.highestIncomeCategory.isNullOrBlank()) {
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (language == AppLanguage.BANGLA) "শীর্ষ আয়ের খাত" else "Top Income Source",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                            )
                                        )
                                        Text(
                                            text = "${month.highestIncomeCategory} (${CurrencyFormatter.format(month.highestIncomeAmount, currencySymbol, language = language)})",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
