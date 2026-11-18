package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.CategoryIcon
import com.example.ui.components.getIconForName
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.ui.util.DragDropListState
import com.example.ui.util.SixDotDragHandle
import com.example.ui.util.categoryDragHandle
import com.example.ui.util.reorderableDragHandle
import com.example.ui.util.rememberDragDropListState
import com.example.ui.screens.transaction.CategorySelectionSheetStyle
import com.example.ui.screens.transaction.DisableSheetForceDark
import com.example.ui.screens.transaction.SheetHeaderCircleActionButton

enum class CategorySortType {
    DEFAULT,
    CUSTOM,
    MOST_USED
}

private val BUILT_IN_CATEGORY_IDS = setOf(
    // Standard default categories
    "cat_food_dining", "cat_shopping", "cat_transport", "cat_rent", "cat_health_medical",
    "cat_entertainment", "cat_personal_care", "cat_groceries", "cat_travel", "cat_family",
    "cat_debt_repayment", "cat_other_expenses", "cat_salary", "cat_bonus", "cat_business",
    "cat_gifts_received", "cat_other_income", "cat_wallet_transfer", "cat_bank_transfer",
    "cat_other_transfer",
    // Previous built-ins for compatibility
    "cat_bike", "cat_essentials", "cat_eating_out", "cat_family_transfer", "cat_health",
    "cat_inayaa", "cat_special_occasion", "cat_education", "cat_financial", "cat_misc",
    "cat_transfer_fee", "cat_payable_debt_interest", "cat_lend_money",
    "cat_receivable_debt_interest", "cat_adjustment", "cat_internal_transfer",
    "cat_freelance", "cat_interest", "cat_fdr", "cat_provident", "cat_loan_rcv",
    "cat_money_rcv", "cat_other_inc"
)

private val COLOR_PALETTE_30 = listOf(
    "#FF4D4D", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
    "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
    "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
    "#FF5722", "#795548", "#9E9E9E", "#607D8B", "#212121",
    "#E2136E", "#8B3392", "#F26522", "#0284C7", "#0D9488",
    "#16A34A", "#D97706", "#DC2626", "#4F46E5", "#059669"
)

// Five stable, varied default colors across distinct color spectrums
private val STABLE_5_PREVIEW_COLORS = listOf("#FF4D4D", "#2196F3", "#10B981", "#FF9800", "#9C27B0")

private val ICON_CATALOG_80 = listOf(
    "restaurant", "shopping_bag", "directions_car", "home", "payments",
    "medical_services", "movie", "face", "shopping_cart", "flight",
    "family_restroom", "receipt_long", "category", "savings", "trending_up",
    "card_giftcard", "account_balance_wallet", "wallet", "account_balance", "swap_horiz",
    "groups", "percent", "pets", "fitness_center", "local_cafe",
    "wifi", "phone_android", "flatware", "fastfood", "local_gas_station",
    "local_parking", "directions_bus", "local_taxi", "cottage", "electric_bolt",
    "water_drop", "checkroom", "devices", "weekend", "palette",
    "child_care", "local_laundry_service", "healing", "business_center", "flight_takeoff",
    "hotel", "beach_access", "menu_book", "receipt", "sports_esports",
    "music_note", "apps", "stroller", "star", "update",
    "pie_chart", "directions_walk", "monetization_on", "directions_bike", "local_pizza",
    "favorite", "person", "storefront", "laptop_mac", "subscriptions",
    "spa", "build", "security", "volunteer_activism", "diamond",
    "redeem", "local_pharmacy", "lunch_dining", "delivery_dining", "work"
)

// Five stable, varied default icons across distinct categories
private val STABLE_5_PREVIEW_ICONS = listOf("restaurant", "shopping_bag", "directions_car", "home", "payments")

@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val subcategories by viewModel.subcategories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBangla = language == AppLanguage.BANGLA

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val navScopeKey = viewModel.rememberNavigationScopeKey(
        screenOverride = com.example.ui.SubScreen.CATEGORIES_MANAGEMENT
    )
    // Navigation sub-state
    var isEditOrAddMode by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }

    // List mode tab: 0 = Expense, 1 = Income, 2 = Transfer
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable(navScopeKey) { mutableIntStateOf(0) }
    var sortType by remember { mutableStateOf(CategorySortType.DEFAULT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val lazyListState = viewModel.rememberScreenLazyListState(
        subKey = "tab_$selectedTab",
        screenOverride = com.example.ui.SubScreen.CATEGORIES_MANAGEMENT
    )

    BackHandler {
        if (isEditOrAddMode) {
            isEditOrAddMode = false
            categoryToEdit = null
        } else {
            viewModel.closeSubScreen()
        }
    }

    if (isEditOrAddMode) {
        val editScrollState = viewModel.rememberScreenScrollState(
            subKey = "edit_${categoryToEdit?.id ?: "new"}",
            screenOverride = com.example.ui.SubScreen.CATEGORIES_MANAGEMENT
        )
        AddEditCategoryPanel(
            categoryToEdit = categoryToEdit,
            initialTypeIndex = selectedTab,
            language = language,
            isDark = isDark,
            allSubcategories = subcategories,
            scrollState = editScrollState,
            onBack = {
                isEditOrAddMode = false
                categoryToEdit = null
            },
            onSave = { name, nameBn, isIncome, isTransfer, isActive, iconName, colorHex, newSubnames ->
                if (categoryToEdit == null) {
                    viewModel.addCategory(
                        name = name,
                        nameBn = nameBn,
                        isIncome = isIncome,
                        isTransfer = isTransfer,
                        isActive = isActive,
                        iconName = iconName,
                        colorHex = colorHex,
                        initialSubcategories = newSubnames
                    )
                } else {
                    val updated = categoryToEdit!!.copy(
                        name = name,
                        nameBn = nameBn.ifBlank { categoryToEdit!!.nameBn },
                        isIncome = isIncome,
                        isTransfer = isTransfer,
                        isActive = isActive,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                    viewModel.updateCategory(updated)
                    if (newSubnames.isNotEmpty()) {
                        newSubnames.forEach { subName ->
                            viewModel.addSubcategory(categoryToEdit!!.id, subName, "")
                        }
                    }
                }
                isEditOrAddMode = false
                categoryToEdit = null
            },
            onAddSubcategory = { catId, subName ->
                viewModel.addSubcategory(catId, subName, "")
            },
            onDeleteSubcategory = { subId ->
                viewModel.deleteSubcategory(subId)
            },
            onDeleteCategory = { catId ->
                viewModel.deleteCategory(catId)
                isEditOrAddMode = false
                categoryToEdit = null
            }
        )
    } else {
        val expenseCount = categories.count { !it.isIncome && !it.isTransfer }
        val incomeCount = categories.count { it.isIncome && !it.isTransfer }
        val transferCount = categories.count { it.isTransfer }

        val activeList = remember(categories, selectedTab, sortType, transactions) {
            val filtered = when (selectedTab) {
                0 -> categories.filter { !it.isIncome && !it.isTransfer }
                1 -> categories.filter { it.isIncome && !it.isTransfer }
                else -> categories.filter { it.isTransfer }
            }
            when (sortType) {
                CategorySortType.DEFAULT, CategorySortType.CUSTOM -> filtered.sortedBy { it.sortOrder }
                CategorySortType.MOST_USED -> {
                    val usageMap = transactions.groupBy { it.category }.mapValues { it.value.size }
                    filtered.sortedByDescending { usageMap[it.name] ?: 0 }
                }
            }
        }

        var localList by remember { mutableStateOf(activeList) }
        val haptic = LocalHapticFeedback.current

        val dragDropState = rememberDragDropListState(
            lazyListState = lazyListState,
            indexOffset = 0,
            onMove = { fromIndex, toIndex ->
                if (fromIndex in localList.indices && toIndex in localList.indices && fromIndex != toIndex) {
                    val list = localList.toMutableList()
                    val item = list.removeAt(fromIndex)
                    list.add(toIndex, item)
                    localList = list
                    sortType = CategorySortType.CUSTOM
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onDragEnd = {
                viewModel.updateCategoriesOrder(localList)
            },
            onDragCancel = {
                localList = activeList
            },
            keyProvider = { idx ->
                if (idx in localList.indices) localList[idx].id else idx
            },
            liveReorder = true
        )

        LaunchedEffect(activeList) {
            if (!dragDropState.isDragging) {
                localList = activeList
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Row: Back button, Title "Categories", Sort button, Add button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Back Button
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF262B3D) else Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(42.dp),
                        onClick = { viewModel.closeSubScreen() }
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

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = if (isBangla) "ক্যাটাগরি" else "Categories",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Sort menu button
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF262B3D) else Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("categories_sort_button"),
                            onClick = { showSortMenu = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort Categories",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF1E2230) else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = if (isBangla) "ডিফল্ট ক্রম" else "Default order",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isBangla) "বিল্ট-ইন ক্রম" else "The built-in order",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (sortType == CategorySortType.DEFAULT) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    sortType = CategorySortType.DEFAULT
                                    showSortMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = if (isBangla) "কাস্টম ক্রম" else "Custom order",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isBangla) "হ্যান্ডেল ধরে সাজানো" else "Custom reordered positions",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (sortType == CategorySortType.CUSTOM) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    sortType = CategorySortType.CUSTOM
                                    showSortMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = if (isBangla) "সবচেয়ে বেশি ব্যবহৃত" else "Most used first",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isBangla) "ব্যবহারের ফ্রিকোয়েন্সি অনুযায়ী" else "Sorted by how often you use them",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (sortType == CategorySortType.MOST_USED) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    sortType = CategorySortType.MOST_USED
                                    showSortMenu = false
                                }
                            )

                            HorizontalDivider(
                                color = if (isDark) Color(0x22FFFFFF) else Color(0x15000000),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        text = if (isBangla) "ডিফল্ট পুনরুদ্ধার করুন" else "Restore defaults",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showSortMenu = false
                                    showRestoreDialog = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Circular + Add button
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF262B3D) else Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("add_category_header_button"),
                        onClick = {
                            categoryToEdit = null
                            isEditOrAddMode = true
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Category",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Segmented Tabs Pill Bar: Expense · N | Income · N | Transfer · N
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isDark) Color(0xFF222638) else Color(0xFFEFF2F6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isExp = selectedTab == 0
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isExp) (if (isDark) Color(0xFF1E2230) else Color.White) else Color.Transparent,
                            shadowElevation = if (isExp) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(3.dp)
                                .fillMaxSize(),
                            onClick = { selectedTab = 0 }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isBangla) "খরচ · $expenseCount" else "Expense · $expenseCount",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isExp) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isExp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        val isInc = selectedTab == 1
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isInc) (if (isDark) Color(0xFF1E2230) else Color.White) else Color.Transparent,
                            shadowElevation = if (isInc) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(3.dp)
                                .fillMaxSize(),
                            onClick = { selectedTab = 1 }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isBangla) "আয় · $incomeCount" else "Income · $incomeCount",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isInc) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isInc) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        val isTrf = selectedTab == 2
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isTrf) (if (isDark) Color(0xFF1E2230) else Color.White) else Color.Transparent,
                            shadowElevation = if (isTrf) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(3.dp)
                                .fillMaxSize(),
                            onClick = { selectedTab = 2 }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isBangla) "ট্রান্সফার · $transferCount" else "Transfer · $transferCount",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isTrf) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isTrf) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Order Drag Notice Banner
                if (sortType != CategorySortType.MOST_USED) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SixDotDragHandle(
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp, 20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isBangla) "সাজাতে লং প্রেস করে ড্র্যাগ করুন" else "Long press and drag to reorder",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Category List Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isDark) Color(0xFF1E2230) else Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0x15000000)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (localList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBangla) "কোন ক্যাটাগরি পাওয়া যায়নি। + চাপুন" else "No categories found. Tap + to add.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("categories_lazy_column"),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            itemsIndexed(localList, key = { _, cat -> cat.id }) { index, cat ->
                                val isBuiltIn = cat.id in BUILT_IN_CATEGORY_IDS || !cat.id.startsWith("cat_")
                                val isBeingDragged = dragDropState.isDragging && dragDropState.draggedItemId == cat.id

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (!isBeingDragged) {
                                                Modifier.animateItem()
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .zIndex(if (isBeingDragged) 10f else 1f)
                                ) {
                                    // Display insertion drop target placeholder behind the floating dragged item
                                    if (isBeingDragged) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isDark) Color(0xFF312E81).copy(alpha = 0.35f) else Color(0xFFEEF2FF).copy(alpha = 0.75f),
                                            border = BorderStroke(
                                                width = 1.5.dp,
                                                color = if (isDark) Color(0xFF818CF8).copy(alpha = 0.60f) else Color(0xFF6366F1).copy(alpha = 0.50f)
                                            ),
                                            modifier = Modifier
                                                .matchParentSize()
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = if (isBangla) "এখানে ড্রপ করুন" else "Drop category here",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Visibly lift selected row using elevation, subtle shadow, and highlighted border
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDark) Color(0xFF1E2230) else Color.White,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        shadowElevation = if (isBeingDragged) 8.dp else 0.dp,
                                        border = if (isBeingDragged) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                if (isBeingDragged) {
                                                    translationY = dragDropState.translationY
                                                    scaleX = 1.01f
                                                    scaleY = 1.01f
                                                }
                                            }
                                            .padding(horizontal = 4.dp, vertical = if (isBeingDragged) 2.dp else 0.dp)
                                    ) {
                                        CategoryListItemRow(
                                            category = cat,
                                            language = language,
                                            isBuiltIn = isBuiltIn,
                                            isDark = isDark,
                                            index = index,
                                            listSize = localList.size,
                                            dragDropState = dragDropState,
                                            isBeingDragged = isBeingDragged,
                                            haptic = haptic,
                                            onClick = {
                                                if (!dragDropState.isDragging) {
                                                    categoryToEdit = cat
                                                    isEditOrAddMode = true
                                                }
                                            }
                                        )
                                    }
                                }

                                if (!isBeingDragged && index < localList.lastIndex) {
                                    HorizontalDivider(
                                        color = if (isDark) Color(0x15FFFFFF) else Color(0x0C000000),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Restore Defaults Confirmation Dialog
        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
                title = {
                    Text(
                        text = if (isBangla) "ডিফল্ট ক্যাটাগরি পুনরুদ্ধার করবেন?" else "Restore Default Categories?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isBangla)
                            "এটি ক্যাটাগরিগুলোকে প্রমিত ডিফল্ট ক্রমে ফিরিয়ে আনবে। আপনার বিদ্যমান লেনদেন অক্ষত থাকবে।"
                        else
                            "This will reset categories to the standard built-in defaults. Your existing transactions will remain safe.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.restoreDefaultCategories()
                            sortType = CategorySortType.DEFAULT
                            showRestoreDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (isBangla) "পুনরুদ্ধার করুন" else "Restore", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text(if (isBangla) "বাতিল" else "Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    }
}

/**
 * Single Row in the Category List Screen.
 * Requirements:
 * - Start dragging only after a long press on a category row; normal taps open edit category.
 * - Visually attached to finger without jumps or flickering (matching Customize Home).
 * - Preserves theme-aware colors, icons, and text in both light and dark mode.
 * - Right-aligned drag handle with haptic feedback and primary tint when active.
 */
@Composable
private fun CategoryListItemRow(
    category: CategoryEntity,
    language: AppLanguage,
    isBuiltIn: Boolean,
    isDark: Boolean,
    index: Int,
    listSize: Int,
    dragDropState: DragDropListState,
    isBeingDragged: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isBangla = language == AppLanguage.BANGLA
    val displayName = category.getDisplayName(language)
    val subtitle = if (isBuiltIn) {
        if (isBangla) "বিল্ট-ইন" else "Built-in"
    } else {
        if (isBangla) "আপনার ক্যাটাগরি" else "Your category"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !dragDropState.isDragging, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category Icon Badge
        CategoryIcon(
            iconName = category.iconName,
            colorHex = category.colorHex,
            size = 44.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Name & Subtitle
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right-aligned Six-Dot Drag Handle (⠿)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isBeingDragged) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
            modifier = Modifier
                .size(48.dp)
                .testTag("drag_handle_$index")
                .reorderableDragHandle(
                    key = category.id,
                    index = index,
                    listSize = listSize,
                    state = dragDropState,
                    onLongPressFeedback = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                SixDotDragHandle(
                    tint = if (isBeingDragged) {
                        MaterialTheme.colorScheme.primary
                    } else if (isDark) {
                        Color(0xFFA0AEC0)
                    } else {
                        Color(0xFF64748B)
                    }
                )
            }
        }
    }
}

/**
 * Add / Edit Category Panel.
 * Requirements:
 * - Reduced back-arrow button size while maintaining effortless tap target.
 * - Category icon displayed in a compact colored box on the left, with name and type on the right.
 * - Removed excessive blank space.
 * - "Category name" label placed outside and above the input box; editable name inside.
 * - 5 varied colors in compact preview row; stable deterministic selection; "Show all" dedicated bottom sheet.
 * - 5 varied icons in compact preview row; "Show all" dedicated bottom sheet in scrollable grid.
 * - Color and icon bottom sheets are completely independent.
 * - Preserves category editing, validation, subcategory management, and saving behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryPanel(
    categoryToEdit: CategoryEntity?,
    initialTypeIndex: Int,
    language: AppLanguage,
    isDark: Boolean,
    allSubcategories: List<SubcategoryEntity>,
    onBack: () -> Unit,
    onSave: (
        name: String,
        nameBn: String,
        isIncome: Boolean,
        isTransfer: Boolean,
        isActive: Boolean,
        iconName: String,
        colorHex: String,
        newSubnames: List<String>
    ) -> Unit,
    onAddSubcategory: (categoryId: String, name: String) -> Unit,
    onDeleteSubcategory: (subId: String) -> Unit,
    onDeleteCategory: ((String) -> Unit)? = null,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()
) {
    BackHandler(onBack = onBack)
    val isBangla = language == AppLanguage.BANGLA
    val isEditing = categoryToEdit != null
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(categoryToEdit?.name ?: "") }
    var nameBn by remember { mutableStateOf(categoryToEdit?.nameBn ?: "") }

    // Type selection: 0 = Income, 1 = Expense, 2 = Transfer
    var selectedTypeIndex by remember {
        mutableIntStateOf(
            when {
                categoryToEdit?.isTransfer == true -> 2
                categoryToEdit?.isIncome == true -> 0
                categoryToEdit != null -> 1
                initialTypeIndex == 1 -> 0
                initialTypeIndex == 2 -> 2
                else -> 1
            }
        )
    }

    var isActive by remember { mutableStateOf(categoryToEdit?.isActive ?: true) }
    var selectedColor by remember { mutableStateOf(categoryToEdit?.colorHex ?: "#FF4D4D") }
    var selectedIcon by remember { mutableStateOf(categoryToEdit?.iconName ?: "restaurant") }

    // Independent bottom sheets for color and icon selectors
    var showColorBottomSheet by remember { mutableStateOf(false) }
    var showIconBottomSheet by remember { mutableStateOf(false) }

    // Subcategories for this category if editing
    val existingSubs = remember(categoryToEdit, allSubcategories) {
        if (categoryToEdit != null) {
            allSubcategories.filter { it.categoryId == categoryToEdit.id }
        } else {
            emptyList()
        }
    }
    var pendingSubcategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var newSubcategoryInput by remember { mutableStateOf("") }

    val parsedColor = try {
        Color(android.graphics.Color.parseColor(selectedColor))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val typeSubtitle = when (selectedTypeIndex) {
        0 -> if (isBangla) "আয়ের ক্যাটাগরি" else "Income Category"
        2 -> if (isBangla) "ট্রান্সফার ক্যাটাগরি" else "Transfer Category"
        else -> if (isBangla) "খরচের ক্যাটাগরি" else "Expense Category"
    }

    // Stable 5-color preview: always contains selectedColor + deterministic 4 varied colors
    val previewColors = remember(selectedColor) {
        val normSelected = selectedColor.uppercase()
        val stableNorm = STABLE_5_PREVIEW_COLORS.map { it.uppercase() }
        if (stableNorm.contains(normSelected)) {
            STABLE_5_PREVIEW_COLORS
        } else {
            STABLE_5_PREVIEW_COLORS.take(4) + selectedColor
        }
    }

    // Stable 5-icon preview: always contains selectedIcon + deterministic 4 varied icons
    val previewIcons = remember(selectedIcon) {
        val normSelected = selectedIcon.lowercase().trim()
        val stableNorm = STABLE_5_PREVIEW_ICONS.map { it.lowercase().trim() }
        if (stableNorm.contains(normSelected)) {
            STABLE_5_PREVIEW_ICONS
        } else {
            STABLE_5_PREVIEW_ICONS.take(4) + selectedIcon
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                color = if (isDark) Color(0xFF161922) else Color(0xFFF8FAFC),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val allSubsToSave = if (categoryToEdit == null) {
                                if (newSubcategoryInput.isNotBlank()) {
                                    pendingSubcategories + newSubcategoryInput.trim()
                                } else {
                                    pendingSubcategories
                                }
                            } else {
                                if (newSubcategoryInput.isNotBlank()) {
                                    listOf(newSubcategoryInput.trim())
                                } else {
                                    emptyList()
                                }
                            }
                            onSave(
                                name.trim(),
                                nameBn.trim(),
                                selectedTypeIndex == 0,
                                selectedTypeIndex == 2,
                                isActive,
                                selectedIcon,
                                selectedColor,
                                allSubsToSave
                            )
                        }
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB),
                        disabledContainerColor = if (isDark) Color(0xFF262B3D) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_category_button")
                ) {
                    Text(
                        text = if (isEditing) {
                            if (isBangla) "ক্যাটাগরি সংরক্ষণ করুন" else "Save category"
                        } else {
                            if (isBangla) "ক্যাটাগরি তৈরি করুন" else "Create category"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 12.dp)
        ) {
            // Header Row: Compact Back Button + Title + Top Delete Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reduced back-arrow button size (36dp visual) with 48dp effortless touch area
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack)
                        .testTag("category_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF262B3D) else Color.White,
                        shadowElevation = 1.5.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = if (isEditing) {
                        if (isBangla) "ক্যাটাগরি সম্পাদনা" else "Edit category"
                    } else {
                        if (isBangla) "নতুন ক্যাটাগরি" else "New category"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (isEditing && categoryToEdit != null && onDeleteCategory != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { showDeleteCategoryDialog = true }
                            .testTag("delete_category_top_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Category",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Compact Category Preview Box:
            // Compact colored box on the left, category name and type on the right, no excessive blank space
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1E2230) else Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0x15000000)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact colored box on the left
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(parsedColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForName(selectedIcon),
                            contentDescription = null,
                            tint = parsedColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Category name and type on the right
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (name.isBlank()) (if (isBangla) "ক্যাটাগরির নাম" else "Category Name") else name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = typeSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Type Selector: Income | Expense | Transfer
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) Color(0xFF222638) else Color(0xFFEFF2F6),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Income Pill
                    val isInc = selectedTypeIndex == 0
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isInc) (if (isDark) Color(0xFF1E2230) else Color.White) else Color.Transparent,
                        shadowElevation = if (isInc) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(3.dp)
                            .fillMaxSize(),
                        onClick = { selectedTypeIndex = 0 }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isInc) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "আয়" else "Income",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isInc) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isInc) (if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Expense Pill
                    val isExp = selectedTypeIndex == 1
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isExp) (if (isDark) Color(0xFF1E2230) else Color.White) else Color.Transparent,
                        shadowElevation = if (isExp) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(3.dp)
                            .fillMaxSize(),
                        onClick = { selectedTypeIndex = 1 }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isExp) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "খরচ" else "Expense",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isExp) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isExp) (if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Transfer Pill
                    val isTrf = selectedTypeIndex == 2
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isTrf) (if (isDark) Color(0xFF1E2230) else Color.White) else Color.Transparent,
                        shadowElevation = if (isTrf) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(3.dp)
                            .fillMaxSize(),
                        onClick = { selectedTypeIndex = 2 }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = if (isTrf) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "ট্রান্সফার" else "Transfer",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isTrf) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isTrf) (if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // "Category name" Label placed outside and above the input box
            Text(
                text = if (isBangla) "ক্যাটাগরির নাম" else "Category name",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )

            // Editable Category Name Input Box
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text(
                        text = if (isBangla) "যেমনঃ মুদি বাজার, জিম, রেস্তোরাঁ" else "E.g. Groceries, Gym, Dining",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0x20000000),
                    focusedContainerColor = if (isDark) Color(0xFF1E2230) else Color.White,
                    unfocusedContainerColor = if (isDark) Color(0xFF1E2230) else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_name_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Active Category Card with Switch
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) Color(0xFF1E2230) else Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0x15000000)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "সক্রিয় ক্যাটাগরি" else "Active category",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBangla) "নতুন লেনদেনের জন্য উপলব্ধ" else "Available for new transactions",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.scale(0.85f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4F46E5),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = if (isDark) Color(0xFF374151) else Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Selector Header with "Show all" action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBangla) "রং নির্বাচন করুন" else "Select color",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = if (isBangla) "সব দেখুন" else "Show all",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier
                        .clickable { showColorBottomSheet = true }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                        .testTag("show_all_colors_btn")
                )
            }

            // Compact horizontal color preview row (exactly 5 colors)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1E2230) else Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0x15000000)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    previewColors.forEach { hex ->
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        val col = Color(android.graphics.Color.parseColor(hex))

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Icon Selector Header with "Show all" action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBangla) "আইকন নির্বাচন করুন" else "Select icon",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = if (isBangla) "সব দেখুন" else "Show all",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier
                        .clickable { showIconBottomSheet = true }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                        .testTag("show_all_icons_btn")
                )
            }

            // Compact horizontal icon preview row (exactly 5 icons)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1E2230) else Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0x15000000)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    previewIcons.forEach { iconKey ->
                        val isSelected = selectedIcon.equals(iconKey, ignoreCase = true)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                if (isDark) Color(0xFF263354) else Color(0xFFEFF4FF)
                            } else {
                                if (isDark) Color(0xFF222638) else Color(0xFFF8FAFC)
                            },
                            border = if (isSelected) {
                                BorderStroke(1.5.dp, Color(0xFF3B82F6))
                            } else {
                                BorderStroke(1.dp, if (isDark) Color(0x15FFFFFF) else Color(0x0C000000))
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clickable { selectedIcon = iconKey }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getIconForName(iconKey),
                                    contentDescription = iconKey,
                                    tint = if (isSelected) {
                                        Color(0xFF3B82F6)
                                    } else {
                                        if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Subcategories Management Section (both New Category and Edit Category)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isBangla) "সাব-ক্যাটাগরি" else "Subcategories",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1E2230) else Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0x15000000)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isEditing && categoryToEdit != null) {
                        // Existing category subcategories
                        if (existingSubs.isEmpty()) {
                            Text(
                                text = if (isBangla) "কোন সাব-ক্যাটাগরি নেই" else "No subcategories yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            existingSubs.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF262B3D) else Color(0xFFF1F5F9))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = sub.getDisplayName(language),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    IconButton(
                                        onClick = { onDeleteSubcategory(sub.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // New category pending subcategories
                        if (pendingSubcategories.isEmpty()) {
                            Text(
                                text = if (isBangla) "কোন সাব-ক্যাটাগরি নেই" else "No subcategories yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            pendingSubcategories.forEachIndexed { index, subName ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF262B3D) else Color(0xFFF1F5F9))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = subName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            pendingSubcategories = pendingSubcategories.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add new subcategory field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubcategoryInput,
                            onValueChange = { newSubcategoryInput = it },
                            placeholder = {
                                Text(
                                    text = if (isBangla) "নতুন সাব-ক্যাটাগরি" else "New subcategory name",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0x20000000),
                                focusedContainerColor = if (isDark) Color(0xFF222638) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) Color(0xFF222638) else Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_subcategory_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newSubcategoryInput.isNotBlank()) {
                                    val trimmed = newSubcategoryInput.trim()
                                    if (isEditing && categoryToEdit != null) {
                                        onAddSubcategory(categoryToEdit.id, trimmed)
                                    } else {
                                        pendingSubcategories = pendingSubcategories + trimmed
                                    }
                                    newSubcategoryInput = ""
                                }
                            },
                            enabled = newSubcategoryInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB),
                                disabledContainerColor = if (isDark) Color(0xFF262B3D) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("add_subcategory_button")
                        ) {
                            Text(if (isBangla) "যোগ করুন" else "Add", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Dedicated Color Palette Modal Bottom Sheet
        if (showColorBottomSheet) {
            val colorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showColorBottomSheet = false },
                sheetState = colorSheetState,
                containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
                dragHandle = null
            ) {
                DisableSheetForceDark()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBangla) "সম্পূর্ণ রঙের প্যালেট" else "Select Color",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        SheetHeaderCircleActionButton(
                            onClick = { showColorBottomSheet = false },
                            icon = CategorySelectionSheetStyle.HighContrastCloseIcon,
                            contentDescription = "Close",
                            isDark = isDark,
                            testTag = "btn_close_category_color_sheet"
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    ) {
                        items(COLOR_PALETTE_30) { hex ->
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)
                            val col = Color(android.graphics.Color.parseColor(hex))

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedColor = hex
                                        showColorBottomSheet = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .border(3.dp, col, CircleShape)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 36.dp else 42.dp)
                                        .clip(CircleShape)
                                        .background(col),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dedicated Icon Catalog Modal Bottom Sheet
        if (showIconBottomSheet) {
            val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showIconBottomSheet = false },
                sheetState = iconSheetState,
                containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
                dragHandle = null
            ) {
                DisableSheetForceDark()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBangla) "সম্পূর্ণ আইকন ক্যাটালগ" else "Select Icon",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        SheetHeaderCircleActionButton(
                            onClick = { showIconBottomSheet = false },
                            icon = CategorySelectionSheetStyle.HighContrastCloseIcon,
                            contentDescription = "Close",
                            isDark = isDark,
                            testTag = "btn_close_category_icon_sheet"
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                    ) {
                        items(ICON_CATALOG_80) { iconKey ->
                            val isSelected = selectedIcon.equals(iconKey, ignoreCase = true)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) {
                                    if (isDark) Color(0xFF263354) else Color(0xFFEFF4FF)
                                } else {
                                    if (isDark) Color(0xFF222638) else Color(0xFFF8FAFC)
                                },
                                border = if (isSelected) {
                                    BorderStroke(2.dp, Color(0xFF3B82F6))
                                } else {
                                    BorderStroke(1.dp, if (isDark) Color(0x15FFFFFF) else Color(0x0C000000))
                                },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable {
                                        selectedIcon = iconKey
                                        showIconBottomSheet = false
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getIconForName(iconKey),
                                        contentDescription = iconKey,
                                        tint = if (isSelected) {
                                            Color(0xFF3B82F6)
                                        } else {
                                            if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                        },
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog for Edit Panel
        if (showDeleteCategoryDialog && categoryToEdit != null) {
            AlertDialog(
                onDismissRequest = { showDeleteCategoryDialog = false },
                containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
                title = {
                    Text(
                        text = if (isBangla) "ক্যাটাগরি মুছবেন?" else "Delete Category?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isBangla)
                            "আপনি কি নিশ্চিত যে \"${categoryToEdit.getDisplayName(language)}\" ক্যাটাগরিটি মুছে ফেলতে চান?"
                        else
                            "Are you sure you want to delete \"${categoryToEdit.getDisplayName(language)}\"?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteCategoryDialog = false
                            onDeleteCategory?.invoke(categoryToEdit.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (isBangla) "মুছুন" else "Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteCategoryDialog = false }) {
                        Text(if (isBangla) "বাতিল" else "Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    }
}
