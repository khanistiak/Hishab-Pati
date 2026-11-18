package com.example.ui.screens.insights

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter
import kotlin.math.abs

@Composable
fun SmartFinancialInsightsCard(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage,
    isIncomeMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val insights = remember(transactions, dateRange, categories, isIncomeMode) {
        if (isIncomeMode) {
            InsightsCalculator.generateSmartIncomeInsights(transactions, dateRange, categories, currencySymbol, language)
        } else {
            InsightsCalculator.generateSmartInsights(transactions, dateRange, categories, currencySymbol, language)
        }
    }

    val headerTitle = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "স্মার্ট আয় পরামর্শ" else "SMART INCOME INSIGHTS"
    } else {
        if (language == AppLanguage.BANGLA) "স্মার্ট ব্যয় পরামর্শ" else "SMART FINANCIAL INSIGHTS"
    }

    val headerSub = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "আয়ের লেনদেনের ওপর ভিত্তি করে স্বয়ংক্রিয় তথ্য" else "Generated automatically from your earnings"
    } else {
        if (language == AppLanguage.BANGLA) "বাস্তব লেনদেনের তথ্যের ওপর ভিত্তি করে তৈরি" else "Generated automatically from your actual records"
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag(if (isIncomeMode) "smart_income_insights_card" else "smart_insights_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = (if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
                    contentColor = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = headerSub,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                insights.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (item.isPositive) IncomeGreen else MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BANGLA) item.titleBn else item.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (language == AppLanguage.BANGLA) item.descriptionBn else item.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeSourcesCard(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    currencySymbol: String,
    language: AppLanguage,
    onSourceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sources = remember(transactions, dateRange) {
        InsightsCalculator.calculateIncomeSources(transactions, dateRange)
    }
    val totalIncome = sources.sumOf { it.amount }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("income_sources_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "আয়ের উৎসসমূহ" else "Income Sources",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (language == AppLanguage.BANGLA) "নির্দিষ্ট সময়ের আয়ের খাত ও অনুপাত" else "Total income breakdown for period",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Text(
                    text = CurrencyFormatter.format(totalIncome, currencySymbol, language = language),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (sources.isEmpty()) {
                Text(
                    text = if (language == AppLanguage.BANGLA) "এই সময়ের জন্য কোনো আয়ের তথ্য নেই" else "No income transactions recorded for this period.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sources.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSourceClick(item.name) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(item.color)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            val txnsText = if (language == AppLanguage.BANGLA) {
                                                "${CurrencyFormatter.convertToBanglaDigits(item.count.toString())} লেনদেন"
                                            } else {
                                                "${item.count} transactions"
                                            }
                                            Text(
                                                text = txnsText,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = CurrencyFormatter.format(item.amount, currencySymbol, language = language),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = IncomeGreen
                                            )
                                        )
                                        val pctText = "${CurrencyFormatter.format(item.percentage.toDouble(), "", language = language)}%"
                                        Text(
                                            text = pctText,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = item.color,
                                    trackColor = item.color.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopSpendingCategoriesCard(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage,
    isIncomeMode: Boolean = false,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryExpenses = remember(transactions, dateRange, categories, isIncomeMode) {
        if (isIncomeMode) {
            InsightsCalculator.calculateCategoryIncomes(transactions, dateRange, categories).take(5)
        } else {
            InsightsCalculator.calculateCategoryExpenses(transactions, dateRange, categories).take(5)
        }
    }

    val title = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "শীর্ষ আয়ের খাতসমূহ" else "Top Income Categories"
    } else {
        if (language == AppLanguage.BANGLA) "শীর্ষ খরচের খাতসমূহ" else "Top Spending Categories"
    }

    val subtitle = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "সর্বোচ্চ আয়ের ক্রমানুসারে সাজানো" else "Ranked by highest earnings"
    } else {
        if (language == AppLanguage.BANGLA) "ট্যাপ করে বিস্তারিত দেখুন" else "Ranked by highest total expenses"
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag(if (isIncomeMode) "top_income_categories_card" else "top_spending_categories_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (categoryExpenses.isEmpty()) {
                Text(
                    text = if (language == AppLanguage.BANGLA) "কোন তথ্য নেই" else "No expense records found.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryExpenses.forEachIndexed { index, item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCategoryClick(item.categoryName) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = item.color.copy(alpha = 0.15f),
                                        contentColor = item.color,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val rankStr = if (language == AppLanguage.BANGLA) {
                                                CurrencyFormatter.convertToBanglaDigits((index + 1).toString())
                                            } else {
                                                "#${index + 1}"
                                            }
                                            Text(
                                                text = rankStr,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = item.color
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.categoryName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val countStr = if (language == AppLanguage.BANGLA) {
                                            "${CurrencyFormatter.convertToBanglaDigits(item.count.toString())} লেনদেন (${CurrencyFormatter.format(item.percentage.toDouble(), "", language = language)}%)"
                                        } else {
                                            "${item.count} txns (${CurrencyFormatter.format(item.percentage.toDouble(), "", language = language)}%)"
                                        }
                                        Text(
                                            text = countStr,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = CurrencyFormatter.format(item.amount, currencySymbol, language = language),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(12.dp)
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

@Composable
fun TopMerchantsCard(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    currencySymbol: String,
    language: AppLanguage,
    onMerchantClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val merchants = remember(transactions, dateRange) {
        InsightsCalculator.calculateTopMerchants(transactions, dateRange, limit = 5)
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("top_merchants_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "শীর্ষ ব্যয় বা প্রতিষ্ঠান" else "Top Merchants / Transaction Names",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (language == AppLanguage.BANGLA) "সর্বোচ্চ খরচের প্রাপক ও শিরোনাম" else "Ranked by highest total spending",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (merchants.isEmpty()) {
                Text(
                    text = if (language == AppLanguage.BANGLA) "কোন লেনদেনের তথ্য নেই" else "No transactions recorded for this period.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    merchants.forEachIndexed { idx, item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onMerchantClick(item.merchantName) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val rankStr = if (language == AppLanguage.BANGLA) {
                                                CurrencyFormatter.convertToBanglaDigits((idx + 1).toString())
                                            } else {
                                                "${idx + 1}"
                                            }
                                            Text(
                                                text = rankStr,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.merchantName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val countStr = if (language == AppLanguage.BANGLA) {
                                            "${CurrencyFormatter.convertToBanglaDigits(item.count.toString())} লেনদেন"
                                        } else {
                                            "${item.count} transactions"
                                        }
                                        Text(
                                            text = countStr,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = CurrencyFormatter.format(item.amount, currencySymbol, language = language),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(12.dp)
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

@Composable
fun TopIncomePayersCard(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    currencySymbol: String,
    language: AppLanguage,
    onPayerClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val payers = remember(transactions, dateRange) {
        InsightsCalculator.calculateTopIncomePayers(transactions, dateRange, limit = 5)
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("top_income_payers_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "শীর্ষ আয়ের উৎস বা প্রদানকারী" else "Top Payers / Income Clients",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (language == AppLanguage.BANGLA) "সর্বোচ্চ আয়ের প্রদানকারী ও ক্লায়েন্ট" else "Ranked by highest received amounts",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = IncomeGreen.copy(alpha = 0.15f),
                    contentColor = IncomeGreen,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (payers.isEmpty()) {
                Text(
                    text = if (language == AppLanguage.BANGLA) "কোন আয়ের লেনদেনের তথ্য নেই" else "No income transactions recorded for this period.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    payers.forEachIndexed { idx, item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPayerClick(item.merchantName) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = IncomeGreen.copy(alpha = 0.15f),
                                        contentColor = IncomeGreen,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val rankStr = if (language == AppLanguage.BANGLA) {
                                                CurrencyFormatter.convertToBanglaDigits((idx + 1).toString())
                                            } else {
                                                "#${idx + 1}"
                                            }
                                            Text(
                                                text = rankStr,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = IncomeGreen
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.merchantName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val countStr = if (language == AppLanguage.BANGLA) {
                                            "${CurrencyFormatter.convertToBanglaDigits(item.count.toString())} লেনদেন"
                                        } else {
                                            "${item.count} transactions"
                                        }
                                        Text(
                                            text = countStr,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = CurrencyFormatter.format(item.amount, currencySymbol, language = language),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(12.dp)
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

@Composable
fun PeriodComparisonCard(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val comp = remember(transactions, dateRange) {
        InsightsCalculator.calculatePeriodComparison(transactions, dateRange)
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("period_comparison_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (language == AppLanguage.BANGLA) "পূর্ববর্তী সময়ের সাথে তুলনা" else "Period Comparison",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = if (language == AppLanguage.BANGLA) "একই মেয়াদের পূর্ববর্তী সময়ের সাথে তারতম্য" else "Changes vs equivalent previous period",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Income Comparison
                ComparisonRowItem(
                    title = if (language == AppLanguage.BANGLA) "আয়" else "Income",
                    current = comp.currentIncome,
                    previous = comp.previousIncome,
                    diff = comp.incomeDiff,
                    changePct = comp.incomeChangePct,
                    isImprovement = comp.incomeDiff >= 0,
                    currencySymbol = currencySymbol,
                    language = language
                )

                // Expense Comparison (for expenses, lower is an improvement!)
                ComparisonRowItem(
                    title = if (language == AppLanguage.BANGLA) "ব্যয়" else "Expenses",
                    current = comp.currentExpense,
                    previous = comp.previousExpense,
                    diff = comp.expenseDiff,
                    changePct = comp.expenseChangePct,
                    isImprovement = comp.expenseDiff <= 0,
                    currencySymbol = currencySymbol,
                    language = language
                )

                // Net Savings Comparison
                ComparisonRowItem(
                    title = if (language == AppLanguage.BANGLA) "নিট সঞ্চয়" else "Net Savings",
                    current = comp.currentNet,
                    previous = comp.previousNet,
                    diff = comp.netDiff,
                    changePct = comp.netChangePct,
                    isImprovement = comp.netDiff >= 0,
                    currencySymbol = currencySymbol,
                    language = language
                )
            }
        }
    }
}

@Composable
private fun ComparisonRowItem(
    title: String,
    current: Double,
    previous: Double,
    diff: Double,
    changePct: Double,
    isImprovement: Boolean,
    currencySymbol: String,
    language: AppLanguage
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "${if (language == AppLanguage.BANGLA) "পূর্ববর্তী:" else "Prev:"} ${CurrencyFormatter.format(previous, currencySymbol, language = language)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(current, currencySymbol, language = language),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                val badgeColor = if (isImprovement) IncomeGreen else ExpenseRed
                val icon = if (diff >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                val sign = if (diff >= 0) "+" else "-"
                val pctStr = "$sign${CurrencyFormatter.format(abs(changePct), "", language = language)}%"

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = pctStr,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialHealthSection(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    dateRange: InsightsDateRange,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val health = remember(transactions, budgets, dateRange) {
        InsightsCalculator.calculateFinancialHealth(transactions, budgets, dateRange, language)
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("financial_health_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "আর্থিক স্বাস্থ্য ড্যাশবোর্ড" else "Financial Health Dashboard",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (language == AppLanguage.BANGLA) "বাস্তব সূচকের ওপর ১০০-এর মধ্যে স্কোর" else "Transparent fitness score out of 100",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Banner with Meter
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val scoreColor = when {
                            health.score >= 80 -> IncomeGreen
                            health.score >= 65 -> MaterialTheme.colorScheme.primary
                            health.score >= 50 -> Color(0xFFF59E0B)
                            else -> ExpenseRed
                        }

                        CircularProgressIndicator(
                            progress = { health.score / 100f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp,
                            color = scoreColor,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val scoreStr = if (language == AppLanguage.BANGLA) {
                                CurrencyFormatter.convertToBanglaDigits(health.score.toString())
                            } else {
                                health.score.toString()
                            }
                            Text(
                                text = scoreStr,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "/100",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val ratingText = if (language == AppLanguage.BANGLA) health.ratingBn else health.rating
                        val ratingColor = when {
                            health.score >= 80 -> IncomeGreen
                            health.score >= 65 -> MaterialTheme.colorScheme.primary
                            health.score >= 50 -> Color(0xFFF59E0B)
                            else -> ExpenseRed
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ratingColor.copy(alpha = 0.15f),
                            contentColor = ratingColor
                        ) {
                            Text(
                                text = ratingText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = ratingColor,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) health.explanationBn else health.explanation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6 Explicit Financial Health Metrics Grid
            // Row 1: Total Income, Total Expenses, Net Savings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Income
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "মোট আয়" else "Total Income",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(health.totalIncome, currencySymbol, language = language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Total Expenses
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "মোট ব্যয়" else "Total Expenses",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(health.totalExpenses, currencySymbol, language = language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Net Savings
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "নিট সঞ্চয়" else "Net Savings",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val netColor = if (health.netSavings >= 0) IncomeGreen else ExpenseRed
                        Text(
                            text = CurrencyFormatter.format(health.netSavings, currencySymbol, showSign = true, language = language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = netColor,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Savings Rate %, Expense-to-Income %, Budget Adherence %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Savings Rate
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "সঞ্চয় হার" else "Savings Rate",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val rateStr = health.savingsRate?.let {
                            "${CurrencyFormatter.format(it, "", language = language)}%"
                        } ?: "N/A"
                        val rateColor = when {
                            health.savingsRate == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            health.savingsRate >= 20.0 -> IncomeGreen
                            health.savingsRate >= 0.0 -> MaterialTheme.colorScheme.primary
                            else -> ExpenseRed
                        }
                        Text(
                            text = rateStr,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = rateColor,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                // Expense-to-Income Ratio
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "ব্যয়-আয় অনুপাত" else "Expense Ratio",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val ratioStr = health.expenseToIncomeRatio?.let {
                            "${CurrencyFormatter.format(it, "", language = language)}%"
                        } ?: "N/A"
                        val ratioColor = when {
                            health.expenseToIncomeRatio == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            health.expenseToIncomeRatio <= 80.0 -> IncomeGreen
                            health.expenseToIncomeRatio <= 100.0 -> Color(0xFFF59E0B)
                            else -> ExpenseRed
                        }
                        Text(
                            text = ratioStr,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ratioColor,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                // Budget Adherence
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "বাজেট ব্যবহার" else "Budget Used",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val budStr = health.budgetAdherencePct?.let {
                            "${CurrencyFormatter.format(it, "", language = language)}%"
                        } ?: "N/A"
                        val budColor = when {
                            health.budgetAdherencePct == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            health.budgetAdherencePct <= 100.0 -> IncomeGreen
                            else -> ExpenseRed
                        }
                        Text(
                            text = budStr,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = budColor,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailySpendingSection(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isBangla = language == AppLanguage.BANGLA
    val dailyData = remember(transactions, language) {
        val cal = java.util.Calendar.getInstance()
        val now = cal.timeInMillis

        // Today start and end
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val todayEnd = cal.timeInMillis

        // Yesterday start and end
        val yesterdayCal = java.util.Calendar.getInstance()
        yesterdayCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        yesterdayCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        yesterdayCal.set(java.util.Calendar.MINUTE, 0)
        yesterdayCal.set(java.util.Calendar.SECOND, 0)
        yesterdayCal.set(java.util.Calendar.MILLISECOND, 0)
        val yesterdayStart = yesterdayCal.timeInMillis

        yesterdayCal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        yesterdayCal.set(java.util.Calendar.MINUTE, 59)
        yesterdayCal.set(java.util.Calendar.SECOND, 59)
        yesterdayCal.set(java.util.Calendar.MILLISECOND, 999)
        val yesterdayEnd = yesterdayCal.timeInMillis

        val expenseTx = transactions.filter {
            !InsightsCalculator.isTransfer(it) && InsightsCalculator.isExpense(it)
        }

        val todaySpent = expenseTx.filter { it.timestamp in todayStart..todayEnd }.sumOf { it.amount }
        val yesterdaySpent = expenseTx.filter { it.timestamp in yesterdayStart..yesterdayEnd }.sumOf { it.amount }

        // Past 7 days history
        val dayFormat = java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.US)
        val bnDayNames = mapOf(
            "Sun" to "রবি", "Mon" to "সোম", "Tue" to "মঙ্গল",
            "Wed" to "বুধ", "Thu" to "বৃহঃ", "Fri" to "শুক্র", "Sat" to "শনি"
        )

        data class DayRecord(val label: String, val amount: Double, val isToday: Boolean)
        val list = mutableListOf<DayRecord>()

        for (i in 0 until 7) {
            val c = java.util.Calendar.getInstance()
            c.add(java.util.Calendar.DAY_OF_YEAR, -i)
            c.set(java.util.Calendar.HOUR_OF_DAY, 0)
            c.set(java.util.Calendar.MINUTE, 0)
            c.set(java.util.Calendar.SECOND, 0)
            c.set(java.util.Calendar.MILLISECOND, 0)
            val s = c.timeInMillis

            c.set(java.util.Calendar.HOUR_OF_DAY, 23)
            c.set(java.util.Calendar.MINUTE, 59)
            c.set(java.util.Calendar.SECOND, 59)
            c.set(java.util.Calendar.MILLISECOND, 999)
            val e = c.timeInMillis

            val amt = expenseTx.filter { it.timestamp in s..e }.sumOf { it.amount }
            val rawDay = dayFormat.format(java.util.Date(s))
            val label = if (i == 0) {
                if (isBangla) "আজ" else "Today"
            } else if (i == 1) {
                if (isBangla) "গতকাল" else "Yesterday"
            } else {
                if (isBangla) {
                    var l = rawDay
                    bnDayNames.forEach { (en, bn) -> l = l.replace(en, bn) }
                    CurrencyFormatter.convertToBanglaDigits(l)
                } else rawDay
            }
            list.add(DayRecord(label, amt, i == 0))
        }

        val maxDaily = list.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
        val avgDaily = list.sumOf { it.amount } / 7.0

        Triple(todaySpent, yesterdaySpent, Triple(avgDaily, maxDaily, list))
    }

    val todaySpent = dailyData.first
    val yesterdaySpent = dailyData.second
    val avgDaily = dailyData.third.first
    val maxDaily = dailyData.third.second
    val daysList = dailyData.third.third

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("daily_spending_section_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isBangla) "দৈনিক ব্যয় বিশ্লেষণ" else "Daily Spending Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isBangla) "প্রতিদিনের বাস্তব খরচের হিসাব" else "Daily breakdown over the last 7 days",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isBangla) "দৈনিক" else "Daily",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Quick Stat Metric Boxes: Today, Yesterday, 7-Day Average
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Today
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ExpenseRed.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isBangla) "আজকের খরচ" else "Today",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(todaySpent, currencySymbol, language = language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                // Yesterday
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isBangla) "গতকাল" else "Yesterday",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(yesterdaySpent, currencySymbol, language = language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                // 7-day Average
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isBangla) "দৈনিক গড়" else "Daily Avg",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(avgDaily, currencySymbol, language = language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day-by-Day Bars
            Text(
                text = if (isBangla) "গত ৭ দিনের প্রতিদিনের খরচ" else "Past 7 Days History",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            daysList.forEach { day ->
                val ratio = if (maxDaily > 0) (day.amount / maxDaily).toFloat().coerceIn(0f, 1f) else 0f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day.label,
                        modifier = Modifier.width(80.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        if (ratio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (day.isToday) ExpenseRed else MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = CurrencyFormatter.format(day.amount, currencySymbol, language = language),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (day.amount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }
}

