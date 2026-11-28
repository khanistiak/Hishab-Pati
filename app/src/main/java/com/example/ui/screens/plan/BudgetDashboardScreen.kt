package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.FinancialIntegrityValidator
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.ui.BudgetDetailsArgs
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.getIconForName
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class BudgetFilterOption(val label: String) {
    ALL("All"),
    ON_TRACK("On Track"),
    NEAR_LIMIT("Near Limit"),
    EXCEEDED("Exceeded"),
    NO_LIMIT("No Limit")
}

enum class BudgetSortOption(val label: String) {
    SPENT_DESC("Highest Spent"),
    SPENT_ASC("Lowest Spent"),
    PERCENT_DESC("Highest % Used"),
    REMAINING_ASC("Lowest Remaining"),
    NAME_ASC("Name (A–Z)")
}

data class CategoryBudgetUiModel(
    val budget: BudgetEntity?,
    val categoryName: String,
    val subcategoryName: String = "",
    val displayName: String = categoryName,
    val categoryIcon: String,
    val categoryColorHex: String,
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val percentage: Float,
    val transactionCount: Int,
    val recentTransactions: List<TransactionEntity>,
    val isOverBudget: Boolean,
    val isNearLimit: Boolean,
    val hasDefinedBudget: Boolean,
    val isSubcategory: Boolean = false
) {
    val uniqueKey: String
        get() = when {
            budget != null && (budget.isManual || budget.parsedCategories().size > 1 || budget.parsedSubcategoryPairs().size > 1) ->
                "id::${budget.id}"
            subcategoryName.isNotBlank() -> "$categoryName::$subcategoryName"
            else -> categoryName
        }
}

private val BudgetRoyalIndigo = Color(0xFF4F46E5)
private val BudgetElectricViolet = Color(0xFF7C3AED)
private val BudgetAmber = Color(0xFFF59E0B)
private val BudgetCoralRose = Color(0xFFF43F5E)

@Composable
fun BudgetDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val pinnedBudgetKeys by viewModel.pinnedBudgetKeys.collectAsState()

    val currencySymbol = settings?.currencySymbol ?: "৳"
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val initialYearMonth = remember(transactions, budgets) {
        FinancialIntegrityValidator.resolveReferenceYearMonth(transactions, budgets)
    }
    var selectedYear by remember { mutableIntStateOf(initialYearMonth.first) }
    var selectedMonth by remember { mutableIntStateOf(initialYearMonth.second) }
    var showZeroSpendCategories by remember { mutableStateOf(false) }
    var showUnbudgetedCategories by remember { mutableStateOf(false) }

    val selectedMonthKey = remember(selectedYear, selectedMonth) {
        String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth + 1)
    }

    val selectedMonthLabel = remember(selectedYear, selectedMonth, language) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        DateFormatter.localizeText(SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.time), language)
    }

    val daysRemainingInMonth = remember(selectedYear, selectedMonth, todayYear, todayMonth, todayDay) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        when {
            selectedYear == todayYear && selectedMonth == todayMonth ->
                (maxDays - todayDay + 1).coerceAtLeast(1)
            selectedYear > todayYear || (selectedYear == todayYear && selectedMonth > todayMonth) ->
                maxDays
            else -> 0
        }
    }

    val monthExpenses = remember(transactions, selectedYear, selectedMonth) {
        transactions.filter { tx ->
            FinancialIntegrityValidator.isInYearMonth(tx.timestamp, selectedYear, selectedMonth) &&
                FinancialIntegrityValidator.isEligibleExpense(tx)
        }
    }

    val totalMonthSpent = remember(monthExpenses) {
        monthExpenses.sumOf { it.amount }
    }

    val monthBudgets = remember(budgets, selectedMonthKey) {
        budgets.filter { it.monthYear == selectedMonthKey || it.monthYear == "ALL" }
    }

    val explicitOverallBudget = remember(monthBudgets) {
        monthBudgets.firstOrNull { it.isOverall && it.monthYear == selectedMonthKey }
            ?: monthBudgets.firstOrNull { it.isOverall }
    }

    val categoryBudgetsOnly = remember(monthBudgets) {
        monthBudgets.filter { !it.isOverall && !it.categoryName.equals("Overall", ignoreCase = true) }
    }

    val activeCategoryBudgets = remember(categoryBudgetsOnly) {
        categoryBudgetsOnly.filter { it.monthlyLimit > 0.0 }
    }

    val summedCategoryLimits = remember(activeCategoryBudgets, selectedMonthKey) {
        FinancialIntegrityValidator.computeTotalMonthlyBudget(activeCategoryBudgets, selectedMonthKey)
    }

    val totalBudgetLimit = remember(explicitOverallBudget, summedCategoryLimits) {
        if (explicitOverallBudget != null && explicitOverallBudget.monthlyLimit > 0.0) {
            explicitOverallBudget.monthlyLimit
        } else {
            summedCategoryLimits
        }
    }

    // Separate Budgeted vs Unbudgeted Math:
    // Calculate Budgeted Spent strictly from active tracked budgets so unbudgeted spend
    // never falsely marks an on-track budget as overspent.
    val budgetedSpent = remember(monthExpenses, activeCategoryBudgets) {
        if (activeCategoryBudgets.isNotEmpty()) {
            monthExpenses.filter { tx ->
                activeCategoryBudgets.any { b ->
                    FinancialIntegrityValidator.isMatchingBudgetTransaction(tx, b)
                }
            }.sumOf { it.amount }
        } else {
            monthExpenses.sumOf { it.amount }
        }
    }

    val unbudgetedSpent = remember(totalMonthSpent, budgetedSpent, activeCategoryBudgets) {
        if (activeCategoryBudgets.isNotEmpty()) {
            (totalMonthSpent - budgetedSpent).coerceAtLeast(0.0)
        } else {
            0.0
        }
    }

    val activeBudgetModels = remember(activeCategoryBudgets, categories, monthExpenses, language, pinnedBudgetKeys) {
        val catMap = categories.associateBy { it.name.trim().lowercase(Locale.ENGLISH) }
        activeCategoryBudgets
            .map { b ->
                val cat = catMap[b.categoryName.trim().lowercase(Locale.ENGLISH)]
                val matchingTxs = monthExpenses.filter { tx ->
                    FinancialIntegrityValidator.isMatchingBudgetTransaction(tx, b)
                }
                val spent = matchingTxs.sumOf { it.amount }
                val limit = b.monthlyLimit.coerceAtLeast(0.0)
                val remaining = limit - spent
                val pct = if (limit > 0.0) ((spent / limit) * 100.0).toFloat() else 0f
                val catDisplay = cat?.getDisplayName(language) ?: b.categoryName
                val disp = when {
                    b.name.isNotBlank() && !b.name.equals(b.categoryName, ignoreCase = true) -> b.name
                    b.subcategoryName.isNotBlank() -> "$catDisplay • ${b.subcategoryName}"
                    else -> catDisplay
                }
                CategoryBudgetUiModel(
                    budget = b,
                    categoryName = b.categoryName,
                    subcategoryName = b.subcategoryName,
                    displayName = disp,
                    categoryIcon = cat?.iconName?.ifBlank { null } ?: b.iconName.ifBlank { "category" },
                    categoryColorHex = cat?.colorHex?.ifBlank { null } ?: b.colorHex.ifBlank { "#4F46E5" },
                    limit = limit,
                    spent = spent,
                    remaining = remaining,
                    percentage = pct,
                    transactionCount = matchingTxs.size,
                    recentTransactions = matchingTxs.take(5),
                    isOverBudget = spent > limit,
                    isNearLimit = pct >= 80f && spent <= limit,
                    hasDefinedBudget = true,
                    isSubcategory = b.subcategoryName.isNotBlank()
                )
            }
            .sortedWith(
                compareByDescending<CategoryBudgetUiModel> { pinnedBudgetKeys.contains(it.uniqueKey) }
                    .thenByDescending { it.percentage }
                    .thenByDescending { it.spent }
            )
    }

    val unbudgetedModels = remember(activeCategoryBudgets, categories, monthExpenses, language) {
        val budgetedCatNames = activeCategoryBudgets
            .filter { !it.isManual }
            .flatMap { b ->
                val multi = b.parsedCategories()
                if (multi.isNotEmpty()) multi else listOf(b.categoryName)
            }
            .map { it.lowercase(Locale.ENGLISH) }
            .toSet()

        val expenseCategories = categories.filter { !it.isIncome && !it.isTransfer }
        val knownNames = expenseCategories.map { it.name.lowercase(Locale.ENGLISH) }.toSet()

        val fromDefinedCategories = expenseCategories
            .filter { !budgetedCatNames.contains(it.name.lowercase(Locale.ENGLISH)) }
            .map { cat ->
                val matchingTxs = monthExpenses.filter { it.category.equals(cat.name, ignoreCase = true) }
                val spent = matchingTxs.sumOf { it.amount }
                CategoryBudgetUiModel(
                    budget = null,
                    categoryName = cat.name,
                    subcategoryName = "",
                    displayName = cat.getDisplayName(language),
                    categoryIcon = cat.iconName.ifBlank { "category" },
                    categoryColorHex = cat.colorHex.ifBlank { "#64748B" },
                    limit = 0.0,
                    spent = spent,
                    remaining = -spent,
                    percentage = 0f,
                    transactionCount = matchingTxs.size,
                    recentTransactions = matchingTxs.take(5),
                    isOverBudget = false,
                    isNearLimit = false,
                    hasDefinedBudget = false,
                    isSubcategory = false
                )
            }

        val extraTxCategories = monthExpenses
            .map { it.category }
            .distinctBy { it.lowercase(Locale.ENGLISH) }
            .filter { name ->
                val key = name.lowercase(Locale.ENGLISH)
                !budgetedCatNames.contains(key) && !knownNames.contains(key) && !name.equals("Overall", ignoreCase = true)
            }
            .map { catName ->
                val matchingTxs = monthExpenses.filter { it.category.equals(catName, ignoreCase = true) }
                val spent = matchingTxs.sumOf { it.amount }
                val matchedCat = categories.firstOrNull { it.name.trim().equals(catName.trim(), ignoreCase = true) }
                CategoryBudgetUiModel(
                    budget = null,
                    categoryName = catName,
                    subcategoryName = "",
                    displayName = matchedCat?.getDisplayName(language) ?: catName,
                    categoryIcon = matchedCat?.iconName?.ifBlank { null } ?: "category",
                    categoryColorHex = matchedCat?.colorHex?.ifBlank { null } ?: "#64748B",
                    limit = 0.0,
                    spent = spent,
                    remaining = -spent,
                    percentage = 0f,
                    transactionCount = matchingTxs.size,
                    recentTransactions = matchingTxs.take(5),
                    isOverBudget = false,
                    isNearLimit = false,
                    hasDefinedBudget = false,
                    isSubcategory = false
                )
            }

        fromDefinedCategories + extraTxCategories
    }

    val unbudgetedWithSpend = remember(unbudgetedModels) {
        unbudgetedModels.filter { it.spent > 0.001 }.sortedByDescending { it.spent }
    }
    val unbudgetedZeroSpend = remember(unbudgetedModels) {
        unbudgetedModels.filter { it.spent <= 0.001 }.sortedBy { it.displayName.lowercase(Locale.ENGLISH) }
    }

    val cardColor = if (isDark) CardBackgroundDark else Color.White
    val cardBorderColor = if (isDark) Color(0x28A5B4FC) else Color(0x1A6366F1)
    val listState = viewModel.rememberScreenLazyListState(
        subKey = "budget_dashboard_list",
        screenOverride = SubScreen.BUDGET_DASHBOARD
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 6.dp,
        bottomNavClearance = 96.dp
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("budget_dashboard_screen")
    ) {
        LazyColumn(
            state = listState,
            contentPadding = scrollLayout.contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    scrollLayout.backdropState,
                    listState,
                    scrollLayout.contentGraphicsLayer
                )
        ) {
            // 1. Period Selector Bar (< November 2026 > + Current Month pill)
            item(key = "budget_period_bar") {
                val isCurrentCalendarMonth = selectedYear == todayYear && selectedMonth == todayMonth
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = cardColor,
                        shadowElevation = if (isDark) 0.dp else 1.dp,
                        border = BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedMonth == 0) {
                                        selectedMonth = 11
                                        selectedYear -= 1
                                    } else {
                                        selectedMonth -= 1
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("budget_prev_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = AppStrings.t("previous_month", language),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = selectedMonthLabel,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (selectedMonth == 11) {
                                        selectedMonth = 0
                                        selectedYear += 1
                                    } else {
                                        selectedMonth += 1
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("budget_next_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = AppStrings.t("next_month", language),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isCurrentCalendarMonth) {
                            BudgetRoyalIndigo.copy(alpha = if (isDark) 0.24f else 0.12f)
                        } else {
                            cardColor
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isCurrentCalendarMonth) {
                                BudgetRoyalIndigo.copy(alpha = 0.40f)
                            } else {
                                cardBorderColor
                            }
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                selectedYear = todayYear
                                selectedMonth = todayMonth
                            }
                            .testTag("budget_current_month_button")
                    ) {
                        Text(
                            text = AppStrings.t("current_month", language),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentCalendarMonth) {
                                if (isDark) Color(0xFFC7D2FE) else BudgetRoyalIndigo
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            // 2. Overall Monthly Budget Hero Bento Card (with Separate Budgeted vs Unbudgeted Math)
            item(key = "overall_budget_hero_card") {
                OverallBudgetHeroCard(
                    budgetedSpent = budgetedSpent,
                    unbudgetedSpent = unbudgetedSpent,
                    totalBudgetLimit = totalBudgetLimit,
                    hasCategoryBudgets = activeCategoryBudgets.isNotEmpty(),
                    daysRemaining = daysRemainingInMonth,
                    currencySymbol = currencySymbol,
                    language = language,
                    isDark = isDark,
                    cardColor = cardColor,
                    borderColor = cardBorderColor,
                    onEditOverallBudget = {
                        viewModel.openAddBudget(
                            budget = explicitOverallBudget,
                            defaultIsOverall = true,
                            defaultMonthKey = selectedMonthKey
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 2b. Interactive Sunburst Chart (Synced with Budgeted Spent & Royal Indigo Accents)
            item(key = "budget_sunburst_chart") {
                BudgetSunburstCard(
                    activeBudgetModels = activeBudgetModels,
                    unbudgetedWithSpend = unbudgetedWithSpend,
                    monthExpenses = monthExpenses,
                    totalMonthSpent = totalMonthSpent,
                    totalBudgetLimit = totalBudgetLimit,
                    currencySymbol = currencySymbol,
                    language = language,
                    cardColor = cardColor,
                    borderColor = cardBorderColor,
                    budgetedSpent = budgetedSpent,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 3. Section 1: Active Budgets (2-Column Bento Grid + Pin/Star Support)
            item(key = "active_budgets_header") {
                val countText = if (isBn) {
                    CurrencyFormatter.convertToBanglaDigits(activeBudgetModels.size.toString())
                } else {
                    activeBudgetModels.size.toString()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${AppStrings.t("active_budgets", language)} ($countText)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = AppStrings.t("tap_to_view", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            if (activeBudgetModels.isEmpty()) {
                item(key = "no_active_budgets_card") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, cardBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                BudgetRoyalIndigo.copy(alpha = 0.16f),
                                                BudgetElectricViolet.copy(alpha = 0.16f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = BudgetRoyalIndigo,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = AppStrings.t("no_budget_set", language),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = AppStrings.t("monthly_budget_desc", language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(BudgetRoyalIndigo, BudgetElectricViolet)
                                        )
                                    )
                                    .clickable {
                                        viewModel.openAddBudget(
                                            budget = null,
                                            defaultMonthKey = selectedMonthKey
                                        )
                                    }
                                    .padding(horizontal = 18.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "+ ${AppStrings.t("set_budget", language)}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                // Crisp 2-Column Bento Card Grid
                val chunkedActive = activeBudgetModels.chunked(2)
                items(
                    items = chunkedActive,
                    key = { row -> "grid_row_${row.joinToString("_") { it.uniqueKey }}" }
                ) { rowModels ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowModels.forEach { model ->
                            ActiveBudgetBentoTile(
                                model = model,
                                isPinned = pinnedBudgetKeys.contains(model.uniqueKey),
                                currencySymbol = currencySymbol,
                                language = language,
                                isDark = isDark,
                                cardColor = cardColor,
                                borderColor = cardBorderColor,
                                onTogglePin = { viewModel.togglePinnedBudget(model.uniqueKey) },
                                onCardClick = {
                                    // Push BudgetDetailsScreen onto back stack (never reset or switch bottom tab!)
                                    viewModel.openBudgetDetails(
                                        BudgetDetailsArgs(
                                            budgetId = model.budget?.id,
                                            categoryName = model.categoryName,
                                            subcategoryName = model.subcategoryName,
                                            year = selectedYear,
                                            month = selectedMonth,
                                            iconName = model.categoryIcon,
                                            colorHex = model.categoryColorHex
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowModels.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 4. Section 2: Unbudgeted Categories (N)
            if (unbudgetedWithSpend.isNotEmpty() || unbudgetedZeroSpend.isNotEmpty()) {
                item(key = "unbudgeted_categories_header") {
                    val unbudgetedCountText = if (isBn) {
                        CurrencyFormatter.convertToBanglaDigits(unbudgetedWithSpend.size.toString())
                    } else {
                        unbudgetedWithSpend.size.toString()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showUnbudgetedCategories = !showUnbudgetedCategories }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("toggle_unbudgeted_categories"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${AppStrings.t("unbudgeted_categories", language)} ($unbudgetedCountText)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Icon(
                                imageVector = if (showUnbudgetedCategories) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showUnbudgetedCategories) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (unbudgetedSpent > 0.001) {
                            Text(
                                text = CurrencyFormatter.format(unbudgetedSpent, currencySymbol, language = language),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                if (showUnbudgetedCategories) {
                    items(unbudgetedWithSpend, key = { "unbudgeted_${it.uniqueKey}" }) { model ->
                        UnbudgetedCategoryCard(
                            model = model,
                            currencySymbol = currencySymbol,
                            language = language,
                            isDark = isDark,
                            cardColor = cardColor,
                            borderColor = cardBorderColor,
                            onCardClick = {
                                // Push BudgetDetailsScreen onto back stack so Back always returns cleanly to BudgetDashboard
                                viewModel.openBudgetDetails(
                                    BudgetDetailsArgs(
                                        budgetId = null,
                                        categoryName = model.categoryName,
                                        subcategoryName = model.subcategoryName,
                                        year = selectedYear,
                                        month = selectedMonth,
                                        iconName = model.categoryIcon,
                                        colorHex = model.categoryColorHex
                                    )
                                )
                            },
                            onSetLimit = {
                                viewModel.openAddBudget(
                                    budget = null,
                                    defaultCategory = model.categoryName,
                                    defaultMonthKey = selectedMonthKey
                                )
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (unbudgetedZeroSpend.isNotEmpty()) {
                        item(key = "zero_spend_toggle") {
                            val zeroCountText = if (isBn) {
                                CurrencyFormatter.convertToBanglaDigits(unbudgetedZeroSpend.size.toString())
                            } else {
                                unbudgetedZeroSpend.size.toString()
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = cardColor,
                                    border = BorderStroke(1.dp, cardBorderColor),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { showZeroSpendCategories = !showZeroSpendCategories }
                                        .testTag("toggle_zero_spend_categories")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (showZeroSpendCategories) {
                                                "${AppStrings.t("hide_zero_spend_categories", language)} ($zeroCountText)"
                                            } else {
                                                "${AppStrings.t("show_zero_spend_categories", language)} ($zeroCountText)"
                                            },
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (showZeroSpendCategories) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (showZeroSpendCategories) {
                            items(unbudgetedZeroSpend, key = { "zero_${it.uniqueKey}" }) { model ->
                                UnbudgetedCategoryCard(
                                    model = model,
                                    currencySymbol = currencySymbol,
                                    language = language,
                                    isDark = isDark,
                                    cardColor = cardColor,
                                    borderColor = cardBorderColor,
                                    onCardClick = {
                                        viewModel.openAddBudget(
                                            budget = null,
                                            defaultCategory = model.categoryName,
                                            defaultMonthKey = selectedMonthKey
                                        )
                                    },
                                    onSetLimit = {
                                        viewModel.openAddBudget(
                                            budget = null,
                                            defaultCategory = model.categoryName,
                                            defaultMonthKey = selectedMonthKey
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fixed scroll-aware frosted liquid-glass header (matches PlanScreen exactly)
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "budget_dashboard_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
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
                        .testTag("budget_dashboard_back_button"),
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.t("budget_dashboard", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = AppStrings.t("monthly_spending_vs_limit", language),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Bottom-right Royal Indigo-to-Electric Violet circular + FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = BudgetRoyalIndigo,
                    spotColor = BudgetElectricViolet
                )
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(BudgetRoyalIndigo, BudgetElectricViolet)
                    )
                )
                .clickable {
                    viewModel.openAddBudget(
                        budget = null,
                        defaultMonthKey = selectedMonthKey
                    )
                }
                .testTag("add_budget_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = AppStrings.t("add_budget_btn", language),
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun OverallBudgetHeroCard(
    budgetedSpent: Double,
    unbudgetedSpent: Double,
    totalBudgetLimit: Double,
    hasCategoryBudgets: Boolean,
    daysRemaining: Int,
    currencySymbol: String,
    language: AppLanguage,
    isDark: Boolean,
    cardColor: Color,
    borderColor: Color,
    onEditOverallBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val hasLimit = totalBudgetLimit > 0.0
    val rawPct = if (hasLimit) (budgetedSpent / totalBudgetLimit) * 100.0 else 0.0
    val pctInt = rawPct.roundToInt()
    val progress = if (hasLimit) (budgetedSpent / totalBudgetLimit).toFloat().coerceIn(0f, 1f) else 0f
    val remaining = totalBudgetLimit - budgetedSpent
    val dailySafeSpend = if (hasLimit && remaining > 0.0 && daysRemaining > 0) {
        remaining / daysRemaining.toDouble()
    } else {
        0.0
    }

    val statusColor = when {
        !hasLimit -> BudgetRoyalIndigo
        rawPct > 100.0 -> BudgetCoralRose
        rawPct >= 80.0 -> BudgetAmber
        else -> IncomeGreen
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        shadowElevation = if (isDark) 0.dp else 2.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("overall_budget_hero_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasCategoryBudgets) {
                            AppStrings.t("budgeted_spent", language).uppercase(Locale.US)
                        } else {
                            AppStrings.t("total_spent", language).uppercase(Locale.US)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.9.sp,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyFormatter.format(budgetedSpent, currencySymbol, language = language),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 27.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (!hasLimit) {
                    Surface(
                        shape = CircleShape,
                        color = BudgetRoyalIndigo.copy(alpha = if (isDark) 0.22f else 0.12f),
                        onClick = onEditOverallBudget,
                        modifier = Modifier.testTag("set_overall_limit_pill")
                    ) {
                        Text(
                            text = AppStrings.t("set_overall_limit", language),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFC7D2FE) else BudgetRoyalIndigo,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    val pctStr = if (isBn) {
                        CurrencyFormatter.convertToBanglaDigits(pctInt.toString())
                    } else {
                        pctInt.toString()
                    }
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "$pctStr% ${AppStrings.t("used_suffix", language)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.t("total_budget", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (hasLimit) {
                            CurrencyFormatter.format(totalBudgetLimit, currencySymbol, language = language)
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    val isOver = hasLimit && remaining < 0.0
                    Text(
                        text = if (isOver) {
                            AppStrings.t("over_label", language)
                        } else {
                            AppStrings.t("remaining_label", language)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = when {
                            !hasLimit -> "—"
                            isOver -> "${AppStrings.t("over_by", language)} ${CurrencyFormatter.format(-remaining, currencySymbol, language = language)}"
                            else -> "${CurrencyFormatter.format(remaining, currencySymbol, language = language)} ${AppStrings.t("left_suffix", language)}"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = when {
                                !hasLimit -> MaterialTheme.colorScheme.onSurface
                                isOver -> BudgetCoralRose
                                else -> IncomeGreen
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = AppStrings.t("daily_safe_spend", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (hasLimit) {
                            "${CurrencyFormatter.format(dailySafeSpend, currencySymbol, language = language)}${AppStrings.t("day_short", language)}"
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Secondary Separate Pill for Unbudgeted Spend (never corrupting Budgeted progress!)
            if (unbudgetedSpent > 0.001) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF161E34) else Color(0xFFF4F3FE),
                    border = BorderStroke(
                        1.dp,
                        BudgetElectricViolet.copy(alpha = if (isDark) 0.30f else 0.18f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unbudgeted_spend_pill")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFC4B5FD) else BudgetElectricViolet,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.t("unbudgeted_spend", language),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyFormatter.format(unbudgetedSpent, currencySymbol, language = language),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFDDD6FE) else BudgetElectricViolet
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val daysText = if (daysRemaining > 0) {
                    val dStr = if (isBn) CurrencyFormatter.convertToBanglaDigits(daysRemaining.toString()) else daysRemaining.toString()
                    "$dStr ${AppStrings.t("days_left", language)}"
                } else {
                    AppStrings.t("period_ended", language)
                }
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                )

                Surface(
                    shape = CircleShape,
                    color = BudgetRoyalIndigo.copy(alpha = if (isDark) 0.22f else 0.11f),
                    onClick = onEditOverallBudget,
                    modifier = Modifier.testTag("edit_overall_budget_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFC7D2FE) else BudgetRoyalIndigo,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (hasLimit) {
                                AppStrings.t("edit_overall_budget", language)
                            } else {
                                AppStrings.t("set_overall_limit", language)
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFC7D2FE) else BudgetRoyalIndigo
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBudgetBentoTile(
    model: CategoryBudgetUiModel,
    isPinned: Boolean,
    currencySymbol: String,
    language: AppLanguage,
    isDark: Boolean,
    cardColor: Color,
    borderColor: Color,
    onTogglePin: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val categoryAccent = remember(model.categoryColorHex) {
        try {
            Color(android.graphics.Color.parseColor(model.categoryColorHex))
        } catch (_: Exception) {
            BudgetRoyalIndigo
        }
    }

    // Semantic Progress Color: Emerald on-track, Amber >=80%, Coral Rose overspent
    val barColor = when {
        model.isOverBudget -> BudgetCoralRose
        model.percentage >= 80f -> BudgetAmber
        else -> IncomeGreen
    }
    val pctInt = model.percentage.roundToInt()
    val pctStr = if (isBn) CurrencyFormatter.convertToBanglaDigits(pctInt.toString()) else pctInt.toString()

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardColor,
        shadowElevation = if (isDark) 0.dp else 1.5.dp,
        border = BorderStroke(
            width = if (isPinned) 1.25.dp else 1.dp,
            color = if (isPinned) BudgetRoyalIndigo.copy(alpha = if (isDark) 0.55f else 0.35f) else borderColor
        ),
        onClick = onCardClick,
        modifier = modifier.testTag("budget_category_card_${model.uniqueKey}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Squircle Category Icon + Star/Pin Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = categoryAccent.copy(alpha = if (isDark) 0.22f else 0.14f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getIconForName(model.categoryIcon),
                            contentDescription = model.displayName,
                            tint = categoryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("pin_budget_${model.uniqueKey}")
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = AppStrings.t("pinned_badge", language),
                        tint = if (isPinned) BudgetAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Budget Name + optional Manual/Multi-category tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (model.budget?.isManual == true) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = BudgetElectricViolet.copy(alpha = if (isDark) 0.24f else 0.12f)
                    ) {
                        Text(
                            text = AppStrings.t("manual_badge_short", language),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFDDD6FE) else BudgetElectricViolet,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Remaining / Overspent Callout
            val diffFormatted = CurrencyFormatter.format(abs(model.remaining), currencySymbol, language = language)
            Text(
                text = if (model.isOverBudget) {
                    "$diffFormatted ${AppStrings.t("overspent_suffix", language)}"
                } else {
                    "$diffFormatted ${AppStrings.t("remaining_suffix", language)}"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (model.isOverBudget) BudgetCoralRose else IncomeGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Color-Coded Progress Bar
            LinearProgressIndicator(
                progress = { (model.percentage / 100f).coerceIn(0f, 1f) },
                color = barColor,
                trackColor = barColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Spent / Limit + Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${CurrencyFormatter.formatCompact(model.spent, currencySymbol, language = language)} / ${CurrencyFormatter.formatCompact(model.limit, currencySymbol, language = language)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = CircleShape,
                    color = barColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "$pctStr%",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = barColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UnbudgetedCategoryCard(
    model: CategoryBudgetUiModel,
    currencySymbol: String,
    language: AppLanguage,
    isDark: Boolean,
    cardColor: Color,
    borderColor: Color,
    onCardClick: () -> Unit,
    onSetLimit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryAccent = remember(model.categoryColorHex) {
        try {
            Color(android.graphics.Color.parseColor(model.categoryColorHex))
        } catch (_: Exception) {
            BudgetRoyalIndigo
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardColor,
        shadowElevation = if (isDark) 0.dp else 1.dp,
        border = BorderStroke(1.dp, borderColor),
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("unbudgeted_category_card_${model.uniqueKey}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = categoryAccent.copy(alpha = if (isDark) 0.22f else 0.13f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getIconForName(model.categoryIcon),
                        contentDescription = model.displayName,
                        tint = categoryAccent,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${CurrencyFormatter.format(model.spent, currencySymbol, language = language)} ${AppStrings.t("spent_short", language)} • ${AppStrings.t("no_limit", language)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = BudgetRoyalIndigo.copy(alpha = if (isDark) 0.22f else 0.10f),
                onClick = onSetLimit
            ) {
                Text(
                    text = AppStrings.t("set_limit", language),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFC7D2FE) else BudgetRoyalIndigo,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                )
            }
        }
    }
}
