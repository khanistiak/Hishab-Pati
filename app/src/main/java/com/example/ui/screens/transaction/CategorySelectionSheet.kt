package com.example.ui.screens.transaction

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIcon
import com.example.ui.util.CurrencyFormatter

/**
 * Visual styling tokens consistent with the Edit Budget category picker design.
 */
object CategorySelectionSheetStyle {
    val SHEET_CORNER_RADIUS = 24.dp
    val ROW_CORNER_RADIUS = 14.dp
    val SEARCH_CORNER_RADIUS = 14.dp

    val UNSELECTED_BORDER_WIDTH = 1.25.dp
    val SELECTED_BORDER_WIDTH = 2.0.dp
    val SEARCH_FOCUSED_BORDER_WIDTH = 1.6.dp
    val SUBCATEGORY_BULLET_SIZE = 8.dp

    // Design-system colors matching Edit Budget picker
    val LIGHT_CONTAINER_BG = Color.White
    val LIGHT_ROW_BG_UNSELECTED = Color(0xFFF8FAFC)
    val LIGHT_ROW_BG_SELECTED = Color(0xFFEFF6FF)
    val LIGHT_BORDER_UNSELECTED = Color(0xFFE2E8F0)

    val DARK_ROW_BG_UNSELECTED = Color(0xFF1E293B)
    val DARK_ROW_BG_SELECTED = Color(0xFF0F253E)
    val DARK_BORDER_UNSELECTED = Color(0xFF334155)

    val ACCENT_BLUE = Color(0xFF0284C7)

    val LIGHT_BUTTON_BG = Color(0xFFF1F5F9)
    val DARK_BUTTON_BG = Color(0xFF334155)

    // High-contrast tokens for circular header action buttons (+, ×, ←)
    val HEADER_ACTION_BUTTON_SIZE = 38.dp
    val HEADER_ACTION_ICON_SIZE = 20.dp
    val HEADER_ACTION_BORDER_WIDTH = 1.2.dp

    val LIGHT_HEADER_ACTION_BG = Color(0xFFE2E8F0)
    val DARK_HEADER_ACTION_BG = Color(0xFF334155)

    val LIGHT_HEADER_ACTION_BORDER = Color(0xFFCBD5E1)
    val DARK_HEADER_ACTION_BORDER = Color(0xFF64748B)

    val LIGHT_HEADER_ACTION_ICON = Color(0xFF0F172A)
    val DARK_HEADER_ACTION_ICON = Color(0xFFFFFFFF)

    val LIGHT_TEXT_PRIMARY = Color(0xFF0F172A)
    val DARK_TEXT_PRIMARY = Color(0xFFF8FAFC)

    val LIGHT_TEXT_SECONDARY = Color(0xFF64748B)
    val DARK_TEXT_SECONDARY = Color(0xFF94A3B8)

    val LIGHT_SUBCATEGORY_BULLET_PALETTE = listOf(
        Color(0xFF3B82F6), // Sky / Transfer Blue
        Color(0xFF10B981), // Emerald / Income Green
        Color(0xFFF59E0B), // Warm Amber
        Color(0xFF8B5CF6), // Soft Violet
        Color(0xFFEC4899), // Rose Pink
        Color(0xFF14B8A6), // Teal
        Color(0xFF6366F1), // Indigo
        Color(0xFFF97316)  // Coral Orange
    )

    val DARK_SUBCATEGORY_BULLET_PALETTE = listOf(
        Color(0xFF60A5FA), // Soft Blue
        Color(0xFF34D399), // Soft Emerald
        Color(0xFFFBBF24), // Soft Amber
        Color(0xFFA78BFA), // Soft Violet
        Color(0xFFF472B6), // Soft Rose
        Color(0xFF2DD4BF), // Soft Teal
        Color(0xFF818CF8), // Soft Indigo
        Color(0xFFFB923C)  // Soft Coral
    )

    fun subcategoryBulletColor(index: Int, isDark: Boolean): Color {
        val palette = if (isDark) DARK_SUBCATEGORY_BULLET_PALETTE else LIGHT_SUBCATEGORY_BULLET_PALETTE
        return palette[index.mod(palette.size)]
    }

    fun unselectedBorderColor(outline: Color, isDark: Boolean): Color {
        return outline.copy(alpha = if (isDark) 0.78f else 0.75f)
    }

    fun headerActionBackgroundColor(isDark: Boolean): Color {
        return if (isDark) DARK_HEADER_ACTION_BG else LIGHT_HEADER_ACTION_BG
    }

    fun headerActionBorderColor(isDark: Boolean): Color {
        return if (isDark) DARK_HEADER_ACTION_BORDER else LIGHT_HEADER_ACTION_BORDER
    }

    fun headerActionIconColor(isDark: Boolean): Color {
        return if (isDark) DARK_HEADER_ACTION_ICON else LIGHT_HEADER_ACTION_ICON
    }

    val HighContrastPlusIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "HighContrastPlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 5.5f)
                lineTo(12f, 18.5f)
                moveTo(5.5f, 12f)
                lineTo(18.5f, 12f)
            }
        }.build()
    }

    val HighContrastCloseIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "HighContrastClose",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 6.5f)
                lineTo(17.5f, 17.5f)
                moveTo(17.5f, 6.5f)
                lineTo(6.5f, 17.5f)
            }
        }.build()
    }
}

/**
 * Disables OEM / Android system Force-Dark on a ModalBottomSheet Dialog window
 * so circular action buttons and high-contrast icons are never inverted into their backgrounds.
 */
@Composable
fun DisableSheetForceDark() {
    val view = LocalView.current
    SideEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.isForceDarkAllowed = false
            view.rootView?.isForceDarkAllowed = false
        }
    }
}

/**
 * Shared high-contrast circular header button used for +, × (close), and ← (back) controls
 * in category-selection and wallet-selection bottom sheets.
 */
@Composable
fun SheetHeaderCircleActionButton(
    icon: ImageVector,
    contentDescription: String?,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val bgColor = CategorySelectionSheetStyle.headerActionBackgroundColor(isDark)
    val borderColor = CategorySelectionSheetStyle.headerActionBorderColor(isDark)
    val iconTint = CategorySelectionSheetStyle.headerActionIconColor(isDark)

    Box(
        modifier = modifier
            .size(CategorySelectionSheetStyle.HEADER_ACTION_BUTTON_SIZE)
            .clip(CircleShape)
            .background(color = bgColor, shape = CircleShape)
            .border(
                width = CategorySelectionSheetStyle.HEADER_ACTION_BORDER_WIDTH,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(CategorySelectionSheetStyle.HEADER_ACTION_ICON_SIZE)
        )
    }
}

/**
 * Clean, modern bottom sheet for Category & Subcategory selection,
 * designed in exact visual consistency with the Edit Budget category picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionBottomSheet(
    selectedCategoryName: String,
    selectedSubcategoryName: String,
    selectedType: TransactionType,
    categories: List<CategoryEntity>,
    subcategories: List<SubcategoryEntity>,
    language: AppLanguage = AppLanguage.ENGLISH,
    onTypeChanged: (TransactionType) -> Unit,
    onCategorySelected: (categoryName: String, subcategoryName: String) -> Unit,
    onAddNewCategory: () -> Unit,
    onDismiss: () -> Unit,
    newlyAddedCategoryName: String? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var drilledCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    val categoryListState = rememberLazyListState()

    val currentOnCategorySelected by rememberUpdatedState(onCategorySelected)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val commitSelection: (String, String) -> Unit = remember {
        { catName, subName ->
            currentOnCategorySelected(catName, subName)
            currentOnDismiss()
        }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val textPrimary = if (isDark) CategorySelectionSheetStyle.DARK_TEXT_PRIMARY else CategorySelectionSheetStyle.LIGHT_TEXT_PRIMARY
    val textSecondary = if (isDark) CategorySelectionSheetStyle.DARK_TEXT_SECONDARY else CategorySelectionSheetStyle.LIGHT_TEXT_SECONDARY
    val cardBgUnselected = if (isDark) CategorySelectionSheetStyle.DARK_ROW_BG_UNSELECTED else CategorySelectionSheetStyle.LIGHT_ROW_BG_UNSELECTED
    val cardBgSelected = if (isDark) CategorySelectionSheetStyle.DARK_ROW_BG_SELECTED else CategorySelectionSheetStyle.LIGHT_ROW_BG_SELECTED
    val borderUnselected = if (isDark) CategorySelectionSheetStyle.DARK_BORDER_UNSELECTED else CategorySelectionSheetStyle.LIGHT_BORDER_UNSELECTED
    val buttonBg = if (isDark) CategorySelectionSheetStyle.DARK_BUTTON_BG else CategorySelectionSheetStyle.LIGHT_BUTTON_BG
    val accentBlue = CategorySelectionSheetStyle.ACCENT_BLUE

    val searchInteractionSource = remember { MutableInteractionSource() }
    val isSearchFocused by searchInteractionSource.collectIsFocusedAsState()

    // Intercept back button to return from subcategory drill-down
    BackHandler(enabled = drilledCategory != null) {
        drilledCategory = null
    }

    val filteredList = remember(categories, selectedType, searchQuery) {
        val typeFiltered = when (selectedType) {
            TransactionType.INCOME -> categories.filter { it.isIncome && !it.isTransfer }
            TransactionType.TRANSFER -> categories.filter { it.isTransfer }
            else -> categories.filter { !it.isIncome && !it.isTransfer }
        }.filter { it.isActive }

        if (searchQuery.isBlank()) {
            typeFiltered
        } else {
            typeFiltered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.nameBn.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(newlyAddedCategoryName, filteredList.size) {
        if (!newlyAddedCategoryName.isNullOrBlank()) {
            val idx = filteredList.indexOfFirst { it.name.equals(newlyAddedCategoryName, ignoreCase = true) }
            if (idx >= 0) {
                categoryListState.scrollToItem(idx)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = CategorySelectionSheetStyle.SHEET_CORNER_RADIUS,
            topEnd = CategorySelectionSheetStyle.SHEET_CORNER_RADIUS
        ),
        containerColor = if (isDark) MaterialTheme.colorScheme.surface else CategorySelectionSheetStyle.LIGHT_CONTAINER_BG,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
            )
        }
    ) {
        DisableSheetForceDark()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (drilledCategory != null) {
                    // Screen 1: Subcategory Picker Header (Back Button + Category Title & "Select subcategory")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        SheetHeaderCircleActionButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (language == AppLanguage.BANGLA) "ক্যাটাগরিতে ফিরে যান" else "Back to categories",
                            isDark = isDark,
                            onClick = { drilledCategory = null },
                            testTag = "btn_category_drill_back"
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = drilledCategory!!.getDisplayName(language),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (language == AppLanguage.BANGLA) "সাবক্যাটাগরি নির্বাচন করুন" else "Select subcategory",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                    }
                } else {
                    // Screen 3: Transaction Category Picker Header ("Select Category")
                    Text(
                        text = if (language == AppLanguage.BANGLA) "ক্যাটাগরি নির্বাচন করুন" else "Select Category",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                // Trailing controls in header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (drilledCategory == null) {
                        // Add-category (+) action in high-contrast circular button
                        SheetHeaderCircleActionButton(
                            icon = CategorySelectionSheetStyle.HighContrastPlusIcon,
                            contentDescription = "Add Category",
                            isDark = isDark,
                            onClick = onAddNewCategory,
                            testTag = "btn_add_category_quick"
                        )
                    }

                    // Close (×) control in high-contrast circular button
                    SheetHeaderCircleActionButton(
                        icon = CategorySelectionSheetStyle.HighContrastCloseIcon,
                        contentDescription = "Close",
                        isDark = isDark,
                        onClick = onDismiss,
                        testTag = "btn_close_category_sheet"
                    )
                }
            }

            // Only show Segmented Toggle and Search Bar when in main categories list
            if (drilledCategory == null) {
                // Segmented Toggle: Expense / Income
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isExpense = selectedType == TransactionType.EXPENSE
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isExpense) (if (isDark) Color(0xFF334155) else Color.White) else Color.Transparent,
                            shadowElevation = if (isExpense) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(3.dp)
                                .fillMaxHeight(),
                            onClick = { onTypeChanged(TransactionType.EXPENSE) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isExpense) Color(0xFFEF4444) else textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "ব্যয়" else "Expense",
                                    fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isExpense) textPrimary else textSecondary
                                )
                            }
                        }

                        val isIncome = selectedType == TransactionType.INCOME
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isIncome) (if (isDark) Color(0xFF334155) else Color.White) else Color.Transparent,
                            shadowElevation = if (isIncome) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(3.dp)
                                .fillMaxHeight(),
                            onClick = { onTypeChanged(TransactionType.INCOME) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isIncome) Color(0xFF10B981) else textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "আয়" else "Income",
                                    fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isIncome) textPrimary else textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search field as full-width rounded rectangle with subtle neutral border
                val searchShape = RoundedCornerShape(CategorySelectionSheetStyle.SEARCH_CORNER_RADIUS)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    interactionSource = searchInteractionSource,
                    placeholder = {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "ক্যাটাগরি অনুসন্ধান করুন..." else "Search categories...",
                            color = textSecondary.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    shape = searchShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = accentBlue,
                        unfocusedBorderColor = borderUnselected,
                        focusedContainerColor = cardBgUnselected,
                        unfocusedContainerColor = cardBgUnselected,
                        cursorColor = accentBlue
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Animated transition between Category List and Subcategories List
            AnimatedContent(
                targetState = drilledCategory,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally(animationSpec = tween(150)) { width -> width / 3 } + fadeIn(tween(150))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(120)) { width -> -width / 3 } + fadeOut(tween(120))
                        )
                    } else {
                        (slideInHorizontally(animationSpec = tween(150)) { width -> -width / 3 } + fadeIn(tween(150))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(120)) { width -> width / 3 } + fadeOut(tween(120))
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "CategoryDrillDownAnimation"
            ) { targetCategory ->
                if (targetCategory == null) {
                    // SCREEN 3: MAIN CATEGORY LIST
                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (language == AppLanguage.BANGLA) "কোন ক্যাটাগরি পাওয়া যায়নি" else "No categories found",
                                fontSize = 14.sp,
                                color = textSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            state = categoryListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("category_picker_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp)
                        ) {
                            items(filteredList, key = { it.id }) { cat ->
                                val isSelected = selectedCategoryName == cat.name
                                val catSubs = remember(cat.id, subcategories) {
                                    subcategories.filter { it.categoryId == cat.id }
                                }

                                CategoryItemCard(
                                    category = cat,
                                    subcategoryCount = catSubs.size,
                                    isSelected = isSelected,
                                    selectedSubcategoryName = if (isSelected) selectedSubcategoryName else "",
                                    language = language,
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    cardBgUnselected = cardBgUnselected,
                                    cardBgSelected = cardBgSelected,
                                    borderUnselected = borderUnselected,
                                    accentBlue = accentBlue,
                                    buttonBg = buttonBg,
                                    onClick = {
                                        if (catSubs.isNotEmpty()) {
                                            // Drill down to show subcategories
                                            drilledCategory = cat
                                        } else {
                                            // No subcategories: Select directly & dismiss on first tap
                                            commitSelection(cat.name, "")
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // SCREEN 1: SUBCATEGORY PICKER
                    val catSubs = remember(targetCategory.id, subcategories) {
                        subcategories.filter { it.categoryId == targetCategory.id }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp)
                    ) {
                        // "All [Category]" Option Row
                        item {
                            val isEntireSelected = selectedCategoryName == targetCategory.name && selectedSubcategoryName.isBlank()
                            val rowShape = RoundedCornerShape(CategorySelectionSheetStyle.ROW_CORNER_RADIUS)

                            Surface(
                                onClick = {
                                    commitSelection(targetCategory.name, "")
                                },
                                shape = rowShape,
                                color = if (isEntireSelected) cardBgSelected else cardBgUnselected,
                                border = BorderStroke(
                                    width = if (isEntireSelected) CategorySelectionSheetStyle.SELECTED_BORDER_WIDTH else CategorySelectionSheetStyle.UNSELECTED_BORDER_WIDTH,
                                    color = if (isEntireSelected) accentBlue else borderUnselected
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_category_entire")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryIcon(
                                        iconName = targetCategory.iconName,
                                        colorHex = targetCategory.colorHex,
                                        size = 38.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (language == AppLanguage.BANGLA) "সম্পূর্ণ ${targetCategory.getDisplayName(language)}" else "All ${targetCategory.getDisplayName(language)}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = if (language == AppLanguage.BANGLA) "সাবক্যাটাগরি ব্যতীত নির্বাচন" else "Select without subcategory",
                                            fontSize = 12.sp,
                                            color = textSecondary
                                        )
                                    }
                                    if (isEntireSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = accentBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // "SUBCATEGORIES" Small Section Label
                        if (catSubs.isNotEmpty()) {
                            item {
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "সাবক্যাটাগরিসমূহ" else "SUBCATEGORIES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 2.dp)
                                )
                            }
                        }

                        // Subcategory list items with colorful bullet dots and consistent rounded cards
                        itemsIndexed(catSubs, key = { _, it -> it.id }) { index, sub ->
                            val isSubSelected = selectedCategoryName == targetCategory.name && selectedSubcategoryName == sub.name
                            val bulletColor = CategorySelectionSheetStyle.subcategoryBulletColor(index, isDark)
                            val rowShape = RoundedCornerShape(CategorySelectionSheetStyle.ROW_CORNER_RADIUS)

                            Surface(
                                onClick = {
                                    commitSelection(targetCategory.name, sub.name)
                                },
                                shape = rowShape,
                                color = if (isSubSelected) cardBgSelected else cardBgUnselected,
                                border = BorderStroke(
                                    width = if (isSubSelected) CategorySelectionSheetStyle.SELECTED_BORDER_WIDTH else CategorySelectionSheetStyle.UNSELECTED_BORDER_WIDTH,
                                    color = if (isSubSelected) accentBlue else borderUnselected
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("subcategory_item_${sub.name}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(CategorySelectionSheetStyle.SUBCATEGORY_BULLET_SIZE)
                                            .clip(CircleShape)
                                            .background(bulletColor)
                                            .testTag("subcategory_dot_${sub.name}")
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = sub.getDisplayName(language),
                                        fontWeight = if (isSubSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSubSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = accentBlue,
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
    }
}

/**
 * Individual Category Item Card in the main category picker.
 */
@Composable
private fun CategoryItemCard(
    category: CategoryEntity,
    subcategoryCount: Int,
    isSelected: Boolean,
    selectedSubcategoryName: String,
    language: AppLanguage,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBgUnselected: Color,
    cardBgSelected: Color,
    borderUnselected: Color,
    accentBlue: Color,
    buttonBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowShape = RoundedCornerShape(CategorySelectionSheetStyle.ROW_CORNER_RADIUS)

    Surface(
        onClick = onClick,
        shape = rowShape,
        color = if (isSelected) cardBgSelected else cardBgUnselected,
        border = BorderStroke(
            width = if (isSelected) CategorySelectionSheetStyle.SELECTED_BORDER_WIDTH else CategorySelectionSheetStyle.UNSELECTED_BORDER_WIDTH,
            color = if (isSelected) accentBlue else borderUnselected
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_item_${category.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                iconName = category.iconName,
                colorHex = category.colorHex,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.getDisplayName(language),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textPrimary
                )

                val subtitle = when {
                    isSelected && selectedSubcategoryName.isNotBlank() -> "• $selectedSubcategoryName"
                    subcategoryCount > 0 -> {
                        val countStr = if (language == AppLanguage.BANGLA) CurrencyFormatter.convertToBanglaDigits(subcategoryCount.toString()) else subcategoryCount.toString()
                        if (language == AppLanguage.BANGLA) "$countStr টি সাবক্যাটাগরি" else "$countStr subcategories"
                    }
                    else -> if (language == AppLanguage.BANGLA) "কোন সাবক্যাটাগরি নেই" else "No subcategories"
                }

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (isSelected && selectedSubcategoryName.isNotBlank()) accentBlue else textSecondary
                )
            }

            if (subcategoryCount > 0) {
                // Trailing chevron indicating tap to drill into subcategories
                Surface(
                    shape = CircleShape,
                    color = buttonBg,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "View subcategories",
                            tint = textSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            } else if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = accentBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
