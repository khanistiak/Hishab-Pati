package com.example.ui.screens.plan

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.model.AppLanguage
import com.example.ui.components.getIconForName
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import java.util.Locale

internal val RoyalIndigoBrand = Color(0xFF4F46E5)
internal val ElectricVioletBrand = Color(0xFF7C3AED)
internal val EmeraldBrand = Color(0xFF10B981)
internal val AmberBrand = Color(0xFFF59E0B)
internal val CoralRoseBrand = Color(0xFFF43F5E)

internal val BUDGET_ACCENT_COLORS = listOf(
    "#4F46E5", // Royal Indigo
    "#7C3AED", // Electric Violet
    "#3B82F6", // Azure Blue
    "#06B6D4", // Ocean Cyan
    "#10B981", // Emerald
    "#14B8A6", // Teal
    "#84CC16", // Lime
    "#F59E0B", // Amber
    "#F97316", // Tangerine
    "#F43F5E", // Coral Rose
    "#EC4899", // Vivid Pink
    "#D946EF", // Fuchsia
    "#6366F1", // Soft Indigo
    "#0EA5E9", // Sky Blue
    "#059669", // Deep Emerald
    "#E11D48", // Crimson
    "#9333EA", // Royal Purple
    "#64748B"  // Slate
)

internal val BUDGET_ICON_CATALOG = listOf(
    "wallet", "payments", "savings", "account_balance", "credit_card", "paid",
    "restaurant", "fastfood", "local_cafe", "shopping_cart", "shopping_bag", "storefront",
    "directions_bike", "directions_car", "local_gas_station", "directions_bus", "local_taxi", "flight",
    "home", "electric_bolt", "water_drop", "wifi", "phone_android", "subscriptions",
    "medical_services", "local_pharmacy", "school", "menu_book", "fitness_center", "spa",
    "movie", "sports_esports", "music_note", "card_giftcard", "volunteer_activism", "pets",
    "child_care", "checkroom", "build", "receipt_long", "handshake", "category"
)

internal fun parseBudgetHexColor(hex: String, fallback: Color = RoyalIndigoBrand): Color {
    return try {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
        fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetIconColorPickerSheet(
    selectedIcon: String,
    selectedColorHex: String,
    budgetTitlePreview: String,
    language: AppLanguage,
    onSelectIcon: (String) -> Unit,
    onSelectColorHex: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sheetBg = if (isDark) Color(0xFF131426) else Color(0xFFFDFDFF)
    val cardBg = if (isDark) Color(0xFF1C1E35) else Color(0xFFF4F5FB)
    val borderCol = if (isDark) Color.White.copy(alpha = 0.08f) else RoyalIndigoBrand.copy(alpha = 0.12f)
    val accentColor = parseBudgetHexColor(selectedColorHex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(accentColor.copy(alpha = 0.22f), accentColor.copy(alpha = 0.10f))
                                )
                            )
                            .border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForName(selectedIcon),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = AppStrings.t("pick_icon_color_title", language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (budgetTitlePreview.isNotBlank()) {
                            Text(
                                text = budgetTitlePreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("icon_picker_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = AppStrings.t("close", language),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Accent Color Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.t("accent_color_label", language),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 152.dp)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(BUDGET_ACCENT_COLORS) { hex ->
                            val swatch = parseBudgetHexColor(hex)
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) {
                                            if (isDark) Color.White else Color(0xFF0F172A)
                                        } else {
                                            Color.White.copy(alpha = 0.25f)
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectColorHex(hex) }
                                    .testTag("color_swatch_$hex"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Icon Grid Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.t("choose_icon_label", language),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(BUDGET_ICON_CATALOG) { iconKey ->
                            val isSelected = selectedIcon.equals(iconKey, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) accentColor.copy(alpha = 0.22f)
                                        else if (isDark) Color.White.copy(alpha = 0.04f)
                                        else Color.White
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) accentColor else borderCol,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectIcon(iconKey) }
                                    .testTag("budget_icon_option_$iconKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconForName(iconKey),
                                    contentDescription = iconKey,
                                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Done button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(RoyalIndigoBrand, ElectricVioletBrand)))
                    .clickable { onDismiss() }
                    .testTag("icon_picker_done_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AppStrings.t("done_label", language),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingModeInfoSheet(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sheetBg = if (isDark) Color(0xFF131426) else Color(0xFFFDFDFF)
    val cardBg = if (isDark) Color(0xFF1C1E35) else Color(0xFFF4F5FB)
    val borderCol = if (isDark) Color.White.copy(alpha = 0.08f) else RoyalIndigoBrand.copy(alpha = 0.14f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = AppStrings.t("tracking_mode_info_title", language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Automatic card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, RoyalIndigoBrand.copy(alpha = 0.30f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(RoyalIndigoBrand.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = RoyalIndigoBrand,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = AppStrings.t("tracking_mode_automatic", language),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = AppStrings.t("automatic_mode_detail", language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Manual card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, ElectricVioletBrand.copy(alpha = 0.30f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricVioletBrand.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = ElectricVioletBrand,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = AppStrings.t("tracking_mode_manual", language),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = AppStrings.t("manual_mode_detail", language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("tracking_mode_info_got_it"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalIndigoBrand,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = AppStrings.t("got_it", language),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetMultiCategoryPickerCard(
    categories: List<CategoryEntity>,
    subcategories: List<SubcategoryEntity>,
    enableSubcategories: Boolean,
    onToggleEnableSubcategories: (Boolean) -> Unit,
    selectedCategoryNames: Set<String>,
    selectedSubcategoryPairs: Set<Pair<String, String>>,
    onToggleCategory: (String) -> Unit,
    onToggleSubcategory: (String, String) -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF17192D) else Color.White
    val nestedBg = if (isDark) Color(0xFF1F223B) else Color(0xFFF6F7FC)
    val borderCol = if (isDark) Color.White.copy(alpha = 0.08f) else RoyalIndigoBrand.copy(alpha = 0.12f)

    val expenseCategories = remember(categories) {
        categories.filter { !it.isIncome && !it.isTransfer }
    }
    val subByCatId = remember(subcategories) {
        subcategories.groupBy { it.categoryId }
    }
    var expandedCatIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val selectedCountSummary = remember(selectedCategoryNames, selectedSubcategoryPairs, language) {
        val catCount = CurrencyFormatter.formatNumber(selectedCategoryNames.size, language)
        val subCount = CurrencyFormatter.formatNumber(selectedSubcategoryPairs.size, language)
        if (selectedSubcategoryPairs.isNotEmpty()) {
            "$catCount ${AppStrings.t("categories_selected_suffix", language)} • $subCount ${AppStrings.t("subcategories_selected_suffix", language)}"
        } else {
            "$catCount ${AppStrings.t("categories_selected_suffix", language)}"
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Enable Subcategories Bento Switch Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleEnableSubcategories(!enableSubcategories) }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("enable_subcategories_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = AppStrings.t("enable_subcategories_label", language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = AppStrings.t("enable_subcategories_hint", language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = enableSubcategories,
                    onCheckedChange = onToggleEnableSubcategories,
                    modifier = Modifier.testTag("enable_subcategories_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RoyalIndigoBrand,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = if (isDark) Color(0xFF282B47) else Color(0xFFE2E8F0)
                    )
                )
            }
        }

        // Multi-Select Category & Subcategory Checklist Bento Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (enableSubcategories) {
                            AppStrings.t("categories_and_subcategories", language)
                        } else {
                            AppStrings.t("select_categories_label", language)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = RoyalIndigoBrand.copy(alpha = if (isDark) 0.22f else 0.10f)
                    ) {
                        Text(
                            text = selectedCountSummary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                expenseCategories.forEachIndexed { index, cat ->
                    val isCatSelected = selectedCategoryNames.any { it.equals(cat.name, ignoreCase = true) }
                    val catSubs = subByCatId[cat.id].orEmpty()
                    val selectedSubsForCat = selectedSubcategoryPairs.filter {
                        it.first.equals(cat.name, ignoreCase = true)
                    }
                    val hasSubs = catSubs.isNotEmpty()
                    val isExpanded = enableSubcategories && hasSubs && (
                        expandedCatIds.contains(cat.id) || selectedSubsForCat.isNotEmpty()
                    )
                    val catColor = parseBudgetHexColor(cat.colorHex.ifBlank { "#4F46E5" })

                    if (index > 0) {
                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = borderCol.copy(alpha = 0.7f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onToggleCategory(cat.name)
                                    if (enableSubcategories && hasSubs && !isCatSelected) {
                                        expandedCatIds = expandedCatIds + cat.id
                                    }
                                }
                                .padding(vertical = 6.dp)
                                .testTag("budget_category_row_${cat.name.lowercase(Locale.US)}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isCatSelected,
                                onCheckedChange = {
                                    onToggleCategory(cat.name)
                                    if (enableSubcategories && hasSubs && !isCatSelected) {
                                        expandedCatIds = expandedCatIds + cat.id
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = RoyalIndigoBrand,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    checkmarkColor = Color.White
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(catColor.copy(alpha = if (isDark) 0.22f else 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconForName(cat.iconName),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.getDisplayName(language),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (enableSubcategories && hasSubs && isCatSelected) {
                                    val subSummary = if (selectedSubsForCat.isEmpty()) {
                                        AppStrings.t("all_subcategories_included", language)
                                    } else {
                                        selectedSubsForCat.joinToString(", ") { it.second }
                                    }
                                    Text(
                                        text = subSummary,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color(0xFFA5B4FC) else RoyalIndigoBrand,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (enableSubcategories && hasSubs) {
                                IconButton(
                                    onClick = {
                                        expandedCatIds = if (expandedCatIds.contains(cat.id)) {
                                            expandedCatIds - cat.id
                                        } else {
                                            expandedCatIds + cat.id
                                        }
                                    },
                                    modifier = Modifier.testTag("expand_subcat_${cat.name.lowercase(Locale.US)}")
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = nestedBg,
                                border = BorderStroke(1.dp, borderCol.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, bottom = 6.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    catSubs.forEach { sub ->
                                        val isSubSelected = selectedSubcategoryPairs.any {
                                            it.first.equals(cat.name, ignoreCase = true) &&
                                                it.second.equals(sub.name, ignoreCase = true)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSubSelected) {
                                                RoyalIndigoBrand
                                            } else {
                                                if (isDark) Color(0xFF282B47) else Color.White
                                            },
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isSubSelected) RoyalIndigoBrand else borderCol
                                            ),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onToggleSubcategory(cat.name, sub.name) }
                                                .testTag("subcat_chip_${cat.name}_${sub.name}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isSubSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Text(
                                                    text = sub.getDisplayName(language),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSubSelected) Color.White else MaterialTheme.colorScheme.onSurface
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
    }
}
