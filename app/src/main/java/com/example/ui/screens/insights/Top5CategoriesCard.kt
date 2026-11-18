package com.example.ui.screens.insights

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter

private data class DonutSegment(
    val name: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@Composable
fun Top5CategoriesCard(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage,
    isIncomeMode: Boolean = false,
    onCategoryClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allCategoryItems = remember(transactions, dateRange, categories, isIncomeMode) {
        if (isIncomeMode) {
            InsightsCalculator.calculateCategoryIncomes(transactions, dateRange, categories)
        } else {
            InsightsCalculator.calculateCategoryExpenses(transactions, dateRange, categories)
        }
    }

    val totalAmount = remember(allCategoryItems) {
        allCategoryItems.sumOf { it.amount }
    }

    val top5Items = remember(allCategoryItems) {
        allCategoryItems.take(5)
    }

    val donutSegments = remember(allCategoryItems, totalAmount) {
        if (totalAmount <= 0) emptyList()
        else {
            val segments = mutableListOf<DonutSegment>()
            val top5 = allCategoryItems.take(5)
            top5.forEach { item ->
                segments.add(
                    DonutSegment(
                        name = item.categoryName,
                        amount = item.amount,
                        percentage = item.percentage,
                        color = item.color
                    )
                )
            }
            if (allCategoryItems.size > 5) {
                val othersAmount = allCategoryItems.drop(5).sumOf { it.amount }
                val othersPct = ((othersAmount / totalAmount) * 100.0).toFloat()
                segments.add(
                    DonutSegment(
                        name = if (language == AppLanguage.BANGLA) "অন্যান্য" else "Others",
                        amount = othersAmount,
                        percentage = othersPct,
                        color = Color(0xFF94A3B8) // Slate 400
                    )
                )
            }
            segments
        }
    }

    val cardTitle = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "শীর্ষ ৫ আয়ের উৎস" else "Top 5 Income Sources"
    } else {
        if (language == AppLanguage.BANGLA) "শীর্ষ ৫ খরচের খাত" else "Top 5 Categories"
    }

    val cardSubtitle = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "আয়ের সামগ্রিক চিত্র ও বণ্টন" else "Unified income distribution analysis"
    } else {
        if (language == AppLanguage.BANGLA) "ব্যয়ের সামগ্রিক চিত্র ও বিশ্লেষণ" else "Unified spending analysis"
    }

    val centerLabel = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "মোট আয়" else "Total Income"
    } else {
        if (language == AppLanguage.BANGLA) "মোট ব্যয়" else "Total Spent"
    }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag(if (isIncomeMode) "top_5_income_sources_card" else "top_5_categories_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cardTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = cardSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (if (isIncomeMode) IncomeGreen else ExpenseRed).copy(alpha = 0.12f),
                    contentColor = if (isIncomeMode) IncomeGreen else ExpenseRed
                ) {
                    Text(
                        text = if (isIncomeMode) {
                            if (language == AppLanguage.BANGLA) "আয়" else "Income"
                        } else {
                            if (language == AppLanguage.BANGLA) "ব্যয়" else "Expense"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isIncomeMode) IncomeGreen else ExpenseRed
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (top5Items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isIncomeMode) {
                            if (language == AppLanguage.BANGLA) "এই সময়ের জন্য কোনো আয়ের তথ্য নেই" else "No income records found for this period"
                        } else {
                            if (language == AppLanguage.BANGLA) "এই সময়ের জন্য কোনো খরচের তথ্য নেই" else "No expense records found for this period"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            } else {
                // Donut Chart with Center Total
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            donutSegments.forEach { seg ->
                                val sweep = (seg.percentage / 100f) * 360f
                                drawArc(
                                    color = seg.color,
                                    startAngle = startAngle,
                                    sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                    useCenter = false,
                                    style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round)
                                )
                                startAngle += sweep
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = centerLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                text = CurrencyFormatter.format(totalAmount, currencySymbol, language = language),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Top 5 List with Rank Badges
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    top5Items.forEachIndexed { index, item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCategoryClick(item.categoryName) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Rank Badge
                                        Surface(
                                            shape = CircleShape,
                                            color = (if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                val rankText = if (language == AppLanguage.BANGLA) {
                                                    CurrencyFormatter.convertToBanglaDigits("#${index + 1}")
                                                } else {
                                                    "#${index + 1}"
                                                }
                                                Text(
                                                    text = rankText,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Category color dot
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(item.color)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

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
                                            val txnText = if (language == AppLanguage.BANGLA) {
                                                "${CurrencyFormatter.convertToBanglaDigits(item.count.toString())} লেনদেন"
                                            } else {
                                                "${item.count} txns"
                                            }
                                            Text(
                                                text = txnText,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = CurrencyFormatter.format(item.amount, currencySymbol, language = language),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            val pctText = "${CurrencyFormatter.format(item.percentage.toDouble(), "", language = language)}%"
                                            Text(
                                                text = pctText,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = "View Subcategories",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = item.color,
                                    trackColor = item.color.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // "View All Categories" Button
                OutlinedButton(
                    onClick = onViewAllClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("top_5_view_all_btn"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isIncomeMode) {
                            if (language == AppLanguage.BANGLA) "সকল আয়ের খাত দেখুন (${allCategoryItems.size})" else "View All Income Categories (${allCategoryItems.size})"
                        } else {
                            if (language == AppLanguage.BANGLA) "সকল খরচের খাত দেখুন (${allCategoryItems.size})" else "View All Categories (${allCategoryItems.size})"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
