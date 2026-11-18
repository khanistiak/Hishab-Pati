package com.example.ui.screens.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.util.CurrencyFormatter

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Notes
import com.example.data.local.entity.AccountEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CategoryDrillDownSection(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    categories: List<CategoryEntity>,
    subcategories: List<SubcategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    onViewTransactions: (category: String?, subcategory: String?) -> Unit,
    currencySymbol: String,
    language: AppLanguage,
    isIncomeMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var subcategoryForTransactions by remember { mutableStateOf<String?>(null) }

    val categoryItems = remember(transactions, dateRange, categories, isIncomeMode) {
        if (isIncomeMode) {
            InsightsCalculator.calculateCategoryIncomes(transactions, dateRange, categories)
        } else {
            InsightsCalculator.calculateCategoryExpenses(transactions, dateRange, categories)
        }
    }

    val totalAmount = categoryItems.sumOf { it.amount }

    TranslucentCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag(if (isIncomeMode) "category_wise_income_card" else "category_wise_expense_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AnimatedContent(
                targetState = selectedCategory,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "category_drill_down_transition"
            ) { activeCategory ->
                if (activeCategory == null) {
                    MainCategoryView(
                        categoryExpenses = categoryItems,
                        totalExpense = totalAmount,
                        currencySymbol = currencySymbol,
                        language = language,
                        isIncomeMode = isIncomeMode,
                        onCategoryClick = { catName -> onSelectCategory(catName) }
                    )
                } else {
                    val subcategoryItems = remember(transactions, dateRange, activeCategory, subcategories, isIncomeMode) {
                        if (isIncomeMode) {
                            InsightsCalculator.calculateSubcategoryIncomes(
                                transactions = transactions,
                                categoryName = activeCategory,
                                dateRange = dateRange,
                                subcategories = subcategories
                            )
                        } else {
                            InsightsCalculator.calculateSubcategoryExpenses(
                                transactions = transactions,
                                categoryName = activeCategory,
                                dateRange = dateRange,
                                subcategories = subcategories
                            )
                        }
                    }
                    val catTotal = subcategoryItems.sumOf { it.amount }
                    val activeCatMeta = categories.firstOrNull { it.name.equals(activeCategory, ignoreCase = true) }

                    SubcategoryDetailView(
                        categoryName = activeCategory,
                        categoryTotal = if (catTotal > 0) catTotal else (categoryItems.firstOrNull { it.categoryName == activeCategory }?.amount ?: 0.0),
                        subcategoriesList = subcategoryItems,
                        categoryMeta = activeCatMeta,
                        currencySymbol = currencySymbol,
                        language = language,
                        isIncomeMode = isIncomeMode,
                        onBackToAllCategories = {
                            subcategoryForTransactions = null
                            onSelectCategory(null)
                        },
                        onViewTransactions = { onViewTransactions(activeCategory, null) },
                        onSubcategoryClick = { subName ->
                            subcategoryForTransactions = subName
                        }
                    )
                }
            }
        }
    }

    // Modal dialog showing individual transactions for the clicked subcategory
    val activeSub = subcategoryForTransactions
    val activeCat = selectedCategory
    if (activeSub != null && activeCat != null) {
        val matchingTransactions = remember(transactions, dateRange, activeCat, activeSub, isIncomeMode) {
            transactions.filter { tx ->
                !InsightsCalculator.isTransfer(tx) &&
                        (if (isIncomeMode) InsightsCalculator.isIncome(tx) else InsightsCalculator.isExpense(tx)) &&
                        tx.timestamp in dateRange.startMillis..dateRange.endMillis &&
                        (tx.category.ifBlank { if (isIncomeMode) "Uncategorized Income" else "Uncategorized" }).equals(activeCat, ignoreCase = true) &&
                        (tx.subcategory.ifBlank { tx.payeeOrPayer.ifBlank { "Uncategorized" } }).equals(activeSub, ignoreCase = true)
            }.sortedByDescending { it.timestamp }
        }

        SubcategoryTransactionsDialog(
            categoryName = activeCat,
            subcategoryName = activeSub,
            transactions = matchingTransactions,
            accounts = accounts,
            currencySymbol = currencySymbol,
            language = language,
            isIncomeMode = isIncomeMode,
            onDismiss = { subcategoryForTransactions = null },
            onViewInActivity = {
                subcategoryForTransactions = null
                onViewTransactions(activeCat, activeSub)
            }
        )
    }
}

@Composable
private fun MainCategoryView(
    categoryExpenses: List<CategoryExpenseItem>,
    totalExpense: Double,
    currencySymbol: String,
    language: AppLanguage,
    isIncomeMode: Boolean = false,
    onCategoryClick: (String) -> Unit
) {
    val title = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "খাতভিত্তিক আয় বিশ্লেষণ" else "Category-wise Income Report"
    } else {
        if (language == AppLanguage.BANGLA) "খাতভিত্তিক ব্যয় বিশ্লেষণ" else "Category-wise Expense Report"
    }
    val subtitle = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "সর্বোচ্চ আয়ের ক্রমানুসারে সাজানো" else "Sorted by highest income"
    } else {
        if (language == AppLanguage.BANGLA) "সর্বোচ্চ ব্যয়ের ক্রমানুসারে সাজানো" else "Sorted by highest spending"
    }
    val centerLabel = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "মোট আয়" else "Total Income"
    } else {
        if (language == AppLanguage.BANGLA) "মোট ব্যয়" else "Total Spent"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
                shape = RoundedCornerShape(12.dp),
                color = if (isIncomeMode) IncomeGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "${categoryExpenses.size} ${if (language == AppLanguage.BANGLA) "খাত" else "Categories"}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (categoryExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isIncomeMode) {
                        if (language == AppLanguage.BANGLA) "এই সময়ের জন্য কোনো আয়ের তথ্য পাওয়া যায়নি" else "No income records found for this period"
                    } else {
                        if (language == AppLanguage.BANGLA) "এই সময়ের জন্য কোনো খরচ পাওয়া যায়নি" else "No expense records found for this period"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        categoryExpenses.forEach { item ->
                            val sweep = (item.percentage / 100f) * 360f
                            drawArc(
                                color = item.color,
                                startAngle = startAngle,
                                sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                useCenter = false,
                                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                            )
                            startAngle += sweep
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = centerLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = CurrencyFormatter.format(totalExpense, currencySymbol, language = language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categoryExpenses.forEach { item ->
                    CategoryExpenseRow(
                        item = item,
                        currencySymbol = currencySymbol,
                        language = language,
                        onClick = { onCategoryClick(item.categoryName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryExpenseRow(
    item: CategoryExpenseItem,
    currencySymbol: String,
    language: AppLanguage,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(item.color)
                    )
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
                        val txnText = if (language == AppLanguage.BANGLA) {
                            "${CurrencyFormatter.convertToBanglaDigits(item.count.toString())} লেনদেন"
                        } else {
                            "${item.count} transactions"
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
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp)
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

@Composable
private fun SubcategoryDetailView(
    categoryName: String,
    categoryTotal: Double,
    subcategoriesList: List<SubcategoryExpenseItem>,
    categoryMeta: CategoryEntity?,
    currencySymbol: String,
    language: AppLanguage,
    isIncomeMode: Boolean = false,
    onBackToAllCategories: () -> Unit,
    onViewTransactions: () -> Unit,
    onSubcategoryClick: (String) -> Unit
) {
    val totalLabel = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "মোট আয়" else "Category Total"
    } else {
        if (language == AppLanguage.BANGLA) "মোট ব্যয়" else "Category Total"
    }

    val btnLabel = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "এই খাতের আয় লেনদেন দেখুন" else "View Income Transactions"
    } else {
        if (language == AppLanguage.BANGLA) "এই খাতের লেনদেন দেখুন" else "View Transactions"
    }

    val breakdownHeader = if (isIncomeMode) {
        if (language == AppLanguage.BANGLA) "উপ-খাত বা আয়ের উৎস" else "Subcategory / Source Breakdown"
    } else {
        if (language == AppLanguage.BANGLA) "উপ-খাতভিত্তিক বিবরণ" else "Subcategory Breakdown"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToAllCategories,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to categories",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onBackToAllCategories,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.BANGLA) "সকল খাত" else "All Categories",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (subcategoriesList.isEmpty()) {
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            style = Stroke(width = 20.dp.toPx())
                        )
                    } else {
                        var startAngle = -90f
                        subcategoriesList.forEach { sub ->
                            val sweep = (sub.percentageOfCategory / 100f) * 360f
                            drawArc(
                                color = sub.color,
                                startAngle = startAngle,
                                sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                useCenter = false,
                                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                            )
                            startAngle += sweep
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = CurrencyFormatter.format(categoryTotal, currencySymbol, language = language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onViewTransactions,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("view_transactions_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.ListAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = btnLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = breakdownHeader,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (subcategoriesList.isEmpty()) {
            Text(
                text = if (language == AppLanguage.BANGLA) "কোন উপ-খাত পাওয়া যায়নি" else "No subcategories found for this category.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                subcategoriesList.forEach { sub ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSubcategoryClick(sub.subcategoryName) }
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
                                            .background(sub.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = sub.subcategoryName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        val txnText = if (language == AppLanguage.BANGLA) {
                                            "${CurrencyFormatter.convertToBanglaDigits(sub.count.toString())} লেনদেন"
                                        } else {
                                            "${sub.count} txns"
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

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.format(sub.amount, currencySymbol, language = language),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    val pct = "${CurrencyFormatter.format(sub.percentageOfCategory.toDouble(), "", language = language)}%"
                                    Text(
                                        text = pct,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (sub.percentageOfCategory / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = sub.color,
                                trackColor = sub.color.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubcategoryTransactionsDialog(
    categoryName: String,
    subcategoryName: String,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    currencySymbol: String,
    language: AppLanguage,
    isIncomeMode: Boolean,
    onDismiss: () -> Unit,
    onViewInActivity: () -> Unit
) {
    val totalAmount = remember(transactions) { transactions.sumOf { it.amount } }
    val timeFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subcategoryName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = (if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isIncomeMode) IncomeGreen else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val countStr = if (language == AppLanguage.BANGLA) {
                    "${CurrencyFormatter.convertToBanglaDigits(transactions.size.toString())} টি লেনদেন • ${CurrencyFormatter.format(totalAmount, currencySymbol, language = language)}"
                } else {
                    "${transactions.size} transactions • ${CurrencyFormatter.format(totalAmount, currencySymbol, language = language)}"
                }
                Text(
                    text = countStr,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                )
            }
        },
        text = {
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "কোনো লেনদেন পাওয়া যায়নি" else "No transactions found for this period",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        val accName = accounts.firstOrNull { it.id == tx.accountId }?.name ?: ""
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tx.title.ifBlank { subcategoryName },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(tx.amount, currencySymbol, language = language),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isIncomeMode) IncomeGreen else ExpenseRed
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val dateStr = timeFormat.format(Date(tx.timestamp))
                                    val formattedDate = if (language == AppLanguage.BANGLA) CurrencyFormatter.convertToBanglaDigits(dateStr) else dateStr
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )

                                    if (accName.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = accName,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            )
                                        }
                                    }
                                }

                                if (tx.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Notes,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = tx.notes,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onViewInActivity) {
                Text(
                    text = if (language == AppLanguage.BANGLA) "সব দেখুন" else "View in Activity",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (language == AppLanguage.BANGLA) "বন্ধ করুন" else "Close")
            }
        }
    )
}

