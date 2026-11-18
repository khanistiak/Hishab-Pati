package com.example.ui.screens.plan

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.model.AppLanguage
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.TranslucentCard
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter

import com.example.ui.navigation.rememberScreenLazyListState

data class ToolItemData(
    val title: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTint: Color,
    val key: String
)

@Composable
fun PlanScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState = viewModel.rememberScreenLazyListState(tabOverride = com.example.ui.MainTab.PLAN)
) {
    val debts by viewModel.debts.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val currencySymbol = settings?.currencySymbol ?: "৳"
    val language = settings?.language ?: AppLanguage.ENGLISH

    // Exactly 6 tools as specified in prompt
    val planTools = listOf(
        ToolItemData(AppStrings.t("converter", language), Icons.Default.CurrencyExchange, Color(0xFFE0E7FF), Color(0xFF4F46E5), "converter"),
        ToolItemData(AppStrings.t("networth", language), Icons.Default.TrendingUp, Color(0xFFDCFCE7), Color(0xFF16A34A), "networth"),
        ToolItemData(AppStrings.t("monthly_recap", language), Icons.Default.Timeline, Color(0xFFEDE9FE), Color(0xFF7C3AED), "recap"),
        ToolItemData(AppStrings.t("spending_comparison", language), Icons.Default.TrendingDown, Color(0xFFFEF3C7), Color(0xFFD97706), "comparison"),
        ToolItemData(AppStrings.t("income_tracker", language), Icons.Default.Payments, Color(0xFFD1FAE5), Color(0xFF059669), "income"),
        ToolItemData(AppStrings.t("calendar", language), Icons.Default.CalendarMonth, Color(0xFFF3E8FF), Color(0xFF9333EA), "calendar")
    )

    // Precompute budget & spending totals for smooth scrolling with double-counting prevention
    val (totalBudget, totalSpent) = remember(budgets, transactions) {
        val (refYear, refMonth) = com.example.data.backup.FinancialIntegrityValidator.resolveReferenceYearMonth(transactions, budgets)
        val monthKey = String.format(java.util.Locale.US, "%04d-%02d", refYear, refMonth + 1)
        val budget = com.example.data.backup.FinancialIntegrityValidator.computeTotalMonthlyBudget(budgets, monthKey)
        val spent = transactions.filter { tx ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            cal.get(java.util.Calendar.YEAR) == refYear &&
                cal.get(java.util.Calendar.MONTH) == refMonth &&
                com.example.data.backup.FinancialIntegrityValidator.isEligibleExpense(tx)
        }.sumOf { it.amount }
        Pair(budget, spent)
    }

    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 4.dp
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
                .testTag("plan_lazy_column"),
            contentPadding = scrollLayout.contentPadding
        ) {
            // 1. SAVINGS GOALS (1st option per user request)
        item(key = "savings_goals_header") {
            SectionHeader(
                title = AppStrings.t("savings_goals", language),
                actionText = if (savingsGoals.isNotEmpty()) AppStrings.t("add_goal", language) else null,
                onActionClick = { viewModel.openNewSavingsGoal() }
            )
        }

        if (savingsGoals.isEmpty()) {
            item(key = "savings_goals_empty") {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    onClick = { viewModel.openNewSavingsGoal() }
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEDE9FE),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = "Piggy Bank",
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (language == AppLanguage.BANGLA) "গুরুত্বপূর্ণ কিছুর জন্য সঞ্চয় করুন" else "Save for something that matters",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (language == AppLanguage.BANGLA) "একটি লক্ষ্য নির্ধারণ করুন এবং হিসাব পাতি আপনাকে প্রতি মাসে কত জমা করতে হবে তা প্রদর্শন করবে।" else "Set a goal and Hishab Pati shows how much to put aside each month.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (language == AppLanguage.BANGLA) "নতুন লক্ষ্য তৈরি করুন" else "Create a goal",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
                        )
                    }
                }
            }
        } else {
            items(savingsGoals, key = { it.id }) { goal ->
                SavingsGoalCard(
                    goal = goal,
                    currencySymbol = currencySymbol,
                    language = language,
                    onAddContribution = { viewModel.addSavingsContribution(goal.id, 500.0) }
                )
            }
        }

        // 2. LOANS & DEBT (2nd option per user request, replace "Payoff plan" with "Explore all")
        item(key = "loans_debt_header") {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(
                title = AppStrings.t("loans_debt", language),
                actionText = AppStrings.t("explore_all", language),
                onActionClick = { viewModel.openManageDebts() }
            )
        }

        if (debts.isEmpty()) {
            item(key = "debts_empty") {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    onClick = { viewModel.openManageDebts() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "কোন সক্রিয় ঋণ বা দেনা নেই। সব দেখতে ট্যাপ করুন।" else "No active loans or debts. Tap to explore all.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        } else {
            items(debts.take(4), key = { it.id }) { debt ->
                PlanDebtSummaryCard(
                    debt = debt,
                    currencySymbol = currencySymbol,
                    language = language,
                    onClick = { viewModel.openDebtDetails(debt) }
                )
            }
        }

        // 3. MONTHLY BUDGETS
        item(key = "monthly_budgets_header") {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(
                title = AppStrings.t("monthly_budget", language),
                actionText = if (language == AppLanguage.BANGLA) "সম্পূর্ণ ড্যাশবোর্ড" else "Full Dashboard",
                onActionClick = { viewModel.openBudgetDashboard() }
            )
        }

        item(key = "monthly_budgets_card") {
            val hasBudget = totalBudget > 0.0
            val overallPct = if (hasBudget) ((totalSpent / totalBudget) * 100).toInt() else 0
            val overallProgress = if (hasBudget) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
            val isOver = hasBudget && totalSpent > totalBudget

            TranslucentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                onClick = { viewModel.openBudgetDashboard() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "মাসিক ব্যয় ও বাজেট" else "Monthly Spending & Budgets",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = (if (isOver) ExpenseRed else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
                        ) {
                            val badgeText = if (!hasBudget) {
                                if (language == AppLanguage.BANGLA) "বাজেট সেট করুন" else "Set Budget"
                            } else if (isOver) {
                                if (language == AppLanguage.BANGLA) "অতিক্রম" else "Exceeded"
                            } else {
                                if (language == AppLanguage.BANGLA) "${CurrencyFormatter.convertToBanglaDigits(overallPct.toString())}% ব্যয়িত" else "$overallPct% Spent"
                            }
                            Text(
                                text = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!hasBudget) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "এই মাসে ব্যয়: ${CurrencyFormatter.format(totalSpent, currencySymbol)}" else "${CurrencyFormatter.format(totalSpent, currencySymbol)} spent this month",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "কোন মাসিক বাজেট নির্ধারিত নেই। সীমা নির্ধারণ করতে ট্যাপ করুন।" else "No monthly budget configured yet. Tap to set limits.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == AppLanguage.BANGLA) "ব্যয়: ${CurrencyFormatter.format(totalSpent, currencySymbol)}" else "${CurrencyFormatter.format(totalSpent, currencySymbol)} spent",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == AppLanguage.BANGLA) "সীমা: ${CurrencyFormatter.format(totalBudget, currencySymbol)}" else "Limit: ${CurrencyFormatter.format(totalBudget, currencySymbol)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(3.5.dp)),
                            color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // 4. TOOLS GRID (Exactly 6 tools, middle center position per user request)
        item(key = "tools_header") {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(title = AppStrings.t("tools", language), actionText = null, onActionClick = {})
        }

        item(key = "tools_grid") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val chunkedTools = planTools.chunked(3)
                chunkedTools.forEach { rowTools ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTools.forEach { tool ->
                            TranslucentCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(106.dp)
                                    .testTag("tool_${tool.title.lowercase().replace(' ', '_')}"),
                                onClick = {
                                    when (tool.key) {
                                        "converter" -> viewModel.openCurrencyConverter()
                                        "networth" -> viewModel.openSubScreen(SubScreen.ACCOUNTS_MANAGEMENT)
                                        "recap" -> viewModel.openMonthlyRecap()
                                        "comparison" -> viewModel.openSpendingComparison()
                                        "income" -> viewModel.openIncomeTracker()
                                        "calendar" -> viewModel.openCalendar()
                                    }
                                }
                            ) {
                                // Middle center position for icon and name
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = tool.iconBgColor,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = tool.icon,
                                                contentDescription = tool.title,
                                                tint = tool.iconTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = tool.title,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        ),
                                        maxLines = 2,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // Fixed scroll-aware frosted header at top of Plan tab
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "plan_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.t("plan_title", language),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = if (language == AppLanguage.BANGLA) "সঞ্চয়, বাজেট ও ঋণ পরিকল্পনা" else "Savings, budgets & debt planning",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(40.dp),
                    onClick = { viewModel.openNewSavingsGoal() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Plan",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsGoalCard(
    goal: SavingsGoalEntity,
    currencySymbol: String,
    language: AppLanguage = CurrencyFormatter.currentLanguage,
    onAddContribution: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val pct = (progress * 100).toInt()
    val goalColor = try {
        Color(android.graphics.Color.parseColor(goal.themeColorHex))
    } catch (e: Exception) {
        Color(0xFFF59E0B)
    }

    TranslucentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        onClick = onAddContribution
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = goalColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = resolveGoalIcon(goal.iconName),
                            contentDescription = goal.name,
                            tint = goalColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    if (goal.targetDeadlineStr.isNotBlank()) {
                        val dueText = if (language == AppLanguage.BANGLA) "মেয়াদ: ${DateFormatter.localizeText(goal.targetDeadlineStr, language)}" else "Due: ${goal.targetDeadlineStr}"
                        Text(
                            text = dueText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val pctText = if (language == AppLanguage.BANGLA) CurrencyFormatter.convertToBanglaDigits("$pct%") else "$pct%"
                    Text(
                        text = pctText,
                        fontWeight = FontWeight.Bold,
                        color = goalColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${CurrencyFormatter.format(goal.savedAmount, currencySymbol)} / ${CurrencyFormatter.format(goal.targetAmount, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = goalColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun PlanDebtSummaryCard(
    debt: DebtEntity,
    currencySymbol: String,
    language: AppLanguage = CurrencyFormatter.currentLanguage,
    onClick: () -> Unit
) {
    val isPayable = debt.type == DebtType.BORROWED
    val initial = debt.personName.trim().take(1).uppercase(java.util.Locale.US).ifEmpty { "?" }

    TranslucentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isPayable) Color(0xFFDBEAFE) else Color(0xFFDCFCE7),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPayable) Icons.Default.AccountBalance else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isPayable) Color(0xFF2563EB) else Color(0xFF16A34A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                val personTitle = if (isPayable) debt.personName else {
                    if (language == AppLanguage.BANGLA) "${debt.personName} আপনাকে দেবে" else "${debt.personName} owes you"
                }
                Text(
                    text = personTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = if (debt.reason.isNotBlank()) debt.reason else if (debt.notes.isNotBlank()) debt.notes else if (isPayable) "EMI ${currencySymbol}0" else debt.personName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (debt.amount > 0) CurrencyFormatter.format(debt.amount, currencySymbol, showSign = !isPayable) else "${currencySymbol}0",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isPayable) MaterialTheme.colorScheme.onBackground else IncomeGreen
                    )
                )
                if (debt.tentativeCloseDateStr.isNotBlank()) {
                    val dueText = if (language == AppLanguage.BANGLA) "মেয়াদ: ${DateFormatter.localizeText(debt.tentativeCloseDateStr, language)}" else "Due: ${debt.tentativeCloseDateStr}"
                    Text(
                        text = dueText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}
