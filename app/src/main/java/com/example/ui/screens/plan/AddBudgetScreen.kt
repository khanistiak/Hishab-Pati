package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.ui.MainViewModel
import com.example.ui.components.CategoryIcon
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.components.getIconForName
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter
import com.example.ui.screens.transaction.CategorySelectionSheetStyle
import com.example.ui.screens.transaction.DisableSheetForceDark
import com.example.ui.screens.transaction.SheetHeaderCircleActionButton
import androidx.compose.foundation.lazy.rememberLazyListState
import java.util.Locale

// 30 rich color palette choices
private val FULL_COLOR_PALETTE = listOf(
    "#10B981", "#3B82F6", "#F59E0B", "#EF4444", "#8B5CF6",
    "#EC4899", "#06B6D4", "#6366F1", "#14B8A6", "#84CC16",
    "#F97316", "#D946EF", "#64748B", "#0F172A", "#0284C7",
    "#059669", "#7C3AED", "#DB2777", "#2563EB", "#CA8A04",
    "#E11D48", "#9333EA", "#0D9488", "#475569", "#4F46E5",
    "#16A34A", "#EA580C", "#C026D3", "#0891B2", "#4B5563"
)

// Stable, diverse preview color swatches spanning the spectrum with zero adjacent hue overlap.
// Deterministic order so it never reshuffles on recomposition or app reload.
private val STABLE_DETERMINISTIC_PREVIEW_COLORS = listOf(
    "#10B981", // Emerald Green
    "#3B82F6", // Royal Blue
    "#F59E0B", // Amber Orange
    "#EF4444", // Crimson Red
    "#8B5CF6", // Lavender Purple
    "#EC4899", // Vivid Pink
    "#06B6D4", // Ocean Cyan
    "#84CC16", // Lime Green
    "#F97316", // Tangerine Orange
    "#6366F1", // Indigo
    "#14B8A6", // Teal
    "#D946EF", // Fuchsia
    "#64748B", // Slate Grey
    "#E11D48"  // Rose Red
)

// Expanded catalog of relevant financial and everyday budgeting icons
private val FULL_ICON_CATALOG = listOf(
    // 1. Wallet and cash
    "wallet", "payments", "local_atm", "paid",
    // 2. Bank and credit card
    "account_balance", "credit_card",
    // 3. Savings and piggy bank
    "savings",
    // 4. Investment and growth
    "trending_up", "monetization_on",
    // 5. Shopping and groceries
    "shopping_cart", "shopping_bag", "storefront",
    // 6. Food and dining
    "restaurant", "fastfood", "local_cafe", "local_pizza",
    // 7. Transportation and fuel
    "directions_car", "local_gas_station", "directions_bus", "directions_bike", "local_taxi",
    // 8. Healthcare and pharmacy
    "medical_services", "local_pharmacy", "healing",
    // 9. Education and books
    "school", "menu_book",
    // 10. Rent and home
    "home", "cottage", "weekend",
    // 11. Electricity and utilities
    "electric_bolt", "water_drop", "lightbulb",
    // 12. Internet and mobile bills
    "wifi", "phone_android", "devices",
    // 13. Travel and holidays
    "flight", "hotel", "beach_access",
    // 14. Entertainment and gaming
    "movie", "sports_esports", "music_note",
    // 15. Subscriptions
    "subscriptions",
    // 16. Insurance
    "security",
    // 17. Gifts and donations
    "card_giftcard", "volunteer_activism",
    // 18. Pets and childcare
    "pets", "child_care",
    // 19. Personal care
    "spa", "fitness_center", "checkroom",
    // 20. Debt repayment
    "handshake", "receipt_long",
    // 21. General & Miscellaneous
    "build", "favorite", "category"
)

// Compact, varied horizontal preview icons representing key budgeting needs
private val STABLE_PREVIEW_ICONS = listOf(
    "wallet", "shopping_cart", "restaurant", "directions_car", "home",
    "electric_bolt", "medical_services", "school", "flight", "savings",
    "subscriptions", "sports_esports", "credit_card", "handshake"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    viewModel: MainViewModel,
    initialBudget: BudgetEntity? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val categories by viewModel.categories.collectAsState()
    val subcategories by viewModel.subcategories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val budgets by viewModel.budgets.collectAsState()

    val currencySymbol = settings?.currencySymbol ?: "৳"

    // Reference month
    val (initialYear, initialMonth) = remember(transactions, budgets) {
        com.example.data.backup.FinancialIntegrityValidator.resolveReferenceYearMonth(transactions, budgets)
    }
    val currentMonthKey = remember(initialYear, initialMonth) {
        String.format(Locale.US, "%04d-%02d", initialYear, initialMonth + 1)
    }

    // Expense categories
    val expenseCategories = remember(categories) {
        categories.filter { !it.isIncome && !it.isTransfer }
    }

    // Form state initialized from initialBudget if editing
    var budgetName by remember {
        mutableStateOf(initialBudget?.name ?: initialBudget?.categoryName ?: "")
    }

    var limitStr by remember {
        mutableStateOf(
            if (initialBudget != null && initialBudget.monthlyLimit > 0) {
                initialBudget.monthlyLimit.toLong().toString()
            } else ""
        )
    }

    var isOverall by remember {
        mutableStateOf(initialBudget?.isOverall ?: false)
    }

    var selectedCategory by remember {
        mutableStateOf(
            if (initialBudget?.isOverall == true) "Overall"
            else initialBudget?.categoryName ?: expenseCategories.firstOrNull()?.name ?: "Daily Essentials"
        )
    }

    var enableSubcategories by remember {
        mutableStateOf(initialBudget?.subcategoryName?.isNotBlank() == true)
    }

    var selectedSubcategories by remember {
        mutableStateOf(
            if (initialBudget?.subcategoryName?.isNotBlank() == true) {
                listOf(initialBudget.subcategoryName)
            } else emptyList()
        )
    }

    var isAlertEnabled by remember {
        mutableStateOf(initialBudget?.alertPercentage != null && (initialBudget.alertPercentage > 0))
    }

    var alertPercentage by remember {
        mutableFloatStateOf(initialBudget?.alertPercentage?.toFloat() ?: 80f)
    }

    var selectedColorHex by remember {
        mutableStateOf(
            initialBudget?.colorHex?.ifBlank { null }
                ?: expenseCategories.find { it.name == selectedCategory }?.colorHex
                ?: "#3B82F6"
        )
    }

    var selectedIconName by remember {
        mutableStateOf(
            initialBudget?.iconName?.ifBlank { null }
                ?: expenseCategories.find { it.name == selectedCategory }?.iconName
                ?: "wallet"
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }
    var showIconSheet by remember { mutableStateOf(false) }

    val limitAmount = limitStr.toDoubleOrNull() ?: 0.0

    // Compute live spending for the preview card
    val currentSpentForSelection = remember(transactions, selectedCategory, selectedSubcategories, isOverall, currentMonthKey) {
        transactions.filter { tx ->
            val txCal = java.util.Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val txMonthKey = String.format(Locale.US, "%04d-%02d", txCal.get(java.util.Calendar.YEAR), txCal.get(java.util.Calendar.MONTH) + 1)
            val monthMatch = txMonthKey == currentMonthKey
            val eligible = com.example.data.backup.FinancialIntegrityValidator.isEligibleExpense(tx)

            if (!monthMatch || !eligible) false
            else if (isOverall) true
            else if (enableSubcategories && selectedSubcategories.isNotEmpty()) {
                tx.category == selectedCategory && selectedSubcategories.contains(tx.subcategory)
            } else {
                tx.category == selectedCategory
            }
        }.sumOf { it.amount }
    }

    val usagePercent = if (limitAmount > 0.0) {
        ((currentSpentForSelection / limitAmount) * 100.0).coerceIn(0.0, 999.0)
    } else 0.0

    val backgroundBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFFE0F2FE), // Pastel cyan
                Color(0xFFEDE9FE), // Pastel lavender
                Color(0xFFF8FAFC)  // Clean soft slate
            )
        )
    }

    val previewColors = remember(selectedColorHex) {
        if (STABLE_DETERMINISTIC_PREVIEW_COLORS.any { it.equals(selectedColorHex, ignoreCase = true) }) {
            STABLE_DETERMINISTIC_PREVIEW_COLORS
        } else {
            listOf(selectedColorHex) + STABLE_DETERMINISTIC_PREVIEW_COLORS
        }
    }

    val previewIcons = remember(selectedIconName) {
        if (STABLE_PREVIEW_ICONS.contains(selectedIconName)) {
            STABLE_PREVIEW_ICONS
        } else {
            listOf(selectedIconName) + STABLE_PREVIEW_ICONS
        }
    }

    val listState = viewModel.rememberScreenLazyListState(
        subKey = initialBudget?.id ?: "new",
        screenOverride = com.example.ui.SubScreen.ADD_BUDGET
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 4.dp,
        bottomNavClearance = 88.dp
    )

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
                .testTag("add_budget_scrollable_content"),
            contentPadding = scrollLayout.contentPadding
        ) {
            item(key = "add_budget_form_content") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // Live Preview Card
                val previewTitle = budgetName.ifBlank {
                    if (isOverall) "Overall Budget"
                    else if (enableSubcategories && selectedSubcategories.isNotEmpty()) {
                        "$selectedCategory • ${selectedSubcategories.first()}"
                    } else {
                        selectedCategory.ifBlank { "Budget Name" }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_budget_preview_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Squircle icon container
                            val parsedCol = try {
                                Color(android.graphics.Color.parseColor(selectedColorHex))
                            } catch (_: Exception) {
                                Color(0xFF3B82F6)
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = parsedCol.copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getIconForName(selectedIconName),
                                        contentDescription = null,
                                        tint = parsedCol,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = previewTitle,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (limitAmount > 0) {
                                        CurrencyFormatter.format(limitAmount, currencySymbol)
                                    } else {
                                        "Set limit amount"
                                    },
                                    fontSize = 14.sp,
                                    color = if (limitAmount > 0) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                )
                            }

                            // Circular percentage indicator
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${usagePercent.toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (usagePercent > 100) ExpenseRed else Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Pill info banner below
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOverall) {
                                        "Overall Budget — Tracks all expense categories"
                                    } else if (enableSubcategories && selectedSubcategories.isNotEmpty()) {
                                        "Subcategory Budget — $selectedCategory (${selectedSubcategories.size} selected)"
                                    } else {
                                        "Category Budget — Tracks $selectedCategory expenses"
                                    },
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Error message if any
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = ExpenseRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                // 3. BUDGET NAME
                Text(
                    text = "BUDGET NAME",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 0.5.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "Name",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        BasicTextField(
                            value = budgetName,
                            onValueChange = {
                                budgetName = it
                                errorMessage = null
                            },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            ),
                            cursorBrush = SolidColor(Color(0xFF0284C7)),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("budget_name_input"),
                            decorationBox = { innerTextField ->
                                if (budgetName.isEmpty()) {
                                    Text(
                                        text = "e.g., Monthly Budget",
                                        fontSize = 16.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Suggestion chips
                val nameSuggestions = listOf("Shopping", "Daily essentials", "Subscriptions", "Dining Out", "Groceries")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    nameSuggestions.forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable {
                                budgetName = suggestion
                                errorMessage = null
                            }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. LIMIT AMOUNT
                Text(
                    text = "LIMIT AMOUNT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 0.5.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "Amount",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$currencySymbol ",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            BasicTextField(
                                value = limitStr,
                                onValueChange = { newVal ->
                                    val filtered = newVal.filter { it.isDigit() }
                                    limitStr = filtered
                                    errorMessage = null
                                },
                                textStyle = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                ),
                                cursorBrush = SolidColor(Color(0xFF0284C7)),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("budget_limit_input"),
                                decorationBox = { innerTextField ->
                                    if (limitStr.isEmpty()) {
                                        Text(
                                            text = "0",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick amount chips
                val quickAmounts = listOf(1000L, 2000L, 5000L, 10000L, 15000L, 20000L)
                val wholeNumberFormatter = remember { java.text.NumberFormat.getIntegerInstance(Locale.US) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickAmounts.forEach { amt ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE0F2FE),
                            modifier = Modifier.clickable {
                                limitStr = amt.toString()
                                errorMessage = null
                            }
                        ) {
                            Text(
                                text = "$currencySymbol${wholeNumberFormatter.format(amt)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5. Enable Subcategories Card (appears above Category selection)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 0.5.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Enable subcategories",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Fine tune your budget by choosing specific subcategories.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 18.sp
                            )
                        }

                        Switch(
                            checked = enableSubcategories,
                            onCheckedChange = { isChecked ->
                                enableSubcategories = isChecked
                                if (isChecked && isOverall) {
                                    isOverall = false
                                    if (selectedCategory.isBlank() || selectedCategory == "Overall") {
                                        selectedCategory = expenseCategories.firstOrNull()?.name ?: "Daily Essentials"
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF1E293B),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 6. CATEGORY
                Text(
                    text = "CATEGORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 0.5.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategorySheet = true }
                        .testTag("select_category_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Category",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val categoryDisplayText = if (isOverall) {
                                "Overall (All Expenses)"
                            } else if (enableSubcategories && selectedSubcategories.isNotEmpty()) {
                                "$selectedCategory (${selectedSubcategories.joinToString(", ")})"
                            } else {
                                selectedCategory.ifBlank { "Select Category" }
                            }
                            Text(
                                text = categoryDisplayText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Category",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 7. NOTIFICATIONS Section
                Text(
                    text = "NOTIFICATIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 0.5.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEDE9FE),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Budget alert",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Notify at ${alertPercentage.toInt()}% spent",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Switch(
                                checked = isAlertEnabled,
                                onCheckedChange = { isAlertEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3B82F6)
                                )
                            )
                        }

                        if (isAlertEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Slider(
                                value = alertPercentage,
                                onValueChange = { alertPercentage = it },
                                valueRange = 50f..100f,
                                steps = 4, // 50, 60, 70, 80, 90, 100
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF3B82F6),
                                    activeTrackColor = Color(0xFF3B82F6),
                                    inactiveTrackColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "50%", fontSize = 11.sp, color = Color(0xFF94A3B8))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "Alert at ${alertPercentage.toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Text(text = "100%", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 8. APPEARANCE Section
                Text(
                    text = "APPEARANCE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Color Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Color", fontSize = 13.sp, color = Color(0xFF64748B))
                    Text(
                        text = "Show all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0284C7),
                        modifier = Modifier
                            .clickable { showColorSheet = true }
                            .padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    previewColors.forEach { colHex ->
                        val isSelected = selectedColorHex.equals(colHex, ignoreCase = true)
                        val col = try {
                            Color(android.graphics.Color.parseColor(colHex))
                        } catch (_: Exception) {
                            Color(0xFF3B82F6)
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { selectedColorHex = colHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .border(2.5.dp, col, CircleShape)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 28.dp else 34.dp)
                                    .clip(CircleShape)
                                    .background(col)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Icon Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Icon", fontSize = 13.sp, color = Color(0xFF64748B))
                    Text(
                        text = "Show all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0284C7),
                        modifier = Modifier
                            .clickable { showIconSheet = true }
                            .padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    previewIcons.forEach { iconName ->
                        val isSelected = selectedIconName == iconName
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedIconName = iconName }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getIconForName(iconName),
                                    contentDescription = iconName,
                                    tint = if (isSelected) Color(0xFF3B82F6) else Color(0xFF475569),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Fixed scroll-aware frosted header matching Home and Plan tabs
    ScrollAwareFixedHeader(
        layoutState = scrollLayout,
        testTag = "add_budget_fixed_header"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("close_add_budget_button"),
                onClick = onBack
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = if (initialBudget != null) "Edit budget" else "Add budget",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }
    }

    // Fixed Save Button at the bottom
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = {
                    if (limitAmount <= 0) {
                        errorMessage = "Please enter a valid spending limit greater than 0."
                        return@Button
                    }

                    if (!isOverall && selectedCategory.isBlank()) {
                        errorMessage = "Please select a category."
                        return@Button
                    }

                    val finalCategoryName = if (isOverall) "Overall" else selectedCategory

                    // Check duplicate budget prevention
                    if (enableSubcategories && selectedSubcategories.isNotEmpty()) {
                        // Save budget for each selected subcategory or single subcategory
                        selectedSubcategories.forEach { subcat ->
                            val existing = budgets.firstOrNull {
                                it.categoryName == finalCategoryName &&
                                        it.subcategoryName == subcat &&
                                        it.monthYear == currentMonthKey &&
                                        it.id != initialBudget?.id
                            }
                            val targetId = existing?.id ?: initialBudget?.id

                            viewModel.saveBudget(
                                id = targetId,
                                categoryName = finalCategoryName,
                                limit = limitAmount,
                                monthYear = currentMonthKey,
                                isOverall = false,
                                rolloverEnabled = initialBudget?.rolloverEnabled ?: false,
                                name = budgetName.ifBlank { "$finalCategoryName • $subcat" },
                                subcategoryName = subcat,
                                iconName = selectedIconName,
                                colorHex = selectedColorHex,
                                alertPercentage = if (isAlertEnabled) alertPercentage.toInt() else 0
                            )
                        }
                    } else {
                        val existing = budgets.firstOrNull {
                            it.categoryName == finalCategoryName &&
                                    it.subcategoryName.isBlank() &&
                                    it.isOverall == isOverall &&
                                    it.monthYear == currentMonthKey &&
                                    it.id != initialBudget?.id
                        }
                        val targetId = existing?.id ?: initialBudget?.id

                        viewModel.saveBudget(
                            id = targetId,
                            categoryName = finalCategoryName,
                            limit = limitAmount,
                            monthYear = currentMonthKey,
                            isOverall = isOverall,
                            rolloverEnabled = initialBudget?.rolloverEnabled ?: false,
                            name = budgetName.ifBlank { if (isOverall) "Overall Budget" else finalCategoryName },
                            subcategoryName = "",
                            iconName = selectedIconName,
                            colorHex = selectedColorHex,
                            alertPercentage = if (isAlertEnabled) alertPercentage.toInt() else 0
                        )
                    }

                    onBack()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_budget_button")
            ) {
                Text(
                    text = if (initialBudget != null) "Update budget" else "Save budget",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

        // Category Selection Bottom Sheet
        if (showCategorySheet) {
            BudgetCategorySelectionSheet(
                categories = expenseCategories,
                allSubcategories = subcategories,
                currentCategory = selectedCategory,
                currentSubcategories = selectedSubcategories,
                isOverall = isOverall,
                enableSubcategories = enableSubcategories,
                onDismiss = { showCategorySheet = false },
                onApply = { chosenCategory, chosenSubcats, chosenIsOverall ->
                    selectedCategory = chosenCategory
                    selectedSubcategories = chosenSubcats
                    isOverall = chosenIsOverall
                    showCategorySheet = false

                    // Auto-sync appearance if not customized
                    val catObj = expenseCategories.find { it.name == chosenCategory }
                    if (catObj != null) {
                        selectedColorHex = catObj.colorHex
                        selectedIconName = catObj.iconName
                    }
                }
            )
        }

        // Full Color Palette Bottom Sheet
        if (showColorSheet) {
            val colorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showColorSheet = false },
                sheetState = colorSheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                DisableSheetForceDark()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Color",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        SheetHeaderCircleActionButton(
                            onClick = { showColorSheet = false },
                            icon = CategorySelectionSheetStyle.HighContrastCloseIcon,
                            contentDescription = "Close",
                            isDark = false,
                            testTag = "btn_close_budget_color_sheet"
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    ) {
                        items(FULL_COLOR_PALETTE) { hex ->
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                            val col = Color(android.graphics.Color.parseColor(hex))

                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedColorHex = hex
                                        showColorSheet = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .border(3.dp, col, CircleShape)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 34.dp else 40.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Icon Catalog Bottom Sheet
        if (showIconSheet) {
            val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showIconSheet = false },
                sheetState = iconSheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                DisableSheetForceDark()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Icon",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        SheetHeaderCircleActionButton(
                            onClick = { showIconSheet = false },
                            icon = CategorySelectionSheetStyle.HighContrastCloseIcon,
                            contentDescription = "Close",
                            isDark = false,
                            testTag = "btn_close_budget_icon_sheet"
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(FULL_ICON_CATALOG) { iconKey ->
                            val isSelected = selectedIconName == iconKey

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .size(52.dp)
                                    .clickable {
                                        selectedIconName = iconKey
                                        showIconSheet = false
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getIconForName(iconKey),
                                        contentDescription = iconKey,
                                        tint = if (isSelected) Color(0xFF0284C7) else Color(0xFF475569),
                                        modifier = Modifier.size(24.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetCategorySelectionSheet(
    categories: List<CategoryEntity>,
    allSubcategories: List<SubcategoryEntity>,
    currentCategory: String,
    currentSubcategories: List<String>,
    isOverall: Boolean,
    enableSubcategories: Boolean,
    onDismiss: () -> Unit,
    onApply: (category: String, subcategories: List<String>, isOverall: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    var tempIsOverall by remember { mutableStateOf(isOverall) }
    var tempCategory by remember { mutableStateOf(currentCategory) }
    var tempSubcategories by remember { mutableStateOf(currentSubcategories.toSet()) }

    var expandedCategoryId by remember {
        mutableStateOf<String?>(
            categories.find { it.name == currentCategory }?.id
        )
    }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.nameBn.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        DisableSheetForceDark()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Category",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                SheetHeaderCircleActionButton(
                    onClick = onDismiss,
                    icon = CategorySelectionSheetStyle.HighContrastCloseIcon,
                    contentDescription = "Close",
                    isDark = false,
                    testTag = "btn_close_budget_category_sheet"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search categories...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B))
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0284C7),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // List of Categories
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Overall Option (only when enableSubcategories is false)
                if (!enableSubcategories && searchQuery.isBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (tempIsOverall) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                if (tempIsOverall) 1.5.dp else 1.dp,
                                if (tempIsOverall) Color(0xFF0284C7) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempIsOverall = true
                                    tempCategory = "Overall"
                                    tempSubcategories = emptySet()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PieChart,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Overall (All Expenses)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Unified limit for all expenses",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                if (tempIsOverall) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                items(filteredCategories, key = { it.id }) { cat ->
                    val isCatSelected = !tempIsOverall && tempCategory == cat.name
                    val catSubcategories = allSubcategories.filter { it.categoryId == cat.id }
                    val isExpanded = expandedCategoryId == cat.id

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCatSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isCatSelected) 1.5.dp else 1.dp,
                            if (isCatSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tempIsOverall = false
                                        tempCategory = cat.name
                                        if (enableSubcategories && catSubcategories.isNotEmpty()) {
                                            expandedCategoryId = if (isExpanded) null else cat.id
                                        } else {
                                            tempSubcategories = emptySet()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIcon(
                                    iconName = cat.iconName,
                                    colorHex = cat.colorHex,
                                    size = 38.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (enableSubcategories && catSubcategories.isNotEmpty()) {
                                        Text(
                                            text = "${catSubcategories.size} subcategories",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                if (enableSubcategories && catSubcategories.isNotEmpty()) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                } else if (isCatSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Subcategories list if enabled
                            if (enableSubcategories && isExpanded && catSubcategories.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    catSubcategories.forEach { subcat ->
                                        val isSubSelected = isCatSelected && tempSubcategories.contains(subcat.name)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    tempIsOverall = false
                                                    tempCategory = cat.name
                                                    tempSubcategories = if (isSubSelected) {
                                                        tempSubcategories - subcat.name
                                                    } else {
                                                        tempSubcategories + subcat.name
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSubSelected,
                                                onCheckedChange = { checked ->
                                                    tempIsOverall = false
                                                    tempCategory = cat.name
                                                    tempSubcategories = if (checked) {
                                                        tempSubcategories + subcat.name
                                                    } else {
                                                        tempSubcategories - subcat.name
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF0284C7)
                                                ),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = subcat.name,
                                                fontSize = 14.sp,
                                                color = Color(0xFF334155),
                                                fontWeight = if (isSubSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Apply & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = Color(0xFF475569))
                }

                Button(
                    onClick = {
                        onApply(tempCategory, tempSubcategories.toList(), tempIsOverall)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
