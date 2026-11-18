package com.example.ui.screens.plan

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.FinancialReportAnalytics
import java.util.Calendar

@Composable
fun MonthlyRecapScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()

    val now = remember { Calendar.getInstance() }
    var currentYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH)) } // 0..11

    val recap = remember(transactions, budgets, currentYear, currentMonth, currencySymbol) {
        FinancialReportAnalytics.computeMonthlyRecap(
            transactions = transactions,
            budgets = budgets,
            year = currentYear,
            month = currentMonth,
            currencySymbol = currencySymbol
        )
    }

    val monthName = FinancialReportAnalytics.getMonthName(currentMonth, full = true)

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.MONTHLY_RECAP
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 66.dp,
        extraTopGap = 8.dp,
        bottomNavClearance = 30.dp
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
                .testTag("monthly_recap_lazy_column"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = scrollLayout.contentPadding.calculateTopPadding(),
                bottom = scrollLayout.contentPadding.calculateBottomPadding()
            )
        ) {
            // Month Stepper Pill
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        if (currentMonth == 0) {
                                            currentMonth = 11
                                            currentYear--
                                        } else {
                                            currentMonth--
                                        }
                                    }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "$monthName $currentYear",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        if (currentMonth == 11) {
                                            currentMonth = 0
                                            currentYear++
                                        } else {
                                            currentMonth++
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            // Financial Health Grade Card
            item {
                TranslucentCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val gradeBadgeColors = when (recap.grade) {
                            "A" -> listOf(Color(0xFF10B981), Color(0xFF059669))
                            "B" -> listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                            "C" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                            "D" -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                            else -> listOf(Color(0xFF94A3B8), Color(0xFF64748B))
                        }

                        // Big Circle Grade Badge
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(gradeBadgeColors)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = recap.grade,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = recap.healthTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = recap.healthDescription,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Sub-grades: Income, Expense, Savings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            SubGradePill(
                                label = AppStrings.t("income", language),
                                grade = recap.incomeGrade,
                                gradeColor = if (recap.incomeGrade == "A") IncomeGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            SubGradePill(
                                label = AppStrings.t("expenses", language),
                                grade = recap.expenseGrade,
                                gradeColor = when (recap.expenseGrade) {
                                    "A" -> IncomeGreen
                                    "B" -> Color(0xFFF59E0B)
                                    "C" -> ExpenseRed
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                            SubGradePill(
                                label = AppStrings.t("savings_goals", language),
                                grade = recap.savingsGrade,
                                gradeColor = when (recap.savingsGrade) {
                                    "A" -> IncomeGreen
                                    "D" -> ExpenseRed
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Savings Rate bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Savings rate", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                text = "${recap.savingsRate}% (${CurrencyFormatter.format(recap.netSavings, currencySymbol, language = language)})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (recap.netSavings >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (recap.savingsRate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (recap.netSavings >= 0) IncomeGreen else ExpenseRed,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // Key Metrics 2x2 Grid
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (language == AppLanguage.BANGLA) "প্রধান মেট্রিক্স" else "Key metrics",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        modifier = Modifier.weight(1f),
                        title = "Transactions",
                        value = "${recap.transactionCount}",
                        subtitle = if (recap.hasData) "Logged entries" else "No records"
                    )
                    MetricBox(
                        modifier = Modifier.weight(1f),
                        title = "Avg size",
                        value = CurrencyFormatter.format(recap.avgExpenseSize, currencySymbol, language = language),
                        subtitle = if (recap.expenseCount > 0) "Per expense" else "No expense"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        modifier = Modifier.weight(1f),
                        title = "Daily average",
                        value = CurrencyFormatter.format(recap.dailyAverage, currencySymbol, language = language),
                        subtitle = if (recap.totalExpense > 0) "Average per day" else "No expense"
                    )
                    MetricBox(
                        modifier = Modifier.weight(1f),
                        title = "Budget OK",
                        value = recap.budgetOkText,
                        subtitle = recap.budgetOkSubtitle
                    )
                }
            }

            // Top Category & Top Merchant
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Category
                    TranslucentCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE0E7FF),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Top Category", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = recap.topCategory ?: if (language == AppLanguage.BANGLA) "কোনোটি নেই" else "None",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = CurrencyFormatter.format(recap.topCategoryAmount, currencySymbol, language = language),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }

                    // Top Merchant
                    TranslucentCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Top Merchant", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = recap.topMerchant ?: if (language == AppLanguage.BANGLA) "কোনোটি নেই" else "None",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = CurrencyFormatter.format(recap.topMerchantAmount, currencySymbol, language = language),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }

            // Insights List
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BANGLA) "মাসিক পর্যবেক্ষণ ও পরামর্শ" else "Monthly Insights",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(recap.insights) { insight ->
                InsightBulletCard(
                    title = insight.title,
                    description = insight.description
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "monthly_recap_fixed_header"
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
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = AppStrings.t("monthly_recap", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }
    }
}

@Composable
private fun SubGradePill(label: String, grade: String, gradeColor: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.width(96.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = grade, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = gradeColor)
        }
    }
}

@Composable
private fun MetricBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String
) {
    TranslucentCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 11.sp, color = IncomeGreen, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun InsightBulletCard(title: String, description: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Text("✨", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), lineHeight = 16.sp)
            }
        }
    }
}
