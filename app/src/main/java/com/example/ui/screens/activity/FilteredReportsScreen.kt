package com.example.ui.screens.activity

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.ActivityFilterType
import com.example.ui.ActivitySort
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.HishabPatiCustomDateRangePickerDialog
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.TranslucentCard
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.screens.insights.ExportFilterContext
import com.example.ui.screens.insights.ExportReportDialog
import com.example.ui.screens.insights.ExportScope
import com.example.ui.screens.insights.InsightsDateRange
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val ReportRoyalIndigo = Color(0xFF4F46E5)
private val ReportElectricViolet = Color(0xFF7C3AED)
private val ReportTransferBlue = Color(0xFF0EA5E9)

private data class DailyBarBucket(
    val label: String,
    val fullDateLabel: String,
    val amount: Double,
    val txCount: Int
)

/**
 * Dedicated Expend-style Filtered Reports screen pushed onto the SubScreen back stack.
 * Pushing directly onto SubScreen.FILTERED_REPORTS preserves InsightsScreen's tab, period,
 * and scroll position so Back returns straight to InsightsScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilteredReportsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val initialArgs by viewModel.filteredReportArgs.collectAsState()

    val currencySymbol = settings?.currencySymbol ?: "৳"
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var filterType by rememberSaveable {
        mutableStateOf(initialArgs?.filterType ?: ActivityFilterType.EXPENSES)
    }
    var selectedTimeRange by rememberSaveable {
        mutableStateOf(if (initialArgs?.dateRange != null) "CUSTOM" else "THIS_MONTH")
    }
    var customDateRange by remember { mutableStateOf(initialArgs?.dateRange) }
    var customDateRangeLabel by rememberSaveable { mutableStateOf(initialArgs?.dateRangeLabel) }
    var selectedCategory by rememberSaveable { mutableStateOf(initialArgs?.category) }
    var selectedAccountId by rememberSaveable { mutableStateOf(initialArgs?.accountId) }
    var selectedSort by rememberSaveable { mutableStateOf(ActivitySort.NEWEST) }

    var isDatePresetsExpanded by remember { mutableStateOf(false) }
    var isTypeSelectorExpanded by remember { mutableStateOf(false) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var showSortBottomSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showCustomDateRangePicker by remember { mutableStateOf(false) }
    var selectedTxForDetails by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    // Synchronize if launched with new args while on stack
    LaunchedEffect(initialArgs) {
        initialArgs?.let { args ->
            filterType = args.filterType
            selectedCategory = args.category
            selectedAccountId = args.accountId
            if (args.dateRange != null) {
                selectedTimeRange = "CUSTOM"
                customDateRange = args.dateRange
                customDateRangeLabel = args.dateRangeLabel
            }
        }
    }

    BackHandler {
        when {
            txToDelete != null -> txToDelete = null
            selectedTxForDetails != null -> selectedTxForDetails = null
            else -> onBack()
        }
    }

    // Resolve active time window cleanly
    val (startTime, endTime) = remember(selectedTimeRange, customDateRange, transactions) {
        computeReportTimeBounds(selectedTimeRange, customDateRange, transactions)
    }

    val periodDisplayLabel = remember(customDateRangeLabel, selectedTimeRange, startTime, endTime, language) {
        formatFilteredDatePill(customDateRangeLabel, selectedTimeRange, startTime, endTime, language)
    }

    // 1. Base transactions filtered by Date Range, Account, and Transaction Type (before Category filter)
    val baseTypeFilteredTxs = remember(transactions, startTime, endTime, selectedAccountId, filterType) {
        transactions.filter { tx ->
            val inDate = tx.timestamp in startTime..endTime
            val inAccount = selectedAccountId == null ||
                tx.accountId == selectedAccountId ||
                tx.toAccountId == selectedAccountId
            val isTransferTx = tx.type == TransactionType.TRANSFER ||
                tx.type == TransactionType.CC_PAYMENT ||
                tx.category.equals("Transfer", ignoreCase = true)
            val matchesType = when (filterType) {
                ActivityFilterType.EXPENSES -> tx.type == TransactionType.EXPENSE && !isTransferTx
                ActivityFilterType.INCOME -> tx.type == TransactionType.INCOME && !isTransferTx
                ActivityFilterType.TRANSFERS -> tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT
                ActivityFilterType.ALL -> true
            }
            inDate && inAccount && matchesType
        }
    }

    // 2. Category breakdown (synced with baseTypeFilteredTxs so tapping any category keeps all shares visible)
    val (totalCategoryAmount, categoryShares) = remember(baseTypeFilteredTxs, categories, language, filterType) {
        if (filterType == ActivityFilterType.TRANSFERS) {
            0.0 to emptyList()
        } else {
            val nonTransfers = baseTypeFilteredTxs.filter {
                it.type != TransactionType.TRANSFER &&
                    it.type != TransactionType.CC_PAYMENT &&
                    !it.category.equals("Transfer", ignoreCase = true)
            }
            FilteredReportCalculator.calculateCategoryBreakdown(nonTransfers, categories, language)
        }
    }

    // 3. Final displayed transactions (immediately synced with selectedCategory & selectedSort)
    val filteredTransactions = remember(baseTypeFilteredTxs, selectedCategory, selectedSort) {
        val catFiltered = if (selectedCategory != null) {
            baseTypeFilteredTxs.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        } else {
            baseTypeFilteredTxs
        }
        when (selectedSort) {
            ActivitySort.NEWEST -> catFiltered.sortedByDescending { it.timestamp }
            ActivitySort.OLDEST -> catFiltered.sortedBy { it.timestamp }
            ActivitySort.HIGHEST_AMOUNT -> catFiltered.sortedByDescending { it.amount }
            ActivitySort.LOWEST_AMOUNT -> catFiltered.sortedBy { it.amount }
        }
    }

    // 4. Synchronized exact report total (strictly respecting filterType, never mixing Income/Transfer into Expense)
    val reportTotalAmount = remember(filteredTransactions, filterType) {
        when (filterType) {
            ActivityFilterType.EXPENSES -> filteredTransactions.filter {
                it.type == TransactionType.EXPENSE && !it.category.equals("Transfer", ignoreCase = true)
            }.sumOf { it.amount }
            ActivityFilterType.INCOME -> filteredTransactions.filter {
                it.type == TransactionType.INCOME && !it.category.equals("Transfer", ignoreCase = true)
            }.sumOf { it.amount }
            ActivityFilterType.TRANSFERS -> filteredTransactions.filter {
                it.type == TransactionType.TRANSFER || it.type == TransactionType.CC_PAYMENT
            }.sumOf { it.amount }
            ActivityFilterType.ALL -> {
                var net = 0.0
                filteredTransactions.forEach { tx ->
                    if (!tx.category.equals("Transfer", ignoreCase = true)) {
                        when (tx.type) {
                            TransactionType.INCOME -> net += tx.amount
                            TransactionType.EXPENSE -> net -= tx.amount
                            else -> {}
                        }
                    }
                }
                net
            }
        }
    }

    // 5. Daily Bar Chart buckets (for Expense / Income / Transfer)
    val dailyBuckets = remember(filteredTransactions, startTime, endTime, language) {
        buildDailyBarBuckets(filteredTransactions, startTime, endTime, language)
    }

    // 6. Running Balance Timeline (for All mode)
    val (startingBalance, endingBalance, timelinePoints) = remember(
        filterType, startTime, endTime, accounts, transactions, selectedAccountId, language
    ) {
        if (filterType == ActivityFilterType.ALL) {
            FilteredReportCalculator.calculateBalanceTimeline(
                startTime = startTime,
                endTime = endTime,
                accounts = accounts,
                transactions = transactions,
                selectedAccountId = selectedAccountId,
                language = language
            )
        } else {
            Triple(0.0, 0.0, emptyList())
        }
    }

    // 7. Grouped Daily Transactions
    val groupedByDate = remember(filteredTransactions, language) {
        val headerFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
        filteredTransactions.groupBy { tx ->
            DateFormatter.localizeText(headerFmt.format(Date(tx.timestamp)), language)
        }
    }

    val listState = rememberLazyListState()
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 124.dp,
        extraTopGap = 4.dp,
        bottomNavClearance = 32.dp
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
                .testTag("filtered_reports_lazy_column"),
            contentPadding = scrollLayout.contentPadding
        ) {
            // 1. Hero Analytics Card: Daily Bar Chart (Expense/Income/Transfer) OR Running Balance Curve (All)
            item(key = "filtered_reports_hero_chart") {
                AnimatedContent(
                    targetState = filterType == ActivityFilterType.ALL,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                    },
                    label = "filtered_report_chart_mode"
                ) { isAllMode ->
                    if (isAllMode) {
                        BalanceTrendCard(
                            startingBalance = startingBalance,
                            endingBalance = endingBalance,
                            timelinePoints = timelinePoints,
                            currencySymbol = currencySymbol,
                            language = language
                        )
                    } else {
                        InteractiveDailyBarChartCard(
                            buckets = dailyBuckets,
                            totalAmount = reportTotalAmount,
                            filterType = filterType,
                            currencySymbol = currencySymbol,
                            language = language
                        )
                    }
                }
            }

            // 2. Category Segmented Bar + Icon Selector + Subcategory Breakdown Card (or Transfer Summary)
            if (filterType == ActivityFilterType.TRANSFERS) {
                item(key = "filtered_reports_transfer_summary") {
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .testTag("filtered_reports_transfer_summary_card")
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
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(ReportTransferBlue.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = ReportTransferBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (isBn) "ট্রান্সফার" else "Transfers",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    val countTxt = if (isBn) {
                                        "${CurrencyFormatter.convertToBanglaDigits(filteredTransactions.size.toString())}টি লেনদেন"
                                    } else {
                                        "${filteredTransactions.size} transactions"
                                    }
                                    Text(
                                        text = countTxt,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            Text(
                                text = CurrencyFormatter.format(reportTotalAmount, currencySymbol, false, language),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }
                    }
                }
            } else if (categoryShares.isNotEmpty()) {
                item(key = "filtered_reports_category_breakdown") {
                    FilteredCategoryBreakdownCard(
                        totalAmount = totalCategoryAmount,
                        categoryShares = categoryShares,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it },
                        currencySymbol = currencySymbol,
                        language = language,
                        isIncomeMode = filterType == ActivityFilterType.INCOME
                    )
                }
            }

            // 3. Synchronized Transactions Section Header
            item(key = "filtered_reports_tx_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val txCountStr = if (isBn) {
                            CurrencyFormatter.convertToBanglaDigits(filteredTransactions.size.toString())
                        } else {
                            filteredTransactions.size.toString()
                        }
                        Text(
                            text = if (isBn) "লেনদেন ($txCountStr)" else "Transactions ($txCountStr)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        if (selectedCategory != null) {
                            Surface(
                                onClick = { selectedCategory = null },
                                shape = RoundedCornerShape(12.dp),
                                color = ReportRoyalIndigo.copy(alpha = if (isDark) 0.24f else 0.12f),
                                modifier = Modifier.testTag("filtered_reports_active_cat_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = selectedCategory!!,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFFA5B4FC) else ReportRoyalIndigo
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear category",
                                        tint = if (isDark) Color(0xFFA5B4FC) else ReportRoyalIndigo,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = CurrencyFormatter.format(
                            reportTotalAmount,
                            currencySymbol,
                            showSign = filterType == ActivityFilterType.ALL,
                            language = language
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.5.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier.testTag("filtered_reports_tx_header_total")
                    )
                }
            }

            // 4. Grouped Daily Transactions or Empty State
            if (groupedByDate.isEmpty()) {
                item(key = "filtered_reports_empty") {
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .testTag("filtered_reports_empty_state")
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
                                color = ReportRoyalIndigo.copy(alpha = 0.12f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ReportRoyalIndigo,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isBn) "এই ফিল্টারে কোনো লেনদেন পাওয়া যায়নি।" else "No transactions match your current filters.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            )
                            OutlinedButton(
                                onClick = {
                                    filterType = ActivityFilterType.EXPENSES
                                    selectedTimeRange = "THIS_MONTH"
                                    customDateRange = null
                                    customDateRangeLabel = null
                                    selectedCategory = null
                                    selectedAccountId = null
                                },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(if (isBn) "ফিল্টার রিসেট করুন" else "Reset Filters")
                            }
                        }
                    }
                }
            } else {
                groupedByDate.forEach { (dateHeader, txsInDate) ->
                    val dayTotal = when (filterType) {
                        ActivityFilterType.EXPENSES -> -txsInDate.filter {
                            it.type == TransactionType.EXPENSE
                        }.sumOf { it.amount }
                        ActivityFilterType.INCOME -> txsInDate.filter {
                            it.type == TransactionType.INCOME
                        }.sumOf { it.amount }
                        ActivityFilterType.TRANSFERS -> txsInDate.sumOf { it.amount }
                        ActivityFilterType.ALL -> txsInDate.sumOf { tx ->
                            when (tx.type) {
                                TransactionType.INCOME -> tx.amount
                                TransactionType.EXPENSE -> -tx.amount
                                else -> 0.0
                            }
                        }
                    }

                    item(key = "fr_date_header_$dateHeader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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

                            val dayColor = when {
                                filterType == ActivityFilterType.TRANSFERS -> ReportTransferBlue
                                dayTotal > 0 -> IncomeGreen
                                dayTotal < 0 -> ExpenseRed
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = CurrencyFormatter.format(
                                    dayTotal,
                                    currencySymbol,
                                    filterType != ActivityFilterType.TRANSFERS,
                                    language
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = dayColor,
                                    fontSize = 12.sp,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }
                    }

                    item(key = "fr_date_card_$dateHeader") {
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
                                        onClick = { selectedTxForDetails = tx }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fixed Scroll-Aware Frosted Header with Back Arrow, Sort, Export & Top Filter Pill Bar
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "filtered_reports_fixed_header"
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val circleBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.75f)
                        val circleBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.88f)

                        Surface(
                            shape = CircleShape,
                            color = circleBg,
                            border = BorderStroke(1.dp, circleBorder),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .testTag("filtered_reports_back_btn"),
                            onClick = onBack
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (isBn) "পেছনে যান" else "Back to Insights",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBn) "ফিল্টারকৃত রিপোর্ট" else "Filtered Reports",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 20.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = periodDisplayLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val circleBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.75f)
                        val circleBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.88f)

                        Surface(
                            shape = CircleShape,
                            color = circleBg,
                            border = BorderStroke(1.dp, circleBorder),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .testTag("filtered_reports_sort_btn"),
                            onClick = { showSortBottomSheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = if (isBn) "সাজান" else "Sort",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = circleBg,
                            border = BorderStroke(1.dp, circleBorder),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .testTag("filtered_reports_export_btn"),
                            onClick = { showExportDialog = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = if (isBn) "এক্সপোর্ট" else "Export Report",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                // Top Filter Pill Bar
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Sliders Icon
                    item {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .testTag("filtered_reports_open_sheet_btn"),
                            onClick = { showFilterBottomSheet = true }
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

                    // Transaction Type Pill (toggles quick 4-type selector row)
                    item {
                        val (typeLabel, typeBg, typeFg) = when (filterType) {
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
                                ReportTransferBlue.copy(alpha = 0.16f),
                                ReportTransferBlue
                            )
                            ActivityFilterType.ALL -> Triple(
                                if (isBn) "সকল" else "All",
                                ReportRoyalIndigo.copy(alpha = 0.16f),
                                if (isDark) Color(0xFFA5B4FC) else ReportRoyalIndigo
                            )
                        }
                        Surface(
                            onClick = {
                                isTypeSelectorExpanded = !isTypeSelectorExpanded
                                if (isTypeSelectorExpanded) isDatePresetsExpanded = false
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = typeBg,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("filtered_reports_type_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = typeLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = typeFg,
                                        fontSize = 12.5.sp
                                    )
                                )
                                Icon(
                                    imageVector = if (isTypeSelectorExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = typeFg,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Date Range Pill (Royal Indigo-to-Electric Violet)
                    item {
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(listOf(ReportRoyalIndigo, ReportElectricViolet))
                                )
                                .clickable {
                                    isDatePresetsExpanded = !isDatePresetsExpanded
                                    if (isDatePresetsExpanded) isTypeSelectorExpanded = false
                                }
                                .testTag("filtered_reports_date_pill"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = periodDisplayLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 12.5.sp
                                    )
                                )
                                Icon(
                                    imageVector = if (isDatePresetsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Category Pill
                    item {
                        val hasCat = selectedCategory != null
                        Surface(
                            onClick = {
                                if (hasCat) selectedCategory = null else showFilterBottomSheet = true
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (hasCat) {
                                ReportRoyalIndigo.copy(alpha = if (isDark) 0.25f else 0.14f)
                            } else {
                                if (isDark) Color(0xFF1E293B).copy(alpha = 0.72f) else Color.White
                            },
                            border = BorderStroke(
                                1.dp,
                                if (hasCat) ReportRoyalIndigo.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("filtered_reports_category_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = selectedCategory ?: (if (isBn) "সব খাত" else "All Categories"),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (hasCat) {
                                            if (isDark) Color(0xFFA5B4FC) else ReportRoyalIndigo
                                        } else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.5.sp
                                    )
                                )
                                if (hasCat) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear category",
                                        tint = if (isDark) Color(0xFFA5B4FC) else ReportRoyalIndigo,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Wallet Pill (when selected)
                    if (selectedAccountId != null) {
                        val accName = accounts.find { it.id == selectedAccountId }?.name ?: "Wallet"
                        item {
                            Surface(
                                onClick = { selectedAccountId = null },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("filtered_reports_wallet_pill")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 11.dp)
                                ) {
                                    Text(
                                        text = "$accName ✕",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Expandable Transaction Type Selector Row
                AnimatedVisibility(
                    visible = isTypeSelectorExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val types = listOf(
                            ActivityFilterType.EXPENSES to (if (isBn) "খরচ" else "Expense"),
                            ActivityFilterType.INCOME to (if (isBn) "আয়" else "Income"),
                            ActivityFilterType.TRANSFERS to (if (isBn) "ট্রান্সফার" else "Transfer"),
                            ActivityFilterType.ALL to (if (isBn) "সকল" else "All")
                        )
                        types.forEach { (typeVal, lbl) ->
                            val selected = filterType == typeVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(
                                        brush = if (selected) {
                                            Brush.linearGradient(listOf(ReportRoyalIndigo, ReportElectricViolet))
                                        } else {
                                            val bg = if (isDark) Color(0xFF1E293B) else Color.White
                                            Brush.linearGradient(listOf(bg, bg))
                                        }
                                    )
                                    .then(
                                        if (!selected) Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                            RoundedCornerShape(17.dp)
                                        ) else Modifier
                                    )
                                    .clickable {
                                        filterType = typeVal
                                        selectedCategory = null
                                        isTypeSelectorExpanded = false
                                    }
                                    .testTag("filtered_reports_type_option_${typeVal.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Expandable Quick Date Range Presets
                AnimatedVisibility(
                    visible = isDatePresetsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "THIS_WEEK" to (if (isBn) "চলতি সপ্তাহ" else "This Week"),
                            "THIS_MONTH" to (if (isBn) "চলতি মাস" else "This Month"),
                            "THIS_YEAR" to (if (isBn) "চলতি বছর" else "This Year"),
                            "PREVIOUS_WEEK" to (if (isBn) "আগের সপ্তাহ" else "Previous Week"),
                            "LAST_MONTH" to (if (isBn) "গত মাস" else "Last Month"),
                            "CUSTOM" to (if (isBn) "কাস্টম" else "Custom")
                        )
                        items(presets) { (key, label) ->
                            val isSel = if (key == "CUSTOM") {
                                selectedTimeRange == "CUSTOM"
                            } else {
                                selectedTimeRange == key && customDateRange == null
                            }
                            Surface(
                                onClick = {
                                    if (key == "CUSTOM") {
                                        showCustomDateRangePicker = true
                                    } else {
                                        selectedTimeRange = key
                                        customDateRange = null
                                        customDateRangeLabel = null
                                        isDatePresetsExpanded = false
                                    }
                                },
                                shape = RoundedCornerShape(17.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer
                                else if (isDark) Color(0xFF1E293B) else Color.White,
                                border = BorderStroke(
                                    width = if (isSel) 1.5.dp else 1.dp,
                                    color = if (isSel) ReportRoyalIndigo else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                ),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("filtered_reports_preset_$key")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 13.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    // Transaction Details Sheet
    selectedTxForDetails?.let { tx ->
        TransactionDetailsBottomSheet(
            transaction = tx,
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            language = language,
            onDismiss = { selectedTxForDetails = null },
            onDuplicate = { target ->
                selectedTxForDetails = null
                viewModel.duplicateTransaction(target)
            },
            onEdit = { target ->
                selectedTxForDetails = null
                viewModel.transactionToEdit.value = target
                viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
            },
            onDelete = { target ->
                txToDelete = target
                selectedTxForDetails = null
            }
        )
    }

    // Delete Confirmation Dialog
    txToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text(if (isBn) "লেনদেন মুছবেন?" else "Delete Transaction") },
            text = {
                Text(
                    if (isBn) "আপনি কি নিশ্চিত যে \"${tx.title}\" স্থায়ীভাবে মুছে ফেলবেন?"
                    else "Are you sure you want to permanently delete \"${tx.title}\"? Account balances will be reverted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx.id)
                        txToDelete = null
                    }
                ) {
                    Text(if (isBn) "মুছুন" else "Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Apply Filters Bottom Sheet
    if (showFilterBottomSheet) {
        ApplyFiltersBottomSheet(
            onDismiss = { showFilterBottomSheet = false },
            currentType = filterType,
            currentTimeRange = selectedTimeRange,
            currentCustomDateRange = customDateRange,
            currentCategory = selectedCategory,
            currentAccountId = selectedAccountId,
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            language = language,
            onApply = { type, timeRange, customRange, cat, accId ->
                filterType = type
                selectedTimeRange = timeRange
                customDateRange = customRange
                customDateRangeLabel = if (customRange != null) {
                    formatFilteredDatePill(null, "CUSTOM", customRange.first, customRange.second, language)
                } else null
                selectedCategory = cat
                selectedAccountId = accId
                showFilterBottomSheet = false
            }
        )
    }

    // Sort Bottom Sheet
    if (showSortBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isBn) "সাজানোর ধরণ" else "Sort Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                val sortOptions = listOf(
                    ActivitySort.NEWEST to AppStrings.t("sort_newest", language),
                    ActivitySort.OLDEST to AppStrings.t("sort_oldest", language),
                    ActivitySort.HIGHEST_AMOUNT to AppStrings.t("sort_highest", language),
                    ActivitySort.LOWEST_AMOUNT to AppStrings.t("sort_lowest", language)
                )
                sortOptions.forEach { (opt, lbl) ->
                    val isSel = selectedSort == opt
                    Surface(
                        onClick = {
                            selectedSort = opt
                            showSortBottomSheet = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ReportRoyalIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        val periodEn = formatFilteredDatePill(customDateRangeLabel, selectedTimeRange, startTime, endTime, AppLanguage.ENGLISH)
        val periodBn = formatFilteredDatePill(customDateRangeLabel, selectedTimeRange, startTime, endTime, AppLanguage.BANGLA)
        val (typeEn, typeBn) = when (filterType) {
            ActivityFilterType.EXPENSES -> "Expense" to "খরচ"
            ActivityFilterType.INCOME -> "Income" to "আয়"
            ActivityFilterType.TRANSFERS -> "Transfer" to "ট্রান্সফার"
            ActivityFilterType.ALL -> "All" to "সকল"
        }
        val walletName = selectedAccountId?.let { id -> accounts.find { it.id == id }?.name }
        ExportReportDialog(
            transactions = filteredTransactions,
            dateRange = InsightsDateRange(startTime, endTime, periodEn, periodBn),
            categories = categories,
            currencySymbol = currencySymbol,
            language = language,
            onDismiss = { showExportDialog = false },
            onExportComplete = { msg -> viewModel.userNotificationMessage.value = msg },
            filterContext = ExportFilterContext(
                periodLabel = periodEn,
                periodLabelBn = periodBn,
                transactionTypeLabel = typeEn,
                transactionTypeLabelBn = typeBn,
                categoryFilter = selectedCategory,
                walletFilter = walletName,
                isPreFiltered = true
            ),
            accounts = accounts,
            initialScope = if (filterType == ActivityFilterType.EXPENSES) {
                ExportScope.EXPENSES_BY_CATEGORY
            } else {
                ExportScope.TRANSACTIONS_IN_DATE_RANGE
            }
        )
    }

    // Custom Date Range Picker
    if (showCustomDateRangePicker) {
        HishabPatiCustomDateRangePickerDialog(
            initialStartDateMillis = startTime,
            initialEndDateMillis = endTime,
            language = language,
            onDateRangeSelected = { range, label ->
                selectedTimeRange = "CUSTOM"
                customDateRange = range
                customDateRangeLabel = label
                isDatePresetsExpanded = false
                showCustomDateRangePicker = false
            },
            onDismissRequest = { showCustomDateRangePicker = false }
        )
    }
}

/**
 * Interactive Daily Bar Chart with touch & horizontal drag scrubbing.
 * Displays Total Expense/Income/Transfers, Average/Day, rounded vertical bars,
 * and a live callout box below showing exact date, amount, and % of total.
 */
@Composable
private fun InteractiveDailyBarChartCard(
    buckets: List<DailyBarBucket>,
    totalAmount: Double,
    filterType: ActivityFilterType,
    currencySymbol: String,
    language: AppLanguage
) {
    val isBn = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var selectedIndex by remember(buckets) {
        val peakIdx = buckets.indices.maxByOrNull { buckets[it].amount }
        mutableStateOf(if (peakIdx != null && (buckets.getOrNull(peakIdx)?.amount ?: 0.0) > 0.0) peakIdx else null)
    }

    val activeAccent = when (filterType) {
        ActivityFilterType.EXPENSES -> ReportRoyalIndigo
        ActivityFilterType.INCOME -> IncomeGreen
        ActivityFilterType.TRANSFERS -> ReportTransferBlue
        ActivityFilterType.ALL -> ReportRoyalIndigo
    }
    val activeAccentEnd = when (filterType) {
        ActivityFilterType.EXPENSES -> ReportElectricViolet
        ActivityFilterType.INCOME -> Color(0xFF059669)
        ActivityFilterType.TRANSFERS -> ReportRoyalIndigo
        ActivityFilterType.ALL -> ReportElectricViolet
    }

    val titleLabel = when (filterType) {
        ActivityFilterType.EXPENSES -> if (isBn) "মোট ব্যয়" else "Total Expense"
        ActivityFilterType.INCOME -> if (isBn) "মোট আয়" else "Total Income"
        ActivityFilterType.TRANSFERS -> if (isBn) "মোট ট্রান্সফার" else "Total Transfers"
        ActivityFilterType.ALL -> if (isBn) "নেট ফ্লো" else "Net Flow"
    }

    val avgPerBucket = if (buckets.isNotEmpty()) totalAmount / buckets.size else 0.0
    val maxAmt = remember(buckets) { (buckets.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(1.0) }

    TranslucentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("filtered_reports_daily_bar_chart_card"),
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
            // Header: Total on Left, Average / Day on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = titleLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(totalAmount, currencySymbol, false, language),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier.testTag("daily_bar_total_amount")
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isBn) "দৈনিক গড়" else "Average / Day",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(avgPerBucket, currencySymbol, false, language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = activeAccent,
                            fontSize = 15.sp,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Bar Canvas with Tap + Horizontal Drag Scrubbing
            if (buckets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBn) "কোনো চার্ট ডেটা নেই" else "No chart data for this period",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else {
                val trackColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0).copy(alpha = 0.7f)
                val mutedBarColor = activeAccent.copy(alpha = if (isDark) 0.34f else 0.28f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .pointerInput(buckets) {
                            detectTapGestures { offset ->
                                val idx = ((offset.x / size.width.coerceAtLeast(1)) * buckets.size)
                                    .toInt()
                                    .coerceIn(0, buckets.lastIndex)
                                selectedIndex = idx
                            }
                        }
                        .pointerInput(buckets) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    val idx = ((offset.x / size.width.coerceAtLeast(1)) * buckets.size)
                                        .toInt()
                                        .coerceIn(0, buckets.lastIndex)
                                    selectedIndex = idx
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    val idx = ((change.position.x / size.width.coerceAtLeast(1)) * buckets.size)
                                        .toInt()
                                        .coerceIn(0, buckets.lastIndex)
                                    selectedIndex = idx
                                }
                            )
                        }
                        .testTag("filtered_reports_daily_bar_canvas")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val n = buckets.size.coerceAtLeast(1)
                        val slotW = size.width / n
                        val barW = (slotW * 0.62f).coerceIn(3.dp.toPx(), 24.dp.toPx())
                        val topPad = 8.dp.toPx()
                        val bottomPad = 6.dp.toPx()
                        val usableH = (size.height - topPad - bottomPad).coerceAtLeast(12f)
                        val baselineY = size.height - bottomPad

                        // Subtle horizontal baseline
                        drawLine(
                            color = trackColor,
                            start = Offset(0f, baselineY),
                            end = Offset(size.width, baselineY),
                            strokeWidth = 1.dp.toPx()
                        )

                        buckets.forEachIndexed { idx, bucket ->
                            val x = idx * slotW + (slotW - barW) / 2f
                            val ratio = (bucket.amount / maxAmt).toFloat().coerceIn(0f, 1f)
                            val barH = if (bucket.amount > 0.0) {
                                (ratio * usableH).coerceAtLeast(5.dp.toPx())
                            } else {
                                2.dp.toPx()
                            }
                            val topY = baselineY - barH
                            val corner = CornerRadius(barW / 2f, barW / 2f)
                            val isSelected = selectedIndex == idx

                            if (bucket.amount <= 0.0) {
                                drawRoundRect(
                                    color = if (isSelected) activeAccent.copy(alpha = 0.5f) else trackColor,
                                    topLeft = Offset(x, topY),
                                    size = Size(barW, barH),
                                    cornerRadius = corner
                                )
                            } else if (isSelected || selectedIndex == null) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(activeAccentEnd, activeAccent),
                                        startY = topY,
                                        endY = baselineY
                                    ),
                                    topLeft = Offset(x, topY),
                                    size = Size(barW, barH),
                                    cornerRadius = corner
                                )
                            } else {
                                drawRoundRect(
                                    color = mutedBarColor,
                                    topLeft = Offset(x, topY),
                                    size = Size(barW, barH),
                                    cornerRadius = corner
                                )
                            }
                        }
                    }
                }

                // Compact Start / Mid / End X-axis labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val firstLbl = buckets.firstOrNull()?.label.orEmpty()
                    val midLbl = if (buckets.size >= 3) buckets[buckets.size / 2].label else ""
                    val lastLbl = if (buckets.size > 1) buckets.last().label else ""
                    listOf(firstLbl, midLbl, lastLbl).forEach { lbl ->
                        Text(
                            text = lbl,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                }

                // Live Scrubbing Callout Box directly below chart
                val inspectedBucket = selectedIndex?.let { buckets.getOrNull(it) }
                if (inspectedBucket != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val sharePct = if (totalAmount > 0.0) {
                        ((inspectedBucket.amount / totalAmount) * 100.0).roundToInt()
                    } else 0
                    val sharePctText = if (isBn) {
                        "${CurrencyFormatter.convertToBanglaDigits(sharePct.toString())}% মোট এর"
                    } else {
                        "$sharePct% of total"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeAccent.copy(alpha = if (isDark) 0.16f else 0.09f))
                            .border(
                                width = 1.dp,
                                color = activeAccent.copy(alpha = if (isDark) 0.32f else 0.22f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("daily_bar_scrub_callout"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = inspectedBucket.fullDateLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.5.sp
                                )
                            )
                            val countLabel = if (isBn) {
                                "${CurrencyFormatter.convertToBanglaDigits(inspectedBucket.txCount.toString())}টি লেনদেন • $sharePctText"
                            } else {
                                "${inspectedBucket.txCount} tx • $sharePctText"
                            }
                            Text(
                                text = countLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.5.sp,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }

                        Text(
                            text = CurrencyFormatter.format(inspectedBucket.amount, currencySymbol, false, language),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark && activeAccent == ReportRoyalIndigo) Color(0xFFA5B4FC) else activeAccent,
                                fontSize = 14.5.sp,
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun computeReportTimeBounds(
    selectedTimeRange: String,
    customDateRange: Pair<Long, Long>?,
    transactions: List<TransactionEntity>
): Pair<Long, Long> {
    if (customDateRange != null) return customDateRange
    val nowMs = System.currentTimeMillis()
    return when (selectedTimeRange) {
        "THIS_WEEK" -> {
            val c = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val s = c.timeInMillis
            c.add(Calendar.DAY_OF_YEAR, 6)
            c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
            s to c.timeInMillis
        }
        "PREVIOUS_WEEK" -> {
            val c = Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, -1)
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val s = c.timeInMillis
            c.add(Calendar.DAY_OF_YEAR, 6)
            c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
            s to c.timeInMillis
        }
        "LAST_MONTH" -> {
            val c = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val s = c.timeInMillis
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
            c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
            s to c.timeInMillis
        }
        "THIS_YEAR" -> {
            val c = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val s = c.timeInMillis
            c.set(Calendar.MONTH, Calendar.DECEMBER); c.set(Calendar.DAY_OF_MONTH, 31)
            c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
            s to c.timeInMillis
        }
        "ALL" -> {
            val earliest = transactions.minOfOrNull { it.timestamp } ?: (nowMs - 30L * 24 * 60 * 60 * 1000)
            earliest to nowMs
        }
        else -> {
            // Default: THIS_MONTH
            val c = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val s = c.timeInMillis
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
            c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
            s to c.timeInMillis
        }
    }
}

private fun formatFilteredDatePill(
    customLabel: String?,
    timeRange: String,
    startTime: Long,
    endTime: Long,
    language: AppLanguage
): String {
    if (!customLabel.isNullOrBlank()) return customLabel
    if (timeRange == "ALL") return if (language == AppLanguage.BANGLA) "সকল সময়" else "All Time"
    val sCal = Calendar.getInstance().apply { timeInMillis = startTime }
    val eCal = Calendar.getInstance().apply { timeInMillis = endTime }
    val monthFmt = SimpleDateFormat("MMM", Locale.US)
    val sMonth = monthFmt.format(Date(startTime))
    val eMonth = monthFmt.format(Date(endTime))
    val sDay = sCal.get(Calendar.DAY_OF_MONTH)
    val eDay = eCal.get(Calendar.DAY_OF_MONTH)
    val raw = if (sCal.get(Calendar.YEAR) == eCal.get(Calendar.YEAR)) {
        "$sMonth $sDay – $eMonth $eDay"
    } else {
        "$sMonth $sDay, ${sCal.get(Calendar.YEAR)} – $eMonth $eDay, ${eCal.get(Calendar.YEAR)}"
    }
    return DateFormatter.localizeText(raw, language)
}

private fun buildDailyBarBuckets(
    transactions: List<TransactionEntity>,
    startTime: Long,
    endTime: Long,
    language: AppLanguage
): List<DailyBarBucket> {
    if (endTime < startTime) return emptyList()
    val dayMs = 24L * 60 * 60 * 1000
    val daySpan = (((endTime - startTime) / dayMs).toInt() + 1).coerceAtLeast(1)

    // Monthly buckets when span exceeds 45 days (e.g. This Year / All Time) for clean, legible bars
    if (daySpan > 45) {
        val monthShortFmt = SimpleDateFormat("MMM", Locale.US)
        val monthFullFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
        val cal = Calendar.getInstance().apply {
            timeInMillis = startTime
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val buckets = mutableListOf<DailyBarBucket>()
        var guard = 0
        while (cal.timeInMillis <= endTime && guard < 24) {
            val s = cal.timeInMillis
            val eCal = Calendar.getInstance().apply {
                timeInMillis = s
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }
            val matching = transactions.filter { it.timestamp in s..eCal.timeInMillis }
            buckets.add(
                DailyBarBucket(
                    label = DateFormatter.localizeText(monthShortFmt.format(Date(s)), language),
                    fullDateLabel = DateFormatter.localizeText(monthFullFmt.format(Date(s)), language),
                    amount = matching.sumOf { it.amount },
                    txCount = matching.size
                )
            )
            cal.add(Calendar.MONTH, 1)
            guard++
        }
        return buckets
    }

    val shortFmt = SimpleDateFormat(if (daySpan <= 7) "EEE" else "d MMM", Locale.US)
    val fullFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
    val cal = Calendar.getInstance().apply {
        timeInMillis = startTime
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val buckets = mutableListOf<DailyBarBucket>()
    var guard = 0
    while (cal.timeInMillis <= endTime && guard < 45) {
        val s = cal.timeInMillis
        val e = s + dayMs - 1
        val matching = transactions.filter { it.timestamp in s..e }
        buckets.add(
            DailyBarBucket(
                label = DateFormatter.localizeText(shortFmt.format(Date(s)), language),
                fullDateLabel = DateFormatter.localizeText(fullFmt.format(Date(s)), language),
                amount = matching.sumOf { it.amount },
                txCount = matching.size
            )
        )
        cal.add(Calendar.DAY_OF_YEAR, 1)
        guard++
    }
    return buckets
}
