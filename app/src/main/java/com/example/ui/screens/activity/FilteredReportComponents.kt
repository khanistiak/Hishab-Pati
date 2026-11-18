package com.example.ui.screens.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.HishabPatiCustomDateRangePickerDialog
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import com.example.ui.components.getIconForName
import com.example.ui.screens.insights.ExportFilterContext
import com.example.ui.screens.insights.ExportReportDialog
import com.example.ui.screens.insights.ExportScope
import com.example.ui.screens.insights.InsightsDateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.ActivityFilterType
import com.example.ui.components.CategoryIcon
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class BalancePoint(
    val timestamp: Long,
    val balance: Double,
    val label: String
)

data class CategoryShare(
    val categoryName: String,
    val displayName: String,
    val amount: Double,
    val percentage: Double,
    val color: Color,
    val iconName: String,
    val subcategories: List<Pair<String, Double>>
)

object FilteredReportCalculator {

    private val fallbackColors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4),
        Color(0xFFEF4444), Color(0xFF14B8A6), Color(0xFFF97316),
        Color(0xFF6366F1), Color(0xFF84CC16), Color(0xFFA855F7)
    )

    fun parseColor(hex: String, fallback: Color): Color {
        return try {
            val clean = hex.removePrefix("#")
            val colorLong = clean.toLong(16)
            if (clean.length == 6) Color(colorLong or 0x00000000FF000000) else Color(colorLong)
        } catch (_: Exception) {
            fallback
        }
    }

    /**
     * Calculates the balance of an account (or total across accounts) at timestamp T.
     * Works backwards from current balance by undoing transactions that occurred after T.
     */
    fun calculateBalanceAtTime(
        targetTimestamp: Long,
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        selectedAccountId: String?
    ): Double {
        val currentBalance = if (selectedAccountId != null) {
            accounts.find { it.id == selectedAccountId }?.balance ?: 0.0
        } else {
            accounts.filter { !it.isHidden && it.includeInTotal }.sumOf { it.balance }
        }

        val laterTxs = transactions.filter { it.timestamp > targetTimestamp }
        var undoneNetEffect = 0.0

        for (tx in laterTxs) {
            if (selectedAccountId != null) {
                if (tx.accountId == selectedAccountId) {
                    when (tx.type) {
                        TransactionType.EXPENSE, TransactionType.LEND -> undoneNetEffect += tx.amount
                        TransactionType.INCOME, TransactionType.BORROW -> undoneNetEffect -= tx.amount
                        TransactionType.TRANSFER, TransactionType.CC_PAYMENT -> undoneNetEffect += tx.amount
                    }
                }
                if (tx.toAccountId == selectedAccountId) {
                    when (tx.type) {
                        TransactionType.TRANSFER, TransactionType.CC_PAYMENT -> undoneNetEffect -= tx.amount
                        else -> {}
                    }
                }
            } else {
                when (tx.type) {
                    TransactionType.EXPENSE, TransactionType.LEND -> undoneNetEffect += tx.amount
                    TransactionType.INCOME, TransactionType.BORROW -> undoneNetEffect -= tx.amount
                    TransactionType.TRANSFER, TransactionType.CC_PAYMENT -> {
                        // Internal transfers between included accounts do not change total net balance
                    }
                }
            }
        }

        return currentBalance + undoneNetEffect
    }

    /**
     * Builds starting balance, ending balance, net change, and timeline balance points.
     */
    fun calculateBalanceTimeline(
        startTime: Long,
        endTime: Long,
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        selectedAccountId: String?,
        language: AppLanguage
    ): Triple<Double, Double, List<BalancePoint>> {
        val now = System.currentTimeMillis()
        val effectiveEnd = minOf(endTime, now)
        val effectiveStart = minOf(startTime, effectiveEnd)

        val startBal = calculateBalanceAtTime(effectiveStart, accounts, transactions, selectedAccountId)
        val endBal = calculateBalanceAtTime(effectiveEnd, accounts, transactions, selectedAccountId)

        // Generate sampling points across the period (up to 14 points)
        val points = mutableListOf<BalancePoint>()
        val dayMillis = 24L * 60 * 60 * 1000
        val totalSpan = (effectiveEnd - effectiveStart).coerceAtLeast(dayMillis)
        val step = (totalSpan / 10).coerceAtLeast(dayMillis)

        val fmt = SimpleDateFormat("dd MMM", Locale.US)
        var t = effectiveStart
        while (t <= effectiveEnd) {
            val b = calculateBalanceAtTime(t, accounts, transactions, selectedAccountId)
            val lbl = DateFormatter.localizeText(fmt.format(Date(t)), language)
            points.add(BalancePoint(t, b, lbl))
            t += step
        }
        if (points.isEmpty() || points.last().timestamp < effectiveEnd) {
            val b = calculateBalanceAtTime(effectiveEnd, accounts, transactions, selectedAccountId)
            val lbl = DateFormatter.localizeText(fmt.format(Date(effectiveEnd)), language)
            points.add(BalancePoint(effectiveEnd, b, lbl))
        }

        return Triple(startBal, endBal, points)
    }

    /**
     * Groups filtered transactions by category and computes percentages and subcategories.
     */
    fun calculateCategoryBreakdown(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        language: AppLanguage
    ): Pair<Double, List<CategoryShare>> {
        val total = transactions.sumOf { it.amount }
        if (total <= 0.0) return 0.0 to emptyList()

        val catMap = categories.associateBy { it.name }
        val grouped = transactions.groupBy { it.category }

        val shares = grouped.entries.toList().mapIndexed { index, entry ->
            val catName = entry.key
            val txs = entry.value
            val sum = txs.sumOf { it.amount }
            val pct = (sum / total) * 100.0
            val catEntity = catMap[catName]
            val dispName = catEntity?.getDisplayName(language) ?: catName
            val fallback = fallbackColors[index % fallbackColors.size]
            val color = if (catEntity?.colorHex != null) parseColor(catEntity.colorHex, fallback) else fallback
            val iconName = catEntity?.iconName ?: "category"

            val uncategorizedLabel = if (language == AppLanguage.BANGLA) "ক্যাটাগরিহীন" else "Uncategorized"
            val subGrouped = txs.groupBy { if (it.subcategory.isBlank()) uncategorizedLabel else it.subcategory }
                .entries.map { subEntry -> subEntry.key to subEntry.value.sumOf { it.amount } }
                .sortedByDescending { it.second }

            CategoryShare(
                categoryName = catName,
                displayName = dispName,
                amount = sum,
                percentage = pct,
                color = color,
                iconName = iconName,
                subcategories = subGrouped
            )
        }.sortedByDescending { it.amount }

        return total to shares
    }
}

/**
 * Balance Trend Card showing Starting Balance, Ending Balance, Net Change,
 * and an interactive bezier area chart with scrubber tooltip.
 */
@Composable
fun BalanceTrendCard(
    startingBalance: Double,
    endingBalance: Double,
    timelinePoints: List<BalancePoint>,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val netChange = endingBalance - startingBalance

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("balance_trend_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Starting & Ending Balance Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = if (isBn) "শুরুর ব্যালেন্স" else "Starting Balance",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(startingBalance, currencySymbol, false, language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isBn) "শেষ ব্যালেন্স" else "Ending Balance",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(endingBalance, currencySymbol, false, language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            // Net Change indicator badge
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isPositive = netChange >= 0
                val badgeColor = if (isPositive) IncomeGreen else ExpenseRed
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = CurrencyFormatter.format(netChange, currencySymbol, true, language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                Text(
                    text = if (isBn) "সময়ের পরিবর্তন" else "net period change",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Balance Area Chart
            InteractiveBalanceChart(
                points = timelinePoints,
                currencySymbol = currencySymbol,
                language = language,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }
    }
}

@Composable
private fun InteractiveBalanceChart(
    points: List<BalancePoint>,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var selectedPointIndex by remember(points) {
        mutableStateOf<Int?>(points.lastIndex)
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val activePoint = selectedPointIndex?.let { points.getOrNull(it) } ?: points.last()

    Column(modifier = modifier) {
        // Scrubber Tooltip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activePoint.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            )
            Text(
                text = CurrencyFormatter.format(activePoint.balance, currencySymbol, false, language),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(points) {
                    detectTapGestures(
                        onPress = { offset ->
                            val width = size.width.toFloat()
                            val segWidth = width / (points.size - 1).coerceAtLeast(1)
                            val idx = (offset.x / segWidth).roundToInt().coerceIn(0, points.lastIndex)
                            selectedPointIndex = idx
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val topPadding = 12f
                val bottomPadding = 8f
                val chartHeight = (h - topPadding - bottomPadding).coerceAtLeast(10f)

                val minVal = points.minOf { it.balance }
                val maxVal = points.maxOf { it.balance }
                val range = (maxVal - minVal).coerceAtLeast(1.0)

                val stepX = w / (points.size - 1).coerceAtLeast(1)

                val path = Path()
                val fillPath = Path()

                points.forEachIndexed { i, p ->
                    val x = i * stepX
                    val normY = ((p.balance - minVal) / range).toFloat()
                    val y = topPadding + (1f - normY) * chartHeight

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, h)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevX = (i - 1) * stepX
                        val prevNormY = ((points[i - 1].balance - minVal) / range).toFloat()
                        val prevY = topPadding + (1f - prevNormY) * chartHeight
                        val cx = (prevX + x) / 2f
                        path.cubicTo(cx, prevY, cx, y, x, y)
                        fillPath.cubicTo(cx, prevY, cx, y, x, y)
                    }

                    if (i == points.lastIndex) {
                        fillPath.lineTo(x, h)
                        fillPath.close()
                    }
                }

                // Fill gradient
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.22f),
                            lineColor.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )

                // Line Stroke
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active point indicator
                selectedPointIndex?.let { selIdx ->
                    if (selIdx in points.indices) {
                        val selX = selIdx * stepX
                        val selNormY = ((points[selIdx].balance - minVal) / range).toFloat()
                        val selY = topPadding + (1f - selNormY) * chartHeight

                        // Vertical guide line
                        drawLine(
                            color = lineColor.copy(alpha = 0.35f),
                            start = Offset(selX, 0f),
                            end = Offset(selX, h),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Outer ring & center dot
                        drawCircle(
                            color = lineColor.copy(alpha = 0.25f),
                            radius = 7.dp.toPx(),
                            center = Offset(selX, selY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.5.dp.toPx(),
                            center = Offset(selX, selY)
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 3.dp.toPx(),
                            center = Offset(selX, selY)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Category Breakdown Card with:
 * - Segmented multi-color proportion bar
 * - Horizontal category icons / chips row
 * - Subcategory breakdown for selected category
 * - Reset category filter button
 */
@Composable
fun FilteredCategoryBreakdownCard(
    totalAmount: Double,
    categoryShares: List<CategoryShare>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    currencySymbol: String,
    language: AppLanguage,
    isIncomeMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    var inspectedCategory by remember(categoryShares, selectedCategory) {
        mutableStateOf<String?>(selectedCategory)
    }

    val activeShare = categoryShares.find { it.categoryName.equals(inspectedCategory, ignoreCase = true) }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("filtered_category_breakdown_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. Header: Category Title + Share % on left, Amount on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeShare != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = activeShare.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.testTag("report_active_category_title")
                        )
                        Text(
                            text = "${activeShare.percentage.roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 15.sp
                            )
                        )
                    }
                    Text(
                        text = CurrencyFormatter.format(
                            activeShare.amount,
                            currencySymbol,
                            false,
                            language
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.testTag("report_active_category_amount")
                    )
                } else {
                    Text(
                        text = if (isBn) "সকল ক্যাটাগরি" else "All Categories",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.testTag("report_all_categories_title")
                    )
                    Text(
                        text = CurrencyFormatter.format(
                            totalAmount,
                            currencySymbol,
                            false,
                            language
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.testTag("report_all_categories_amount")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Multi-segment horizontal proportion bar (taller, clickable segments)
            SegmentedProportionBar(
                shares = categoryShares,
                selectedCategory = inspectedCategory,
                onSelectCategory = { cat ->
                    inspectedCategory = cat
                },
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Subcategories breakdown (when a category is inspected)
            if (activeShare != null && activeShare.subcategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_subcategories_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeShare.subcategories.forEach { (subName, subAmt) ->
                        val subPct = if (activeShare.amount > 0.0) ((subAmt / activeShare.amount) * 100.0).roundToInt() else 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_subcategory_row_$subName"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$subName : $subPct%",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    fontSize = 14.sp
                                ),
                                modifier = Modifier.testTag("report_subcategory_label_$subName")
                            )
                            Text(
                                text = CurrencyFormatter.format(subAmt, currencySymbol, false, language),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                ),
                                modifier = Modifier.testTag("report_subcategory_amount_$subName")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Horizontal Category Selector Chips Row (All Categories pill + circular icons)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "All Categories" pill
                item {
                    val isAllSelected = inspectedCategory == null
                    Surface(
                        onClick = { inspectedCategory = null },
                        shape = RoundedCornerShape(22.dp),
                        color = if (isAllSelected) Color(0xFF232D3F) else Color.White,
                        border = if (isAllSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .testTag("filter_all_categories_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = if (isBn) "সকল ক্যাটাগরি" else "All Categories",
                                tint = if (isAllSelected) Color.White else Color(0xFF1E293B),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isBn) "সকল ক্যাটাগরি" else "All Categories",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isAllSelected) Color.White else Color(0xFF1E293B),
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                // Circular Category Badges
                items(categoryShares, key = { it.categoryName }) { share ->
                    val isSelected = inspectedCategory?.equals(share.categoryName, ignoreCase = true) == true
                    Surface(
                        onClick = {
                            inspectedCategory = if (isSelected) null else share.categoryName
                        },
                        shape = CircleShape,
                        color = if (isSelected) share.color else Color.White,
                        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .testTag("filter_category_chip_${share.categoryName}")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconForName(share.iconName),
                                contentDescription = share.displayName,
                                tint = if (isSelected) Color.White else share.color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 5. Action Button: View Transactions or Reset Filter
            val isCurrentlyFiltered = selectedCategory != null
            val isInspectingFilteredCategory = activeShare != null && selectedCategory?.equals(activeShare.categoryName, ignoreCase = true) == true

            if (activeShare != null || isCurrentlyFiltered) {
                Spacer(modifier = Modifier.height(16.dp))

                if (isInspectingFilteredCategory) {
                    Surface(
                        onClick = {
                            inspectedCategory = null
                            onSelectCategory(null)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("reset_category_filter_button")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBn) "ফিল্টার মুছুন" else "Reset Filter",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                } else if (activeShare != null) {
                    Surface(
                        onClick = {
                            onSelectCategory(activeShare.categoryName)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("view_transactions_category_button")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBn) "লেনদেন দেখুন" else "View Transactions",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                } else if (isCurrentlyFiltered) {
                    Surface(
                        onClick = {
                            inspectedCategory = null
                            onSelectCategory(null)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("reset_category_filter_button")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBn) "ফিল্টার মুছুন" else "Reset Filter",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
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
fun SegmentedProportionBar(
    shares: List<CategoryShare>,
    selectedCategory: String?,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (shares.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        shares.forEach { share ->
            val isHighlighted = selectedCategory == null || share.categoryName.equals(selectedCategory, ignoreCase = true)
            val weight = (share.percentage / 100.0).toFloat().coerceAtLeast(0.015f)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isHighlighted) share.color else share.color.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelectCategory(share.categoryName) }
                    .testTag("report_category_bar_segment_${share.categoryName}")
            )
        }
    }
}

/**
 * Apply Filters Modal / Bottom Sheet.
 * Implements the full flow shown in the video:
 * - Transaction Type: Expense, Income, Transfer
 * - Date Range: Quick presets and Custom Date Range picker
 * - Category: All Categories switch toggle and individual category selection
 * - Wallets: All Wallets and individual wallet account selection
 * - Apply & Reset buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyFiltersBottomSheet(
    onDismiss: () -> Unit,
    currentType: ActivityFilterType,
    currentTimeRange: String,
    currentCustomDateRange: Pair<Long, Long>?,
    currentCategory: String?,
    currentAccountId: String?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage,
    onApply: (type: ActivityFilterType, timeRange: String, customDateRange: Pair<Long, Long>?, category: String?, accountId: String?) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val context = LocalContext.current

    var tempType by remember { mutableStateOf(if (currentType == ActivityFilterType.ALL) ActivityFilterType.EXPENSES else currentType) }
    var tempTimeRange by remember { mutableStateOf(currentTimeRange) }
    var tempCustomDateRange by remember { mutableStateOf(currentCustomDateRange) }
    var tempAllCategories by remember { mutableStateOf(currentCategory == null) }
    var tempCategory by remember { mutableStateOf(currentCategory) }
    var tempAccountId by remember { mutableStateOf(currentAccountId) }
    var showCustomDateRangePicker by remember { mutableStateOf(false) }

    val formattedDateRange = remember(tempTimeRange, tempCustomDateRange, language) {
        when {
            tempCustomDateRange != null -> {
                val fmt = SimpleDateFormat("MMMM d, yyyy", Locale.US)
                val s = DateFormatter.localizeText(fmt.format(Date(tempCustomDateRange!!.first)), language)
                val e = DateFormatter.localizeText(fmt.format(Date(tempCustomDateRange!!.second)), language)
                "$s - $e"
            }
            tempTimeRange == "THIS_MONTH" -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val fmt = SimpleDateFormat("MMMM 1 - MMMM ", Locale.US)
                val endFmt = SimpleDateFormat("d, yyyy", Locale.US)
                val start = fmt.format(cal.time)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                DateFormatter.localizeText("$start${endFmt.format(cal.time)}", language)
            }
            tempTimeRange == "LAST_MONTH" -> {
                val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val fmt = SimpleDateFormat("MMMM 1 - MMMM ", Locale.US)
                val endFmt = SimpleDateFormat("d, yyyy", Locale.US)
                val start = fmt.format(cal.time)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                DateFormatter.localizeText("$start${endFmt.format(cal.time)}", language)
            }
            tempTimeRange == "THIS_WEEK" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                }
                val sFmt = SimpleDateFormat("MMMM d - ", Locale.US).format(cal.time)
                cal.add(Calendar.DAY_OF_WEEK, 6)
                val eFmt = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(cal.time)
                DateFormatter.localizeText("$sFmt$eFmt", language)
            }
            tempTimeRange == "PREVIOUS_WEEK" -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.WEEK_OF_YEAR, -1)
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                }
                val sFmt = SimpleDateFormat("MMMM d - ", Locale.US).format(cal.time)
                cal.add(Calendar.DAY_OF_WEEK, 6)
                val eFmt = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(cal.time)
                DateFormatter.localizeText("$sFmt$eFmt", language)
            }
            tempTimeRange == "THIS_YEAR" -> {
                val yr = Calendar.getInstance().get(Calendar.YEAR)
                DateFormatter.localizeText("January 1 - December 31, $yr", language)
            }
            else -> if (isBn) "সকল সময়" else "All Time"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("apply_filters_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header: Title, Subtitle, Checkmark Apply button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) "ফিল্টার প্রয়োগ করুন" else "Apply Filters",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBn) "আপনার ফিল্টার নির্বাচন করুন এবং চেক চাপুন" else "Customize your filters and press the check button to apply.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Checkmark Apply Button
                Surface(
                    onClick = {
                        onApply(
                            tempType,
                            tempTimeRange,
                            tempCustomDateRange,
                            if (tempAllCategories) null else tempCategory,
                            tempAccountId
                        )
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .testTag("apply_filters_check_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Apply",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 1. Transaction Type Selector (Expense, Income, Transfer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Expense
                val isExp = tempType == ActivityFilterType.EXPENSES
                Surface(
                    onClick = {
                        tempType = ActivityFilterType.EXPENSES
                        tempCategory = null
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isExp) ExpenseRed.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = if (isExp) BorderStroke(1.5.dp, ExpenseRed) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("filter_type_expense_tab")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isExp) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "খরচ" else "Expense",
                            fontWeight = if (isExp) FontWeight.Bold else FontWeight.Medium,
                            color = if (isExp) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }

                // Income
                val isInc = tempType == ActivityFilterType.INCOME
                Surface(
                    onClick = {
                        tempType = ActivityFilterType.INCOME
                        tempCategory = null
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isInc) IncomeGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = if (isInc) BorderStroke(1.5.dp, IncomeGreen) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("filter_type_income_tab")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isInc) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "আয়" else "Income",
                            fontWeight = if (isInc) FontWeight.Bold else FontWeight.Medium,
                            color = if (isInc) IncomeGreen else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }

                // Transfer
                val isTrf = tempType == ActivityFilterType.TRANSFERS
                val transferColor = MaterialTheme.colorScheme.primary
                Surface(
                    onClick = {
                        tempType = ActivityFilterType.TRANSFERS
                        tempCategory = null
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isTrf) transferColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = if (isTrf) BorderStroke(1.5.dp, transferColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("filter_type_transfer_tab")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = if (isTrf) transferColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "ট্রান্সফার" else "Transfer",
                            fontWeight = if (isTrf) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTrf) transferColor else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 2. Date Range Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isBn) "তারিখের সীমা" else "Date Range",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                // Date Range Display Card
                Surface(
                    onClick = { showCustomDateRangePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("filter_date_range_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isBn) "তারিখ" else "Date",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formattedDateRange,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Quick Period Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    val presets = listOf(
                        "THIS_WEEK" to (if (isBn) "চলতি সপ্তাহ" else "This Week"),
                        "THIS_MONTH" to (if (isBn) "চলতি মাস" else "This Month"),
                        "THIS_YEAR" to (if (isBn) "চলতি বছর" else "This Year"),
                        "PREVIOUS_WEEK" to (if (isBn) "আগের সপ্তাহ" else "Previous Week"),
                        "LAST_MONTH" to (if (isBn) "গত মাস" else "Last Month"),
                        "PAST_3_MONTHS" to (if (isBn) "গত ৩ মাস" else "Past 3 Months"),
                        "LAST_YEAR" to (if (isBn) "গত বছর" else "Last Year"),
                        "ALL" to (if (isBn) "সকল সময়" else "All Time")
                    )

                    items(presets) { (key, label) ->
                        val isSelected = tempTimeRange == key && tempCustomDateRange == null
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                tempTimeRange = key
                                tempCustomDateRange = null
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // 3. Category Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // All Categories switch toggle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isBn) "সকল ক্যাটাগরি" else "All Categories",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (isBn) "সব ক্যাটাগরির লেনদেন দেখুন" else "View transactions across all categories",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Switch(
                            checked = tempAllCategories,
                            onCheckedChange = { checked ->
                                tempAllCategories = checked
                                if (checked) tempCategory = null
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("all_categories_toggle")
                        )
                    }
                }

                // If All Categories toggle is OFF, show category selection chips
                AnimatedVisibility(visible = !tempAllCategories) {
                    val matchingCategories = categories.filter { cat ->
                        when (tempType) {
                            ActivityFilterType.EXPENSES -> !cat.isIncome && !cat.isTransfer
                            ActivityFilterType.INCOME -> cat.isIncome && !cat.isTransfer
                            ActivityFilterType.TRANSFERS -> cat.isTransfer
                            ActivityFilterType.ALL -> true
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(matchingCategories, key = { it.id }) { cat ->
                            val isSel = tempCategory?.equals(cat.name, ignoreCase = true) == true
                            val catColor = FilteredReportCalculator.parseColor(cat.colorHex, MaterialTheme.colorScheme.primary)
                            FilterChip(
                                selected = isSel,
                                onClick = { tempCategory = cat.name },
                                label = { Text(cat.getDisplayName(language), fontSize = 12.sp) },
                                leadingIcon = {
                                    CategoryIcon(
                                        iconName = cat.iconName,
                                        colorHex = cat.colorHex,
                                        size = 20.dp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = catColor.copy(alpha = 0.16f),
                                    selectedLabelColor = catColor,
                                    selectedLeadingIconColor = catColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSel,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                    selectedBorderColor = catColor,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.5.dp
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            // 4. Wallets / Accounts Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "ওয়ালেটসমূহ (${accounts.size})" else "Wallets (${accounts.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    if (tempAccountId != null) {
                        TextButton(onClick = { tempAccountId = null }) {
                            Text(if (isBn) "সকল নির্বাচন করুন" else "Select All", fontSize = 12.sp)
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    // "All Wallets" chip card
                    item {
                        val isAllWallets = tempAccountId == null
                        val allWalletsColor = MaterialTheme.colorScheme.primary
                        Surface(
                            onClick = { tempAccountId = null },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isAllWallets) allWalletsColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isAllWallets) BorderStroke(1.5.dp, allWalletsColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .testTag("filter_all_wallets_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isAllWallets) allWalletsColor else allWalletsColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, if (isAllWallets) allWalletsColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = if (isAllWallets) Color.White else allWalletsColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (isBn) "সকল ওয়ালেট" else "All Wallets",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isAllWallets) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isAllWallets) allWalletsColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Individual Wallets
                    items(accounts.filter { !it.isHidden }, key = { it.id }) { acc ->
                        val isSelected = tempAccountId == acc.id
                        val walletColor = FilteredReportCalculator.parseColor(acc.colorHex, MaterialTheme.colorScheme.primary)
                        Surface(
                            onClick = { tempAccountId = acc.id },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) walletColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) BorderStroke(1.5.dp, walletColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .testTag("filter_wallet_card_${acc.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) walletColor else walletColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, if (isSelected) walletColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else walletColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) walletColor else MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(acc.balance, currencySymbol, false, language),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons: Reset & Apply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        tempType = ActivityFilterType.ALL
                        tempTimeRange = "ALL"
                        tempCustomDateRange = null
                        tempAllCategories = true
                        tempCategory = null
                        tempAccountId = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(if (isBn) "রিসেট করুন" else "Reset Filters")
                }

                Button(
                    onClick = {
                        onApply(
                            tempType,
                            tempTimeRange,
                            tempCustomDateRange,
                            if (tempAllCategories) null else tempCategory,
                            tempAccountId
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isBn) "প্রয়োগ করুন" else "Apply Filters", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showCustomDateRangePicker) {
        val nowMillis = remember { System.currentTimeMillis() }
        HishabPatiCustomDateRangePickerDialog(
            initialStartDateMillis = tempCustomDateRange?.first ?: nowMillis,
            initialEndDateMillis = tempCustomDateRange?.second,
            language = language,
            onDateRangeSelected = { range, _ ->
                tempCustomDateRange = range
                tempTimeRange = "CUSTOM"
                showCustomDateRangePicker = false
            },
            onDismissRequest = { showCustomDateRangePicker = false }
        )
    }
}

/**
 * Bottom summary footer showing total amount for the filtered results.
 */
@Composable
fun FilteredReportSummaryBar(
    totalAmount: Double,
    filterType: ActivityFilterType,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val label = when (filterType) {
        ActivityFilterType.EXPENSES -> if (isBn) "মোট খরচ" else "Total Expense"
        ActivityFilterType.INCOME -> if (isBn) "মোট আয়" else "Total Income"
        ActivityFilterType.TRANSFERS -> if (isBn) "মোট ট্রান্সফার" else "Total Transfers"
        ActivityFilterType.ALL -> if (isBn) "নেট ব্যালেন্স" else "Total Net"
    }

    val amountColor = when (filterType) {
        ActivityFilterType.EXPENSES -> ExpenseRed
        ActivityFilterType.INCOME -> IncomeGreen
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            val prefix = if (filterType == ActivityFilterType.EXPENSES) "-" else if (filterType == ActivityFilterType.INCOME) "+" else ""
            Text(
                text = "$prefix${CurrencyFormatter.format(totalAmount, currencySymbol, false, language)}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    fontSize = 20.sp
                )
            )
        }
    }
}

@Composable
fun TransactionRowItem(
    tx: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val isExpense = tx.type == TransactionType.EXPENSE || tx.type == TransactionType.LEND
    val isIncome = tx.type == TransactionType.INCOME || tx.type == TransactionType.BORROW
    val isTransfer = tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT

    val acc = accounts.firstOrNull { it.id == tx.accountId }
    val toAcc = if (tx.toAccountId != null) accounts.firstOrNull { it.id == tx.toAccountId } else null
    val accountLabel = if (isTransfer && toAcc != null) {
        "${acc?.name ?: "Wallet"} → ${toAcc.name}"
    } else {
        acc?.name ?: "Wallet"
    }

    val catEntity = categories.firstOrNull { it.name.equals(tx.category, ignoreCase = true) }
    val iconName = when {
        isTransfer -> "swap_horiz"
        else -> catEntity?.iconName ?: when (tx.category) {
            "Daily Essentials" -> "shopping_cart"
            "Eating Out" -> "restaurant"
            "Transportation" -> "directions_car"
            "Health Care" -> "medical_services"
            "Rent & Utilities" -> "home"
            else -> "category"
        }
    }

    val colorHex = when {
        isIncome -> "#10B981"
        isTransfer -> "#3B82F6"
        else -> catEntity?.colorHex ?: "#EF4444"
    }

    val categorySubcategory = when {
        tx.subcategory.isNotBlank() -> "${tx.category} • ${tx.subcategory}"
        tx.category.isNotBlank() -> tx.category
        isTransfer -> if (isBn) "স্থানান্তর" else "Transfer"
        else -> ""
    }

    val amountPrefix = when {
        isIncome -> "+ "
        isExpense -> "- "
        else -> ""
    }

    val amountColor = when {
        isIncome -> IncomeGreen
        isExpense -> ExpenseRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag("report_tx_item_${tx.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            iconName = iconName,
            colorHex = colorHex,
            size = 42.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (categorySubcategory.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = categorySubcategory,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amountPrefix + CurrencyFormatter.format(tx.amount, currencySymbol, false, language),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = amountColor
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = accountLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDateRangePill(
    customLabel: String?,
    timeRange: String,
    startTime: Long,
    endTime: Long,
    language: AppLanguage
): String {
    if (customLabel != null) return customLabel
    if (timeRange == "ALL" || startTime <= 0L) {
        return if (language == AppLanguage.BANGLA) "সকল সময়" else "All Time"
    }

    val startCal = Calendar.getInstance().apply { timeInMillis = startTime }
    val endCal = Calendar.getInstance().apply { timeInMillis = endTime }

    val startMonthFmt = SimpleDateFormat("MMM", Locale.US)
    val endMonthFmt = SimpleDateFormat("MMM", Locale.US)
    val sMonth = startMonthFmt.format(Date(startTime))
    val eMonth = endMonthFmt.format(Date(endTime))
    val sDay = startCal.get(Calendar.DAY_OF_MONTH)
    val eDay = endCal.get(Calendar.DAY_OF_MONTH)

    val rawText = if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)) {
        "$sMonth $sDay - $eMonth $eDay"
    } else {
        val sYear = startCal.get(Calendar.YEAR)
        val endYear = endCal.get(Calendar.YEAR)
        "$sMonth $sDay, $sYear - $eMonth $eDay, $endYear"
    }

    return DateFormatter.localizeText(rawText, language)
}

/**
 * Filtered Report Main Content view within the Activity Tab.
 * Presents:
 * 1. Top bar with Back button and "Filtered Reports" Title (matching Screenshot 1).
 * 2. Active filter controls: Settings/sliders icon button, Transaction Type chip, and Date Range chip.
 * 3. Quick period tabs (This Week, This Month, This Year, Previous Week, Last Month, Past 3 Months, Last Year).
 * 4. Balance Trend Card with starting balance, ending balance, net change, and interactive curve.
 * 5. Category Breakdown Card or Transfer Summary Card.
 * 6. Transactions section header with count, category badge, and active sort order.
 * 7. Grouped transactions list or empty state.
 */
@Composable
fun FilteredReportContent(
    filteredList: List<TransactionEntity>,
    groupedByDate: Map<String, List<TransactionEntity>>,
    selectedFilter: ActivityFilterType,
    selectedSort: com.example.ui.ActivitySort,
    selectedTimeRange: String,
    customDateRangeLabel: String?,
    selectedAccountIdFilter: String?,
    selectedCategoryFilter: String?,
    startingBalance: Double,
    endingBalance: Double,
    timelinePoints: List<BalancePoint>,
    totalCategoryAmount: Double,
    categoryShares: List<CategoryShare>,
    currencySymbol: String,
    language: AppLanguage,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    startTime: Long = 0L,
    endTime: Long = System.currentTimeMillis(),
    reportTotalAmount: Double? = null,
    onBack: () -> Unit,
    onOpenFilter: () -> Unit,
    onOpenSort: () -> Unit = {},
    onCycleFilterType: () -> Unit,
    onSelectPeriod: (String) -> Unit,
    onSelectCustomDateRange: (Pair<Long, Long>, String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onClearCategory: () -> Unit,
    onClearAccount: () -> Unit,
    onResetAll: () -> Unit,
    onTxClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    searchQuery: String = "",
    selectedSubcategoryFilter: String? = null,
    onExportComplete: (String) -> Unit = {}
) {
    val isBn = language == AppLanguage.BANGLA
    val context = LocalContext.current
    var isDatePresetsExpanded by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showCustomDateRangePicker by remember { mutableStateOf(false) }

    val effectiveReportTotal = reportTotalAmount ?: remember(filteredList, selectedFilter) {
        when (selectedFilter) {
            ActivityFilterType.EXPENSES -> filteredList.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LEND }.sumOf { it.amount }
            ActivityFilterType.INCOME -> filteredList.filter { it.type == TransactionType.INCOME || it.type == TransactionType.BORROW }.sumOf { it.amount }
            ActivityFilterType.TRANSFERS -> filteredList.filter { it.type == TransactionType.TRANSFER || it.type == TransactionType.CC_PAYMENT }.sumOf { it.amount }
            ActivityFilterType.ALL -> {
                var sum = 0.0
                filteredList.forEach { tx ->
                    when (tx.type) {
                        TransactionType.INCOME, TransactionType.BORROW -> sum += tx.amount
                        TransactionType.EXPENSE, TransactionType.LEND -> sum -= tx.amount
                        else -> {}
                    }
                }
                sum
            }
        }
    }

    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 120.dp,
        extraTopGap = 4.dp,
        bottomNavClearance = 104.dp
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    scrollLayout.backdropState,
                    listState,
                    scrollLayout.contentGraphicsLayer
                )
                .testTag("filtered_report_lazy_column"),
            contentPadding = scrollLayout.contentPadding
        ) {
            // 1. Balance Trend Card
            item(key = "report_balance_trend") {
            BalanceTrendCard(
                startingBalance = startingBalance,
                endingBalance = endingBalance,
                timelinePoints = timelinePoints,
                currencySymbol = currencySymbol,
                language = language
            )
        }

        // 5. Transfer Summary Card (when Transfer selected) OR Category Breakdown Card (for Expense / Income)
        if (selectedFilter == ActivityFilterType.TRANSFERS) {
            item(key = "report_transfer_summary_card") {
                val totalTransfers = filteredList.filter { it.type == TransactionType.TRANSFER || it.type == TransactionType.CC_PAYMENT }.sumOf { it.amount }
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .testTag("report_transfer_summary_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isBn) "ট্রান্সফার" else "Transfer",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (isBn) "${filteredList.size} টি লেনদেন" else "${filteredList.size} Transactions",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Text(
                            text = CurrencyFormatter.format(totalTransfers, currencySymbol, false, language),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        } else if (categoryShares.isNotEmpty()) {
            item(key = "report_category_breakdown") {
                FilteredCategoryBreakdownCard(
                    totalAmount = totalCategoryAmount,
                    categoryShares = categoryShares,
                    selectedCategory = selectedCategoryFilter,
                    onSelectCategory = onSelectCategory,
                    currencySymbol = currencySymbol,
                    language = language,
                    isIncomeMode = selectedFilter == ActivityFilterType.INCOME
                )
            }
        }

        // 6. Transactions Header with Count & Sort button
        item(key = "report_tx_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isBn) "লেনদেন" else "Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    if (selectedCategoryFilter != null) {
                        Surface(
                            onClick = { onSelectCategory(null) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("active_category_filter_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedCategoryFilter,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear category filter",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                // Far right: Show the currently filtered total amount
                Text(
                    text = CurrencyFormatter.format(
                        effectiveReportTotal,
                        currencySymbol,
                        false,
                        language
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.testTag("report_filtered_transactions_total")
                )
            }
        }

        // 7. Transactions List or Empty State
        if (groupedByDate.isEmpty()) {
            item(key = "report_empty_state") {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .testTag("report_empty_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isBn) "আপনার নির্বাচিত ফিল্টারে কোনো লেনদেন পাওয়া যায়নি।" else "You don't have transactions within your selected filters.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onResetAll,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(if (isBn) "ফিল্টার রিসেট করুন" else "Reset Filter")
                        }
                    }
                }
            }
        } else {
            groupedByDate.forEach { (dateHeader, txsInDate) ->
                val dayExpenses = txsInDate.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LEND }.sumOf { it.amount }
                val dayIncome = txsInDate.filter { it.type == TransactionType.INCOME || it.type == TransactionType.BORROW }.sumOf { it.amount }

                item(key = "date_header_$dateHeader") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = dateHeader,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        if (dayExpenses > 0) {
                            Text(
                                text = "Δ -" + CurrencyFormatter.format(dayExpenses, currencySymbol, false, language),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            )
                        } else if (dayIncome > 0) {
                            Text(
                                text = "Δ +" + CurrencyFormatter.format(dayIncome, currencySymbol, false, language),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen,
                                    fontSize = 12.sp
                                )
                            )
                        } else {
                            Text(
                                text = "Δ " + CurrencyFormatter.format(0.0, currencySymbol, false, language),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                item(key = "date_card_$dateHeader") {
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            txsInDate.forEachIndexed { index, tx ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        thickness = 0.5.dp
                                    )
                                }
                                TransactionRowItem(
                                    tx = tx,
                                    accounts = accounts,
                                    categories = categories,
                                    currencySymbol = currencySymbol,
                                    language = language,
                                    onClick = { onTxClick(tx) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Shared Fixed Scroll-Aware Frosted Header for Filtered Reports (pins title and compact filter controls)
    ScrollAwareFixedHeader(
        layoutState = scrollLayout,
        testTag = "report_fixed_header"
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("report_back_button"),
                        onClick = onBack
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isBn) "ফিল্টার রিপোর্ট" else "Filtered Reports",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = if (isBn) "নির্বাচিত ফিল্টার অনুযায়ী লেনদেনের বিশ্লেষণ" else "Custom transaction filters & analytics",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Export Report Button in Filtered Reports Header
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("filtered_report_export_btn"),
                    onClick = { showExportDialog = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Report",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Compact Filter Controls pinned directly beneath header
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings / Sliders Filter Button (compact 34dp)
                item {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .testTag("report_settings_filter_button"),
                        onClick = onOpenFilter
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Apply Filters",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Transaction Type Pill (e.g. Expense / Income / Transfer / All) - compact 34dp height
                item {
                    val (typeLabel, typeBg, typeFg) = when (selectedFilter) {
                        ActivityFilterType.EXPENSES -> Triple(
                            if (isBn) "খরচ" else "Expense",
                            ExpenseRed.copy(alpha = 0.16f),
                            ExpenseRed
                        )
                        ActivityFilterType.INCOME -> Triple(
                            if (isBn) "আয়" else "Income",
                            IncomeGreen.copy(alpha = 0.16f),
                            IncomeGreen
                        )
                        ActivityFilterType.TRANSFERS -> Triple(
                            if (isBn) "ট্রান্সফার" else "Transfer",
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.primary
                        )
                        ActivityFilterType.ALL -> Triple(
                            if (isBn) "সকল" else "All",
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        onClick = onCycleFilterType,
                        shape = RoundedCornerShape(17.dp),
                        color = typeBg,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .testTag("report_type_chip")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = typeFg,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                    }
                }

                // Date Range Pill (Dark Slate capsule with Calendar icon, date range, and expand/collapse toggle) - compact 34dp height
                item {
                    val dateText = formatDateRangePill(
                        customLabel = customDateRangeLabel,
                        timeRange = selectedTimeRange,
                        startTime = startTime,
                        endTime = endTime,
                        language = language
                    )
                    Surface(
                        onClick = { isDatePresetsExpanded = !isDatePresetsExpanded },
                        shape = RoundedCornerShape(17.dp),
                        color = Color(0xFF4A5568),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .testTag("report_date_range_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 11.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontSize = 12.5.sp
                                )
                            )
                            Icon(
                                imageVector = if (isDatePresetsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isDatePresetsExpanded) "Collapse date presets" else "Expand date presets",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Wallet pill (if specific wallet selected) - compact 34dp
                if (selectedAccountIdFilter != null) {
                    val acc = accounts.find { it.id == selectedAccountIdFilter }
                    item {
                        Surface(
                            onClick = onClearAccount,
                            shape = RoundedCornerShape(17.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .testTag("report_wallet_chip")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 11.dp)
                            ) {
                                Text(
                                    text = "${acc?.name ?: "Wallet"} ✕",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 12.5.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Category pill (if specific category selected) - compact 34dp
                if (selectedCategoryFilter != null) {
                    item {
                        Surface(
                            onClick = onClearCategory,
                            shape = RoundedCornerShape(17.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .testTag("report_category_chip")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 11.dp)
                            ) {
                                Text(
                                    text = "$selectedCategoryFilter ✕",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 12.5.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Reset Text button if any filter is customized
                val hasCustomFilters = selectedFilter != ActivityFilterType.ALL ||
                        selectedAccountIdFilter != null ||
                        selectedCategoryFilter != null ||
                        selectedTimeRange != "ALL" ||
                        customDateRangeLabel != null

                if (hasCustomFilters) {
                    item {
                        TextButton(
                            onClick = onResetAll,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("report_reset_button")
                        ) {
                            Text(
                                text = if (isBn) "মুছুন" else "Reset",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Quick Period Tabs (expandable presets below the filter controls, if expanded)
            AnimatedVisibility(
                visible = isDatePresetsExpanded,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val periodTabs = listOf(
                        "THIS_WEEK" to (if (isBn) "চলতি সপ্তাহ" else "This Week"),
                        "THIS_MONTH" to (if (isBn) "চলতি মাস" else "This Month"),
                        "THIS_YEAR" to (if (isBn) "চলতি বছর" else "This Year"),
                        "PREVIOUS_WEEK" to (if (isBn) "আগের সপ্তাহ" else "Previous Week"),
                        "LAST_MONTH" to (if (isBn) "গত মাস" else "Last Month"),
                        "PAST_3_MONTHS" to (if (isBn) "গত ৩ মাস" else "Past 3 Months"),
                        "LAST_YEAR" to (if (isBn) "গত বছর" else "Last Year"),
                        "CUSTOM" to (if (isBn) "কাস্টম" else "Custom")
                    )
                    items(periodTabs) { (key, label) ->
                        val isSelected = if (key == "CUSTOM") {
                            customDateRangeLabel != null || selectedTimeRange == "CUSTOM"
                        } else {
                            selectedTimeRange == key && customDateRangeLabel == null
                        }
                        Surface(
                            onClick = {
                                if (key == "CUSTOM") {
                                    showCustomDateRangePicker = true
                                } else {
                                    onSelectPeriod(key)
                                }
                            },
                            shape = RoundedCornerShape(17.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .testTag("period_tab_$key")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

        if (showExportDialog) {
            val periodLabelEn = formatDateRangePill(
                customLabel = customDateRangeLabel,
                timeRange = selectedTimeRange,
                startTime = startTime,
                endTime = endTime,
                language = AppLanguage.ENGLISH
            )
            val periodLabelBn = formatDateRangePill(
                customLabel = customDateRangeLabel,
                timeRange = selectedTimeRange,
                startTime = startTime,
                endTime = endTime,
                language = AppLanguage.BANGLA
            )
            val (typeEn, typeBn) = when (selectedFilter) {
                ActivityFilterType.EXPENSES -> "Expense" to "খরচ"
                ActivityFilterType.INCOME -> "Income" to "আয়"
                ActivityFilterType.TRANSFERS -> "Transfer" to "ট্রান্সফার"
                ActivityFilterType.ALL -> "All" to "সকল"
            }
            val walletName = selectedAccountIdFilter?.let { id ->
                accounts.find { it.id == id }?.name ?: "Wallet"
            }
            val defaultScope = if (selectedFilter == ActivityFilterType.INCOME || selectedFilter == ActivityFilterType.TRANSFERS) {
                ExportScope.TRANSACTIONS_IN_DATE_RANGE
            } else {
                ExportScope.EXPENSES_BY_CATEGORY
            }

            ExportReportDialog(
                transactions = filteredList,
                dateRange = InsightsDateRange(
                    startMillis = startTime,
                    endMillis = endTime,
                    formattedDisplay = periodLabelEn,
                    formattedDisplayBn = periodLabelBn
                ),
                categories = categories,
                currencySymbol = currencySymbol,
                language = language,
                onDismiss = { showExportDialog = false },
                onExportComplete = onExportComplete,
                filterContext = ExportFilterContext(
                    periodLabel = periodLabelEn,
                    periodLabelBn = periodLabelBn,
                    transactionTypeLabel = typeEn,
                    transactionTypeLabelBn = typeBn,
                    categoryFilter = selectedCategoryFilter,
                    subcategoryFilter = selectedSubcategoryFilter,
                    walletFilter = walletName,
                    searchQuery = searchQuery.takeIf { it.isNotBlank() },
                    isPreFiltered = true
                ),
                accounts = accounts,
                initialScope = defaultScope
            )
        }

        if (showCustomDateRangePicker) {
            val initialStart = remember(selectedTimeRange, startTime) {
                if (selectedTimeRange == "CUSTOM" && startTime > 0L) startTime else System.currentTimeMillis()
            }
            val initialEnd = remember(selectedTimeRange, endTime) {
                if (selectedTimeRange == "CUSTOM" && endTime > 0L) endTime else null
            }
            HishabPatiCustomDateRangePickerDialog(
                initialStartDateMillis = initialStart,
                initialEndDateMillis = initialEnd,
                language = language,
                onDateRangeSelected = { range, formattedLabel ->
                    onSelectCustomDateRange(range, formattedLabel)
                    showCustomDateRangePicker = false
                },
                onDismissRequest = { showCustomDateRangePicker = false }
            )
        }
    }
}

