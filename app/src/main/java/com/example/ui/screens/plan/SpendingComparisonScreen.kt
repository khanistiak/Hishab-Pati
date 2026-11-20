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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.getIconForName
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.util.AppStrings
import com.example.ui.util.ComparisonCategoryItem
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.FinancialReportAnalytics
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private enum class ComparisonSortMode {
    AMOUNT,
    CHANGE
}

private val CurrentMonthBlueFill = Color(0xFF2563EB)
private val CurrentMonthBlueTrack = Color(0xFFDBEAFE)
private val CurrentMonthBlueText = Color(0xFF2563EB)

private val PreviousMonthGreenFill = Color(0xFF10B981)
private val PreviousMonthGreenTrack = Color(0xFFD1FAE5)
private val PreviousMonthGreenText = Color(0xFF059669)

private val SoftCoralBg = Color(0xFFFEE2E2)
private val SoftCoralText = Color(0xFFDC2626)
private val SoftEmeraldBg = Color(0xFFDCFCE7)
private val SoftEmeraldText = Color(0xFF16A34A)

@Composable
fun SpendingComparisonScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val now = remember { Calendar.getInstance() }
    val todayYear = now.get(Calendar.YEAR)
    val todayMonth = now.get(Calendar.MONTH)
    val todayDay = now.get(Calendar.DAY_OF_MONTH)

    var currentYear by remember { mutableIntStateOf(todayYear) }
    var currentMonth by remember { mutableIntStateOf(todayMonth) } // 0..11
    var sortMode by remember { mutableStateOf(ComparisonSortMode.AMOUNT) }

    val summary = remember(transactions, currentYear, currentMonth) {
        FinancialReportAnalytics.computeSpendingComparison(
            transactions = transactions,
            currentYear = currentYear,
            currentMonth = currentMonth
        )
    }

    val curMonthName = FinancialReportAnalytics.getMonthName(currentMonth)
    val prevMonthName = FinancialReportAnalytics.getMonthName(summary.prevMonth)

    val categoryLookup = remember(categories) {
        categories.associateBy { it.name.lowercase(Locale.ENGLISH) }
    }

    // Days elapsed in current vs previous month for Daily Spend Pace
    val currentMonthDaysElapsed = remember(currentYear, currentMonth, todayYear, todayMonth, todayDay) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (currentYear == todayYear && currentMonth == todayMonth) {
            todayDay.coerceIn(1, maxDays)
        } else {
            maxDays
        }
    }

    val prevMonthDaysElapsed = remember(summary.prevYear, summary.prevMonth, todayYear, todayMonth, todayDay) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, summary.prevYear)
            set(Calendar.MONTH, summary.prevMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (summary.prevYear == todayYear && summary.prevMonth == todayMonth) {
            todayDay.coerceIn(1, maxDays)
        } else {
            maxDays
        }
    }

    val currentDailyPace = if (currentMonthDaysElapsed > 0) {
        summary.currentMonthExpense / currentMonthDaysElapsed.toDouble()
    } else 0.0

    val previousDailyPace = if (prevMonthDaysElapsed > 0) {
        summary.previousMonthExpense / prevMonthDaysElapsed.toDouble()
    } else 0.0

    // Net Savings (Cashflow) = Income - Expense
    val currentNetSavings = summary.currentMonthIncome - summary.currentMonthExpense
    val previousNetSavings = summary.previousMonthIncome - summary.previousMonthExpense
    val netSavingsDiff = currentNetSavings - previousNetSavings
    val netSavingsDeltaPct = if (abs(previousNetSavings) > 0.001) {
        ((currentNetSavings - previousNetSavings) / abs(previousNetSavings)) * 100.0
    } else if (abs(currentNetSavings) > 0.001) {
        if (currentNetSavings > 0) 100.0 else -100.0
    } else {
        0.0
    }

    val sortedCategories = remember(summary.categories, sortMode) {
        when (sortMode) {
            ComparisonSortMode.AMOUNT -> summary.categories.sortedByDescending {
                maxOf(it.currentMonthAmount, it.previousMonthAmount)
            }
            ComparisonSortMode.CHANGE -> summary.categories.sortedByDescending {
                abs(it.currentMonthAmount - it.previousMonthAmount)
            }
        }
    }

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.SPENDING_COMPARISON
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 16.dp,
        bottomNavClearance = 32.dp
    )

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
                .testTag("spending_comparison_lazy_column"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = scrollLayout.contentPadding.calculateTopPadding(),
                bottom = scrollLayout.contentPadding.calculateBottomPadding()
            )
        ) {
            // 1. Sleek Compact Single-Row Top Month Comparison Bar (48.dp height, 20.dp radius)
            item(key = "month_comparison_bar") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardColor,
                    border = BorderStroke(1.dp, cardBorderColor),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("spending_comparison_month_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (currentMonth == 0) {
                                        currentMonth = 11
                                        currentYear--
                                    } else {
                                        currentMonth--
                                    }
                                }
                                .testTag("comparison_prev_month_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(CurrentMonthBlueText)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "$curMonthName $currentYear",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color(0xFF60A5FA) else CurrentMonthBlueText
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = AppStrings.t("vs", language),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(PreviousMonthGreenText)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "$prevMonthName ${summary.prevYear}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) PreviousMonthGreenFill else PreviousMonthGreenText
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (currentMonth == 11) {
                                        currentMonth = 0
                                        currentYear++
                                    } else {
                                        currentMonth++
                                    }
                                }
                                .testTag("comparison_next_month_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // 2. Enriched Overview Bento Card (Expense + Daily Pace | Income | Net Savings Cashflow)
            item(key = "overview_bento_card") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardColor,
                    border = BorderStroke(1.dp, cardBorderColor),
                    shadowElevation = 1.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // A. Expense Overview + Daily Spend Pace
                        val hasExpenseData = summary.currentMonthExpense > 0 || summary.previousMonthExpense > 0
                        val isExpenseSaved = summary.expenseDiff <= 0
                        val expPctFormatted = String.format(Locale.US, "%.1f", abs(summary.expenseDeltaPct)).let {
                            if (isBn) CurrencyFormatter.convertToBanglaDigits(it) else it
                        }
                        val expenseDeltaText = if (!hasExpenseData) {
                            if (isBn) "০.০%" else "0.0%"
                        } else if (isExpenseSaved) {
                            "↓ ${CurrencyFormatter.format(abs(summary.expenseDiff), currencySymbol, language = language)} ($expPctFormatted%)"
                        } else {
                            "↑ ${CurrencyFormatter.format(summary.expenseDiff, currencySymbol, language = language)} ($expPctFormatted%)"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppStrings.t("expense_overview", language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    !hasExpenseData -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    isExpenseSaved -> SoftEmeraldBg
                                    else -> SoftCoralBg
                                }
                            ) {
                                Text(
                                    text = expenseDeltaText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        !hasExpenseData -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        isExpenseSaved -> SoftEmeraldText
                                        else -> SoftCoralText
                                    },
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "$curMonthName $currentYear",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.format(summary.currentMonthExpense, currencySymbol, language = language),
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${CurrencyFormatter.format(currentDailyPace, currencySymbol, language = language)}${AppStrings.t("day_short", language)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFF60A5FA) else CurrentMonthBlueText.copy(alpha = 0.88f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$prevMonthName ${summary.prevYear}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.format(summary.previousMonthExpense, currencySymbol, language = language),
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${CurrencyFormatter.format(previousDailyPace, currencySymbol, language = language)}${AppStrings.t("day_short", language)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) PreviousMonthGreenFill else PreviousMonthGreenText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // B. Income Overview
                        val hasIncomeData = summary.currentMonthIncome > 0 || summary.previousMonthIncome > 0
                        val isIncomeHigher = summary.incomeDiff >= 0
                        val incPctFormatted = String.format(Locale.US, "%.1f", abs(summary.incomeDeltaPct)).let {
                            if (isBn) CurrencyFormatter.convertToBanglaDigits(it) else it
                        }
                        val incomeDeltaText = if (!hasIncomeData) {
                            if (isBn) "০.০%" else "0.0%"
                        } else if (isIncomeHigher) {
                            "↑ ${CurrencyFormatter.format(summary.incomeDiff, currencySymbol, language = language)} ($incPctFormatted%)"
                        } else {
                            "↓ ${CurrencyFormatter.format(abs(summary.incomeDiff), currencySymbol, language = language)} ($incPctFormatted%)"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppStrings.t("income_overview", language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    !hasIncomeData -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    isIncomeHigher -> SoftEmeraldBg
                                    else -> SoftCoralBg
                                }
                            ) {
                                Text(
                                    text = incomeDeltaText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        !hasIncomeData -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        isIncomeHigher -> SoftEmeraldText
                                        else -> SoftCoralText
                                    },
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "$curMonthName $currentYear",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.format(summary.currentMonthIncome, currencySymbol, language = language),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$prevMonthName ${summary.prevYear}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.format(summary.previousMonthIncome, currencySymbol, language = language),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // C. Net Savings (Cashflow) Comparison Row
                        val hasCashflowData = hasExpenseData || hasIncomeData
                        val isNetImproved = netSavingsDiff >= 0
                        val netPctFormatted = String.format(Locale.US, "%.1f", abs(netSavingsDeltaPct)).let {
                            if (isBn) CurrencyFormatter.convertToBanglaDigits(it) else it
                        }
                        val netDeltaText = if (!hasCashflowData) {
                            if (isBn) "০.০%" else "0.0%"
                        } else if (isNetImproved) {
                            "↑ ${CurrencyFormatter.format(abs(netSavingsDiff), currencySymbol, language = language)} ($netPctFormatted%)"
                        } else {
                            "↓ ${CurrencyFormatter.format(abs(netSavingsDiff), currencySymbol, language = language)} ($netPctFormatted%)"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppStrings.t("net_savings_cashflow", language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    !hasCashflowData -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    isNetImproved -> SoftEmeraldBg
                                    else -> SoftCoralBg
                                }
                            ) {
                                Text(
                                    text = netDeltaText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        !hasCashflowData -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        isNetImproved -> SoftEmeraldText
                                        else -> SoftCoralText
                                    },
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "$curMonthName $currentYear",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.format(currentNetSavings, currencySymbol, language = language),
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        currentNetSavings > 0.001 -> SoftEmeraldText
                                        currentNetSavings < -0.001 -> SoftCoralText
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$prevMonthName ${summary.prevYear}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.format(previousNetSavings, currencySymbol, language = language),
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        previousNetSavings > 0.001 -> SoftEmeraldText
                                        previousNetSavings < -0.001 -> SoftCoralText
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // 3. Side-by-Side Highlights (Biggest Increase & Biggest Decrease)
            item(key = "biggest_change_cards") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Biggest Increase Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, cardBorderColor),
                        shadowElevation = 1.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = SoftCoralBg,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = SoftCoralText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = AppStrings.t("biggest_increase", language),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftCoralText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val incCat = summary.biggestIncrease
                            val incCatDisplay = incCat?.let {
                                categoryLookup[it.name.lowercase(Locale.ENGLISH)]?.getDisplayName(language) ?: it.name
                            } ?: AppStrings.t("none_label", language)
                            Text(
                                text = incCatDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val incDiff = (incCat?.currentMonthAmount ?: 0.0) - (incCat?.previousMonthAmount ?: 0.0)
                            val incPctStr = incCat?.let {
                                val p = String.format(Locale.US, "%.1f", it.percentageChange)
                                "+${if (isBn) CurrencyFormatter.convertToBanglaDigits(p) else p}%"
                            } ?: "–"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (incCat != null) {
                                        "+${CurrencyFormatter.format(incDiff, currencySymbol, language = language)}"
                                    } else {
                                        CurrencyFormatter.format(0.0, currencySymbol, language = language)
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (incCat != null) SoftCoralText else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    maxLines = 1
                                )
                                Text(
                                    text = incPctStr,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }

                    // Biggest Decrease Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, cardBorderColor),
                        shadowElevation = 1.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = SoftEmeraldBg,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = SoftEmeraldText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = AppStrings.t("biggest_decrease", language),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftEmeraldText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val decCat = summary.biggestDecrease
                            val decCatDisplay = decCat?.let {
                                categoryLookup[it.name.lowercase(Locale.ENGLISH)]?.getDisplayName(language) ?: it.name
                            } ?: AppStrings.t("none_label", language)
                            Text(
                                text = decCatDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val decDiff = (decCat?.previousMonthAmount ?: 0.0) - (decCat?.currentMonthAmount ?: 0.0)
                            val decPctStr = decCat?.let {
                                val p = String.format(Locale.US, "%.1f", it.percentageChange)
                                "${if (isBn) CurrencyFormatter.convertToBanglaDigits(p) else p}%"
                            } ?: "–"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (decCat != null) {
                                        "-${CurrencyFormatter.format(decDiff, currencySymbol, language = language)}"
                                    } else {
                                        CurrencyFormatter.format(0.0, currencySymbol, language = language)
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (decCat != null) SoftEmeraldText else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    maxLines = 1
                                )
                                Text(
                                    text = decPctStr,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Smart Insight Banner (keep lightbulb icon)
            item(key = "smart_insight_banner") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    } else {
                        Color.White.copy(alpha = 0.88f)
                    },
                    border = BorderStroke(1.dp, cardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = summary.insightText,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            // 5. Category Breakdown Section Header + Legend + Sort Toggle Chip
            item(key = "category_breakdown_header") {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.t("category_breakdown", language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        // Compact Sort Toggle Chip (Sort: Amount / Sort: Change)
                        Surface(
                            shape = CircleShape,
                            color = cardColor,
                            border = BorderStroke(1.dp, cardBorderColor),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    sortMode = if (sortMode == ComparisonSortMode.AMOUNT) {
                                        ComparisonSortMode.CHANGE
                                    } else {
                                        ComparisonSortMode.AMOUNT
                                    }
                                }
                                .testTag("comparison_sort_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = null,
                                    tint = CurrentMonthBlueText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (sortMode == ComparisonSortMode.AMOUNT) {
                                        AppStrings.t("sort_amount", language)
                                    } else {
                                        AppStrings.t("sort_change", language)
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color Legend Row (● Nov 2026 in #2563EB, ● Oct 2026 in #059669)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CurrentMonthBlueText)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$curMonthName $currentYear",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF60A5FA) else CurrentMonthBlueText
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PreviousMonthGreenText)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$prevMonthName ${summary.prevYear}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) PreviousMonthGreenFill else PreviousMonthGreenText
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 6. Category Breakdown Cards
            if (sortedCategories.isEmpty()) {
                item(key = "empty_comparison_categories") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, cardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBn) "তুলনা করার মতো কোনো খরচ নেই" else "No category spending data to compare",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBn) {
                                    "লেনদেন যুক্ত করুন যাতে খরচের পরিবর্তন ট্র্যাক করা যায়"
                                } else {
                                    "Record transactions in current or previous months to view comparative category differences."
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                val maxAmount = (sortedCategories.maxOfOrNull {
                    maxOf(it.currentMonthAmount, it.previousMonthAmount)
                } ?: 1.0).coerceAtLeast(1.0)

                itemsIndexed(sortedCategories, key = { _, cat -> cat.name }) { index, cat ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    val catEntity = categoryLookup[cat.name.lowercase(Locale.ENGLISH)]
                    val displayCatName = catEntity?.getDisplayName(language) ?: cat.name
                    val iconName = catEntity?.iconName?.ifBlank { "category" } ?: "category"
                    val tintColor = remember(catEntity?.colorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(catEntity?.colorHex ?: "#2563EB"))
                        } catch (_: Exception) {
                            CurrentMonthBlueFill
                        }
                    }

                    ComparisonCategoryBentoCard(
                        item = cat,
                        displayName = displayCatName,
                        iconName = iconName,
                        iconTint = tintColor,
                        curMonthShort = curMonthName,
                        prevMonthShort = prevMonthName,
                        totalCurrentMonthExpense = summary.currentMonthExpense,
                        maxAmount = maxAmount,
                        currencySymbol = currencySymbol,
                        language = language,
                        isDark = isDark,
                        cardColor = cardColor,
                        borderColor = cardBorderColor
                    )
                }
            }
        }

        // Fixed Scroll-Aware Frosted Header
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "spending_comparison_fixed_header"
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
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    } else {
                        Color.White.copy(alpha = 0.85f)
                    },
                    shadowElevation = if (isDark) 0.dp else 1.dp,
                    modifier = Modifier.size(40.dp),
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

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = AppStrings.t("spending_comparison", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }
    }
}

@Composable
private fun ComparisonCategoryBentoCard(
    item: ComparisonCategoryItem,
    displayName: String,
    iconName: String,
    iconTint: Color,
    curMonthShort: String,
    prevMonthShort: String,
    totalCurrentMonthExpense: Double,
    maxAmount: Double,
    currencySymbol: String,
    language: AppLanguage,
    isDark: Boolean,
    cardColor: Color,
    borderColor: Color
) {
    val isBn = language == AppLanguage.BANGLA
    val diff = item.currentMonthAmount - item.previousMonthAmount
    val isIncrease = diff > 0.001
    val isDecrease = diff < -0.001

    val shareOfTotalPct = if (totalCurrentMonthExpense > 0.0) {
        ((item.currentMonthAmount / totalCurrentMonthExpense) * 100.0).toInt().coerceIn(0, 100)
    } else 0
    val sharePctStr = if (isBn) {
        "${CurrencyFormatter.convertToBanglaDigits(shareOfTotalPct.toString())}%"
    } else {
        "$shareOfTotalPct%"
    }
    val sharePhrase = if (isBn) {
        "${AppStrings.t("of_total", language)} $sharePctStr"
    } else {
        "$sharePctStr ${AppStrings.t("of_total", language)}"
    }

    val diffPhrase = when {
        isIncrease -> "+${CurrencyFormatter.format(diff, currencySymbol, language = language)} ${AppStrings.t("vs", language)} $prevMonthShort"
        isDecrease -> "-${CurrencyFormatter.format(abs(diff), currencySymbol, language = language)} ${AppStrings.t("vs", language)} $prevMonthShort"
        else -> "${CurrencyFormatter.format(0.0, currencySymbol, language = language)} ${AppStrings.t("vs", language)} $prevMonthShort"
    }

    val metaSubtitle = "$sharePhrase • $diffPhrase"

    val isNewFromZero = item.previousMonthAmount <= 0.001 && item.currentMonthAmount > 0.001
    val pctFormatted = String.format(Locale.US, "%.1f", item.percentageChange).let {
        if (isBn) CurrencyFormatter.convertToBanglaDigits(it) else it
    }
    val pctDisplay = if (isNewFromZero) {
        "${AppStrings.t("new_badge", language)} • +$pctFormatted%"
    } else {
        "${if (item.percentageChange > 0) "+" else ""}$pctFormatted%"
    }

    val currentFraction = if (item.currentMonthAmount > 0.0) {
        (item.currentMonthAmount / maxAmount).toFloat().coerceIn(0.03f, 1f)
    } else 0f

    val previousFraction = if (item.previousMonthAmount > 0.0) {
        (item.previousMonthAmount / maxAmount).toFloat().coerceIn(0.03f, 1f)
    } else 0f

    val currentTrackColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else CurrentMonthBlueTrack
    val previousTrackColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else PreviousMonthGreenTrack

    val currentMonthColor = if (isDark) Color(0xFF60A5FA) else CurrentMonthBlueText
    val previousMonthColor = if (isDark) PreviousMonthGreenFill else PreviousMonthGreenText

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: 40.dp squircle icon (12.dp radius) + 12.dp gap + Name & subtitle | Soft-tinted delta pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = iconTint.copy(alpha = 0.14f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getIconForName(iconName),
                                contentDescription = displayName,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = metaSubtitle,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Soft-Tinted Delta Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        item.percentageChange > 0.0 -> SoftCoralBg
                        item.percentageChange < 0.0 -> SoftEmeraldBg
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }
                ) {
                    Text(
                        text = pctDisplay,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            item.percentageChange > 0.0 -> SoftCoralText
                            item.percentageChange < 0.0 -> SoftEmeraldText
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 1 (Current Month — Royal Blue #2563EB): Short month tag (32.dp) + 8.dp CircleShape progress bar + bold Blue amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = curMonthShort,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = currentMonthColor
                    ),
                    maxLines = 1,
                    modifier = Modifier.width(34.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(currentTrackColor)
                ) {
                    if (currentFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(currentFraction)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(CurrentMonthBlueFill)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = CurrencyFormatter.format(item.currentMonthAmount, currencySymbol, language = language),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentMonthColor,
                    modifier = Modifier.width(86.dp),
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2 (Previous Month — Emerald Green #10B981 / #059669): Short month tag (34.dp) + 8.dp CircleShape progress bar + bold Green amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prevMonthShort,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = previousMonthColor
                    ),
                    maxLines = 1,
                    modifier = Modifier.width(34.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(previousTrackColor)
                ) {
                    if (previousFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(previousFraction)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(PreviousMonthGreenFill)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = CurrencyFormatter.format(item.previousMonthAmount, currencySymbol, language = language),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = previousMonthColor,
                    modifier = Modifier.width(86.dp),
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }
    }
}
