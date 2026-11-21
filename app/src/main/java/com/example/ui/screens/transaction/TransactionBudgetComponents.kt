package com.example.ui.screens.transaction

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.FinancialIntegrityValidator
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.ui.util.CurrencyFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private val IndigoAccent = Color(0xFF4F46E5)
private val VioletAccent = Color(0xFF7C3AED)
private val HealthyEmerald = Color(0xFF10B981)
private val WarningAmber = Color(0xFFF59E0B)
private val ExceededRose = Color(0xFFF43F5E)

/**
 * Compact horizontal row of selectable quick subcategory pills displayed directly below
 * the selected Category card so users can switch subcategories with one tap without
 * reopening the Category modal sheet.
 */
@Composable
internal fun InlineSubcategoryQuickPills(
    subcategories: List<SubcategoryEntity>,
    selectedSubcategoryName: String,
    language: AppLanguage,
    onSelectSubcategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (subcategories.isEmpty()) return

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isBn = language == AppLanguage.BANGLA

    val inactiveBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)
    } else {
        Color.White.copy(alpha = 0.84f)
    }
    val inactiveBorder = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color(0xFFE2E8F0)
    }
    val activeGradient = Brush.horizontalGradient(listOf(IndigoAccent, VioletAccent))

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inline_subcategory_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        // "All" pill (clears subcategory)
        item(key = "sub_all") {
            val isAllSelected = selectedSubcategoryName.isBlank()
            SubcategoryQuickChip(
                label = if (isBn) "সব" else "All",
                isSelected = isAllSelected,
                activeGradient = activeGradient,
                inactiveBg = inactiveBg,
                inactiveBorder = inactiveBorder,
                onClick = { onSelectSubcategory("") },
                testTag = "inline_sub_chip_all"
            )
        }

        items(subcategories, key = { it.id }) { sub ->
            val isSelected = selectedSubcategoryName.equals(sub.name, ignoreCase = true)
            SubcategoryQuickChip(
                label = sub.getDisplayName(language),
                isSelected = isSelected,
                activeGradient = activeGradient,
                inactiveBg = inactiveBg,
                inactiveBorder = inactiveBorder,
                onClick = { onSelectSubcategory(sub.name) },
                testTag = "inline_sub_chip_${sub.name}"
            )
        }
    }
}

@Composable
private fun SubcategoryQuickChip(
    label: String,
    isSelected: Boolean,
    activeGradient: Brush,
    inactiveBg: Color,
    inactiveBorder: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = RoundedCornerShape(20.dp)
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(160),
        label = "sub_chip_text_$testTag"
    )

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 36.dp)
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = shape,
                        spotColor = IndigoAccent.copy(alpha = 0.35f),
                        ambientColor = VioletAccent.copy(alpha = 0.18f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier.background(activeGradient, shape)
                } else {
                    Modifier
                        .background(inactiveBg, shape)
                        .border(width = 1.dp, color = inactiveBorder, shape = shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.5.sp,
                color = textColor
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Interactive Assigned Budgets preview card displayed directly below the Amount card for Expense
 * transactions. Shows live remaining balance & progress for matching automatic and manually
 * assigned budgets, and opens [AssignedBudgetsPickerSheet] on tap so users can toggle Manual
 * (or additional) budgets directly while adding/editing a transaction.
 */
@Composable
internal fun AssignedBudgetPreviewCard(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    selectedCategoryName: String,
    selectedSubcategoryName: String,
    selectedDateTimestamp: Long,
    pendingAmount: Double,
    excludeFromBudgets: Boolean,
    editingTransactionId: String?,
    currencySymbol: String,
    language: AppLanguage,
    assignedBudgetIds: Set<String> = emptySet(),
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isBn = language == AppLanguage.BANGLA

    val monthBudgets = remember(budgets, selectedDateTimestamp) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateTimestamp }
        val txYear = cal.get(Calendar.YEAR)
        val txMonth = cal.get(Calendar.MONTH)
        val monthKey = String.format(Locale.US, "%04d-%02d", txYear, txMonth + 1)
        budgets.filter {
            (it.monthYear == monthKey || it.monthYear == "ALL") && it.monthlyLimit > 0.0
        }.ifEmpty {
            budgets.filter { it.monthlyLimit > 0.0 }
        }
    }

    val matchedBudgetsPreview = remember(
        monthBudgets,
        transactions,
        selectedCategoryName,
        selectedSubcategoryName,
        selectedDateTimestamp,
        pendingAmount,
        excludeFromBudgets,
        editingTransactionId,
        assignedBudgetIds,
        isBn
    ) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateTimestamp }
        val txYear = cal.get(Calendar.YEAR)
        val txMonth = cal.get(Calendar.MONTH)

        val probeTx = TransactionEntity(
            id = editingTransactionId ?: "probe",
            type = com.example.data.model.TransactionType.EXPENSE,
            amount = pendingAmount.coerceAtLeast(0.0),
            title = "",
            category = selectedCategoryName,
            subcategory = selectedSubcategoryName,
            accountId = "",
            timestamp = selectedDateTimestamp,
            excludeFromBudgets = false,
            assignedBudgetIds = assignedBudgetIds.joinToString(",")
        )

        val matched = monthBudgets.filter { b ->
            FinancialIntegrityValidator.isMatchingBudgetTransaction(probeTx, b)
        }.sortedWith(
            compareByDescending<BudgetEntity> { it.isManual && assignedBudgetIds.contains(it.id) }
                .thenByDescending { !it.isOverall && (it.subcategoryName.isNotBlank() || it.parsedSubcategoryPairs().isNotEmpty()) }
                .thenByDescending { !it.isOverall }
        )

        val primary = matched.firstOrNull()
        if (primary == null) {
            null
        } else {
            val existingSpent = transactions
                .filter { tx ->
                    tx.id != editingTransactionId &&
                        FinancialIntegrityValidator.isInYearMonth(tx.timestamp, txYear, txMonth) &&
                        FinancialIntegrityValidator.isMatchingBudgetTransaction(tx, primary)
                }
                .sumOf { it.amount }
            val projectedSpent = if (excludeFromBudgets) existingSpent else existingSpent + pendingAmount.coerceAtLeast(0.0)
            val limit = primary.monthlyLimit
            val remaining = limit - projectedSpent
            val ratio = if (limit > 0.0) (projectedSpent / limit).toFloat() else 0f
            val label = when {
                primary.name.isNotBlank() && !primary.name.equals("Overall", ignoreCase = true) -> primary.name
                primary.isOverall || primary.categoryName.equals("Overall", ignoreCase = true) ->
                    if (isBn) "সামগ্রিক বাজেট" else "Overall Budget"
                primary.subcategoryName.isNotBlank() ->
                    "${primary.categoryName} • ${primary.subcategoryName}"
                else -> primary.categoryName
            }
            Quadruple(label, remaining, ratio, matched.size)
        }
    }

    val hasManualBudgets = remember(monthBudgets) { monthBudgets.any { it.isManual } }
    val canTapToAssign = onClick != null && !excludeFromBudgets && monthBudgets.isNotEmpty()
    val isActiveBudgetState = (matchedBudgetsPreview != null || hasManualBudgets) && !excludeFromBudgets

    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)

    val (accentColor, statusLabel, pctInt, progressTarget) = when {
        excludeFromBudgets -> {
            Quadruple(
                Color(0xFF64748B),
                if (isBn) "বাজেট থেকে বাদ দেওয়া হয়েছে" else "Excluded from monthly budgets",
                null,
                0f
            )
        }
        matchedBudgetsPreview == null -> {
            Quadruple(
                if (hasManualBudgets) IndigoAccent else Color(0xFF64748B),
                if (hasManualBudgets) {
                    com.example.ui.util.AppStrings.t("tap_to_assign_budgets", language)
                } else {
                    if (isBn) "বাজেটহীন ক্যাটাগরি" else "Unbudgeted Category"
                },
                null,
                0f
            )
        }
        else -> {
            val (budgetLabel, remaining, ratio, matchCount) = matchedBudgetsPreview
            val color = when {
                ratio > 1f -> ExceededRose
                ratio >= 0.8f -> WarningAmber
                else -> HealthyEmerald
            }
            val pct = (ratio * 100f).toInt().coerceIn(0, 999)
            val extraSuffix = if (matchCount > 1) {
                val extra = matchCount - 1
                val extraTxt = if (isBn) CurrencyFormatter.convertToBanglaDigits(extra.toString()) else extra.toString()
                " (+$extraTxt)"
            } else {
                ""
            }
            val status = if (remaining >= 0.0) {
                val remStr = CurrencyFormatter.format(remaining, currencySymbol, language = language)
                if (isBn) "$budgetLabel$extraSuffix • $remStr বাকি" else "$budgetLabel$extraSuffix • $remStr left"
            } else {
                val overStr = CurrencyFormatter.format(abs(remaining), currencySymbol, language = language)
                if (isBn) "$budgetLabel$extraSuffix • $overStr বেশি" else "$budgetLabel$extraSuffix • Over by $overStr"
            }
            Quadruple(color, status, pct, ratio.coerceIn(0f, 1f))
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "assigned_budget_progress"
    )
    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 220),
        label = "assigned_budget_accent"
    )

    val shape = RoundedCornerShape(18.dp)
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                if (isActiveBudgetState) animatedAccent.copy(alpha = 0.36f) else Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                if (isActiveBudgetState) animatedAccent.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceColor, shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(if (canTapToAssign) Modifier.clickable { onClick?.invoke() } else Modifier)
            .testTag("assigned_budget_preview_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(animatedAccent.copy(alpha = if (isDark) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (excludeFromBudgets) Icons.Rounded.Block else Icons.Rounded.PieChart,
                    contentDescription = com.example.ui.util.AppStrings.t("assigned_budgets_title", language),
                    tint = animatedAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = com.example.ui.util.AppStrings.t("assigned_budgets_title", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        ),
                        maxLines = 1
                    )
                    if (canTapToAssign) {
                        Text(
                            text = if (isBn) "• সম্পাদনা" else "• Tap to toggle",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA5B4FC) else IndigoAccent
                            ),
                            maxLines = 1
                        )
                    }
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if ((matchedBudgetsPreview != null || hasManualBudgets) && !excludeFromBudgets) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (pctInt != null && !excludeFromBudgets) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    val pctText = if (isBn) {
                        "${CurrencyFormatter.convertToBanglaDigits(pctInt.toString())}%"
                    } else {
                        "$pctInt%"
                    }
                    Text(
                        text = pctText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = animatedAccent,
                            fontFeatureSettings = "tnum"
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.10f)
                                else Color(0xFFE2E8F0)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = animatedProgress)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(animatedAccent)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun AssignedBudgetsPickerSheet(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    selectedCategoryName: String,
    selectedSubcategoryName: String,
    selectedDateTimestamp: Long,
    editingTransactionId: String?,
    assignedBudgetIds: Set<String>,
    currencySymbol: String,
    language: AppLanguage,
    onToggleBudgetId: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sheetBg = if (isDark) Color(0xFF131426) else Color(0xFFFDFDFF)
    val cardBg = if (isDark) Color(0xFF1C1E35) else Color(0xFFF4F5FB)
    val borderCol = if (isDark) Color.White.copy(alpha = 0.08f) else IndigoAccent.copy(alpha = 0.12f)

    val cal = remember(selectedDateTimestamp) {
        Calendar.getInstance().apply { timeInMillis = selectedDateTimestamp }
    }
    val txYear = cal.get(Calendar.YEAR)
    val txMonth = cal.get(Calendar.MONTH)
    val monthKey = remember(txYear, txMonth) {
        String.format(Locale.US, "%04d-%02d", txYear, txMonth + 1)
    }

    val activeBudgets = remember(budgets, monthKey) {
        budgets.filter {
            !it.isOverall &&
                !it.categoryName.equals("Overall", ignoreCase = true) &&
                it.monthlyLimit > 0.0 &&
                (it.monthYear == monthKey || it.monthYear == "ALL")
        }
    }

    val manualBudgets = remember(activeBudgets) { activeBudgets.filter { it.isManual } }
    val autoBudgets = remember(activeBudgets) { activeBudgets.filter { !it.isManual } }

    val autoProbeTx = remember(selectedCategoryName, selectedSubcategoryName, selectedDateTimestamp) {
        TransactionEntity(
            id = "probe_auto",
            type = com.example.data.model.TransactionType.EXPENSE,
            amount = 1.0,
            title = "",
            category = selectedCategoryName,
            subcategory = selectedSubcategoryName,
            accountId = "",
            timestamp = selectedDateTimestamp,
            excludeFromBudgets = false,
            assignedBudgetIds = ""
        )
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = com.example.ui.util.AppStrings.t("assigned_budgets_title", language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (activeBudgets.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Text(
                        text = com.example.ui.util.AppStrings.t("no_budgets_available_assign", language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (manualBudgets.isNotEmpty()) {
                        item {
                            Text(
                                text = com.example.ui.util.AppStrings.t("manual_budgets_header", language),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFC4B5FD) else VioletAccent
                            )
                        }
                        items(manualBudgets.size, key = { "manual_${manualBudgets[it].id}" }) { idx ->
                            val budget = manualBudgets[idx]
                            val isAssigned = assignedBudgetIds.contains(budget.id)
                            val spent = remember(transactions, budget.id, txYear, txMonth, editingTransactionId) {
                                transactions.filter { tx ->
                                    tx.id != editingTransactionId &&
                                        FinancialIntegrityValidator.isInYearMonth(tx.timestamp, txYear, txMonth) &&
                                        FinancialIntegrityValidator.isMatchingBudgetTransaction(tx, budget)
                                }.sumOf { it.amount }
                            }
                            AssignedBudgetSheetRow(
                                budget = budget,
                                spent = spent,
                                isChecked = isAssigned,
                                isAutoMatched = false,
                                currencySymbol = currencySymbol,
                                language = language,
                                isDark = isDark,
                                cardBg = cardBg,
                                borderCol = borderCol,
                                onToggle = { onToggleBudgetId(budget.id) }
                            )
                        }
                    }

                    if (autoBudgets.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = com.example.ui.util.AppStrings.t("automatic_budgets_header", language),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA5B4FC) else IndigoAccent
                            )
                        }
                        items(autoBudgets.size, key = { "auto_${autoBudgets[it].id}" }) { idx ->
                            val budget = autoBudgets[idx]
                            val isAutoMatch = FinancialIntegrityValidator.isMatchingBudgetTransaction(autoProbeTx, budget)
                            val isExplicit = assignedBudgetIds.contains(budget.id)
                            val spent = remember(transactions, budget.id, txYear, txMonth, editingTransactionId) {
                                transactions.filter { tx ->
                                    tx.id != editingTransactionId &&
                                        FinancialIntegrityValidator.isInYearMonth(tx.timestamp, txYear, txMonth) &&
                                        FinancialIntegrityValidator.isMatchingBudgetTransaction(tx, budget)
                                }.sumOf { it.amount }
                            }
                            AssignedBudgetSheetRow(
                                budget = budget,
                                spent = spent,
                                isChecked = isAutoMatch || isExplicit,
                                isAutoMatched = isAutoMatch,
                                currencySymbol = currencySymbol,
                                language = language,
                                isDark = isDark,
                                cardBg = cardBg,
                                borderCol = borderCol,
                                onToggle = {
                                    if (!isAutoMatch) {
                                        onToggleBudgetId(budget.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(IndigoAccent, VioletAccent)))
                    .clickable { onDismiss() }
                    .testTag("assigned_budgets_sheet_done"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = com.example.ui.util.AppStrings.t("done_label", language),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AssignedBudgetSheetRow(
    budget: BudgetEntity,
    spent: Double,
    isChecked: Boolean,
    isAutoMatched: Boolean,
    currencySymbol: String,
    language: AppLanguage,
    isDark: Boolean,
    cardBg: Color,
    borderCol: Color,
    onToggle: () -> Unit
) {
    val accent = try {
        val clean = if (budget.colorHex.startsWith("#")) budget.colorHex else "#${budget.colorHex}"
        Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
        IndigoAccent
    }
    val remaining = budget.monthlyLimit - spent
    val isBn = language == AppLanguage.BANGLA
    val subLine = if (remaining >= 0.0) {
        val remStr = CurrencyFormatter.format(remaining, currencySymbol, language = language)
        if (isBn) "$remStr বাকি" else "$remStr remaining"
    } else {
        val overStr = CurrencyFormatter.format(abs(remaining), currencySymbol, language = language)
        if (isBn) "$overStr বেশি" else "Over by $overStr"
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isChecked) IndigoAccent.copy(alpha = if (isDark) 0.18f else 0.08f) else cardBg,
        border = BorderStroke(
            width = if (isChecked) 1.4.dp else 1.dp,
            color = if (isChecked) IndigoAccent.copy(alpha = 0.55f) else borderCol
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = !isAutoMatched) { onToggle() }
            .testTag("assign_budget_row_${budget.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = if (isDark) 0.24f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = com.example.ui.components.getIconForName(budget.iconName.ifBlank { "category" }),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = budget.name.ifBlank { budget.categoryName },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isAutoMatched) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = HealthyEmerald.copy(alpha = if (isDark) 0.22f else 0.14f)
                        ) {
                            Text(
                                text = com.example.ui.util.AppStrings.t("auto_matched_badge", language),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = HealthyEmerald,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    } else if (budget.isManual) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = VioletAccent.copy(alpha = if (isDark) 0.24f else 0.14f)
                        ) {
                            Text(
                                text = com.example.ui.util.AppStrings.t("manual_badge_short", language),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFC4B5FD) else VioletAccent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remaining < 0.0) ExceededRose else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.Checkbox(
                checked = isChecked,
                onCheckedChange = { if (!isAutoMatched) onToggle() },
                enabled = !isAutoMatched,
                colors = androidx.compose.material3.CheckboxDefaults.colors(
                    checkedColor = IndigoAccent,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    checkmarkColor = Color.White,
                    disabledCheckedColor = HealthyEmerald
                )
            )
        }
    }
}

/**
 * Sleek "Additional Options" Bento section hosting the "Exclude from budgets" toggle
 * for one-off or non-budgeted expenses.
 */
@Composable
internal fun AdditionalOptionsBudgetToggleCard(
    excludeFromBudgets: Boolean,
    onToggleExclude: (Boolean) -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isBn = language == AppLanguage.BANGLA

    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val shape = RoundedCornerShape(18.dp)
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                if (excludeFromBudgets) IndigoAccent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                if (excludeFromBudgets) IndigoAccent.copy(alpha = 0.30f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("additional_options_section")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = IndigoAccent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isBn) "অতিরিক্ত বিকল্প" else "Additional Options",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(surfaceColor, shape)
                .border(width = 1.dp, brush = borderBrush, shape = shape)
                .clickable { onToggleExclude(!excludeFromBudgets) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (excludeFromBudgets) IndigoAccent.copy(alpha = if (isDark) 0.25f else 0.13f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = if (excludeFromBudgets) IndigoAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) "বাজেট থেকে বাদ দিন" else "Exclude from budgets",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isBn) {
                            "মাসিক ও ক্যাটাগরি বাজেট সীমায় গণনা হবে না"
                        } else {
                            "Don't count toward monthly & category limits"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        ),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = excludeFromBudgets,
                    onCheckedChange = onToggleExclude,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IndigoAccent,
                        uncheckedThumbColor = if (isDark) Color(0xFF94A3B8) else Color.White,
                        uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                        uncheckedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("exclude_from_budgets_switch")
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
