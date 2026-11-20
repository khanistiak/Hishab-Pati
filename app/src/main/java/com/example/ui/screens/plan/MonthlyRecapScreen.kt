package com.example.ui.screens.plan

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import com.example.ui.util.FinancialReportAnalytics
import com.example.ui.util.RecapInsightType
import com.example.ui.util.RecapSummary
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@Composable
fun MonthlyRecapScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val now = remember { Calendar.getInstance() }
    var currentYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH)) } // 0..11

    val recap = remember(transactions, budgets, accounts, currentYear, currentMonth, currencySymbol) {
        FinancialReportAnalytics.computeMonthlyRecap(
            transactions = transactions,
            budgets = budgets,
            year = currentYear,
            month = currentMonth,
            currencySymbol = currencySymbol,
            accounts = accounts
        )
    }

    val categoryLookup = remember(categories) {
        categories.associateBy { it.name.lowercase(Locale.ENGLISH) }
    }

    val monthName = FinancialReportAnalytics.getMonthName(currentMonth, full = true)
    val localizedMonthYear = remember(monthName, currentYear, language) {
        DateFormatter.localizeText("$monthName $currentYear", language)
    }

    val localizedInsights = remember(recap, categoryLookup, currencySymbol, language, localizedMonthYear) {
        buildLocalizedInsights(
            recap = recap,
            categoryLookup = categoryLookup,
            currencySymbol = currencySymbol,
            language = language,
            localizedMonthYear = localizedMonthYear
        )
    }

    val palette = rememberRecapCardPalette(isDark = isDark)

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

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.MONTHLY_RECAP
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 66.dp,
        extraTopGap = 8.dp,
        bottomNavClearance = 32.dp
    )

    val shareMonthlyRecap: () -> Unit = {
        val shareBody = buildMonthlyRecapShareText(
            recap = recap,
            localizedMonthYear = localizedMonthYear,
            categoryLookup = categoryLookup,
            currencySymbol = currencySymbol,
            language = language
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                "${AppStrings.t("app_name", language)} — ${AppStrings.t("monthly_recap_title", language)} ($localizedMonthYear)"
            )
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(intent, AppStrings.t("share_summary", language))
            )
        }
    }

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
                .testTag("monthly_recap_lazy_column"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = scrollLayout.contentPadding.calculateTopPadding(),
                bottom = scrollLayout.contentPadding.calculateBottomPadding()
            )
        ) {
            // 1. Month Selector Pill + Airy Financial Health Hero Bento Card
            item(key = "recap_hero_section") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecapMonthSelectorPill(
                        label = localizedMonthYear,
                        language = language,
                        palette = palette,
                        onPrevMonth = {
                            if (currentMonth == 0) {
                                currentMonth = 11
                                currentYear--
                            } else {
                                currentMonth--
                            }
                        },
                        onNextMonth = {
                            if (currentMonth == 11) {
                                currentMonth = 0
                                currentYear++
                            } else {
                                currentMonth++
                            }
                        }
                    )

                    FinancialHealthHeroCard(
                        recap = recap,
                        currencySymbol = currencySymbol,
                        language = language,
                        palette = palette
                    )
                }
            }

            // 2. Typography-First 2-Column Key Metrics Bento Grid (20.dp top gap, 10.dp header gap, 12.dp card gaps)
            item(key = "recap_key_metrics") {
                Spacer(modifier = Modifier.height(20.dp))
                RecapSectionHeader(
                    title = AppStrings.t("key_metrics", language),
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(10.dp))

                val budgetValueText = recap.budgetOkPct?.let {
                    CurrencyFormatter.formatPercent(it, language)
                } ?: "–"
                val budgetSubtitleText = if (recap.budgetTotalCount > 0) {
                    val okStr = CurrencyFormatter.formatNumber(recap.budgetOkCount, language)
                    val totalStr = CurrencyFormatter.formatNumber(recap.budgetTotalCount, language)
                    if (isBn) {
                        "$totalStr এর $okStr ${AppStrings.t("in_budget_suffix", language)}"
                    } else {
                        "$okStr of $totalStr ${AppStrings.t("in_budget_suffix", language)}"
                    }
                } else {
                    AppStrings.t("no_budget_set_short", language)
                }
                val budgetHealthy = (recap.budgetOkPct ?: 100) >= 80
                val budgetSubColor = when {
                    recap.budgetTotalCount == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                    budgetHealthy -> if (isDark) RecapEmeraldDarkText else RecapEmerald
                    else -> if (isDark) RecapAmberDarkText else RecapAmber
                }

                val topCatEntity = recap.topCategory?.let {
                    categoryLookup[it.lowercase(Locale.ENGLISH)]
                }
                val topCatDisplay = topCatEntity?.getDisplayName(language)
                    ?: recap.topCategory
                    ?: AppStrings.t("none_label", language)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Transactions & Avg Expense
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RecapMetricBentoCard(
                            title = AppStrings.t("transactions_label", language),
                            value = CurrencyFormatter.formatNumber(recap.transactionCount, language),
                            subtitle = if (recap.hasData) {
                                AppStrings.t("logged_entries", language)
                            } else {
                                AppStrings.t("no_records", language)
                            },
                            subtitleColor = if (isDark) RecapRoyalBlueDarkText else RecapRoyalBlue,
                            palette = palette,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        RecapMetricBentoCard(
                            title = AppStrings.t("avg_expense", language),
                            value = CurrencyFormatter.format(recap.avgExpenseSize, currencySymbol, language = language),
                            subtitle = if (recap.expenseCount > 0) {
                                AppStrings.t("per_expense", language)
                            } else {
                                AppStrings.t("no_expense", language)
                            },
                            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            valueFontSize = 18.sp,
                            palette = palette,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    // Row 2: Daily Average & Budget Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RecapMetricBentoCard(
                            title = AppStrings.t("daily_average", language),
                            value = CurrencyFormatter.format(recap.dailyAverage, currencySymbol, language = language),
                            subtitle = if (recap.totalExpense > 0) {
                                AppStrings.t("average_per_day", language)
                            } else {
                                AppStrings.t("no_expense", language)
                            },
                            subtitleColor = if (isDark) RecapTealDarkText else RecapTeal,
                            valueFontSize = 18.sp,
                            palette = palette,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        RecapMetricBentoCard(
                            title = AppStrings.t("budget_status", language),
                            value = budgetValueText,
                            subtitle = budgetSubtitleText,
                            subtitleColor = budgetSubColor,
                            palette = palette,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    // Row 3: Top Category & Top Merchant
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RecapMetricBentoCard(
                            title = AppStrings.t("top_category", language),
                            value = topCatDisplay,
                            subtitle = CurrencyFormatter.format(recap.topCategoryAmount, currencySymbol, language = language),
                            subtitleColor = if (recap.topCategoryAmount > 0) {
                                if (isDark) RecapCoralDarkText else RecapCoral
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            valueFontSize = 16.5.sp,
                            palette = palette,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        RecapMetricBentoCard(
                            title = AppStrings.t("top_merchant", language),
                            value = recap.topMerchant ?: AppStrings.t("none_label", language),
                            subtitle = CurrencyFormatter.format(recap.topMerchantAmount, currencySymbol, language = language),
                            subtitleColor = if (recap.topMerchantAmount > 0) {
                                if (isDark) RecapAmberDarkText else RecapAmber
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            valueFontSize = 16.5.sp,
                            palette = palette,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            // 3. Top Spending Categories Section (external header + borderless card with 38.dp entity icons)
            item(key = "recap_top_categories") {
                Spacer(modifier = Modifier.height(20.dp))
                RecapSectionHeader(
                    title = AppStrings.t("top_spending_categories", language),
                    trailingBadge = if (recap.topCategories.isNotEmpty()) {
                        AppStrings.t("top_3_badge", language)
                    } else {
                        null
                    },
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                TopCategoriesBentoCard(
                    topCategories = recap.topCategories,
                    categoryLookup = categoryLookup,
                    currencySymbol = currencySymbol,
                    language = language,
                    palette = palette
                )
            }

            // 4. Wallet & Payment Usage Section (external header + borderless card with 38.dp entity icons)
            item(key = "recap_wallet_usage") {
                Spacer(modifier = Modifier.height(20.dp))
                val walletBadge = if (recap.walletUsages.isNotEmpty()) {
                    val countText = CurrencyFormatter.formatNumber(recap.walletUsages.size, language)
                    if (isBn) {
                        "$countText${AppStrings.t("active_wallets_suffix", language)}"
                    } else {
                        "$countText ${AppStrings.t("active_wallets_suffix", language)}"
                    }
                } else {
                    null
                }
                RecapSectionHeader(
                    title = AppStrings.t("wallet_payment_usage", language),
                    trailingBadge = walletBadge,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                WalletUsageBentoCard(
                    walletUsages = recap.walletUsages,
                    currencySymbol = currencySymbol,
                    language = language,
                    palette = palette
                )
            }

            // 5. Monthly Insights Section (single sparkle beside external header + dot-based borderless card)
            item(key = "recap_monthly_insights") {
                Spacer(modifier = Modifier.height(20.dp))
                RecapSectionHeader(
                    title = AppStrings.t("monthly_insights", language),
                    showSparkle = true,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                MonthlyInsightsBentoCard(
                    insights = localizedInsights,
                    palette = palette
                )
            }
        }

        // Fixed Scroll-Aware Header with borderless circular Back & Share buttons
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "monthly_recap_fixed_header"
        ) {
            val headerBtnColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            } else {
                Color.White
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = headerBtnColor,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("monthly_recap_back_button"),
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
                        text = AppStrings.t("monthly_recap_title", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = AppStrings.t("financial_health_summary", language),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = CircleShape,
                    color = headerBtnColor,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("monthly_recap_share_button"),
                    onClick = shareMonthlyRecap
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = AppStrings.t("share_summary", language),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun buildLocalizedInsights(
    recap: RecapSummary,
    categoryLookup: Map<String, CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage,
    localizedMonthYear: String
): List<LocalizedRecapInsight> {
    val isBn = language == AppLanguage.BANGLA
    if (!recap.hasData) {
        val desc = if (isBn) {
            "$localizedMonthYear মাসের লেনদেন যোগ করলে স্বয়ংক্রিয় ব্যয় বিশ্লেষণ ও বাজেট পর্যবেক্ষণ এখানে দেখা যাবে।"
        } else {
            "Start logging transactions for $localizedMonthYear to unlock automated spending analysis and budget health."
        }
        return listOf(
            LocalizedRecapInsight(
                title = AppStrings.t("no_transactions_logged_yet", language),
                description = desc,
                type = RecapInsightType.EMPTY
            )
        )
    }

    val list = mutableListOf<LocalizedRecapInsight>()

    // 1. Savings insight
    val savingsPctText = CurrencyFormatter.formatPercent(recap.savingsRate, language)
    when {
        recap.savingsRate >= 30 -> {
            val savedFormatted = CurrencyFormatter.format(recap.netSavings, currencySymbol, language = language)
            list.add(
                LocalizedRecapInsight(
                    title = AppStrings.t("healthy_savings_rate", language),
                    description = "$savingsPctText • $savedFormatted",
                    type = RecapInsightType.SAVINGS_POSITIVE
                )
            )
        }
        recap.netSavings < 0 -> {
            val deficitFormatted = CurrencyFormatter.format(abs(recap.netSavings), currencySymbol, language = language)
            val desc = if (isBn) {
                "আয়ের চেয়ে $deficitFormatted বেশি ব্যয় হয়েছে"
            } else {
                "Expenses exceeded income by $deficitFormatted"
            }
            list.add(
                LocalizedRecapInsight(
                    title = AppStrings.t("negative_monthly_balance", language),
                    description = desc,
                    type = RecapInsightType.SAVINGS_NEGATIVE
                )
            )
        }
        recap.totalIncome > 0 -> {
            val savedFormatted = CurrencyFormatter.format(recap.netSavings, currencySymbol, language = language)
            list.add(
                LocalizedRecapInsight(
                    title = AppStrings.t("balanced_monthly_cashflow", language),
                    description = "$savingsPctText • $savedFormatted",
                    type = RecapInsightType.SAVINGS_BALANCED
                )
            )
        }
    }

    // 2. Top spending category insight
    if (recap.topCategory != null) {
        val catDisplay = categoryLookup[recap.topCategory.lowercase(Locale.ENGLISH)]
            ?.getDisplayName(language) ?: recap.topCategory
        val amtFormatted = CurrencyFormatter.format(recap.topCategoryAmount, currencySymbol, language = language)
        val pctFormatted = CurrencyFormatter.formatPercent(recap.topCategorySharePct, language)
        val desc = if (isBn) {
            "$amtFormatted • মোট ব্যয়ের $pctFormatted"
        } else {
            "$amtFormatted • $pctFormatted ${AppStrings.t("of_expenses", language)}"
        }
        list.add(
            LocalizedRecapInsight(
                title = "${AppStrings.t("top_spending_prefix", language)}: $catDisplay",
                description = desc,
                type = RecapInsightType.TOP_CATEGORY
            )
        )
    }

    // 3. Income recorded insight
    if (recap.totalIncome > 0) {
        val incFormatted = CurrencyFormatter.format(recap.totalIncome, currencySymbol, language = language)
        val countFormatted = CurrencyFormatter.formatNumber(recap.incomeCount, language)
        val desc = if (isBn) {
            "$countFormatted টি আয়ের রসিদে মোট $incFormatted"
        } else {
            val receiptUnit = if (recap.incomeCount == 1) "receipt" else "receipts"
            "$incFormatted across $countFormatted $receiptUnit"
        }
        list.add(
            LocalizedRecapInsight(
                title = AppStrings.t("income_recorded", language),
                description = desc,
                type = RecapInsightType.INCOME_RECORDED
            )
        )
    }

    return list.ifEmpty {
        listOf(
            LocalizedRecapInsight(
                title = AppStrings.t("no_transactions_logged_yet", language),
                description = AppStrings.t("no_records_for_period", language),
                type = RecapInsightType.EMPTY
            )
        )
    }
}

private fun buildMonthlyRecapShareText(
    recap: RecapSummary,
    localizedMonthYear: String,
    categoryLookup: Map<String, CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage
): String {
    val isBn = language == AppLanguage.BANGLA
    val sb = StringBuilder()
    sb.appendLine("${AppStrings.t("app_name", language)} — ${AppStrings.t("monthly_recap_title", language)} ($localizedMonthYear)")
    sb.appendLine("${AppStrings.t("grade_prefix", language)}: ${recap.grade}")
    sb.appendLine("${AppStrings.t("income", language)}: ${CurrencyFormatter.format(recap.totalIncome, currencySymbol, language = language)}")
    sb.appendLine("${AppStrings.t("expenses", language)}: ${CurrencyFormatter.format(recap.totalExpense, currencySymbol, language = language)}")
    sb.appendLine("${AppStrings.t("net_saved", language)}: ${CurrencyFormatter.format(recap.netSavings, currencySymbol, showSign = recap.netSavings > 0, language = language)} (${CurrencyFormatter.formatPercent(recap.savingsRate, language)})")
    sb.appendLine("${AppStrings.t("transactions_label", language)}: ${CurrencyFormatter.formatNumber(recap.transactionCount, language)}")

    if (recap.topCategories.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("${AppStrings.t("top_spending_categories", language)}:")
        recap.topCategories.forEach { cat ->
            val display = categoryLookup[cat.category.lowercase(Locale.ENGLISH)]?.getDisplayName(language) ?: cat.category
            sb.appendLine("• $display: ${CurrencyFormatter.format(cat.amount, currencySymbol, language = language)} (${CurrencyFormatter.formatPercent(cat.sharePct, language)})")
        }
    }

    if (recap.walletUsages.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("${AppStrings.t("wallet_payment_usage", language)}:")
        recap.walletUsages.forEach { w ->
            val txCountStr = CurrencyFormatter.formatNumber(w.transactionCount, language)
            val txUnit = if (isBn) "লেনদেন" else "txns"
            val amt = if (w.spendTotal > 0.0) {
                CurrencyFormatter.format(w.spendTotal, currencySymbol, language = language)
            } else {
                CurrencyFormatter.format(w.incomeTotal, currencySymbol, showSign = true, language = language)
            }
            sb.appendLine("• ${w.accountName}: $amt ($txCountStr $txUnit)")
        }
    }

    return sb.toString().trim()
}
