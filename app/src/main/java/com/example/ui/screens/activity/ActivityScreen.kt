package com.example.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.ActivityFilterType
import com.example.ui.ActivitySort
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.CategoryIcon
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.TranslucentCard
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import com.example.data.model.AppLanguage
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.navigation.rememberScreenLazyListState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ActivityDateGroup(
    val dateKey: String,
    val dateLabel: String,
    val dayTotalText: String,
    val transactions: List<TransactionEntity>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState = viewModel.rememberScreenLazyListState(tabOverride = MainTab.ACTIVITY)
) {
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val searchQuery by viewModel.activitySearchQuery.collectAsState()
    val selectedFilter by viewModel.activityFilter.collectAsState()
    val selectedSort by viewModel.activitySort.collectAsState()

    val currencySymbol = settings?.currencySymbol ?: "৳"
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA

    var selectedTxForDetails by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val vmCategoryFilter by viewModel.activitySelectedCategory.collectAsState()
    val vmAccountFilter by viewModel.activitySelectedAccount.collectAsState()
    val vmCustomDateRange by viewModel.activityCustomDateRange.collectAsState()
    val vmDateRangeLabel by viewModel.activityDateRangeLabel.collectAsState()

    var isFilteredReportMode by rememberSaveable { mutableStateOf(false) }
    var selectedTimeRange by rememberSaveable { mutableStateOf("ALL") } // ALL, THIS_WEEK, PREVIOUS_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR
    var selectedAccountIdFilter by rememberSaveable { mutableStateOf(viewModel.activitySelectedAccount.value) }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf(viewModel.activitySelectedCategory.value) }
    var customDateRange by remember { mutableStateOf(viewModel.activityCustomDateRange.value) }
    var customDateRangeLabel by rememberSaveable { mutableStateOf(viewModel.activityDateRangeLabel.value) }
    var showSortBottomSheet by remember { mutableStateOf(false) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    val filteredReportListState = viewModel.rememberScreenLazyListState(
        subKey = "filtered_report",
        tabOverride = MainTab.ACTIVITY
    )

    val currentTab by viewModel.currentTab.collectAsState()

    LaunchedEffect(vmCategoryFilter) {
        selectedCategoryFilter = vmCategoryFilter
    }

    LaunchedEffect(vmAccountFilter) {
        selectedAccountIdFilter = vmAccountFilter
    }

    LaunchedEffect(vmCustomDateRange, vmDateRangeLabel) {
        customDateRange = vmCustomDateRange
        customDateRangeLabel = vmDateRangeLabel
    }

    LaunchedEffect(currentTab) {
        if (currentTab == MainTab.ACTIVITY) {
            selectedCategoryFilter = viewModel.activitySelectedCategory.value
            selectedAccountIdFilter = viewModel.activitySelectedAccount.value
            customDateRange = viewModel.activityCustomDateRange.value
            customDateRangeLabel = viewModel.activityDateRangeLabel.value
        }
    }

    BackHandler(enabled = isFilteredReportMode) {
        isFilteredReportMode = false
    }

    BackHandler(enabled = selectedTxForDetails != null) {
        selectedTxForDetails = null
    }

    BackHandler(enabled = txToDelete != null) {
        txToDelete = null
    }

    // Time boundary calculation supporting all periods
    val (reportStartTime, reportEndTime) = remember(selectedTimeRange, customDateRange, transactions) {
        if (customDateRange != null) {
            customDateRange!!.first to customDateRange!!.second
        } else {
            val nowMs = System.currentTimeMillis()
            when (selectedTimeRange) {
                "THIS_WEEK" -> {
                    val c = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.DAY_OF_WEEK, firstDayOfWeek)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val s = c.timeInMillis
                    c.add(java.util.Calendar.DAY_OF_WEEK, 6)
                    c.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    c.set(java.util.Calendar.MINUTE, 59)
                    c.set(java.util.Calendar.SECOND, 59)
                    c.set(java.util.Calendar.MILLISECOND, 999)
                    s to c.timeInMillis
                }
                "PREVIOUS_WEEK" -> {
                    val c = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.WEEK_OF_YEAR, -1)
                        set(java.util.Calendar.DAY_OF_WEEK, firstDayOfWeek)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val s = c.timeInMillis
                    c.add(java.util.Calendar.DAY_OF_WEEK, 6)
                    c.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    c.set(java.util.Calendar.MINUTE, 59)
                    c.set(java.util.Calendar.SECOND, 59)
                    c.set(java.util.Calendar.MILLISECOND, 999)
                    s to c.timeInMillis
                }
                "THIS_MONTH" -> {
                    val c = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.DAY_OF_MONTH, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val s = c.timeInMillis
                    c.set(java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                    c.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    c.set(java.util.Calendar.MINUTE, 59)
                    c.set(java.util.Calendar.SECOND, 59)
                    c.set(java.util.Calendar.MILLISECOND, 999)
                    s to c.timeInMillis
                }
                "LAST_MONTH" -> {
                    val c = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.MONTH, -1)
                        set(java.util.Calendar.DAY_OF_MONTH, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val s = c.timeInMillis
                    c.set(java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                    c.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    c.set(java.util.Calendar.MINUTE, 59)
                    c.set(java.util.Calendar.SECOND, 59)
                    c.set(java.util.Calendar.MILLISECOND, 999)
                    s to c.timeInMillis
                }
                "THIS_YEAR" -> {
                    val c = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.DAY_OF_YEAR, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val s = c.timeInMillis
                    c.set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER)
                    c.set(java.util.Calendar.DAY_OF_MONTH, 31)
                    c.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    c.set(java.util.Calendar.MINUTE, 59)
                    c.set(java.util.Calendar.SECOND, 59)
                    c.set(java.util.Calendar.MILLISECOND, 999)
                    s to c.timeInMillis
                }
                "PAST_3_MONTHS" -> {
                    val c = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.MONTH, -3)
                        set(java.util.Calendar.DAY_OF_MONTH, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    c.timeInMillis to nowMs
                }
                "LAST_YEAR" -> {
                    val c = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.YEAR, -1)
                        set(java.util.Calendar.DAY_OF_YEAR, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val s = c.timeInMillis
                    c.set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER)
                    c.set(java.util.Calendar.DAY_OF_MONTH, 31)
                    c.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    c.set(java.util.Calendar.MINUTE, 59)
                    c.set(java.util.Calendar.SECOND, 59)
                    c.set(java.util.Calendar.MILLISECOND, 999)
                    s to c.timeInMillis
                }
                else -> {
                    val earliest = transactions.minOfOrNull { it.timestamp } ?: (nowMs - 30L * 24 * 3600 * 1000)
                    earliest to nowMs
                }
            }
        }
    }

    // Filter and Sort logic
    val transactionsForReport = remember(
        transactions,
        searchQuery,
        selectedFilter,
        selectedTimeRange,
        reportStartTime,
        reportEndTime,
        customDateRange,
        selectedAccountIdFilter
    ) {
        transactions.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    tx.subcategory.contains(searchQuery, ignoreCase = true)

            val isStrictExpense = tx.type == TransactionType.EXPENSE &&
                    !tx.category.equals("Transfer", ignoreCase = true)
            val isStrictIncome = tx.type == TransactionType.INCOME &&
                    !tx.category.equals("Transfer", ignoreCase = true)
            val isTransferTx = tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT

            val matchesType = when (selectedFilter) {
                ActivityFilterType.ALL -> true
                ActivityFilterType.EXPENSES -> isStrictExpense
                ActivityFilterType.INCOME -> isStrictIncome
                ActivityFilterType.TRANSFERS -> isTransferTx
            }

            val matchesTime = when {
                customDateRange != null -> tx.timestamp in customDateRange!!.first..customDateRange!!.second
                selectedTimeRange == "ALL" -> true
                else -> tx.timestamp in reportStartTime..reportEndTime
            }

            val matchesAccount = selectedAccountIdFilter == null || tx.accountId == selectedAccountIdFilter

            matchesQuery && matchesType && matchesTime && matchesAccount
        }
    }

    val filteredList = remember(
        transactionsForReport,
        selectedCategoryFilter,
        selectedSort
    ) {
        val list = if (selectedCategoryFilter == null) {
            transactionsForReport
        } else {
            transactionsForReport.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
        }
        when (selectedSort) {
            ActivitySort.NEWEST -> list.sortedByDescending { it.timestamp }
            ActivitySort.OLDEST -> list.sortedBy { it.timestamp }
            ActivitySort.HIGHEST_AMOUNT -> list.sortedByDescending { it.amount }
            ActivitySort.LOWEST_AMOUNT -> list.sortedBy { it.amount }
        }
    }

    // Totals for filtered transactions computed in a single pass and remembered
    val (totalMoneyIn, totalMoneyOut, netTotal) = remember(filteredList) {
        var moneyIn = 0.0
        var moneyOut = 0.0
        filteredList.forEach { tx ->
            if (tx.type == TransactionType.INCOME && !tx.category.equals("Transfer", ignoreCase = true)) {
                moneyIn += tx.amount
            } else if (tx.type == TransactionType.EXPENSE && !tx.category.equals("Transfer", ignoreCase = true)) {
                moneyOut += tx.amount
            }
        }
        Triple(moneyIn, moneyOut, moneyIn - moneyOut)
    }

    // Report timeline and breakdown metrics
    val (startingBalance, endingBalance, timelinePoints) = remember(
        reportStartTime, reportEndTime, accounts, transactions, selectedAccountIdFilter, language
    ) {
        FilteredReportCalculator.calculateBalanceTimeline(
            startTime = reportStartTime,
            endTime = reportEndTime,
            accounts = accounts,
            transactions = transactions,
            selectedAccountId = selectedAccountIdFilter,
            language = language
        )
    }

    val (totalCategoryAmount, categoryShares) = remember(transactionsForReport, categories, language) {
        FilteredReportCalculator.calculateCategoryBreakdown(
            transactions = transactionsForReport,
            categories = categories,
            language = language
        )
    }

    val reportTotalAmount = remember(filteredList, selectedFilter) {
        when (selectedFilter) {
            ActivityFilterType.EXPENSES -> filteredList.filter {
                it.type == TransactionType.EXPENSE && !it.category.equals("Transfer", ignoreCase = true)
            }.sumOf { it.amount }
            ActivityFilterType.INCOME -> filteredList.filter {
                it.type == TransactionType.INCOME && !it.category.equals("Transfer", ignoreCase = true)
            }.sumOf { it.amount }
            ActivityFilterType.TRANSFERS -> filteredList.filter {
                it.type == TransactionType.TRANSFER || it.type == TransactionType.CC_PAYMENT
            }.sumOf { it.amount }
            ActivityFilterType.ALL -> {
                var sum = 0.0
                filteredList.forEach { tx ->
                    if (!tx.category.equals("Transfer", ignoreCase = true)) {
                        when (tx.type) {
                            TransactionType.INCOME -> sum += tx.amount
                            TransactionType.EXPENSE -> sum -= tx.amount
                            else -> {}
                        }
                    }
                }
                sum
            }
        }
    }

    // Grouping by Date
    val groupedByDate = remember(filteredList, language) {
        filteredList.groupBy { DateFormatter.formatDateGroup(it.timestamp, language) }
    }

    val dateGroups = remember(filteredList, selectedSort, language, currencySymbol) {
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayLabelFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)

        val grouped = filteredList.groupBy { tx ->
            dayKeyFormat.format(Date(tx.timestamp))
        }

        val sortedGroupEntries = if (selectedSort == ActivitySort.OLDEST) {
            grouped.entries.sortedBy { it.key }
        } else {
            grouped.entries.sortedByDescending { it.key }
        }

        sortedGroupEntries.map { (dateKey, txs) ->
            val firstTimestamp = txs.firstOrNull()?.timestamp ?: System.currentTimeMillis()
            val formattedLabel = DateFormatter.localizeText(dayLabelFormat.format(Date(firstTimestamp)), language)

            val dayExpenses = txs.filter {
                it.type == TransactionType.EXPENSE && !it.category.equals("Transfer", ignoreCase = true)
            }.sumOf { it.amount }
            val dayIncome = txs.filter {
                it.type == TransactionType.INCOME && !it.category.equals("Transfer", ignoreCase = true)
            }.sumOf { it.amount }

            val totalText = when {
                dayExpenses > 0 && dayIncome == 0.0 -> "Δ -" + CurrencyFormatter.format(dayExpenses, currencySymbol, language = language)
                dayIncome > 0 && dayExpenses == 0.0 -> "Δ +" + CurrencyFormatter.format(dayIncome, currencySymbol, language = language)
                dayExpenses > 0 && dayIncome > 0 -> {
                    val net = dayIncome - dayExpenses
                    if (net >= 0) "Δ +" + CurrencyFormatter.format(net, currencySymbol, language = language)
                    else "Δ -" + CurrencyFormatter.format(-net, currencySymbol, language = language)
                }
                else -> ""
            }

            ActivityDateGroup(
                dateKey = dateKey,
                dateLabel = formattedLabel,
                dayTotalText = totalText,
                transactions = txs
            )
        }
    }

    BackHandler(enabled = isFilteredReportMode) {
        isFilteredReportMode = false
    }

    if (isFilteredReportMode) {
        FilteredReportContent(
            modifier = modifier.fillMaxSize(),
            listState = filteredReportListState,
            filteredList = filteredList,
            groupedByDate = groupedByDate,
            selectedFilter = selectedFilter,
            selectedSort = selectedSort,
            selectedTimeRange = selectedTimeRange,
            customDateRangeLabel = customDateRangeLabel,
            selectedAccountIdFilter = selectedAccountIdFilter,
            selectedCategoryFilter = selectedCategoryFilter,
            startingBalance = startingBalance,
            endingBalance = endingBalance,
            timelinePoints = timelinePoints,
            totalCategoryAmount = totalCategoryAmount,
            categoryShares = categoryShares,
            currencySymbol = currencySymbol,
            language = language,
            accounts = accounts,
            categories = categories,
            startTime = reportStartTime,
            endTime = reportEndTime,
            reportTotalAmount = reportTotalAmount,
            onBack = { isFilteredReportMode = false },
            onOpenFilter = { showFilterBottomSheet = true },
            onOpenSort = { showSortBottomSheet = true },
            onCycleFilterType = {
                val nextType = when (selectedFilter) {
                    ActivityFilterType.EXPENSES -> ActivityFilterType.INCOME
                    ActivityFilterType.INCOME -> ActivityFilterType.TRANSFERS
                    ActivityFilterType.TRANSFERS -> ActivityFilterType.EXPENSES
                    ActivityFilterType.ALL -> ActivityFilterType.EXPENSES
                }
                viewModel.activityFilter.value = nextType
            },
            onSelectPeriod = { period ->
                selectedTimeRange = period
                customDateRange = null
                customDateRangeLabel = null
                viewModel.activityCustomDateRange.value = null
                viewModel.activityDateRangeLabel.value = null
            },
            onSelectCustomDateRange = { range, label ->
                customDateRange = range
                customDateRangeLabel = label
                selectedTimeRange = "CUSTOM"
                viewModel.activityCustomDateRange.value = range
                viewModel.activityDateRangeLabel.value = label
            },
            onSelectCategory = { cat ->
                selectedCategoryFilter = cat
                viewModel.activitySelectedCategory.value = cat
            },
            onClearCategory = {
                selectedCategoryFilter = null
                viewModel.activitySelectedCategory.value = null
            },
            onClearAccount = {
                selectedAccountIdFilter = null
            },
            onResetAll = {
                viewModel.activityFilter.value = ActivityFilterType.EXPENSES
                selectedTimeRange = "THIS_MONTH"
                customDateRange = null
                customDateRangeLabel = null
                viewModel.activityCustomDateRange.value = null
                viewModel.activityDateRangeLabel.value = null
                selectedCategoryFilter = null
                viewModel.activitySelectedCategory.value = null
                selectedAccountIdFilter = null
            },
            onTxClick = { tx -> selectedTxForDetails = tx },
            searchQuery = searchQuery,
            onExportComplete = { msg ->
                viewModel.userNotificationMessage.value = msg
            }
        )
    } else {
        val hasActiveFilters = selectedFilter != ActivityFilterType.ALL ||
                selectedAccountIdFilter != null ||
                selectedCategoryFilter != null ||
                selectedTimeRange != "ALL" ||
                customDateRange != null

        val scrollLayout = rememberScrollAwareGlassLayout(
            listState = listState,
            defaultHeaderHeight = 108.dp,
            extraTopGap = 1.dp
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
                    .testTag("activity_lazy_column"),
                contentPadding = scrollLayout.contentPadding
            ) {

            // 3. Active Filters Chips (Only displayed if filters are active)
            if (hasActiveFilters) {
                item(key = "activity_active_filter_chips") {
                    LazyRow(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedFilter != ActivityFilterType.ALL) {
                            item(key = "filter_chip_type") {
                                val typeLabel = when (selectedFilter) {
                                    ActivityFilterType.EXPENSES -> AppStrings.t("expenses", language)
                                    ActivityFilterType.INCOME -> AppStrings.t("income", language)
                                    ActivityFilterType.TRANSFERS -> AppStrings.t("transfers", language)
                                    else -> ""
                                }
                                AssistChip(
                                    onClick = { viewModel.activityFilter.value = ActivityFilterType.ALL },
                                    label = { Text("$typeLabel ✕") },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                )
                            }
                        }

                        if (selectedAccountIdFilter != null) {
                            val acc = accounts.find { it.id == selectedAccountIdFilter }
                            item(key = "filter_chip_account") {
                                AssistChip(
                                    onClick = { selectedAccountIdFilter = null },
                                    label = { Text("${acc?.name ?: "Account"} ✕") },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                )
                            }
                        }

                        if (selectedCategoryFilter != null) {
                            item(key = "filter_chip_category") {
                                AssistChip(
                                    onClick = {
                                        selectedCategoryFilter = null
                                        viewModel.activitySelectedCategory.value = null
                                    },
                                    label = { Text("$selectedCategoryFilter ✕") },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                )
                            }
                        }

                        if (customDateRangeLabel != null || selectedTimeRange != "ALL") {
                            val dateLabel = customDateRangeLabel ?: when (selectedTimeRange) {
                                "THIS_WEEK" -> if (isBn) "এই সপ্তাহ" else "This Week"
                                "PREVIOUS_WEEK" -> if (isBn) "গত সপ্তাহ" else "Previous Week"
                                "THIS_MONTH" -> AppStrings.t("this_month", language)
                                "LAST_MONTH" -> AppStrings.t("last_month", language)
                                "THIS_YEAR" -> AppStrings.t("this_year", language)
                                else -> "Date Filter"
                            }
                            item(key = "filter_chip_date") {
                                AssistChip(
                                    onClick = {
                                        selectedTimeRange = "ALL"
                                        customDateRange = null
                                        customDateRangeLabel = null
                                        viewModel.activityCustomDateRange.value = null
                                        viewModel.activityDateRangeLabel.value = null
                                    },
                                    label = { Text("$dateLabel ✕") },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                )
                            }
                        }

                        item(key = "filter_chip_report_view") {
                            AssistChip(
                                onClick = {
                                    if (selectedFilter == ActivityFilterType.ALL) {
                                        viewModel.activityFilter.value = ActivityFilterType.EXPENSES
                                    }
                                    if (selectedTimeRange == "ALL" && customDateRange == null) {
                                        selectedTimeRange = "THIS_MONTH"
                                    }
                                    isFilteredReportMode = true
                                },
                                label = { Text(if (isBn) "রিপোর্ট দেখুন" else "View Report") },
                                shape = RoundedCornerShape(20.dp),
                                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            )
                        }

                        item(key = "filter_chip_reset_all") {
                            TextButton(
                                onClick = {
                                    viewModel.activityFilter.value = ActivityFilterType.ALL
                                    selectedTimeRange = "ALL"
                                    customDateRange = null
                                    customDateRangeLabel = null
                                    viewModel.activityCustomDateRange.value = null
                                    viewModel.activityDateRangeLabel.value = null
                                    selectedCategoryFilter = null
                                    viewModel.activitySelectedCategory.value = null
                                    selectedAccountIdFilter = null
                                }
                            ) {
                                Text(
                                    text = if (isBn) "রিসেট করুন" else "Reset All",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // 4. Grouped Transactions by Date
            if (dateGroups.isEmpty()) {
                item(key = "activity_empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                if (isBn) "\"$searchQuery\" এর জন্য কোনো লেনদেন পাওয়া যায়নি" else "No transactions found for \"$searchQuery\""
                            } else {
                                if (isBn) "কোনো লেনদেন পাওয়া যায়নি" else "No transactions found"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (searchQuery.isNotBlank() || hasActiveFilters) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.activitySearchQuery.value = ""
                                    viewModel.activityFilter.value = ActivityFilterType.ALL
                                    selectedTimeRange = "ALL"
                                    customDateRange = null
                                    customDateRangeLabel = null
                                    viewModel.activityCustomDateRange.value = null
                                    viewModel.activityDateRangeLabel.value = null
                                    selectedCategoryFilter = null
                                    viewModel.activitySelectedCategory.value = null
                                    selectedAccountIdFilter = null
                                }
                            ) {
                                Text(if (isBn) "ফিল্টার ও সার্চ সাফ করুন" else "Clear Search & Filters")
                            }
                        }
                    }
                }
            } else {
                dateGroups.forEach { group ->
                    item(key = "date_header_${group.dateKey}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = group.dateLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            if (group.dayTotalText.isNotBlank()) {
                                Text(
                                    text = group.dayTotalText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    item(key = "date_card_${group.dateKey}") {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                group.transactions.forEachIndexed { index, tx ->
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

                                    val categorySubcategory = when {
                                        tx.subcategory.isNotBlank() -> "${tx.category} • ${tx.subcategory}"
                                        tx.category.isNotBlank() -> tx.category
                                        isTransfer -> if (isBn) "স্থানান্তর" else "Transfer"
                                        else -> ""
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedTxForDetails = tx }
                                            .padding(vertical = 10.dp)
                                            .testTag("tx_item_${tx.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Category Icon
                                        val (rowIconName, rowColorHex) = remember(tx, categories) {
                                            resolveTransactionCategoryVisuals(tx, categories)
                                        }
                                        CategoryIcon(
                                            iconName = rowIconName,
                                            colorHex = rowColorHex,
                                            size = 42.dp
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Left Column: Title (top) & Category • Subcategory (below)
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

                                        // Right Column: Amount (top) & Wallet / Transfer path (below)
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = amountPrefix + CurrencyFormatter.format(tx.amount, currencySymbol, language = language),
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

                                    if (index < group.transactions.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                            thickness = 0.8.dp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            // Shared Fixed Scroll-Aware Frosted Header for Activity Tab (combined title and search bar)
            ScrollAwareFixedHeader(
                layoutState = scrollLayout,
                testTag = "activity_fixed_header"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Title and Filter Button Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBn) "সকল লেনদেন" else "All Transactions",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 22.sp
                            )
                        )

                        IconButton(
                            onClick = {
                                if (selectedFilter == ActivityFilterType.ALL) {
                                    viewModel.activityFilter.value = ActivityFilterType.EXPENSES
                                }
                                if (selectedTimeRange == "ALL" && customDateRange == null) {
                                    selectedTimeRange = "THIS_MONTH"
                                }
                                isFilteredReportMode = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("activity_top_filter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = "Filter",
                                tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Fixed Search Field and Sort Control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.activitySearchQuery.value = it },
                            placeholder = {
                                Text(
                                    text = if (isBn) "কীওয়ার্ড খুঁজুন..." else "Search Keywords",
                                    fontSize = 13.5.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.activitySearchQuery.value = "" },
                                        modifier = Modifier.testTag("activity_search_clear_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("activity_search_field")
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Distinct sort control immediately to the right of search field
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("activity_search_sort_button"),
                            onClick = { showSortBottomSheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Redesigned Transaction Details Bottom Sheet
    selectedTxForDetails?.let { tx ->
        TransactionDetailsBottomSheet(
            transaction = tx,
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            language = language,
            onDismiss = { selectedTxForDetails = null },
            onDuplicate = { targetTx ->
                selectedTxForDetails = null
                viewModel.duplicateTransaction(targetTx)
            },
            onEdit = { targetTx ->
                selectedTxForDetails = null
                viewModel.transactionToEdit.value = targetTx
                viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
            },
            onDelete = { targetTx ->
                txToDelete = targetTx
                selectedTxForDetails = null
            }
        )
    }

    // Delete Confirmation Dialog
    txToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            modifier = Modifier.testTag("tx_delete_confirm_dialog"),
            title = { Text(if (isBn) "লেনদেন মুছবেন?" else "Delete Transaction") },
            text = { Text(if (isBn) "আপনি কি নিশ্চিত যে \"${tx.title}\" স্থায়ীভাবে মুছে ফেলবেন?" else "Are you sure you want to permanently delete \"${tx.title}\"? Account balances will be reverted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx.id)
                        txToDelete = null
                    },
                    modifier = Modifier.testTag("tx_delete_confirm_button")
                ) {
                    Text(if (isBn) "মুছুন" else "Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { txToDelete = null },
                    modifier = Modifier.testTag("tx_delete_cancel_button")
                ) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isBn) "সাজানোর ধরণ" else "Sort Transactions",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val sortOptions = listOf(
                    ActivitySort.NEWEST to AppStrings.t("sort_newest", language),
                    ActivitySort.OLDEST to AppStrings.t("sort_oldest", language),
                    ActivitySort.HIGHEST_AMOUNT to AppStrings.t("sort_highest", language),
                    ActivitySort.LOWEST_AMOUNT to AppStrings.t("sort_lowest", language)
                )

                sortOptions.forEach { (sortOpt, label) ->
                    val isSelected = selectedSort == sortOpt
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.activitySort.value = sortOpt
                                showSortBottomSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterBottomSheet) {
        ApplyFiltersBottomSheet(
            onDismiss = { showFilterBottomSheet = false },
            currentType = selectedFilter,
            currentTimeRange = selectedTimeRange,
            currentCustomDateRange = customDateRange,
            currentCategory = selectedCategoryFilter,
            currentAccountId = selectedAccountIdFilter,
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            language = language,
            onApply = { type, timeRange, customRange, cat, accId ->
                viewModel.activityFilter.value = type
                selectedTimeRange = timeRange
                customDateRange = customRange
                customDateRangeLabel = if (customRange != null) {
                    val sCal = Calendar.getInstance().apply { timeInMillis = customRange.first }
                    val eCal = Calendar.getInstance().apply { timeInMillis = customRange.second }
                    val sMonth = SimpleDateFormat("MMM", Locale.US).format(Date(customRange.first))
                    val eMonth = SimpleDateFormat("MMM", Locale.US).format(Date(customRange.second))
                    val sDay = sCal.get(Calendar.DAY_OF_MONTH)
                    val eDay = eCal.get(Calendar.DAY_OF_MONTH)
                    if (sCal.get(Calendar.YEAR) == eCal.get(Calendar.YEAR)) {
                        "$sMonth $sDay - $eMonth $eDay"
                    } else {
                        val sYear = sCal.get(Calendar.YEAR)
                        val eYear = eCal.get(Calendar.YEAR)
                        "$sMonth $sDay, $sYear - $eMonth $eDay, $eYear"
                    }
                } else null
                selectedCategoryFilter = cat
                viewModel.activitySelectedCategory.value = cat
                viewModel.activityCustomDateRange.value = customRange
                viewModel.activityDateRangeLabel.value = customDateRangeLabel
                selectedAccountIdFilter = accId
                isFilteredReportMode = true
                showFilterBottomSheet = false
            }
        )
    }
}
