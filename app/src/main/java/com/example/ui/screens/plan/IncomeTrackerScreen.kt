package com.example.ui.screens.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.TranslucentCard
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.FinancialReportAnalytics
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private data class IncomePeriodOption(
    val key: String,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeTrackerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val transactions by viewModel.transactions.collectAsState()

    var selectedPeriod by remember { mutableStateOf("6M") } // "3M", "6M", "1Y", "Last Year", "Custom"
    var customStartMs by remember { mutableStateOf<Long?>(null) }
    var customEndMs by remember { mutableStateOf<Long?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val periodOptions = remember(language) {
        listOf(
            IncomePeriodOption("3M", if (isBn) "৩ মাস" else "3M"),
            IncomePeriodOption("6M", if (isBn) "৬ মাস" else "6M"),
            IncomePeriodOption("1Y", if (isBn) "১ বছর" else "1Y"),
            IncomePeriodOption("Last Year", AppStrings.t("last_year", language)),
            IncomePeriodOption("Custom", AppStrings.t("custom", language))
        )
    }

    val summary = remember(transactions, selectedPeriod, customStartMs, customEndMs) {
        FinancialReportAnalytics.computeIncomeTracker(
            transactions = transactions,
            period = selectedPeriod,
            customStartMs = customStartMs,
            customEndMs = customEndMs
        )
    }

    val customRangeLabel = remember(customStartMs, customEndMs, language) {
        val start = customStartMs
        val end = customEndMs
        if (start != null && end != null) {
            formatIncomeCustomRangeLabel(start, end)
        } else {
            null
        }
    }

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.INCOME_TRACKER
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 66.dp,
        extraTopGap = 8.dp,
        bottomNavClearance = 30.dp
    )

    if (showDateRangePicker) {
        val initialStartUtc = customStartMs?.let { localMillisToUtcMidnight(it) }
        val initialEndUtc = customEndMs?.let { localMillisToUtcMidnight(it) }
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialStartUtc,
            initialSelectedEndDateMillis = initialEndUtc
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sUtc = dateRangePickerState.selectedStartDateMillis
                        val eUtc = dateRangePickerState.selectedEndDateMillis ?: sUtc
                        if (sUtc != null && eUtc != null) {
                            val minUtc = minOf(sUtc, eUtc)
                            val maxUtc = maxOf(sUtc, eUtc)
                            customStartMs = utcMidnightToLocalStartOfDay(minUtc)
                            customEndMs = utcMidnightToLocalEndOfDay(maxUtc)
                            selectedPeriod = "Custom"
                        }
                        showDateRangePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null
                ) {
                    Text(AppStrings.t("done", language), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text(AppStrings.t("cancel", language))
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = AppStrings.t("select_date_range", language),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            )
        }
    }

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
                .testTag("income_tracker_lazy_column"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = scrollLayout.contentPadding.calculateTopPadding(),
                bottom = scrollLayout.contentPadding.calculateBottomPadding()
            )
        ) {
            // Horizontally Scrollable Period Filter Row (3M, 6M, 1Y, Last Year, Custom)
            item(key = "income_period_filters") {
                Column(modifier = Modifier.padding(bottom = 14.dp)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("income_period_filter_row")
                    ) {
                        items(periodOptions, key = { it.key }) { option ->
                            val isSelected = selectedPeriod == option.key
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) {
                                    Color(0xFF1E293B)
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) {
                                        Color.Transparent
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    }
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (option.key == "Custom") {
                                            showDateRangePicker = true
                                        } else {
                                            selectedPeriod = option.key
                                        }
                                    }
                                    .testTag("income_period_${option.key.lowercase(Locale.US).replace(' ', '_')}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (option.key == "Custom") {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }
                                    Text(
                                        text = option.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Compact selected date range badge when Custom is active
                    if (selectedPeriod == "Custom" && customRangeLabel != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF047857).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF047857).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showDateRangePicker = true }
                                .testTag("income_custom_range_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = customRangeLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                }
            }

            // Total Income Card
            item(key = "total_income_card") {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = AppStrings.t("total_income", language),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = CurrencyFormatter.format(summary.totalIncome, currencySymbol, language = language),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF047857)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = AppStrings.t("avg_monthly", language),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.format(summary.avgMonthly, currencySymbol, language = language),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val delta = summary.deltaPct
                            if (delta != null) {
                                val isPositive = delta >= 0
                                val pctText = String.format(Locale.US, "%.1f", kotlin.math.abs(delta))
                                val localizedPct = if (isBn) CurrencyFormatter.convertToBanglaDigits(pctText) else pctText
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isPositive) Color(0xFFDCFCE7) else Color(0xFFFFE4E6),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPositive) Color(0xFF86EFAC) else Color(0xFFFDA4AF)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isPositive) "▲ $localizedPct%" else "▼ $localizedPct%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPositive) Color(0xFF15803D) else Color(0xFFE11D48)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = if (isBn) "০.০%" else "0.0%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Income Trend Section
            item(key = "income_trend_section") {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = AppStrings.t("monthly_income_trend", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        if (summary.hasTrendData && summary.trendPoints.isNotEmpty()) {
                            val months = summary.trendMonths
                            val normalizedPoints = summary.trendPoints

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height - 30f

                                    val stepX = if (normalizedPoints.size > 1) w / (normalizedPoints.size - 1) else w
                                    val points = normalizedPoints.mapIndexed { idx, normY ->
                                        val x = idx * stepX
                                        val y = h - (normY * h * 0.9f)
                                        Offset(x, y)
                                    }

                                    val fillPath = Path().apply {
                                        moveTo(points.first().x, points.first().y)
                                        for (i in 1 until points.size) {
                                            lineTo(points[i].x, points[i].y)
                                        }
                                        lineTo(points.last().x, h)
                                        lineTo(points.first().x, h)
                                        close()
                                    }

                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF10B981).copy(alpha = 0.28f),
                                                Color(0xFF10B981).copy(alpha = 0.02f)
                                            )
                                        )
                                    )

                                    val linePath = Path().apply {
                                        moveTo(points.first().x, points.first().y)
                                        for (i in 1 until points.size) {
                                            lineTo(points[i].x, points[i].y)
                                        }
                                    }

                                    drawPath(
                                        path = linePath,
                                        color = Color(0xFF059669),
                                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                                    )

                                    points.forEach { pt ->
                                        drawCircle(
                                            color = Color.White,
                                            radius = 4.5f,
                                            center = pt
                                        )
                                        drawCircle(
                                            color = Color(0xFF059669),
                                            radius = 3f,
                                            center = pt
                                        )
                                    }
                                }
                            }

                            // Month / Bucket label row (thinned cleanly when > 7 points so labels never collide)
                            val displayedLabels = remember(months) {
                                if (months.size <= 7) {
                                    months
                                } else {
                                    val step = (months.size / 6).coerceAtLeast(2)
                                    months.filterIndexed { index, _ ->
                                        index == 0 || index == months.lastIndex || index % step == 0
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                displayedLabels.forEach { m ->
                                    Text(
                                        text = m,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isBn) "কোনো আয়ের তথ্য পাওয়া যায়নি" else "No income data for the selected period",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBn) {
                                        "আয় যুক্ত করলে এখানে মাসিক গতিপ্রকৃতি দেখা যাবে"
                                    } else {
                                        "Logged income will display monthly trajectories and trend lines here."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Income Streams Section
            item(key = "income_streams_header") {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = AppStrings.t("income_streams", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (summary.incomeStreams.isEmpty()) {
                item(key = "income_streams_empty") {
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isBn) "কোনো আয়ের উৎস পাওয়া যায়নি" else "No income streams recorded yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBn) {
                                    "নির্বাচিত সময়সীমার মধ্যে আয়ের লেনদেন যোগ করুন"
                                } else {
                                    "Log income transactions to track multiple sources, regularity, and share."
                                },
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(summary.incomeStreams, key = { it.title }) { stream ->
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stream.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                val localizedBadge = when (stream.typeBadge) {
                                    "Monthly" -> if (isBn) "মাসিক" else "Monthly"
                                    "Regular" -> if (isBn) "নিয়মিত" else "Regular"
                                    "One-time" -> if (isBn) "এককালীন" else "One-time"
                                    else -> stream.typeBadge
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = localizedBadge,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = CurrencyFormatter.format(stream.totalAmount, currencySymbol, language = language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = IncomeGreen
                                )

                                val countStr = if (isBn) {
                                    CurrencyFormatter.convertToBanglaDigits(stream.paymentCount.toString())
                                } else {
                                    stream.paymentCount.toString()
                                }
                                Text(
                                    text = "$countStr ${AppStrings.t("payments_count", language)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = AppStrings.t("consistency", language),
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                                LinearProgressIndicator(
                                    progress = { (stream.consistencyPct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 10.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (stream.consistencyPct > 50) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                val pctStr = if (isBn) {
                                    CurrencyFormatter.convertToBanglaDigits(stream.consistencyPct.toString())
                                } else {
                                    stream.consistencyPct.toString()
                                }
                                Text(
                                    text = "$pctStr%",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val shareFormatted = String.format(Locale.US, "%.1f", stream.sharePct).let {
                                if (isBn) CurrencyFormatter.convertToBanglaDigits(it) else it
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${AppStrings.t("share", language)}: $shareFormatted%",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "${AppStrings.t("last", language)}: ${stream.lastReceivedDate}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "income_tracker_bottom_spacer") {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "income_tracker_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.size(42.dp),
                    onClick = onBack
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AppStrings.t("back", language),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = AppStrings.t("income_tracker", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }
    }
}

private fun localMillisToUtcMidnight(localMillis: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localCal.get(Calendar.YEAR),
            localCal.get(Calendar.MONTH),
            localCal.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
    }
    return utcCal.timeInMillis
}

private fun utcMidnightToLocalStartOfDay(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val localCal = Calendar.getInstance().apply {
        clear()
        set(
            utcCal.get(Calendar.YEAR),
            utcCal.get(Calendar.MONTH),
            utcCal.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
        set(Calendar.MILLISECOND, 0)
    }
    return localCal.timeInMillis
}

private fun utcMidnightToLocalEndOfDay(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val localCal = Calendar.getInstance().apply {
        clear()
        set(
            utcCal.get(Calendar.YEAR),
            utcCal.get(Calendar.MONTH),
            utcCal.get(Calendar.DAY_OF_MONTH),
            23,
            59,
            59
        )
        set(Calendar.MILLISECOND, 999)
    }
    return localCal.timeInMillis
}

private fun formatIncomeCustomRangeLabel(startMs: Long, endMs: Long): String {
    val sCal = Calendar.getInstance().apply { timeInMillis = startMs }
    val eCal = Calendar.getInstance().apply { timeInMillis = endMs }
    val sameYear = sCal.get(Calendar.YEAR) == eCal.get(Calendar.YEAR)
    val startFmt = if (sameYear) {
        SimpleDateFormat("MMM d", Locale.US)
    } else {
        SimpleDateFormat("MMM d, yyyy", Locale.US)
    }
    val endFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
    return "${startFmt.format(Date(startMs))} – ${endFmt.format(Date(endMs))}"
}
