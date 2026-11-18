package com.example.ui.screens.plan

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.util.AppStrings
import com.example.ui.util.ComparisonCategoryItem
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.FinancialReportAnalytics
import java.util.Calendar
import java.util.Locale

@Composable
fun SpendingComparisonScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val transactions by viewModel.transactions.collectAsState()

    val now = remember { Calendar.getInstance() }
    var currentYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH)) } // 0..11

    val summary = remember(transactions, currentYear, currentMonth) {
        FinancialReportAnalytics.computeSpendingComparison(
            transactions = transactions,
            currentYear = currentYear,
            currentMonth = currentMonth
        )
    }

    val curMonthName = FinancialReportAnalytics.getMonthName(currentMonth)
    val prevMonthName = FinancialReportAnalytics.getMonthName(summary.prevMonth)

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.SPENDING_COMPARISON
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
                .testTag("spending_comparison_lazy_column"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = scrollLayout.contentPadding.calculateTopPadding(),
                bottom = scrollLayout.contentPadding.calculateBottomPadding()
            )
        ) {
            // Month vs Month Selector Header Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    if (currentMonth == 0) {
                                        currentMonth = 11
                                        currentYear--
                                    } else {
                                        currentMonth--
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Previous",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$curMonthName $currentYear",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF2563EB) // Blue
                            )
                            Text(
                                text = "vs",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "$prevMonthName ${summary.prevYear}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFB45309) // Amber/Brown
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    if (currentMonth == 11) {
                                        currentMonth = 0
                                        currentYear++
                                    } else {
                                        currentMonth++
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Overview Card: Expense and Income comparison
            item {
                Spacer(modifier = Modifier.height(10.dp))
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Expense Overview
                        Text(
                            text = "Expense overview",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("$curMonthName $currentYear", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text(
                                    CurrencyFormatter.format(summary.currentMonthExpense, currencySymbol, language = language),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$prevMonthName ${summary.prevYear}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text(
                                    CurrencyFormatter.format(summary.previousMonthExpense, currencySymbol, language = language),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Expense delta pill
                        val hasExpenseData = summary.currentMonthExpense > 0 || summary.previousMonthExpense > 0
                        val isExpenseSaved = summary.expenseDiff <= 0
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!hasExpenseData) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else if (isExpenseSaved) Color(0xFFDCFCE7) else Color(0xFFFFE4E6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val expenseDeltaText = if (!hasExpenseData) "0.0%"
                            else if (isExpenseSaved) "↓ ${CurrencyFormatter.format(kotlin.math.abs(summary.expenseDiff), currencySymbol, language = language)} (${String.format(Locale.US, "%.1f", kotlin.math.abs(summary.expenseDeltaPct))}%)"
                            else "↑ ${CurrencyFormatter.format(summary.expenseDiff, currencySymbol, language = language)} (${String.format(Locale.US, "%.1f", summary.expenseDeltaPct)}%)"

                            Text(
                                text = expenseDeltaText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!hasExpenseData) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                else if (isExpenseSaved) Color(0xFF15803D) else Color(0xFFBE123C),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Income Overview
                        Text(
                            text = "Income overview",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("$curMonthName $currentYear", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text(
                                    CurrencyFormatter.format(summary.currentMonthIncome, currencySymbol, language = language),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$prevMonthName ${summary.prevYear}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text(
                                    CurrencyFormatter.format(summary.previousMonthIncome, currencySymbol, language = language),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Income delta pill
                        val hasIncomeData = summary.currentMonthIncome > 0 || summary.previousMonthIncome > 0
                        val isIncomeHigher = summary.incomeDiff >= 0
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!hasIncomeData) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else if (isIncomeHigher) Color(0xFFDCFCE7) else Color(0xFFFFE4E6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val incomeDeltaText = if (!hasIncomeData) "0.0%"
                            else if (isIncomeHigher) "↑ ${CurrencyFormatter.format(summary.incomeDiff, currencySymbol, language = language)} (${String.format(Locale.US, "%.1f", summary.incomeDeltaPct)}%)"
                            else "↓ ${CurrencyFormatter.format(kotlin.math.abs(summary.incomeDiff), currencySymbol, language = language)} (${String.format(Locale.US, "%.1f", kotlin.math.abs(summary.incomeDeltaPct))}%)"

                            Text(
                                text = incomeDeltaText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!hasIncomeData) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                else if (isIncomeHigher) Color(0xFF15803D) else Color(0xFFBE123C),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Biggest increase and Biggest decrease cards
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Biggest Increase
                    TranslucentCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Biggest increase",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDC2626) // Red label for increased expense
                                )
                                Text(
                                    text = summary.biggestIncrease?.name ?: if (language == AppLanguage.BANGLA) "কোনোটি নেই" else "None",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                val incDiff = (summary.biggestIncrease?.currentMonthAmount ?: 0.0) - (summary.biggestIncrease?.previousMonthAmount ?: 0.0)
                                Text(
                                    text = CurrencyFormatter.format(incDiff, currencySymbol, language = language),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.biggestIncrease != null) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = summary.biggestIncrease?.let { "+${String.format(Locale.US, "%.1f", it.percentageChange)}%" } ?: "–",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    // Biggest Decrease
                    TranslucentCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFE4E6),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = Color(0xFFE11D48),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Biggest decrease",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF16A34A) // Green label for reduced expense
                                )
                                Text(
                                    text = summary.biggestDecrease?.name ?: if (language == AppLanguage.BANGLA) "কোনোটি নেই" else "None",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                val decDiff = (summary.biggestDecrease?.previousMonthAmount ?: 0.0) - (summary.biggestDecrease?.currentMonthAmount ?: 0.0)
                                Text(
                                    text = CurrencyFormatter.format(decDiff, currencySymbol, language = language),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.biggestDecrease != null) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = summary.biggestDecrease?.let { "${String.format(Locale.US, "%.1f", it.percentageChange)}%" } ?: "–",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Insight Card
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9).copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = summary.insightText,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Category Breakdown Section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "▲ Category breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2563EB)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("$curMonthName $currentYear", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFB45309)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("$prevMonthName ${summary.prevYear}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (summary.categories.isEmpty()) {
                item {
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (language == AppLanguage.BANGLA) "তুলনা করার মতো কোনো খরচ নেই" else "No category spending data to compare",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == AppLanguage.BANGLA) "লেনদেন যুক্ত করুন যাতে খরচের পরিবর্তন ট্র্যাক করা যায়" else "Record transactions in current or previous months to view comparative category differences.",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                val maxAmount = (summary.categories.maxOfOrNull { kotlin.math.max(it.currentMonthAmount, it.previousMonthAmount) } ?: 1.0).coerceAtLeast(1.0)
                items(summary.categories) { cat ->
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFCCFBF1),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = "▲", fontSize = 10.sp, color = Color(0xFF0F766E))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cat.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Delta Pill
                                val isIncrease = cat.percentageChange > 0
                                val deltaBg = if (isIncrease) Color(0xFFDC2626) else Color(0xFF15803D)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = deltaBg
                                ) {
                                    Text(
                                        text = "${if (isIncrease) "+" else ""}${String.format(Locale.US, "%.1f", cat.percentageChange)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Blue bar (Current month)
                            val currProgress = (cat.currentMonthAmount / maxAmount).toFloat().coerceIn(0f, 1f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = { currProgress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(3.5.dp)),
                                    color = Color(0xFF2563EB),
                                    trackColor = Color(0xFFDBEAFE).copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = CurrencyFormatter.format(cat.currentMonthAmount, currencySymbol, language = language),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.width(88.dp),
                                    textAlign = TextAlign.End
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Brown bar (Previous month)
                            val prevProgress = (cat.previousMonthAmount / maxAmount).toFloat().coerceIn(0f, 1f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = { prevProgress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(3.5.dp)),
                                    color = Color(0xFFB45309),
                                    trackColor = Color(0xFFFEF3C7).copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = CurrencyFormatter.format(cat.previousMonthAmount, currencySymbol, language = language),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.width(88.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "spending_comparison_fixed_header"
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
                    text = AppStrings.t("spending_comparison", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }
    }
}
