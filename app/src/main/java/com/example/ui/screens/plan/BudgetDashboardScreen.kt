package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.PieChart
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
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.getIconForName
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.navigation.rememberScreenLazyListState
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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
        get() = if (subcategoryName.isNotBlank()) "$categoryName::$subcategoryName" else categoryName
}

private val BudgetAmber = Color(0xFFF59E0B)
private val BudgetSkyBlue = Color(0xFF0284C7)

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

    val selectedMonthKey = remember(selectedYear, selectedMonth) {
        String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth + 1)
    }

    val selectedMonthLabel = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.time)
    }

    val selectedMonthDateRange = remember(selectedYear, selectedMonth) {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, startCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        startCal.timeInMillis to endCal.timeInMillis
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

    val listState = rememberScreenLazyListState(viewModel = viewModel)
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 6.dp
    )

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

    val summedCategoryLimits = remember(categoryBudgetsOnly, selectedMonthKey) {
        FinancialIntegrityValidator.computeTotalMonthlyBudget(categoryBudgetsOnly, selectedMonthKey)
    }

    val totalBudgetLimit = remember(explicitOverallBudget, summedCategoryLimits) {
        if (explicitOverallBudget != null && explicitOverallBudget.monthlyLimit > 0.0) {
            explicitOverallBudget.monthlyLimit
        } else {
            summedCategoryLimits
        }
    }

    val activeBudgetModels = remember(categoryBudgetsOnly, categories, monthExpenses, language) {
        val catMap = categories.associateBy { it.name.lowercase(Locale.ENGLISH) }
        categoryBudgetsOnly
            .filter { it.monthlyLimit > 0.0 }
            .map { b ->
                val cat = catMap[b.categoryName.lowercase(Locale.ENGLISH)]
                val matchingTxs = monthExpenses.filter { tx ->
                    tx.category.equals(b.categoryName, ignoreCase = true) &&
                        (b.subcategoryName.isBlank() || tx.subcategory.equals(b.subcategoryName, ignoreCase = true))
                }
                val spent = matchingTxs.sumOf { it.amount }
                val limit = b.monthlyLimit.coerceAtLeast(0.0)
                val remaining = limit - spent
                val pct = if (limit > 0.0) ((spent / limit) * 100.0).toFloat() else 0f
                val catDisplay = cat?.getDisplayName(language) ?: b.categoryName
                val disp = if (b.subcategoryName.isNotBlank()) "$catDisplay • ${b.subcategoryName}" else catDisplay
                CategoryBudgetUiModel(
                    budget = b,
                    categoryName = b.categoryName,
                    subcategoryName = b.subcategoryName,
                    displayName = disp,
                    categoryIcon = b.iconName.ifBlank { cat?.iconName ?: "category" },
                    categoryColorHex = b.colorHex.ifBlank { cat?.colorHex ?: "#0284C7" },
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
            .sortedWith(compareByDescending<CategoryBudgetUiModel> { it.percentage }.thenByDescending { it.spent })
    }

    val unbudgetedModels = remember(categoryBudgetsOnly, categories, monthExpenses, language) {
        val budgetedCatNames = categoryBudgetsOnly
            .filter { it.monthlyLimit > 0.0 }
            .map { it.categoryName.lowercase(Locale.ENGLISH) }
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
                CategoryBudgetUiModel(
                    budget = null,
                    categoryName = catName,
                    subcategoryName = "",
                    displayName = catName,
                    categoryIcon = "category",
                    categoryColorHex = "#64748B",
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

    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF090D16),
                    Color(0xFF111827),
                    Color(0xFF0B132B)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFEAF4F8),
                    Color(0xFFF4F2FA)
                )
            )
        }
    }

    val cardColor = if (isDark) CardBackgroundDark else MaterialTheme.colorScheme.surfaceContainerLowest
    val cardBorderColor = if (isDark) Color(0x22FFFFFF) else Color(0x146366F1)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .testTag("budget_dashboard_screen")
    ) {
        LazyColumn(
            state = listState,
            contentPadding = scrollLayout.contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    backdropState = scrollLayout.backdropState,
                    lazyListState = listState,
                    contentLayer = scrollLayout.contentGraphicsLayer
                )
        ) {
            // 1. Period Selector Bar (< November 2026 > + Current Month quick reset)
            item(key = "budget_period_bar") {
                val isCurrentCalendarMonth = selectedYear == todayYear && selectedMonth == todayMonth
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = cardColor,
                        shadowElevation = 1.dp,
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
                                    .size(34.dp)
                                    .testTag("budget_prev_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Month",
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
                                    .size(34.dp)
                                    .testTag("budget_next_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next Month",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isCurrentCalendarMonth) {
                            BudgetSkyBlue.copy(alpha = 0.14f)
                        } else {
                            cardColor
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isCurrentCalendarMonth) BudgetSkyBlue.copy(alpha = 0.35f) else cardBorderColor
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
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrentCalendarMonth) BudgetSkyBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // 2. Overall Monthly Budget Hero Bento Card
            item(key = "overall_budget_hero_card") {
                OverallBudgetHeroCard(
                    totalSpent = totalMonthSpent,
                    totalBudgetLimit = totalBudgetLimit,
                    daysRemaining = daysRemainingInMonth,
                    currencySymbol = currencySymbol,
                    language = language,
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

            // 2b. Native Compose Interactive Sunburst Chart (Category Spending vs Limit Distribution)
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
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 3. Section 1: Active Budgets (N)
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
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = AppStrings.t("tap_to_view", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            if (activeBudgetModels.isEmpty()) {
                item(key = "no_active_budgets_card") {
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
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BudgetSkyBlue.copy(alpha = 0.12f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = BudgetSkyBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
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
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = CircleShape,
                                color = BudgetSkyBlue.copy(alpha = 0.13f),
                                onClick = {
                                    viewModel.openAddBudget(
                                        budget = null,
                                        defaultMonthKey = selectedMonthKey
                                    )
                                }
                            ) {
                                Text(
                                    text = "+ ${AppStrings.t("set_budget", language)}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BudgetSkyBlue,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                items(activeBudgetModels, key = { "active_${it.uniqueKey}" }) { model ->
                    DashboardCategoryBudgetCard(
                        model = model,
                        currencySymbol = currencySymbol,
                        language = language,
                        cardColor = cardColor,
                        borderColor = cardBorderColor,
                        onCardClick = {
                            viewModel.navigateToActivityWithFilter(
                                category = model.categoryName,
                                subcategory = model.subcategoryName.ifBlank { null },
                                dateRange = selectedMonthDateRange,
                                dateRangeLabel = selectedMonthLabel
                            )
                        },
                        onEditBudget = {
                            viewModel.openAddBudget(
                                budget = model.budget,
                                defaultCategory = model.categoryName,
                                defaultMonthKey = selectedMonthKey
                            )
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
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
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${AppStrings.t("unbudgeted_categories", language)} ($unbudgetedCountText)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                }

                items(unbudgetedWithSpend, key = { "unbudgeted_${it.uniqueKey}" }) { model ->
                    DashboardCategoryBudgetCard(
                        model = model,
                        currencySymbol = currencySymbol,
                        language = language,
                        cardColor = cardColor,
                        borderColor = cardBorderColor,
                        onCardClick = {
                            viewModel.navigateToActivityWithFilter(
                                category = model.categoryName,
                                dateRange = selectedMonthDateRange,
                                dateRangeLabel = selectedMonthLabel
                            )
                        },
                        onEditBudget = {
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
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
                            DashboardCategoryBudgetCard(
                                model = model,
                                currencySymbol = currencySymbol,
                                language = language,
                                cardColor = cardColor,
                                borderColor = cardBorderColor,
                                onCardClick = {
                                    viewModel.openAddBudget(
                                        budget = null,
                                        defaultCategory = model.categoryName,
                                        defaultMonthKey = selectedMonthKey
                                    )
                                },
                                onEditBudget = {
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

            item(key = "budget_bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Fixed Scroll-Aware Frosted Header
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "budget_dashboard_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        } else {
                            Color.White.copy(alpha = 0.85f)
                        },
                        shadowElevation = if (isDark) 0.dp else 1.dp,
                        modifier = Modifier
                            .size(40.dp)
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

                    Column {
                        Text(
                            text = AppStrings.t("budget_dashboard", language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = AppStrings.t("monthly_spending_vs_limit", language),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                // Circular + Create / Add Budget button
                Surface(
                    shape = CircleShape,
                    color = BudgetSkyBlue.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, BudgetSkyBlue.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("add_budget_button"),
                    onClick = {
                        viewModel.openAddBudget(
                            budget = null,
                            defaultMonthKey = selectedMonthKey
                        )
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Budget",
                            tint = BudgetSkyBlue,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallBudgetHeroCard(
    totalSpent: Double,
    totalBudgetLimit: Double,
    daysRemaining: Int,
    currencySymbol: String,
    language: AppLanguage,
    cardColor: Color,
    borderColor: Color,
    onEditOverallBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val hasLimit = totalBudgetLimit > 0.0
    val rawPct = if (hasLimit) (totalSpent / totalBudgetLimit) * 100.0 else 0.0
    val pctInt = rawPct.roundToInt()
    val progress = if (hasLimit) (totalSpent / totalBudgetLimit).toFloat().coerceIn(0f, 1f) else 0f
    val remaining = totalBudgetLimit - totalSpent
    val dailySafeSpend = if (hasLimit && remaining > 0.0 && daysRemaining > 0) {
        remaining / daysRemaining.toDouble()
    } else {
        0.0
    }

    val statusColor = when {
        !hasLimit -> BudgetSkyBlue
        rawPct > 100.0 -> ExpenseRed
        rawPct > 80.0 -> BudgetAmber
        else -> IncomeGreen
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardColor,
        shadowElevation = 2.dp,
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
                        text = AppStrings.t("total_spent", language).uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                            letterSpacing = 0.9.sp,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyFormatter.format(totalSpent, currencySymbol, language = language),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (!hasLimit) {
                    Surface(
                        shape = CircleShape,
                        color = BudgetSkyBlue.copy(alpha = 0.14f),
                        onClick = onEditOverallBudget,
                        modifier = Modifier.testTag("set_overall_limit_pill")
                    ) {
                        Text(
                            text = AppStrings.t("set_overall_limit", language),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BudgetSkyBlue,
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
                            fontWeight = FontWeight.Bold,
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
                trackColor = statusColor.copy(alpha = 0.14f),
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
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
                                isOver -> ExpenseRed
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (hasLimit) {
                            "${CurrencyFormatter.format(dailySafeSpend, currencySymbol, language = language)} ${AppStrings.t("per_day", language)}"
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
                    AppStrings.t("monthly_budget", language)
                }
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontSize = 11.5.sp
                    )
                )

                Surface(
                    shape = CircleShape,
                    color = BudgetSkyBlue.copy(alpha = 0.12f),
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
                            tint = BudgetSkyBlue,
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
                            color = BudgetSkyBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCategoryBudgetCard(
    model: CategoryBudgetUiModel,
    currencySymbol: String,
    language: AppLanguage,
    cardColor: Color,
    borderColor: Color,
    onCardClick: () -> Unit,
    onEditBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val categoryAccent = remember(model.categoryColorHex) {
        try {
            Color(android.graphics.Color.parseColor(model.categoryColorHex))
        } catch (_: Exception) {
            BudgetSkyBlue
        }
    }

    val barColor = when {
        !model.hasDefinedBudget -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        model.percentage >= 100f -> ExpenseRed
        model.percentage >= 80f -> BudgetAmber
        else -> categoryAccent
    }
    val pctInt = model.percentage.roundToInt()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        shadowElevation = 1.5.dp,
        border = BorderStroke(1.dp, borderColor),
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_category_card_${model.uniqueKey}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = categoryAccent.copy(alpha = 0.14f),
                        modifier = Modifier.size(42.dp)
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (model.hasDefinedBudget) {
                        val pctStr = if (isBn) {
                            CurrencyFormatter.convertToBanglaDigits(pctInt.toString())
                        } else {
                            pctInt.toString()
                        }
                        Surface(
                            shape = CircleShape,
                            color = barColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "$pctStr%",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = barColor,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = AppStrings.t("no_limit", language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onEditBudget,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = {
                    if (model.hasDefinedBudget) {
                        (model.percentage / 100f).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                color = barColor,
                trackColor = if (model.hasDefinedBudget) {
                    barColor.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val spentFormatted = CurrencyFormatter.format(model.spent, currencySymbol, language = language)
                if (model.hasDefinedBudget) {
                    val limitFormatted = CurrencyFormatter.format(model.limit, currencySymbol, language = language)
                    Text(
                        text = if (isBn) {
                            "$spentFormatted ব্যয় / সীমা $limitFormatted"
                        } else {
                            "$spentFormatted spent of $limitFormatted"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                            fontSize = 12.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val isOver = model.remaining < 0.0
                    val diffFormatted = CurrencyFormatter.format(
                        if (isOver) -model.remaining else model.remaining,
                        currencySymbol,
                        language = language
                    )
                    Text(
                        text = if (isOver) {
                            "$diffFormatted ${AppStrings.t("over_suffix", language)}"
                        } else {
                            "$diffFormatted ${AppStrings.t("left_suffix", language)}"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = if (isOver) ExpenseRed else IncomeGreen
                        ),
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "$spentFormatted ${AppStrings.t("spent_of", language).substringBefore(",")} • ${AppStrings.t("no_limit", language)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                            fontSize = 12.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = AppStrings.t("set_limit", language),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = BudgetSkyBlue
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEditBudget() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
