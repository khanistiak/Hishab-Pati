package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.CategoryIcon
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailsScreen(
    model: CategoryBudgetUiModel,
    monthName: String,
    selectedYear: Int,
    selectedMonth: Int,
    currencySymbol: String,
    daysRemaining: Int,
    daysElapsed: Int,
    totalDaysInMonth: Int,
    categoryTransactions: List<TransactionEntity>,
    allTransactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onEditBudget: () -> Unit,
    onDeleteBudget: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState()
) {
    BackHandler(onBack = onBack)

    var isFavorite by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var isSpendingHistoryExpanded by remember { mutableStateOf(true) }
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionEntity?>(null) }


    val monthRangeText = remember(selectedYear, selectedMonth, totalDaysInMonth) {
        val calStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val calEnd = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, totalDaysInMonth)
        }
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
        "${sdf.format(calStart.time)} - ${sdf.format(calEnd.time)}"
    }

    val projectedTotal = remember(model.spent, daysElapsed, totalDaysInMonth) {
        if (daysElapsed > 0) {
            (model.spent / daysElapsed.toDouble()) * totalDaysInMonth.toDouble()
        } else {
            model.spent
        }
    }

    val progressFraction = if (model.hasDefinedBudget && model.limit > 0) {
        (model.spent / model.limit).toFloat().coerceIn(0f, 1f)
    } else 0f

    val statusLabel = when {
        !model.hasDefinedBudget -> "No Limit"
        model.isOverBudget -> "Overspent"
        model.isNearLimit -> "Near Limit"
        else -> "On Track"
    }

    val statusColor = when {
        !model.hasDefinedBudget -> Color(0xFF64748B)
        model.isOverBudget -> Color(0xFFE11D48)
        model.isNearLimit -> Color(0xFFD97706)
        else -> Color(0xFF059669)
    }

    val statusBg = when {
        !model.hasDefinedBudget -> Color(0xFFF1F5F9)
        model.isOverBudget -> Color(0xFFFFE4E6)
        model.isNearLimit -> Color(0xFFFEF3C7)
        else -> Color(0xFFD1FAE5)
    }

    val overspentPct = if (model.limit > 0) {
        ((model.spent - model.limit) / model.limit * 100.0)
    } else 100.0

    val usedPct = if (model.limit > 0) {
        ((model.spent / model.limit) * 100.0)
    } else 0.0

    // Group transactions by date
    val groupedTransactions = remember(categoryTransactions) {
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        categoryTransactions
            .sortedByDescending { it.timestamp }
            .groupBy { tx -> sdf.format(tx.timestamp) }
    }

    // Subcategory spending breakdown for Spending History
    val subcategoryBreakdown = remember(categoryTransactions) {
        categoryTransactions
            .groupBy { it.subcategory.ifBlank { "Uncategorized" } }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Past months spending for this category
    val pastMonthsSpending = remember(allTransactions, model.categoryName, selectedYear, selectedMonth) {
        val list = mutableListOf<Pair<String, Double>>()
        for (i in 1..2) {
            val pastCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -i)
            }
            val pYear = pastCal.get(Calendar.YEAR)
            val pMonth = pastCal.get(Calendar.MONTH)
            val maxD = pastCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val startStr = sdf.format(pastCal.time)
            pastCal.set(Calendar.DAY_OF_MONTH, maxD)
            val endStr = sdf.format(pastCal.time)
            val range = "$startStr - $endStr"

            val spent = allTransactions.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                val matchMonth = txCal.get(Calendar.YEAR) == pYear && txCal.get(Calendar.MONTH) == pMonth
                val matchSubcat = if (model.subcategoryName.isNotBlank()) {
                    tx.subcategory.equals(model.subcategoryName, ignoreCase = true)
                } else true
                matchMonth &&
                        tx.category == model.categoryName &&
                        matchSubcat &&
                        com.example.data.backup.FinancialIntegrityValidator.isEligibleExpense(tx)
            }.sumOf { it.amount }

            if (spent > 0.0) {
                list.add(Pair(range, spent))
            }
        }
        list
    }

    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 4.dp,
        bottomNavClearance = 32.dp
    )

    Box(
        modifier = modifier
            .fillMaxSize()
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
            contentPadding = scrollLayout.contentPadding
        ) {
            // Main Budget Card
            item(key = "budget_details_summary_card") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .testTag("budget_details_summary_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header Row: Icon, Title & Budget Subtitle, Automatic Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIcon(
                                iconName = model.categoryIcon,
                                colorHex = model.categoryColorHex,
                                size = 48.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.categoryName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (model.hasDefinedBudget) {
                                        "Budget: ${CurrencyFormatter.format(model.limit, currencySymbol)}"
                                    } else {
                                        "No limit active"
                                    },
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Automatic",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Spending Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (model.isOverBudget && model.hasDefinedBudget) {
                                    "${CurrencyFormatter.format(kotlin.math.abs(model.remaining), currencySymbol)} overspent"
                                } else {
                                    "${CurrencyFormatter.format(model.remaining, currencySymbol)} remaining"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (model.isOverBudget && model.hasDefinedBudget) Color(0xFFE11D48) else Color(0xFF0F172A)
                            )

                            Text(
                                text = if (model.isOverBudget && model.hasDefinedBudget) {
                                    "-${String.format(Locale.US, "%.2f", overspentPct)}%"
                                } else {
                                    "${String.format(Locale.US, "%.2f", usedPct)}%"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (model.isOverBudget && model.hasDefinedBudget) Color(0xFFE11D48) else statusColor
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Schedule Metadata Rows
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = monthRangeText,
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$daysRemaining day${if (daysRemaining > 1) "s" else ""} left",
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Every month",
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Two Bottom Tiles: Projected Total & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left Tile: Projected Total
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = CurrencyFormatter.format(projectedTotal, currencySymbol),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Projected Total",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Right Tile: Status
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = statusBg,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = statusColor
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Status",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Collapsible Spending History Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSpendingHistoryExpanded = !isSpendingHistoryExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Spending History",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Icon(
                                imageVector = if (isSpendingHistoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isSpendingHistoryExpanded) "Collapse" else "Expand",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
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
                                    .padding(top = 16.dp)
                            ) {
                                // Category Total Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryIcon(
                                            iconName = model.categoryIcon,
                                            colorHex = model.categoryColorHex,
                                            size = 22.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = model.categoryName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    Text(
                                        text = CurrencyFormatter.format(model.spent, currencySymbol),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Subcategory rows
                                if (subcategoryBreakdown.isEmpty()) {
                                    Text(
                                        text = "No recorded spending yet",
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(start = 30.dp, bottom = 4.dp)
                                    )
                                } else {
                                    subcategoryBreakdown.forEach { (subcatName, subcatAmount) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$subcatName:",
                                                fontSize = 14.sp,
                                                color = Color(0xFF475569),
                                                modifier = Modifier.padding(start = 30.dp)
                                            )
                                            Text(
                                                text = CurrencyFormatter.format(subcatAmount, currencySymbol),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                }

                                // Historical Past Months
                                if (pastMonthsSpending.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color(0xFFF1F5F9))
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Previous Months",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    pastMonthsSpending.forEach { (range, amount) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = range,
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            Text(
                                                text = "${CurrencyFormatter.format(amount, currencySymbol)} spent",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Transactions Section Header
            item {
                Text(
                    text = "Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            // Empty transactions state
            if (categoryTransactions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No transactions for ${model.categoryName} in $monthName",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else {
                // Grouped by Date
                groupedTransactions.forEach { (dateStr, txList) ->
                    val daySum = txList.sumOf { it.amount }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = dateStr,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (txList.size > 1) {
                                Text(
                                    text = "Δ -${CurrencyFormatter.format(daySum, currencySymbol)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    items(txList, key = { it.id }) { tx ->
                        val account = remember(tx.accountId, accounts) {
                            accounts.firstOrNull { it.id == tx.accountId }
                        }
                        val accountName = account?.name ?: "Wallet Cash"

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 0.5.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .clickable { selectedTransactionForDetails = tx }
                                .testTag("transaction_item_${tx.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIcon(
                                    iconName = model.categoryIcon,
                                    colorHex = model.categoryColorHex,
                                    size = 42.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1
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
                                        color = Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "- ${CurrencyFormatter.format(tx.amount, currencySymbol)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = accountName,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fixed scroll-aware frosted header matching Home and Plan tabs
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "budget_details_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack)
                            .testTag("budget_details_back_button")
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

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Budget Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("budget_details_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(40.dp)
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
                                text = { Text("Edit Budget") },
                                onClick = {
                                    showMenu = false
                                    onEditBudget()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            if (model.hasDefinedBudget) {
                                DropdownMenuItem(
                                    text = { Text("Delete Budget", color = ExpenseRed) },
                                    onClick = {
                                        showMenu = false
                                        onDeleteBudget()
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

    // Modal Bottom Sheet for Transaction Details
    selectedTransactionForDetails?.let { tx ->
        TransactionDetailsSheet(
            transaction = tx,
            budgetName = model.categoryName,
            categoryIcon = model.categoryIcon,
            categoryColorHex = model.categoryColorHex,
            currencySymbol = currencySymbol,
            accounts = accounts,
            onDismiss = { selectedTransactionForDetails = null },
            onEditTransaction = { targetTx ->
                selectedTransactionForDetails = null
                onEditTransaction(targetTx)
            }
        )
    }
}
