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
 * Compact Assigned Budget preview card displayed directly below the Amount card for Expense
 * transactions. Shows live remaining balance & progress for the matching subcategory, category,
 * or overall budget—or clearly indicates Unbudgeted Category / Excluded from budgets.
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
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isBn = language == AppLanguage.BANGLA

    val budgetPreview = remember(
        budgets,
        transactions,
        selectedCategoryName,
        selectedSubcategoryName,
        selectedDateTimestamp,
        pendingAmount,
        excludeFromBudgets,
        editingTransactionId
    ) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateTimestamp }
        val txYear = cal.get(Calendar.YEAR)
        val txMonth = cal.get(Calendar.MONTH)
        val monthKey = String.format(Locale.US, "%04d-%02d", txYear, txMonth + 1)

        val monthBudgets = budgets.filter {
            (it.monthYear == monthKey || it.monthYear == "ALL") && it.monthlyLimit > 0.0
        }.ifEmpty {
            budgets.filter { it.monthlyLimit > 0.0 }
        }

        // Priority: exact subcategory budget -> category budget -> overall budget
        val exactSubBudget = if (selectedSubcategoryName.isNotBlank()) {
            monthBudgets.firstOrNull {
                !it.isOverall &&
                    it.categoryName.trim().equals(selectedCategoryName.trim(), ignoreCase = true) &&
                    it.subcategoryName.trim().equals(selectedSubcategoryName.trim(), ignoreCase = true)
            }
        } else {
            null
        }
        val categoryBudget = monthBudgets.firstOrNull {
            !it.isOverall &&
                it.categoryName.trim().equals(selectedCategoryName.trim(), ignoreCase = true) &&
                it.subcategoryName.isBlank()
        }
        val overallBudget = monthBudgets.firstOrNull {
            it.isOverall || it.categoryName.equals("Overall", ignoreCase = true)
        }

        val matchedBudget = exactSubBudget ?: categoryBudget ?: overallBudget
        if (matchedBudget == null) {
            null
        } else {
            val existingSpent = transactions
                .filter { tx ->
                    tx.id != editingTransactionId &&
                        FinancialIntegrityValidator.isInYearMonth(tx.timestamp, txYear, txMonth) &&
                        FinancialIntegrityValidator.isMatchingBudgetTransaction(tx, matchedBudget)
                }
                .sumOf { it.amount }
            val projectedSpent = if (excludeFromBudgets) existingSpent else existingSpent + pendingAmount.coerceAtLeast(0.0)
            val limit = matchedBudget.monthlyLimit
            val remaining = limit - projectedSpent
            val ratio = if (limit > 0.0) (projectedSpent / limit).toFloat() else 0f
            val label = when {
                matchedBudget.isOverall || matchedBudget.categoryName.equals("Overall", ignoreCase = true) ->
                    if (isBn) "সামগ্রিক বাজেট" else "Overall Budget"
                matchedBudget.subcategoryName.isNotBlank() ->
                    "${matchedBudget.categoryName} • ${matchedBudget.subcategoryName}"
                else -> matchedBudget.categoryName
            }
            Triple(label, remaining, ratio)
        }
    }

    val surfaceColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
    } else {
        Color.White.copy(alpha = 0.90f)
    }

    val (accentColor, statusLabel, pctInt, progressTarget) = when {
        excludeFromBudgets -> {
            Quadruple(
                Color(0xFF64748B),
                if (isBn) "বাজেট থেকে বাদ দেওয়া হয়েছে" else "Excluded from monthly budgets",
                null,
                0f
            )
        }
        budgetPreview == null -> {
            Quadruple(
                Color(0xFF64748B),
                if (isBn) "বাজেটহীন ক্যাটাগরি" else "Unbudgeted Category",
                null,
                0f
            )
        }
        else -> {
            val (budgetLabel, remaining, ratio) = budgetPreview
            val color = when {
                ratio > 1f -> ExceededRose
                ratio >= 0.8f -> WarningAmber
                else -> HealthyEmerald
            }
            val pct = (ratio * 100f).toInt().coerceIn(0, 999)
            val status = if (remaining >= 0.0) {
                val remStr = CurrencyFormatter.format(remaining, currencySymbol, language = language)
                if (isBn) "$budgetLabel • $remStr বাকি" else "$budgetLabel • $remStr left"
            } else {
                val overStr = CurrencyFormatter.format(abs(remaining), currencySymbol, language = language)
                if (isBn) "$budgetLabel • $overStr বেশি" else "$budgetLabel • Over by $overStr"
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

    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = surfaceColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (budgetPreview != null && !excludeFromBudgets) {
                animatedAccent.copy(alpha = if (isDark) 0.35f else 0.25f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            }
        ),
        shadowElevation = if (isDark) 0.dp else 1.5.dp,
        modifier = modifier
            .fillMaxWidth()
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
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(animatedAccent.copy(alpha = if (isDark) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (excludeFromBudgets) Icons.Rounded.Block else Icons.Rounded.PieChart,
                    contentDescription = if (isBn) "নির্ধারিত বাজেট" else "Assigned Budget",
                    tint = animatedAccent,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBn) "নির্ধারিত বাজেট" else "Assigned Budget",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    ),
                    maxLines = 1
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (budgetPreview != null && !excludeFromBudgets) {
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

    val surfaceColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.White.copy(alpha = 0.94f)
    }
    val shape = RoundedCornerShape(18.dp)

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

        Surface(
            shape = shape,
            color = surfaceColor,
            border = BorderStroke(
                width = 1.dp,
                color = if (excludeFromBudgets) {
                    IndigoAccent.copy(alpha = if (isDark) 0.45f else 0.30f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                }
            ),
            shadowElevation = if (isDark) 0.dp else 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
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
