package com.example.ui.screens.plan

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.getIconForName
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.navigation.rememberScreenLazyListState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

@Composable
fun BudgetDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currencySymbol ?: "৳"

    val currentMonthKey = remember {
        SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())
    }

    var selectedFilter by remember { mutableStateOf(BudgetFilterOption.ALL) }
    val listState = rememberScreenLazyListState(viewModel = viewModel)
    val scrollLayout = rememberScrollAwareGlassLayout(listState = listState)

    val monthExpenses = remember(transactions, currentMonthKey) {
        val fmt = SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
        transactions.filter {
            it.type == TransactionType.EXPENSE && fmt.format(Date(it.timestamp)) == currentMonthKey
        }
    }

    val models = remember(budgets, categories, monthExpenses, selectedFilter) {
        val catMap = categories.associateBy { it.name.lowercase(Locale.ENGLISH) }
        val built = budgets.map { b ->
            val cat = catMap[b.categoryName.lowercase(Locale.ENGLISH)]
            val matchingTxs = monthExpenses.filter { tx ->
                tx.category.equals(b.categoryName, ignoreCase = true) &&
                    (b.subcategoryName.isBlank() || tx.subcategory.equals(b.subcategoryName, ignoreCase = true))
            }
            val spent = matchingTxs.sumOf { it.amount }
            val limit = b.monthlyLimit.coerceAtLeast(0.0)
            val remaining = limit - spent
            val pct = if (limit > 0.0) ((spent / limit) * 100.0).toFloat() else 0f
            val disp = if (b.subcategoryName.isNotBlank()) "${b.categoryName} • ${b.subcategoryName}" else b.categoryName
            CategoryBudgetUiModel(
                budget = b,
                categoryName = b.categoryName,
                subcategoryName = b.subcategoryName,
                displayName = disp,
                categoryIcon = b.iconName.ifBlank { cat?.iconName ?: "Category" },
                categoryColorHex = b.colorHex.ifBlank { cat?.colorHex ?: "#0EA5E9" },
                limit = limit,
                spent = spent,
                remaining = remaining,
                percentage = pct,
                transactionCount = matchingTxs.size,
                recentTransactions = matchingTxs.take(5),
                isOverBudget = limit > 0.0 && spent > limit,
                isNearLimit = limit > 0.0 && pct >= b.alertPercentage.toFloat() && spent <= limit,
                hasDefinedBudget = limit > 0.0,
                isSubcategory = b.subcategoryName.isNotBlank()
            )
        }
        when (selectedFilter) {
            BudgetFilterOption.ALL -> built
            BudgetFilterOption.ON_TRACK -> built.filter { !it.isOverBudget && !it.isNearLimit && it.hasDefinedBudget }
            BudgetFilterOption.NEAR_LIMIT -> built.filter { it.isNearLimit }
            BudgetFilterOption.EXCEEDED -> built.filter { it.isOverBudget }
            BudgetFilterOption.NO_LIMIT -> built.filter { !it.hasDefinedBudget }
        }
    }

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
                    backdropState = scrollLayout.backdropState,
                    lazyListState = listState,
                    contentLayer = scrollLayout.contentGraphicsLayer
                )
                .padding(horizontal = 16.dp)
        ) {
            item {
                Button(
                    onClick = {
                        viewModel.budgetToEdit.value = null
                        viewModel.openSubScreen(SubScreen.ADD_BUDGET)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_budget_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Budget", fontWeight = FontWeight.SemiBold)
                }
            }

            items(models, key = { it.uniqueKey }) { model ->
                DashboardCategoryBudgetCard(
                    model = model,
                    currencySymbol = currencySymbol,
                    onCardClick = {
                        viewModel.budgetToEdit.value = model.budget
                        viewModel.openSubScreen(SubScreen.ADD_BUDGET)
                    },
                    onEditBudget = {
                        viewModel.budgetToEdit.value = model.budget
                        viewModel.openSubScreen(SubScreen.ADD_BUDGET)
                    }
                )
            }
        }

        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "budget_dashboard_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("budget_dashboard_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Budgets",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = {
                        viewModel.budgetToEdit.value = null
                        viewModel.openSubScreen(SubScreen.ADD_BUDGET)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Budget",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardCategoryBudgetCard(
    model: CategoryBudgetUiModel,
    currencySymbol: String,
    onCardClick: () -> Unit,
    onEditBudget: () -> Unit
) {
    val accentColor = remember(model.categoryColorHex) {
        try {
            Color(android.graphics.Color.parseColor(model.categoryColorHex))
        } catch (_: Exception) {
            Color(0xFF0EA5E9)
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForName(model.categoryIcon),
                            contentDescription = model.displayName,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = model.displayName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%.0f", model.spent)} / $currencySymbol${String.format(Locale.US, "%.0f", model.limit)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onEditBudget) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Budget",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (model.percentage / 100f).coerceIn(0f, 1f) },
                color = if (model.isOverBudget) Color(0xFFEF4444) else accentColor,
                trackColor = accentColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}
