package com.example.ui.screens.insights

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeMode
import com.example.ui.ActivityFilterType
import com.example.ui.InsightsPeriod
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout

import com.example.ui.MainTab
import com.example.ui.navigation.DeferredBentoChartPlaceholder
import com.example.ui.navigation.rememberScreenLazyListState
import com.example.ui.navigation.rememberTransitionSettled

@Composable
fun InsightsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState = viewModel.rememberScreenLazyListState(tabOverride = MainTab.INSIGHTS)
) {
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val period by viewModel.insightsPeriod.collectAsState()
    val periodOffset by viewModel.insightsPeriodOffset.collectAsState()
    val customStart by viewModel.insightsCustomStartDate.collectAsState()
    val customEnd by viewModel.insightsCustomEndDate.collectAsState()
    val selectedCategory by viewModel.insightsSelectedCategory.collectAsState()

    val language = settings?.language ?: AppLanguage.ENGLISH
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val isBn = language == AppLanguage.BANGLA

    var showExportDialog by remember { mutableStateOf(false) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    // Dynamic date range calculation
    val dateRange = remember(period, periodOffset, customStart, customEnd, language) {
        InsightsCalculator.computeDateRange(
            period = period,
            offset = periodOffset,
            customStart = customStart,
            customEnd = customEnd,
            language = language
        )
    }

    val chartsSettled = rememberTransitionSettled(
        key = "insights_tab",
        settleDelayMillis = 120L
    )

    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 164.dp,
        extraTopGap = 4.dp
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    scrollLayout.backdropState,
                    listState,
                    scrollLayout.contentGraphicsLayer
                )
                .testTag("insights_lazy_column"),
            contentPadding = scrollLayout.contentPadding
        ) {
            // 1. Financial Overview (Total Income, Total Expenses, Net Cash Flow with prev period comparison)
            item(key = "financial_overview_card") {
                FinancialOverviewCard(
                    transactions = transactions,
                    dateRange = dateRange,
                    currencySymbol = currencySymbol,
                    language = language
                )
            }

            // 3. Budget Status (Progress bar, budget, spent, remaining, % used - hides if no budget exists)
            item(key = "budget_status_card") {
                BudgetStatusCard(
                    budgets = budgets,
                    transactions = transactions,
                    period = period,
                    dateRange = dateRange,
                    currencySymbol = currencySymbol,
                    language = language
                )
            }

            // 4. Spending Trend (Interactive line chart with inspection, peak day & daily average)
            item(key = "spending_trend_card") {
                if (chartsSettled) {
                    SpendingTrendCard(
                        transactions = transactions,
                        period = period,
                        dateRange = dateRange,
                        currencySymbol = currencySymbol,
                        language = language
                    )
                } else {
                    DeferredBentoChartPlaceholder(height = 260.dp)
                }
            }

            // 5. Spending by Category (Segmented Donut + Progress Bars + Direct Tap-to-Open Transactions)
            item(key = "category_spending_card") {
                if (chartsSettled) {
                    CategorySpendingCard(
                        transactions = transactions,
                        categories = categories,
                        dateRange = dateRange,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { catName ->
                            viewModel.insightsSelectedCategory.value = catName
                        },
                        onViewTransactions = { cat, datePair, dateLabel, filterType ->
                            viewModel.openFilteredReports(
                                category = cat,
                                dateRange = datePair,
                                dateRangeLabel = dateLabel,
                                filterType = filterType
                            )
                        },
                        currencySymbol = currencySymbol,
                        language = language
                    )
                } else {
                    DeferredBentoChartPlaceholder(height = 300.dp)
                }
            }

            // 6. Key Spending Highlights (At most 2-3 data-backed observations)
            item(key = "spending_highlights_card") {
                if (chartsSettled) {
                    SpendingHighlightsCard(
                        transactions = transactions,
                        categories = categories,
                        dateRange = dateRange,
                        currencySymbol = currencySymbol,
                        language = language
                    )
                } else {
                    DeferredBentoChartPlaceholder(height = 160.dp)
                }
            }

            // 7. Cash Flow (Simple comparison chart of income vs expenses and net total)
            item(key = "cash_flow_card") {
                if (chartsSettled) {
                    CashFlowCard(
                        transactions = transactions,
                        dateRange = dateRange,
                        currencySymbol = currencySymbol,
                        language = language
                    )
                } else {
                    DeferredBentoChartPlaceholder(height = 220.dp)
                }
            }
        }

        // Fixed scroll-aware frosted header at top of Insights tab (pins title and date controls)
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "insights_fixed_header"
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBn) "আর্থিক ইনসাইটস" else "Financial Insights",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = if (isBn) "আয় ও ব্যয়ের সহজ ও স্পষ্ট সারসংক্ষেপ" else "Clear financial overview & analytics",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        )
                    }

                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val glassCircleBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.72f)
                    val glassCircleBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.85f)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Calendar Picker Button
                        Surface(
                            shape = CircleShape,
                            color = glassCircleBg,
                            border = BorderStroke(1.dp, glassCircleBorder),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .testTag("insights_calendar_btn"),
                            onClick = { showCustomDateDialog = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Select Date Range",
                                    tint = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // Export Report Button
                        Surface(
                            shape = CircleShape,
                            color = glassCircleBg,
                            border = BorderStroke(1.dp, glassCircleBorder),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .testTag("insights_export_btn"),
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
                }

                // Date-selection controls pinned directly beneath the header title
                ReportPeriodControls(
                    selectedPeriod = period,
                    periodOffset = periodOffset,
                    dateRange = dateRange,
                    language = language,
                    onPeriodSelected = { p ->
                        viewModel.insightsPeriod.value = p
                        viewModel.insightsPeriodOffset.value = 0
                        if (p == InsightsPeriod.CUSTOM) {
                            showCustomDateDialog = true
                        }
                    },
                    onPreviousPeriod = {
                        when {
                            period == InsightsPeriod.THIS_MONTH && periodOffset == 0 -> {
                                // Stepping back 1 month from This Month lands cleanly on Last Month
                                viewModel.insightsPeriod.value = InsightsPeriod.LAST_MONTH
                                viewModel.insightsPeriodOffset.value = 0
                            }
                            period == InsightsPeriod.LAST_MONTH -> {
                                // Stepping back from Last Month moves into custom month offset (-2, -3, ...)
                                // so Last Month chip does not stay falsely highlighted
                                viewModel.insightsPeriod.value = InsightsPeriod.THIS_MONTH
                                viewModel.insightsPeriodOffset.value = periodOffset - 2
                            }
                            period == InsightsPeriod.CUSTOM -> {
                                // Step back to Last Month if user hits < from a custom date range
                                viewModel.insightsPeriod.value = InsightsPeriod.LAST_MONTH
                                viewModel.insightsPeriodOffset.value = 0
                            }
                            else -> {
                                viewModel.insightsPeriodOffset.value -= 1
                            }
                        }
                    },
                    onNextPeriod = {
                        when {
                            period == InsightsPeriod.LAST_MONTH && periodOffset == 0 -> {
                                // Stepping forward from Last Month returns cleanly to This Month
                                viewModel.insightsPeriod.value = InsightsPeriod.THIS_MONTH
                                viewModel.insightsPeriodOffset.value = 0
                            }
                            period == InsightsPeriod.THIS_MONTH && periodOffset == -2 -> {
                                // Stepping forward from 2 months ago lands cleanly on Last Month
                                viewModel.insightsPeriod.value = InsightsPeriod.LAST_MONTH
                                viewModel.insightsPeriodOffset.value = 0
                            }
                            periodOffset < 0 && period != InsightsPeriod.CUSTOM -> {
                                viewModel.insightsPeriodOffset.value += 1
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Export Dialog Modal
        if (showExportDialog) {
            ExportReportDialog(
                transactions = transactions,
                dateRange = dateRange,
                categories = categories,
                currencySymbol = currencySymbol,
                language = language,
                onDismiss = { showExportDialog = false },
                onExportComplete = { msg ->
                    viewModel.userNotificationMessage.value = msg
                },
                filterContext = ExportFilterContext(
                    periodLabel = dateRange.formattedDisplay,
                    periodLabelBn = dateRange.formattedDisplayBn,
                    categoryFilter = selectedCategory,
                    isPreFiltered = false
                ),
                accounts = accounts
            )
        }

        // Custom Date Range Dialog Modal
        if (showCustomDateDialog) {
            CustomDateRangeDialog(
                initialStart = customStart,
                initialEnd = customEnd,
                language = language,
                onDismiss = { showCustomDateDialog = false },
                onConfirm = { s, e ->
                    viewModel.insightsPeriod.value = InsightsPeriod.CUSTOM
                    viewModel.insightsCustomStartDate.value = s
                    viewModel.insightsCustomEndDate.value = e
                    viewModel.insightsPeriodOffset.value = 0
                }
            )
        }
    }
}
