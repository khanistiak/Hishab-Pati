package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BudgetEntity
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.CalculatorEvaluator
import com.example.ui.components.CalculatorKeypad
import com.example.ui.components.HishabPatiDatePickerDialog
import com.example.ui.components.getIconForName
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class BudgetRecurrenceOption(val code: String, val stringKey: String) {
    MONTHLY("MONTHLY", "recurrence_every_month"),
    WEEKLY("WEEKLY", "recurrence_every_week"),
    YEARLY("YEARLY", "recurrence_every_year"),
    ONE_TIME("ONE_TIME", "recurrence_one_time")
}

private fun defaultStartMillisForMonthKey(monthKey: String?): Long {
    val cal = Calendar.getInstance()
    if (!monthKey.isNullOrBlank() && monthKey.matches(Regex("\\d{4}-\\d{2}"))) {
        val parts = monthKey.split("-")
        val y = parts[0].toIntOrNull()
        val m = parts[1].toIntOrNull()
        if (y != null && m != null && m in 1..12) {
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m - 1)
        }
    }
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
    viewModel: MainViewModel,
    initialBudget: BudgetEntity? = null,
    defaultIsOverall: Boolean = false,
    defaultCategory: String? = null,
    defaultMonthKey: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val subcategories by viewModel.subcategories.collectAsState()

    val language = settings?.language ?: AppLanguage.ENGLISH
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val currencyCode = settings?.currencyCode?.ifBlank { "BDT" } ?: "BDT"
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val isEditing = initialBudget != null
    val isOverallMode = remember(initialBudget, defaultIsOverall) {
        initialBudget?.isOverall == true || (initialBudget == null && defaultIsOverall)
    }

    val expenseCategories = remember(categories) {
        categories.filter { !it.isIncome && !it.isTransfer }
    }

    // Core form state
    var budgetName by remember(initialBudget, defaultCategory, isOverallMode) {
        mutableStateOf(
            when {
                initialBudget != null && initialBudget.name.isNotBlank() -> initialBudget.name
                initialBudget != null && initialBudget.categoryName.isNotBlank() -> initialBudget.categoryName
                isOverallMode -> AppStrings.t("overall_budget_mode_banner", language)
                !defaultCategory.isNullOrBlank() -> defaultCategory
                else -> ""
            }
        )
    }

    var selectedIcon by remember(initialBudget, defaultCategory, expenseCategories, isOverallMode) {
        val defaultCatEntity = defaultCategory?.let { name ->
            expenseCategories.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }
        mutableStateOf(
            initialBudget?.iconName?.ifBlank { null }
                ?: defaultCatEntity?.iconName?.ifBlank { null }
                ?: if (isOverallMode) "wallet" else "category"
        )
    }

    var selectedColorHex by remember(initialBudget, defaultCategory, expenseCategories) {
        val defaultCatEntity = defaultCategory?.let { name ->
            expenseCategories.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }
        mutableStateOf(
            initialBudget?.colorHex?.ifBlank { null }
                ?: defaultCatEntity?.colorHex?.ifBlank { null }
                ?: "#4F46E5"
        )
    }

    var amountExpression by remember(initialBudget) {
        val initLimit = initialBudget?.monthlyLimit ?: 0.0
        mutableStateOf(
            if (initLimit > 0.0) {
                if (initLimit % 1.0 == 0.0) initLimit.toLong().toString()
                else String.format(Locale.US, "%.2f", initLimit)
            } else {
                ""
            }
        )
    }

    var showCalculator by remember { mutableStateOf(false) }

    var startDateMillis by remember(initialBudget, defaultMonthKey) {
        val saved = initialBudget?.startDateMillis ?: 0L
        mutableLongStateOf(
            if (saved > 0L) saved else defaultStartMillisForMonthKey(initialBudget?.monthYear ?: defaultMonthKey)
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var recurrence by remember(initialBudget) {
        val savedCode = initialBudget?.recurrence ?: "MONTHLY"
        mutableStateOf(
            BudgetRecurrenceOption.entries.firstOrNull { it.code.equals(savedCode, ignoreCase = true) }
                ?: BudgetRecurrenceOption.MONTHLY
        )
    }

    var description by remember(initialBudget) {
        mutableStateOf(initialBudget?.description ?: "")
    }

    var isManualMode by remember(initialBudget, isOverallMode) {
        mutableStateOf(!isOverallMode && (initialBudget?.isManual == true))
    }

    var enableSubcategories by remember(initialBudget) {
        mutableStateOf(
            initialBudget != null &&
                (initialBudget.selectedSubcategories.isNotBlank() || initialBudget.subcategoryName.isNotBlank())
        )
    }

    var selectedCategoryNames by remember(initialBudget, defaultCategory, isOverallMode) {
        val initialSet = when {
            isOverallMode -> emptySet()
            initialBudget != null -> initialBudget.parsedCategories().toSet()
            !defaultCategory.isNullOrBlank() -> setOf(defaultCategory)
            else -> emptySet()
        }
        mutableStateOf(initialSet)
    }

    var selectedSubcategoryPairs by remember(initialBudget) {
        val initialPairs = initialBudget?.parsedSubcategoryPairs()?.toSet() ?: emptySet()
        mutableStateOf(initialPairs)
    }

    var showIconColorSheet by remember { mutableStateOf(false) }
    var showTrackingInfoSheet by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val parsedAmount = remember(amountExpression) {
        CalculatorEvaluator.evaluate(amountExpression).takeIf { !it.isNaN() && !it.isInfinite() } ?: 0.0
    }

    val formattedStartDate = remember(startDateMillis, language) {
        val raw = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(startDateMillis))
        DateFormatter.localizeText(raw, language)
    }

    val accentColor = parseBudgetHexColor(selectedColorHex)

    // Theme tokens (strictly preserving Hishab Pati Light/Dark Royal Indigo-to-Electric Violet Bento)
    val screenBg = MaterialTheme.colorScheme.background
    val cardBg = if (isDark) Color(0xFF17192D) else Color.White
    val subtleBg = if (isDark) Color(0xFF1F223B) else Color(0xFFF4F5FB)
    val headerBg = if (isDark) Color(0xFF121426).copy(alpha = 0.94f) else Color(0xFFF9FAFF).copy(alpha = 0.94f)
    val borderCol = if (isDark) Color.White.copy(alpha = 0.08f) else RoyalIndigoBrand.copy(alpha = 0.12f)

    BackHandler {
        if (showCalculator) {
            showCalculator = false
        } else {
            onBack()
        }
    }

    fun handleSave() {
        val trimmedName = budgetName.trim()
        val resolvedLimit = CalculatorEvaluator.evaluate(amountExpression)
        if (resolvedLimit.isNaN() || resolvedLimit.isInfinite() || resolvedLimit <= 0.0) {
            validationError = AppStrings.t("invalid_amount_error", language)
            showCalculator = true
            return
        }
        if (!isOverallMode && !isManualMode && selectedCategoryNames.isEmpty()) {
            validationError = AppStrings.t("select_at_least_one_category_error", language)
            return
        }
        val finalName = when {
            trimmedName.isNotBlank() -> trimmedName
            isOverallMode -> AppStrings.t("overall_budget_mode_banner", language)
            selectedCategoryNames.size == 1 -> selectedCategoryNames.first()
            else -> {
                validationError = AppStrings.t("enter_budget_name_error", language)
                return
            }
        }

        val orderedCategories = selectedCategoryNames.toList()
        val primaryCategory = when {
            isOverallMode -> "Overall"
            isManualMode -> "Manual"
            orderedCategories.isNotEmpty() -> orderedCategories.first()
            else -> finalName
        }

        val activeSubPairs = if (!isOverallMode && !isManualMode && enableSubcategories) {
            selectedSubcategoryPairs.filter { pair ->
                orderedCategories.any { it.equals(pair.first, ignoreCase = true) }
            }
        } else {
            emptyList()
        }

        val serializedCategories = if (!isOverallMode && !isManualMode) {
            orderedCategories.joinToString("|")
        } else {
            ""
        }
        val serializedSubcategories = if (activeSubPairs.isNotEmpty()) {
            activeSubPairs.joinToString("|") { "${it.first}::${it.second}" }
        } else {
            ""
        }
        val singleSubName = if (activeSubPairs.size == 1 && orderedCategories.size == 1) {
            activeSubPairs.first().second
        } else {
            ""
        }

        val targetMonthYear = if (recurrence == BudgetRecurrenceOption.ONE_TIME) {
            val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
            String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        } else {
            initialBudget?.monthYear?.takeIf { it.isNotBlank() } ?: "ALL"
        }

        viewModel.saveBudget(
            id = initialBudget?.id,
            categoryName = primaryCategory,
            limit = resolvedLimit,
            monthYear = targetMonthYear,
            isOverall = isOverallMode,
            rolloverEnabled = initialBudget?.rolloverEnabled ?: false,
            name = finalName,
            subcategoryName = singleSubName,
            iconName = selectedIcon,
            colorHex = selectedColorHex,
            alertPercentage = initialBudget?.alertPercentage ?: 80,
            isManual = !isOverallMode && isManualMode,
            recurrence = recurrence.code,
            startDateMillis = startDateMillis,
            description = description.trim(),
            selectedCategories = serializedCategories,
            selectedSubcategories = serializedSubcategories
        )
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBg,
        topBar = {
            Surface(
                color = headerBg,
                shadowElevation = 0.dp,
                border = BorderStroke(0.8.dp, borderCol)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("add_edit_budget_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AppStrings.t("back", language),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditing) {
                                AppStrings.t("edit_budget_screen_title", language)
                            } else {
                                AppStrings.t("create_budget_title", language)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isOverallMode) {
                            Text(
                                text = AppStrings.t("overall_budget_mode_banner", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (showCalculator) {
                CalculatorKeypad(
                    expression = amountExpression,
                    onExpressionChange = {
                        amountExpression = it
                        validationError = null
                    },
                    onDone = { evaluated ->
                        if (evaluated > 0.0) {
                            amountExpression = if (evaluated % 1.0 == 0.0) {
                                evaluated.toLong().toString()
                            } else {
                                String.format(Locale.US, "%.2f", evaluated)
                            }
                        }
                        showCalculator = false
                    },
                    onDismiss = { showCalculator = false },
                    language = language,
                    currencySymbol = currencySymbol
                )
            } else {
                Surface(
                    color = headerBg,
                    border = BorderStroke(0.8.dp, borderCol)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!validationError.isNullOrBlank()) {
                            Text(
                                text = validationError!!,
                                style = MaterialTheme.typography.labelMedium,
                                color = CoralRoseBrand,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(RoyalIndigoBrand, ElectricVioletBrand)
                                    )
                                )
                                .clickable { handleSave() }
                                .testTag("save_budget_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEditing) {
                                    AppStrings.t("update_budget_cta", language)
                                } else {
                                    AppStrings.t("save_budget_cta", language)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Optional Overall Mode Banner
            if (isOverallMode) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = RoyalIndigoBrand.copy(alpha = if (isDark) 0.18f else 0.08f),
                        border = BorderStroke(1.dp, RoyalIndigoBrand.copy(alpha = 0.28f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RoyalIndigoBrand.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand
                                )
                            }
                            Column {
                                Text(
                                    text = AppStrings.t("overall_budget_mode_banner", language),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = AppStrings.t("overall_budget_mode_desc", language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 1. Budget Name & Squircle Icon/Color Picker Bento Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Clickable squircle icon & accent badge
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            accentColor.copy(alpha = if (isDark) 0.30f else 0.18f),
                                            accentColor.copy(alpha = if (isDark) 0.14f else 0.08f)
                                        )
                                    )
                                )
                                .border(1.5.dp, accentColor.copy(alpha = 0.50f), RoundedCornerShape(20.dp))
                                .clickable {
                                    showCalculator = false
                                    showIconColorSheet = true
                                }
                                .testTag("budget_icon_picker_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconForName(selectedIcon),
                                contentDescription = AppStrings.t("pick_icon_color_title", language),
                                tint = accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }

                        // Budget Name Input
                        OutlinedTextField(
                            value = budgetName,
                            onValueChange = {
                                budgetName = it
                                validationError = null
                            },
                            label = { Text(AppStrings.t("budget_name_field", language)) },
                            placeholder = {
                                Text(
                                    text = AppStrings.t("budget_name_placeholder", language),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalIndigoBrand,
                                unfocusedBorderColor = borderCol,
                                focusedLabelColor = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand,
                                cursorColor = RoyalIndigoBrand
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("budget_name_input")
                        )
                    }
                }
            }

            // 2. Currency & Target Limit Amount Bento Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    border = BorderStroke(
                        width = if (showCalculator) 1.5.dp else 1.dp,
                        color = if (showCalculator) RoyalIndigoBrand else borderCol
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            showCalculator = true
                            validationError = null
                        }
                        .testTag("budget_amount_input_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppStrings.t("target_limit_label", language),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RoyalIndigoBrand.copy(alpha = if (isDark) 0.22f else 0.10f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$currencyCode $currencySymbol",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val displayAmount = when {
                                    amountExpression.isBlank() -> "${currencySymbol}0"
                                    parsedAmount > 0.0 -> CurrencyFormatter.format(
                                        parsedAmount,
                                        currencySymbol,
                                        language = language
                                    )
                                    else -> "$currencySymbol$amountExpression"
                                }
                                Text(
                                    text = displayAmount,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (amountExpression.isBlank()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (amountExpression.any { it in listOf('+', '-', '*', '/', '×', '÷') }) {
                                    Text(
                                        text = amountExpression,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand
                                    )
                                } else if (amountExpression.isBlank()) {
                                    Text(
                                        text = AppStrings.t("tap_to_enter_amount", language),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                RoyalIndigoBrand.copy(alpha = 0.16f),
                                                ElectricVioletBrand.copy(alpha = 0.16f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = AppStrings.t("target_limit_label", language),
                                    tint = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Schedule, Recurrence & Optional Description Bento Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Starting Date Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(subtleBg)
                                .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                                .clickable {
                                    showCalculator = false
                                    showDatePicker = true
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("budget_start_date_pill"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(RoyalIndigoBrand.copy(alpha = if (isDark) 0.22f else 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = AppStrings.t("starting_date_label", language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formattedStartDate,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = RoyalIndigoBrand.copy(alpha = if (isDark) 0.22f else 0.10f)
                            ) {
                                Text(
                                    text = AppStrings.t("change", language),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Recurrence 2x2 Bento Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = AppStrings.t("recurrence_field_label", language),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val rows = BudgetRecurrenceOption.entries.chunked(2)
                            rows.forEach { rowOptions ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowOptions.forEach { option ->
                                        val selected = recurrence == option
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .then(
                                                    if (selected) {
                                                        Modifier.background(
                                                            Brush.horizontalGradient(
                                                                listOf(RoyalIndigoBrand, ElectricVioletBrand)
                                                            )
                                                        )
                                                    } else {
                                                        Modifier
                                                            .background(subtleBg)
                                                            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                                                    }
                                                )
                                                .clickable {
                                                    showCalculator = false
                                                    recurrence = option
                                                }
                                                .testTag("recurrence_option_${option.code.lowercase(Locale.US)}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = AppStrings.t(option.stringKey, language),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Optional Description Field
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(AppStrings.t("description_optional_label", language)) },
                            placeholder = {
                                Text(
                                    text = AppStrings.t("description_placeholder", language),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Notes,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            maxLines = 3,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalIndigoBrand,
                                unfocusedBorderColor = borderCol,
                                focusedLabelColor = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand,
                                cursorColor = RoyalIndigoBrand
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("budget_description_input")
                        )
                    }
                }
            }

            // 4. Budget Tracking Mode (Automatic vs Manual + ? Tooltip)
            if (!isOverallMode) {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.t("budget_tracking_mode", language),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = {
                                        showCalculator = false
                                        showTrackingInfoSheet = true
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("tracking_mode_help_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = AppStrings.t("tracking_mode_info_title", language),
                                        tint = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Segmented Bento Pill Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(subtleBg)
                                    .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val autoSelected = !isManualMode
                                val manualSelected = isManualMode

                                // Automatic Segment
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .then(
                                            if (autoSelected) {
                                                Modifier.background(
                                                    Brush.horizontalGradient(
                                                        listOf(RoyalIndigoBrand, ElectricVioletBrand)
                                                    )
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            isManualMode = false
                                            validationError = null
                                        }
                                        .testTag("tracking_mode_automatic_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (autoSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = AppStrings.t("tracking_mode_automatic", language),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (autoSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (autoSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Manual Segment
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .then(
                                            if (manualSelected) {
                                                Modifier.background(
                                                    Brush.horizontalGradient(
                                                        listOf(RoyalIndigoBrand, ElectricVioletBrand)
                                                    )
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            isManualMode = true
                                            validationError = null
                                        }
                                        .testTag("tracking_mode_manual_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TouchApp,
                                            contentDescription = null,
                                            tint = if (manualSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = AppStrings.t("tracking_mode_manual", language),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (manualSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (manualSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Mode helper caption
                            Text(
                                text = if (isManualMode) {
                                    AppStrings.t("manual_mode_summary", language)
                                } else {
                                    AppStrings.t("automatic_mode_summary", language)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // 5. Enable Subcategories Toggle & Multi-Category Picker (Automatic Mode)
                item {
                    AnimatedVisibility(
                        visible = !isManualMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        BudgetMultiCategoryPickerCard(
                            categories = categories,
                            subcategories = subcategories,
                            enableSubcategories = enableSubcategories,
                            onToggleEnableSubcategories = { enabled ->
                                enableSubcategories = enabled
                                if (!enabled) {
                                    selectedSubcategoryPairs = emptySet()
                                }
                            },
                            selectedCategoryNames = selectedCategoryNames,
                            selectedSubcategoryPairs = selectedSubcategoryPairs,
                            onToggleCategory = { catName ->
                                validationError = null
                                val already = selectedCategoryNames.firstOrNull { it.equals(catName, ignoreCase = true) }
                                if (already != null) {
                                    selectedCategoryNames = selectedCategoryNames - already
                                    selectedSubcategoryPairs = selectedSubcategoryPairs.filterNot {
                                        it.first.equals(catName, ignoreCase = true)
                                    }.toSet()
                                } else {
                                    selectedCategoryNames = selectedCategoryNames + catName
                                    // Smart default name/icon/color when picking the first category on a new budget
                                    if (!isEditing && budgetName.isBlank()) {
                                        val catObj = expenseCategories.firstOrNull { it.name.equals(catName, ignoreCase = true) }
                                        budgetName = catObj?.getDisplayName(language) ?: catName
                                        if (catObj != null) {
                                            if (catObj.iconName.isNotBlank()) selectedIcon = catObj.iconName
                                            if (catObj.colorHex.isNotBlank()) selectedColorHex = catObj.colorHex
                                        }
                                    }
                                }
                            },
                            onToggleSubcategory = { catName, subName ->
                                validationError = null
                                val existingPair = selectedSubcategoryPairs.firstOrNull {
                                    it.first.equals(catName, ignoreCase = true) &&
                                        it.second.equals(subName, ignoreCase = true)
                                }
                                if (existingPair != null) {
                                    selectedSubcategoryPairs = selectedSubcategoryPairs - existingPair
                                } else {
                                    // Ensure parent category is selected when picking a subcategory
                                    if (selectedCategoryNames.none { it.equals(catName, ignoreCase = true) }) {
                                        selectedCategoryNames = selectedCategoryNames + catName
                                    }
                                    selectedSubcategoryPairs = selectedSubcategoryPairs + (catName to subName)
                                }
                            },
                            language = language
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showIconColorSheet) {
        BudgetIconColorPickerSheet(
            selectedIcon = selectedIcon,
            selectedColorHex = selectedColorHex,
            budgetTitlePreview = budgetName.ifBlank { AppStrings.t("create_budget_title", language) },
            language = language,
            onSelectIcon = { selectedIcon = it },
            onSelectColorHex = { selectedColorHex = it },
            onDismiss = { showIconColorSheet = false }
        )
    }

    if (showTrackingInfoSheet) {
        TrackingModeInfoSheet(
            language = language,
            onDismiss = { showTrackingInfoSheet = false }
        )
    }

    if (showDatePicker) {
        HishabPatiDatePickerDialog(
            initialSelectedDateMillis = startDateMillis,
            onDateSelected = { chosen ->
                startDateMillis = chosen
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false },
            language = language,
            titleText = AppStrings.t("starting_date_label", language)
        )
    }
}
