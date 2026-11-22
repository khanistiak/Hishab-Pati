package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.FinancialIntegrityValidator
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.CategoryIcon
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val DetailsRoyalIndigo = Color(0xFF4F46E5)
private val DetailsElectricViolet = Color(0xFF7C3AED)
private val DetailsAmber = Color(0xFFF59E0B)
private val DetailsCoralRose = Color(0xFFF43F5E)

private data class PastBillingPeriodItem(
    val year: Int,
    val month: Int,
    val shortRangeLabel: String,
    val spent: Double,
    val daysInMonth: Int
)

@Composable
fun BudgetDetailsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val args by viewModel.selectedBudgetDetailsArgs.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val pinnedKeys by viewModel.pinnedBudgetKeys.collectAsState()

    val lastValidArgs = remember { mutableStateOf(args) }
    if (args != null) {
        lastValidArgs.value = args
    }
    val currentArgs = args ?: lastValidArgs.value
    if (currentArgs == null) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    val currencySymbol = settings?.currencySymbol ?: "৳"
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val presentYear = currentArgs.year
    val presentMonth = currentArgs.month

    var viewingYear by remember(presentYear, presentMonth, currentArgs.uniqueKey) {
        mutableIntStateOf(presentYear)
    }
    var viewingMonth by remember(presentYear, presentMonth, currentArgs.uniqueKey) {
        mutableIntStateOf(presentMonth)
    }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showInsightsSheet by remember { mutableStateOf(false) }
    var isSpendingHistoryExpanded by rememberSaveable(currentArgs.uniqueKey) { mutableStateOf(true) }
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionEntity?>(null) }

    val isViewingHistorical = viewingYear != presentYear || viewingMonth != presentMonth
    val viewingMonthKey = remember(viewingYear, viewingMonth) {
        String.format(Locale.US, "%04d-%02d", viewingYear, viewingMonth + 1)
    }
    val presentMonthKey = remember(presentYear, presentMonth) {
        String.format(Locale.US, "%04d-%02d", presentYear, presentMonth + 1)
    }

    // Resolve live budget entity from DB prioritizing exact budgetId (for Manual & custom multi-category budgets)
    val activeBudget = remember(budgets, currentArgs, viewingMonthKey, presentMonthKey) {
        val byId = currentArgs.budgetId?.let { id -> budgets.firstOrNull { it.id == id } }
        if (byId != null) {
            byId
        } else {
            val exactViewing = budgets.firstOrNull { b ->
                !b.isOverall &&
                    b.categoryName.equals(currentArgs.categoryName, ignoreCase = true) &&
                    b.subcategoryName.equals(currentArgs.subcategoryName, ignoreCase = true) &&
                    (b.monthYear == viewingMonthKey || b.monthYear == "ALL")
            }
            exactViewing ?: budgets.firstOrNull { b ->
                !b.isOverall &&
                    b.categoryName.equals(currentArgs.categoryName, ignoreCase = true) &&
                    b.subcategoryName.equals(currentArgs.subcategoryName, ignoreCase = true) &&
                    (b.monthYear == presentMonthKey || b.monthYear == "ALL")
            }
        }
    }

    val effectivePinKey = remember(activeBudget, currentArgs) {
        when {
            activeBudget != null && (activeBudget.isManual || activeBudget.parsedCategories().size > 1 || activeBudget.parsedSubcategoryPairs().size > 1) ->
                "id::${activeBudget.id}"
            else -> currentArgs.uniqueKey
        }
    }

    val matchedCategory = remember(categories, currentArgs.categoryName) {
        categories.firstOrNull { it.name.equals(currentArgs.categoryName, ignoreCase = true) }
    }

    val displayName = remember(activeBudget, matchedCategory, currentArgs, language) {
        val catDisp = matchedCategory?.getDisplayName(language) ?: currentArgs.categoryName
        when {
            activeBudget != null && activeBudget.name.isNotBlank() &&
                !activeBudget.name.equals(activeBudget.categoryName, ignoreCase = true) -> activeBudget.name
            currentArgs.subcategoryName.isNotBlank() -> "$catDisp • ${currentArgs.subcategoryName}"
            else -> catDisp
        }
    }

    val iconName = activeBudget?.iconName?.ifBlank { null }
        ?: matchedCategory?.iconName?.ifBlank { null }
        ?: currentArgs.iconName
    val colorHex = activeBudget?.colorHex?.ifBlank { null }
        ?: matchedCategory?.colorHex?.ifBlank { null }
        ?: currentArgs.colorHex

    val hasDefinedBudget = activeBudget != null && activeBudget.monthlyLimit > 0.0
    val budgetLimit = activeBudget?.monthlyLimit?.coerceAtLeast(0.0) ?: 0.0
    val isPinned = pinnedKeys.contains(effectivePinKey)

    // Calendar math for the currently viewed period
    val totalDaysInViewingMonth = remember(viewingYear, viewingMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, viewingYear)
            set(Calendar.MONTH, viewingMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val daysElapsed = remember(viewingYear, viewingMonth, totalDaysInViewingMonth, todayYear, todayMonth, todayDay) {
        when {
            viewingYear == todayYear && viewingMonth == todayMonth -> todayDay.coerceIn(1, totalDaysInViewingMonth)
            viewingYear < todayYear || (viewingYear == todayYear && viewingMonth < todayMonth) -> totalDaysInViewingMonth
            else -> 1
        }
    }

    val daysRemaining = remember(viewingYear, viewingMonth, totalDaysInViewingMonth, todayYear, todayMonth, todayDay) {
        when {
            viewingYear == todayYear && viewingMonth == todayMonth -> (totalDaysInViewingMonth - todayDay + 1).coerceAtLeast(1)
            viewingYear > todayYear || (viewingYear == todayYear && viewingMonth > todayMonth) -> totalDaysInViewingMonth
            else -> 0
        }
    }

    val shortPeriodRangeLabel = remember(viewingYear, viewingMonth, totalDaysInViewingMonth, language) {
        formatShortDateRange(viewingYear, viewingMonth, totalDaysInViewingMonth, language)
    }

    val viewingMonthName = remember(viewingYear, viewingMonth, language) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, viewingYear)
            set(Calendar.MONTH, viewingMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        DateFormatter.localizeText(SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time), language)
    }

    // Filter transactions matching this budget for the currently viewed period
    val periodTransactions = remember(transactions, viewingYear, viewingMonth, currentArgs, activeBudget) {
        transactions.filter { tx ->
            if (!FinancialIntegrityValidator.isInYearMonth(tx.timestamp, viewingYear, viewingMonth)) return@filter false
            if (activeBudget != null) {
                FinancialIntegrityValidator.isMatchingBudgetTransaction(tx, activeBudget)
            } else {
                FinancialIntegrityValidator.isEligibleExpense(tx) &&
                    tx.category.trim().equals(currentArgs.categoryName.trim(), ignoreCase = true) &&
                    (currentArgs.subcategoryName.isBlank() ||
                        tx.subcategory.trim().equals(currentArgs.subcategoryName.trim(), ignoreCase = true))
            }
        }.sortedByDescending { it.timestamp }
    }

    val periodSpent = remember(periodTransactions) {
        periodTransactions.sumOf { it.amount }
    }
    val periodRemaining = budgetLimit - periodSpent
    val isOverBudget = hasDefinedBudget && periodSpent > budgetLimit
    val usedPct = if (hasDefinedBudget && budgetLimit > 0.0) (periodSpent / budgetLimit) * 100.0 else 0.0
    val isNearLimit = hasDefinedBudget && usedPct >= 80.0 && !isOverBudget
    val progressFraction = if (hasDefinedBudget && budgetLimit > 0.0) {
        (periodSpent / budgetLimit).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val projectedTotal = remember(periodSpent, daysElapsed, totalDaysInViewingMonth, isViewingHistorical, daysRemaining) {
        if (isViewingHistorical || daysRemaining == 0) {
            periodSpent
        } else if (daysElapsed > 0) {
            (periodSpent / daysElapsed.toDouble()) * totalDaysInViewingMonth.toDouble()
        } else {
            periodSpent
        }
    }

    // Past 3 billing periods relative to presentMonth (e.g. Oct, Sep, Aug)
    val pastBillingPeriods = remember(transactions, presentYear, presentMonth, currentArgs, activeBudget, language) {
        (1..3).map { offset ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, presentYear)
                set(Calendar.MONTH, presentMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -offset)
            }
            val pYear = cal.get(Calendar.YEAR)
            val pMonth = cal.get(Calendar.MONTH)
            val maxD = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val pSpent = transactions.filter { tx ->
                if (!FinancialIntegrityValidator.isInYearMonth(tx.timestamp, pYear, pMonth)) return@filter false
                if (activeBudget != null) {
                    FinancialIntegrityValidator.isMatchingBudgetTransaction(tx, activeBudget)
                } else {
                    FinancialIntegrityValidator.isEligibleExpense(tx) &&
                        tx.category.trim().equals(currentArgs.categoryName.trim(), ignoreCase = true) &&
                        (currentArgs.subcategoryName.isBlank() ||
                            tx.subcategory.trim().equals(currentArgs.subcategoryName.trim(), ignoreCase = true))
                }
            }.sumOf { it.amount }
            PastBillingPeriodItem(
                year = pYear,
                month = pMonth,
                shortRangeLabel = formatShortDateRange(pYear, pMonth, maxD, language),
                spent = pSpent,
                daysInMonth = maxD
            )
        }
    }

    // Historical analysis metrics (averaged over past periods that have activity)
    val nonZeroPastPeriods = remember(pastBillingPeriods) {
        pastBillingPeriods.filter { it.spent > 0.001 }
    }
    val typicalPeriodSpend = remember(nonZeroPastPeriods) {
        if (nonZeroPastPeriods.isNotEmpty()) {
            nonZeroPastPeriods.sumOf { it.spent } / nonZeroPastPeriods.size.toDouble()
        } else {
            0.0
        }
    }
    val typicalDailySpend = remember(nonZeroPastPeriods) {
        if (nonZeroPastPeriods.isNotEmpty()) {
            val totalPastDays = nonZeroPastPeriods.sumOf { it.daysInMonth }.coerceAtLeast(1)
            nonZeroPastPeriods.sumOf { it.spent } / totalPastDays.toDouble()
        } else {
            0.0
        }
    }

    // Category & Subcategory breakdown for currently viewed period
    val uncategorizedLabel = AppStrings.t("uncategorized_label", language)
    val isMultiScopeBudget = activeBudget != null && (activeBudget.isManual || activeBudget.parsedCategories().size > 1)
    val subcategoryBreakdown = remember(periodTransactions, uncategorizedLabel, isMultiScopeBudget) {
        periodTransactions
            .groupBy { tx ->
                if (isMultiScopeBudget) {
                    val cat = tx.category.ifBlank { uncategorizedLabel }
                    if (tx.subcategory.isNotBlank()) "$cat • ${tx.subcategory}" else cat
                } else {
                    tx.subcategory.ifBlank { uncategorizedLabel }
                }
            }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Transactions grouped by date
    val groupedTransactions = remember(periodTransactions, language) {
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        periodTransactions.groupBy { tx ->
            DateFormatter.localizeText(sdf.format(Date(tx.timestamp)), language)
        }
    }

    val statusLabel = when {
        !hasDefinedBudget -> AppStrings.t("status_no_limit", language)
        isOverBudget -> AppStrings.t("status_overspent", language)
        isNearLimit -> AppStrings.t("status_near_limit", language)
        else -> AppStrings.t("status_on_track", language)
    }

    val statusColor = when {
        !hasDefinedBudget -> MaterialTheme.colorScheme.onSurfaceVariant
        isOverBudget -> DetailsCoralRose
        isNearLimit -> DetailsAmber
        else -> IncomeGreen
    }

    val statusTileBg = when {
        !hasDefinedBudget -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.7f)
        isOverBudget -> DetailsCoralRose.copy(alpha = if (isDark) 0.18f else 0.12f)
        isNearLimit -> DetailsAmber.copy(alpha = if (isDark) 0.18f else 0.14f)
        else -> IncomeGreen.copy(alpha = if (isDark) 0.18f else 0.12f)
    }

    val cardColor = if (isDark) CardBackgroundDark else Color.White
    val cardBorderColor = if (isDark) Color(0x28A5B4FC) else Color(0x1A6366F1)
    val listState = viewModel.rememberScreenLazyListState(
        subKey = "budget_details_list",
        screenOverride = SubScreen.BUDGET_DETAILS
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 8.dp,
        bottomNavClearance = 28.dp
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("budget_details_screen")
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
                .testTag("budget_details_lazy_column"),
            contentPadding = scrollLayout.contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 0. Historical Data Banner
            item(key = "historical_banner") {
                AnimatedVisibility(
                    visible = isViewingHistorical,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(DetailsRoyalIndigo, DetailsElectricViolet)
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("historical_data_banner")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = AppStrings.t("viewing_historical_data", language),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = shortPeriodRangeLabel,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                onClick = {
                                    viewingYear = presentYear
                                    viewingMonth = presentMonth
                                },
                                modifier = Modifier.testTag("back_to_present_button")
                            ) {
                                Text(
                                    text = AppStrings.t("back_to_present", language),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DetailsRoyalIndigo,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 1. Hero Status Bento Card
            item(key = "budget_details_summary_card") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardColor,
                    border = BorderStroke(1.dp, cardBorderColor),
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("budget_details_summary_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Header: Icon, Name, Budget Limit, Automatic badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIcon(
                                iconName = iconName,
                                colorHex = colorHex,
                                size = 48.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (hasDefinedBudget) {
                                        "${AppStrings.t("budget", language)}: ${CurrencyFormatter.format(budgetLimit, currencySymbol, language = language)}"
                                    } else {
                                        AppStrings.t("no_limit_active", language)
                                    },
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            val isManualBudget = activeBudget?.isManual == true
                            val modeAccent = if (isManualBudget) DetailsElectricViolet else DetailsRoyalIndigo
                            Surface(
                                shape = CircleShape,
                                color = modeAccent.copy(alpha = if (isDark) 0.22f else 0.10f),
                                border = BorderStroke(1.dp, modeAccent.copy(alpha = if (isDark) 0.38f else 0.20f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFC7D2FE) else modeAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isManualBudget) {
                                            AppStrings.t("tracking_mode_manual", language)
                                        } else {
                                            AppStrings.t("automatic_mode", language)
                                        },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFC7D2FE) else modeAccent
                                    )
                                }
                            }
                        }

                        if (!activeBudget?.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = activeBudget!!.description,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Remaining / Overspent Callout + Percentage Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    !hasDefinedBudget -> "${CurrencyFormatter.format(periodSpent, currencySymbol, language = language)} ${AppStrings.t("spent_short", language)}"
                                    isOverBudget -> "${CurrencyFormatter.format(abs(periodRemaining), currencySymbol, language = language)} ${AppStrings.t("overspent_suffix", language)}"
                                    else -> "${CurrencyFormatter.format(periodRemaining, currencySymbol, language = language)} ${AppStrings.t("remaining_suffix", language)}"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = if (isOverBudget) DetailsCoralRose else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (hasDefinedBudget) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val pctTextRaw = if (isOverBudget) {
                                    val overPct = ((periodSpent - budgetLimit) / budgetLimit) * 100.0
                                    "-${String.format(Locale.US, "%.1f", overPct)}%"
                                } else {
                                    "${String.format(Locale.US, "%.1f", usedPct)}%"
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = statusColor.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = CurrencyFormatter.localizeDigits(pctTextRaw, language),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Semantic Progress Bar
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            color = statusColor,
                            trackColor = statusColor.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Date Range • Days Left • Recurrence metadata strip
                        val daysLeftStr = if (daysRemaining > 0) {
                            val dNum = CurrencyFormatter.formatNumber(daysRemaining, language)
                            val unit = if (daysRemaining == 1) AppStrings.t("day_left", language) else AppStrings.t("days_left", language)
                            "$dNum $unit"
                        } else {
                            AppStrings.t("period_ended", language)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF151C2F) else Color(0xFFF6F7FE),
                            border = BorderStroke(1.dp, cardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = DetailsRoyalIndigo,
                                    modifier = Modifier.size(15.dp)
                                )
                                val recurrenceLabel = when (activeBudget?.recurrence?.uppercase(Locale.US)) {
                                    "WEEKLY" -> AppStrings.t("recurrence_every_week", language)
                                    "YEARLY" -> AppStrings.t("recurrence_every_year", language)
                                    "ONE_TIME" -> AppStrings.t("recurrence_one_time", language)
                                    else -> AppStrings.t("every_month", language)
                                }
                                Text(
                                    text = "$shortPeriodRangeLabel • $daysLeftStr • $recurrenceLabel",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Two Compact Tappable Bento Analytics Tiles: Projected Total & Status -> Open Insights Modal Sheet
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Left Tile: Projected Total
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF161D33) else Color(0xFFF3F4FE),
                                border = BorderStroke(1.dp, DetailsRoyalIndigo.copy(alpha = if (isDark) 0.30f else 0.18f)),
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp,
                                onClick = { showInsightsSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("budget_projected_total_tile")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = CurrencyFormatter.format(projectedTotal, currencySymbol, language = language),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = AppStrings.t("budget_insights", language),
                                            tint = if (isDark) Color(0xFFA5B4FC) else DetailsRoyalIndigo,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = AppStrings.t("projected_total", language),
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Right Tile: Status
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = statusTileBg,
                                border = BorderStroke(1.dp, statusColor.copy(alpha = if (isDark) 0.35f else 0.25f)),
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp,
                                onClick = { showInsightsSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("budget_status_tile")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            lineHeight = 18.sp,
                                            color = statusColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = AppStrings.t("budget_insights", language),
                                            tint = statusColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = AppStrings.t("status_label", language),
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = AppStrings.t("tap_for_insights", language),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            // 2. Collapsible Spending History Card (Past Billing Periods with View toggle)
            item(key = "spending_history_card") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cardColor,
                    border = BorderStroke(1.dp, cardBorderColor),
                    shadowElevation = if (isDark) 0.dp else 1.5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("spending_history_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isSpendingHistoryExpanded = !isSpendingHistoryExpanded }
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DetailsElectricViolet.copy(alpha = if (isDark) 0.22f else 0.11f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFFDDD6FE) else DetailsElectricViolet,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = AppStrings.t("spending_history", language),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Icon(
                                imageVector = if (isSpendingHistoryExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = if (isSpendingHistoryExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isSpendingHistoryExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pastBillingPeriods.forEach { period ->
                                    val isSelectedPeriod = viewingYear == period.year && viewingMonth == period.month
                                    val periodOverLimit = hasDefinedBudget && period.spent > budgetLimit

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelectedPeriod) {
                                            DetailsRoyalIndigo.copy(alpha = if (isDark) 0.22f else 0.10f)
                                        } else if (isDark) {
                                            Color(0xFF151C2F)
                                        } else {
                                            Color(0xFFF8F9FE)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelectedPeriod) {
                                                DetailsRoyalIndigo.copy(alpha = 0.55f)
                                            } else {
                                                cardBorderColor
                                            }
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = period.shortRangeLabel,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${CurrencyFormatter.format(period.spent, currencySymbol, language = language)} ${AppStrings.t("spent_short", language)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (periodOverLimit) DetailsCoralRose else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelectedPeriod) {
                                                            Brush.horizontalGradient(
                                                                listOf(DetailsRoyalIndigo, DetailsElectricViolet)
                                                            )
                                                        } else {
                                                            Brush.horizontalGradient(
                                                                listOf(
                                                                    DetailsRoyalIndigo.copy(alpha = if (isDark) 0.22f else 0.10f),
                                                                    DetailsElectricViolet.copy(alpha = if (isDark) 0.22f else 0.10f)
                                                                )
                                                            )
                                                        }
                                                    )
                                                    .clickable {
                                                        if (isSelectedPeriod) {
                                                            viewingYear = presentYear
                                                            viewingMonth = presentMonth
                                                        } else {
                                                            viewingYear = period.year
                                                            viewingMonth = period.month
                                                        }
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                                    .testTag("history_period_view_${period.year}_${period.month}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (isSelectedPeriod) {
                                                        AppStrings.t("viewing_period", language)
                                                    } else {
                                                        AppStrings.t("view_period", language)
                                                    },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelectedPeriod) {
                                                        Color.White
                                                    } else if (isDark) {
                                                        Color(0xFFC7D2FE)
                                                    } else {
                                                        DetailsRoyalIndigo
                                                    }
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

            // 3. Category & Subcategory Breakdown Card
            item(key = "category_subcategory_breakdown_card") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cardColor,
                    border = BorderStroke(1.dp, cardBorderColor),
                    shadowElevation = if (isDark) 0.dp else 1.5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("category_subcategory_breakdown_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = AppStrings.t("category_subcat_breakdown", language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Total Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                CategoryIcon(
                                    iconName = iconName,
                                    colorHex = colorHex,
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = CurrencyFormatter.format(periodSpent, currencySymbol, language = language),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        Spacer(modifier = Modifier.height(10.dp))

                        if (subcategoryBreakdown.isEmpty()) {
                            Text(
                                text = AppStrings.t("no_recorded_spending", language),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            val accentColor = remember(colorHex) {
                                try {
                                    Color(android.graphics.Color.parseColor(colorHex))
                                } catch (_: Exception) {
                                    DetailsRoyalIndigo
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                subcategoryBreakdown.forEach { (subcatName, subcatAmount) ->
                                    val share = if (periodSpent > 0.001) {
                                        (subcatAmount / periodSpent).toFloat().coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                    val sharePctStr = CurrencyFormatter.formatPercent(share * 100f, language)
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = subcatName,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${CurrencyFormatter.format(subcatAmount, currencySymbol, language = language)} ($sharePctStr)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(5.dp))
                                        LinearProgressIndicator(
                                            progress = { share },
                                            color = accentColor,
                                            trackColor = accentColor.copy(alpha = 0.14f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(2.5.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Grouped Daily Transactions List Header
            item(key = "transactions_section_header") {
                val txCountStr = CurrencyFormatter.formatNumber(periodTransactions.size, language)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${AppStrings.t("transactions_label", language)} ($txCountStr)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = shortPeriodRangeLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (periodTransactions.isEmpty()) {
                item(key = "empty_transactions_card") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, cardBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = AppStrings.t("no_transactions_found", language),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                groupedTransactions.forEach { (dateStr, txList) ->
                    val daySum = txList.sumOf { it.amount }
                    item(key = "date_hdr_$dateStr") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DetailsRoyalIndigo.copy(alpha = if (isDark) 0.20f else 0.09f)
                            ) {
                                Text(
                                    text = dateStr,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFC7D2FE) else DetailsRoyalIndigo,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = "-${CurrencyFormatter.format(daySum, currencySymbol, language = language)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(txList, key = { it.id }) { tx ->
                        val account = remember(tx.accountId, accounts) {
                            accounts.firstOrNull { it.id == tx.accountId }
                        }
                        val accountName = account?.name ?: AppStrings.t("cash", language)

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = cardColor,
                            border = BorderStroke(1.dp, cardBorderColor),
                            shadowElevation = if (isDark) 0.dp else 1.dp,
                            onClick = { selectedTransactionForDetails = tx },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("transaction_item_${tx.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                        text = tx.title.ifBlank { tx.category },
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = buildString {
                                            append(tx.category)
                                            if (tx.subcategory.isNotBlank()) {
                                                append(" • ")
                                                append(tx.subcategory)
                                            }
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "- ${CurrencyFormatter.format(tx.amount, currencySymbol, language = language)}",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ExpenseRed
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = accountName,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        // Fixed scroll-aware frosted liquid-glass header (matches PlanScreen exactly)
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "budget_details_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            Color.White.copy(alpha = 0.72f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.85f)
                        ),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .testTag("budget_details_back_button"),
                        onClick = onBack
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = AppStrings.t("back", language),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = AppStrings.t("budget_details", language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = viewingMonthName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            ),
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.togglePinnedBudget(effectivePinKey) },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("budget_details_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = AppStrings.t("pinned_badge", language),
                            tint = if (isPinned) DetailsAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("budget_details_more_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (hasDefinedBudget) {
                                            AppStrings.t("edit_budget", language)
                                        } else {
                                            AppStrings.t("set_budget", language)
                                        }
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.openAddBudget(
                                        budget = activeBudget,
                                        defaultCategory = currentArgs.categoryName,
                                        defaultMonthKey = presentMonthKey
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            if (activeBudget != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = AppStrings.t("delete_budget", language),
                                            color = ExpenseRed
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Budget Insights Modal Sheet (opened by tapping Projected Total or Status tile)
    if (showInsightsSheet) {
        BudgetInsightsModalSheet(
            budgetName = displayName,
            categoryIcon = iconName,
            categoryColorHex = colorHex,
            currencySymbol = currencySymbol,
            language = language,
            hasDefinedBudget = hasDefinedBudget,
            limit = budgetLimit,
            spent = periodSpent,
            remaining = periodRemaining,
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining,
            totalDaysInPeriod = totalDaysInViewingMonth,
            projectedTotal = projectedTotal,
            transactionCount = periodTransactions.size,
            typicalDailySpend = typicalDailySpend,
            typicalPeriodSpend = typicalPeriodSpend,
            hasHistoricalData = nonZeroPastPeriods.isNotEmpty(),
            isHistoricalPeriod = isViewingHistorical || daysRemaining == 0,
            onDismiss = { showInsightsSheet = false }
        )
    }

    // Transaction Details Modal Bottom Sheet
    selectedTransactionForDetails?.let { tx ->
        TransactionDetailsSheet(
            transaction = tx,
            budgetName = displayName,
            categoryIcon = iconName,
            categoryColorHex = colorHex,
            currencySymbol = currencySymbol,
            accounts = accounts,
            language = language,
            onDismiss = { selectedTransactionForDetails = null },
            onEditTransaction = { targetTx ->
                selectedTransactionForDetails = null
                viewModel.openEditTransaction(targetTx)
            }
        )
    }

    // Delete Budget Confirmation Dialog
    if (showDeleteConfirm && activeBudget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = AppStrings.t("delete_budget_confirm_title", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = AppStrings.t("delete_budget_confirm_body", language))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteBudget(activeBudget.id)
                        onBack()
                    }
                ) {
                    Text(
                        text = AppStrings.t("delete", language),
                        color = ExpenseRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = AppStrings.t("cancel", language))
                }
            }
        )
    }
}

private fun formatShortDateRange(
    year: Int,
    month: Int,
    maxDays: Int,
    language: AppLanguage
): String {
    val startCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val endCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, maxDays)
    }
    val sdf = SimpleDateFormat("MMM d", Locale.US)
    val raw = "${sdf.format(startCal.time)} – ${sdf.format(endCal.time)}"
    return DateFormatter.localizeText(raw, language)
}
