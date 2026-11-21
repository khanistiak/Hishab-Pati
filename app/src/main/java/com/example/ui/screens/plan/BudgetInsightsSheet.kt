package com.example.ui.screens.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.data.model.AppLanguage
import com.example.ui.components.CategoryIcon
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val InsightsRoyalIndigo = Color(0xFF4F46E5)
private val InsightsElectricViolet = Color(0xFF7C3AED)
private val InsightsAmber = Color(0xFFF59E0B)
private val InsightsCoralRose = Color(0xFFF43F5E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetInsightsModalSheet(
    budgetName: String,
    categoryIcon: String,
    categoryColorHex: String,
    currencySymbol: String,
    language: AppLanguage,
    hasDefinedBudget: Boolean,
    limit: Double,
    spent: Double,
    remaining: Double,
    daysElapsed: Int,
    daysRemaining: Int,
    totalDaysInPeriod: Int,
    projectedTotal: Double,
    transactionCount: Int,
    typicalDailySpend: Double,
    typicalPeriodSpend: Double,
    hasHistoricalData: Boolean,
    isHistoricalPeriod: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isBn = language == AppLanguage.BANGLA

    val sheetBg = if (isDark) Color(0xFF0F1424) else Color(0xFFF6F7FE)
    val cardBg = if (isDark) CardBackgroundDark else Color.White
    val cardBorder = if (isDark) Color(0x28A5B4FC) else Color(0x1A6366F1)

    val safeTotalDays = totalDaysInPeriod.coerceAtLeast(1)
    val safeElapsed = daysElapsed.coerceAtLeast(1)
    val actualDailyAvg = spent / safeElapsed.toDouble()
    val targetDailyBudget = if (hasDefinedBudget && limit > 0.0) limit / safeTotalDays.toDouble() else 0.0
    val remainingDailyAllowance = if (hasDefinedBudget && remaining > 0.0 && daysRemaining > 0 && !isHistoricalPeriod) {
        remaining / daysRemaining.toDouble()
    } else {
        0.0
    }
    val burnRate = if (targetDailyBudget > 0.0) actualDailyAvg / targetDailyBudget else 0.0
    val avgTransaction = if (transactionCount > 0) spent / transactionCount.toDouble() else 0.0

    val burnRateColor = when {
        !hasDefinedBudget -> InsightsRoyalIndigo
        burnRate > 1.15 -> InsightsCoralRose
        burnRate >= 0.95 -> InsightsAmber
        else -> IncomeGreen
    }

    val burnRateFormatted = rememberBurnRateText(burnRate, hasDefinedBudget, language)

    val projectedDelta = if (hasDefinedBudget) projectedTotal - limit else 0.0
    val projectedColor = when {
        !hasDefinedBudget -> MaterialTheme.colorScheme.onSurface
        projectedTotal > limit -> InsightsCoralRose
        projectedTotal > limit * 0.85 -> InsightsAmber
        else -> IncomeGreen
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("budget_insights_modal_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Royal Indigo-Violet icon badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(InsightsRoyalIndigo, InsightsElectricViolet)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.t("budget_insights", language),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$budgetName • ${AppStrings.t("budget_insights_subtitle", language)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                CategoryIcon(
                    iconName = categoryIcon,
                    colorHex = categoryColorHex,
                    size = 36.dp
                )
            }

            // 1. Top 2-Column Bento: Remaining Daily Allowance & Average / Day vs Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Remaining Daily Allowance
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = if (isDark) 0.dp else 1.5.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = if (remainingDailyAllowance > 0.0) IncomeGreen else InsightsCoralRose,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.t("remaining_daily_allowance", language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (hasDefinedBudget) {
                                CurrencyFormatter.format(remainingDailyAllowance, currencySymbol, language = language)
                            } else {
                                "—"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (hasDefinedBudget && remaining <= 0.0) InsightsCoralRose else IncomeGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isHistoricalPeriod) {
                                AppStrings.t("period_ended", language)
                            } else {
                                AppStrings.t("per_day", language)
                            },
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Average / Day vs Target Daily Budget
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = if (isDark) 0.dp else 1.5.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = InsightsRoyalIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.t("average_per_day_label", language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = CurrencyFormatter.format(actualDailyAvg, currencySymbol, language = language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hasDefinedBudget) {
                                "${AppStrings.t("target", language)}: ${CurrencyFormatter.format(targetDailyBudget, currencySymbol, language = language)}${AppStrings.t("day_short", language)}"
                            } else {
                                AppStrings.t("per_day", language)
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 2. Budget Burn Rate & Avg. Transaction Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 1.5.dp,
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
                                shape = RoundedCornerShape(10.dp),
                                color = burnRateColor.copy(alpha = 0.14f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = burnRateColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = AppStrings.t("budget_burn_rate", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = burnRateFormatted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = burnRateColor
                                )
                            }
                        }

                        if (hasDefinedBudget) {
                            val multiplierRaw = String.format(Locale.US, "%.2fx", burnRate)
                            val multiplierText = CurrencyFormatter.localizeDigits(multiplierRaw, language)
                            Surface(
                                shape = CircleShape,
                                color = burnRateColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = multiplierText,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = burnRateColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (hasDefinedBudget && targetDailyBudget > 0.0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (burnRate / 2.0).toFloat().coerceIn(0f, 1f) },
                            color = burnRateColor,
                            trackColor = burnRateColor.copy(alpha = 0.14f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Avg. Transaction row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = InsightsElectricViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = AppStrings.t("avg_transaction", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val countStr = CurrencyFormatter.formatNumber(transactionCount, language)
                                Text(
                                    text = "$countStr ${AppStrings.t("transactions_label", language)}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = CurrencyFormatter.format(avgTransaction, currencySymbol, language = language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3. Projected Total Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 1.5.dp,
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
                                shape = RoundedCornerShape(10.dp),
                                color = InsightsRoyalIndigo.copy(alpha = 0.14f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = InsightsRoyalIndigo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = AppStrings.t("projected_total", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasDefinedBudget) {
                                        if (projectedDelta > 0.001) {
                                            "${AppStrings.t("over_by", language)} ${CurrencyFormatter.format(projectedDelta, currencySymbol, language = language)}"
                                        } else {
                                            "${CurrencyFormatter.format(abs(projectedDelta), currencySymbol, language = language)} ${AppStrings.t("remaining_suffix", language)}"
                                        }
                                    } else {
                                        AppStrings.t("no_limit_active", language)
                                    },
                                    fontSize = 12.sp,
                                    color = projectedColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = CurrencyFormatter.format(projectedTotal, currencySymbol, language = language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = projectedColor
                        )
                    }
                }
            }

            // 4. Historical Analysis Bento Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = InsightsElectricViolet.copy(alpha = 0.14f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = InsightsElectricViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = AppStrings.t("historical_analysis", language),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!hasHistoricalData) {
                        Text(
                            text = AppStrings.t("no_past_history", language),
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF161D31) else Color(0xFFF6F7FE),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = AppStrings.t("typical_daily_spend", language),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${CurrencyFormatter.format(typicalDailySpend, currencySymbol, language = language)}${AppStrings.t("day_short", language)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF161D31) else Color(0xFFF6F7FE),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = AppStrings.t("typical_period_spend", language),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = CurrencyFormatter.format(typicalPeriodSpend, currencySymbol, language = language),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (typicalPeriodSpend > 0.001) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val diffPct = (((projectedTotal - typicalPeriodSpend) / typicalPeriodSpend) * 100.0).roundToInt()
                            val absPctStr = CurrencyFormatter.formatNumber(abs(diffPct), language)
                            val compareLabel = when {
                                diffPct > 2 -> if (isBn) {
                                    "স্বাভাবিক মাসের চেয়ে +$absPctStr% বেশি ব্যয়ের পূর্বাভাস"
                                } else {
                                    "Projected +$absPctStr% above typical period spend"
                                }
                                diffPct < -2 -> if (isBn) {
                                    "স্বাভাবিক মাসের চেয়ে $absPctStr% কম ব্যয়ের পূর্বাভাস"
                                } else {
                                    "Projected $absPctStr% below typical period spend"
                                }
                                else -> if (isBn) {
                                    "স্বাভাবিক মাসিক ব্যয়ের প্রায় সমান"
                                } else {
                                    "In line with your typical period spend"
                                }
                            }
                            Text(
                                text = compareLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (diffPct > 5) InsightsCoralRose else if (diffPct < -5) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun rememberBurnRateText(
    burnRate: Double,
    hasDefinedBudget: Boolean,
    language: AppLanguage
): String {
    if (!hasDefinedBudget) return AppStrings.t("no_limit_active", language)
    val isBn = language == AppLanguage.BANGLA
    return when {
        burnRate > 1.03 -> {
            val pctOver = ((burnRate - 1.0) * 100.0).roundToInt()
            val pctStr = CurrencyFormatter.formatNumber(pctOver, language)
            if (isBn) "লক্ষ্যের চেয়ে $pctStr% দ্রুত ব্যয়" else "Spending $pctStr% ${AppStrings.t("faster_than_pace", language)}"
        }
        burnRate < 0.97 -> {
            val pctUnder = ((1.0 - burnRate) * 100.0).roundToInt()
            val pctStr = CurrencyFormatter.formatNumber(pctUnder, language)
            if (isBn) "লক্ষ্যের চেয়ে $pctStr% কম গতিতে ব্যয়" else "$pctStr% ${AppStrings.t("slower_than_pace", language)}"
        }
        else -> AppStrings.t("on_pace", language)
    }
}
