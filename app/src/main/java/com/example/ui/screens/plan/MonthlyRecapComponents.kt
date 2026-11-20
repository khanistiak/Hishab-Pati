package com.example.ui.screens.plan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.ui.components.getIconForName
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.RecapCategorySpend
import com.example.ui.util.RecapInsightType
import com.example.ui.util.RecapSummary
import com.example.ui.util.RecapWalletUsage
import java.util.Locale

internal val RecapTrackLight = Color(0xFFE2E8F0)
internal val RecapTrackDark = Color(0xFF334155)

internal val RecapEmerald = Color(0xFF16A34A)
internal val RecapEmeraldDarkText = Color(0xFF4ADE80)
internal val RecapEmeraldSoft = Color(0xFFDCFCE7)

internal val RecapCoral = Color(0xFFDC2626)
internal val RecapCoralDarkText = Color(0xFFF87171)
internal val RecapCoralSoft = Color(0xFFFEE2E2)

internal val RecapRoyalBlue = Color(0xFF2563EB)
internal val RecapRoyalBlueDarkText = Color(0xFF60A5FA)
internal val RecapRoyalBlueSoft = Color(0xFFDBEAFE)

internal val RecapAmber = Color(0xFFD97706)
internal val RecapAmberDarkText = Color(0xFFFBBF24)
internal val RecapAmberSoft = Color(0xFFFEF3C7)

internal val RecapTeal = Color(0xFF0D9488)
internal val RecapTealDarkText = Color(0xFF2DD4BF)

internal val RecapViolet = Color(0xFF7C3AED)

internal data class RecapCardPalette(
    val fill: Color,
    val divider: Color,
    val track: Color,
    val isDark: Boolean
)

@Composable
internal fun rememberRecapCardPalette(isDark: Boolean): RecapCardPalette {
    return remember(isDark) {
        if (isDark) {
            RecapCardPalette(
                fill = CardBackgroundDark,
                divider = Color(0x1AFFFFFF),
                track = RecapTrackDark,
                isDark = true
            )
        } else {
            RecapCardPalette(
                fill = Color.White,
                divider = Color(0xFFF1F5F9),
                track = RecapTrackLight,
                isDark = false
            )
        }
    }
}

/**
 * Clean, borderless floating Bento card matching Hishab Pati's Home, Personal Finance, and Calendar screens.
 */
@Composable
internal fun RecapBentoCard(
    palette: RecapCardPalette,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    horizontalPadding: Dp = 18.dp,
    verticalPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = palette.fill,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            content = content
        )
    }
}

@Composable
internal fun RecapSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingBadge: String? = null,
    showSparkle: Boolean = false,
    isDark: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (showSparkle) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFC4B5FD) else RecapViolet,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        if (!trailingBadge.isNullOrBlank()) {
            Text(
                text = trailingBadge,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
internal fun RecapMonthSelectorPill(
    label: String,
    language: AppLanguage,
    palette: RecapCardPalette,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = palette.fill,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("monthly_recap_month_selector")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .clickable(onClick = onPrevMonth)
                    .testTag("recap_prev_month_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = AppStrings.t("previous_month", language),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .clickable(onClick = onNextMonth)
                    .testTag("recap_next_month_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = AppStrings.t("next_month", language),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
internal fun FinancialHealthHeroCard(
    recap: RecapSummary,
    currencySymbol: String,
    language: AppLanguage,
    palette: RecapCardPalette,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA

    val healthTitle = when {
        !recap.hasData || (recap.totalIncome == 0.0 && recap.totalExpense == 0.0) ->
            AppStrings.t("no_data_for_month", language)
        recap.grade == "A" -> AppStrings.t("excellent_financial_health", language)
        recap.grade == "B" -> AppStrings.t("good_financial_health", language)
        recap.grade == "C" -> AppStrings.t("fair_financial_health", language)
        else -> AppStrings.t("expenses_exceeded_income", language)
    }

    val localizedSavingsPct = CurrencyFormatter.formatPercent(recap.savingsRate, language)
    val healthSubtitle = when {
        !recap.hasData || (recap.totalIncome == 0.0 && recap.totalExpense == 0.0) ->
            AppStrings.t("no_records_for_period", language)
        recap.grade in listOf("A", "B", "C") -> {
            if (isBn) "আপনি আয়ের $localizedSavingsPct সঞ্চয় করেছেন"
            else "You saved ${recap.savingsRate}% of your income"
        }
        else -> AppStrings.t("expenses_exceeded_income_desc", language)
    }

    val (gradePillBg, gradePillText) = when (recap.grade) {
        "A" -> if (palette.isDark) {
            RecapEmerald.copy(alpha = 0.22f) to RecapEmeraldDarkText
        } else {
            RecapEmeraldSoft to RecapEmerald
        }
        "B" -> if (palette.isDark) {
            RecapRoyalBlue.copy(alpha = 0.22f) to RecapRoyalBlueDarkText
        } else {
            RecapRoyalBlueSoft to RecapRoyalBlue
        }
        "C" -> if (palette.isDark) {
            RecapAmber.copy(alpha = 0.22f) to RecapAmberDarkText
        } else {
            RecapAmberSoft to RecapAmber
        }
        "D" -> if (palette.isDark) {
            RecapCoral.copy(alpha = 0.22f) to RecapCoralDarkText
        } else {
            RecapCoralSoft to RecapCoral
        }
        else -> if (palette.isDark) {
            Color(0xFF334155) to Color(0xFFCBD5E1)
        } else {
            Color(0xFFF1F5F9) to Color(0xFF64748B)
        }
    }

    val gradeLabel = if (recap.grade == "–") {
        "${AppStrings.t("grade_prefix", language)} –"
    } else {
        "${AppStrings.t("grade_prefix", language)} ${recap.grade}"
    }

    val incomeColor = if (palette.isDark) RecapEmeraldDarkText else RecapEmerald
    val expenseColor = if (palette.isDark) RecapCoralDarkText else RecapCoral
    val netColor = when {
        recap.netSavings < 0 -> expenseColor
        palette.isDark -> RecapRoyalBlueDarkText
        else -> RecapRoyalBlue
    }

    val animatedSavingsProgress by animateFloatAsState(
        targetValue = if (recap.hasData && recap.netSavings > 0) {
            (recap.savingsRate / 100f).coerceIn(0f, 1f)
        } else {
            0f
        },
        animationSpec = tween(durationMillis = 420),
        label = "recap_savings_progress"
    )

    RecapBentoCard(
        palette = palette,
        cornerRadius = 24.dp,
        horizontalPadding = 20.dp,
        verticalPadding = 20.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_recap_hero_card")
    ) {
        // Top Row: Title + Subtitle on left, soft emerald Grade Pill on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = healthTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = healthSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = CircleShape,
                color = gradePillBg
            ) {
                Text(
                    text = gradeLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = gradePillText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    maxLines = 1
                )
            }
        }

        // Middle 3-Column Strip (20.dp vertical padding, clean layout without vertical divider clutter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeroNumberColumn(
                label = AppStrings.t("income", language),
                value = CurrencyFormatter.format(recap.totalIncome, currencySymbol, language = language),
                valueColor = incomeColor,
                alignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            )

            HeroNumberColumn(
                label = AppStrings.t("expenses", language),
                value = CurrencyFormatter.format(recap.totalExpense, currencySymbol, language = language),
                valueColor = expenseColor,
                alignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            )

            HeroNumberColumn(
                label = AppStrings.t("net_saved", language),
                value = CurrencyFormatter.format(
                    amount = recap.netSavings,
                    symbol = currencySymbol,
                    showSign = recap.netSavings > 0,
                    language = language
                ),
                valueColor = netColor,
                alignment = Alignment.End,
                modifier = Modifier.weight(1f)
            )
        }

        // Bottom Savings Rate Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppStrings.t("savings_rate", language),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = "$localizedSavingsPct (${CurrencyFormatter.format(recap.netSavings, currencySymbol, language = language)})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (recap.netSavings >= 0) incomeColor else expenseColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(palette.track)
        ) {
            if (animatedSavingsProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedSavingsProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (recap.netSavings >= 0) incomeColor else expenseColor)
                )
            }
        }
    }
}

@Composable
private fun HeroNumberColumn(
    label: String,
    value: String,
    valueColor: Color,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    val textAlign = when (alignment) {
        Alignment.Start -> TextAlign.Start
        Alignment.End -> TextAlign.End
        else -> TextAlign.Center
    }
    Column(
        modifier = modifier,
        horizontalAlignment = alignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign
        )
    }
}

/**
 * Typography-first KPI Bento card (no squircle icons) matching Home's Total Balance / Spent cards.
 */
@Composable
internal fun RecapMetricBentoCard(
    title: String,
    value: String,
    subtitle: String,
    palette: RecapCardPalette,
    modifier: Modifier = Modifier,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueFontSize: TextUnit = 20.sp
) {
    RecapBentoCard(
        palette = palette,
        cornerRadius = 22.dp,
        horizontalPadding = 18.dp,
        verticalPadding = 18.dp,
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = valueFontSize,
                fontWeight = FontWeight.Bold,
                color = valueColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = subtitleColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun TopCategoriesBentoCard(
    topCategories: List<RecapCategorySpend>,
    categoryLookup: Map<String, CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage,
    palette: RecapCardPalette,
    modifier: Modifier = Modifier
) {
    val fallbackColors = remember {
        listOf(RecapRoyalBlue, RecapCoral, RecapAmber)
    }

    RecapBentoCard(
        palette = palette,
        cornerRadius = 22.dp,
        horizontalPadding = 18.dp,
        verticalPadding = 18.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_recap_top_categories_card")
    ) {
        if (topCategories.isEmpty()) {
            Text(
                text = AppStrings.t("no_expenses_this_month", language),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                topCategories.forEachIndexed { index, item ->
                    val catEntity = categoryLookup[item.category.lowercase(Locale.ENGLISH)]
                    val displayName = catEntity?.getDisplayName(language) ?: item.category
                    val iconVec = catEntity?.iconName?.takeIf { it.isNotBlank() }?.let {
                        getIconForName(it)
                    } ?: Icons.Rounded.Category
                    val accentColor = parseHexColorOrDefault(
                        hex = catEntity?.colorHex,
                        fallback = fallbackColors[index % fallbackColors.size]
                    )

                    val barFraction by animateFloatAsState(
                        targetValue = (item.sharePct / 100f).coerceIn(0.03f, 1f),
                        animationSpec = tween(durationMillis = 380),
                        label = "top_cat_bar_$index"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tinted 38.dp entity squircle icon
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = if (palette.isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVec,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 10.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = CurrencyFormatter.format(item.amount, currencySymbol, language = language),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatPercent(item.sharePct, language),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(7.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(palette.track)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(barFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(accentColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WalletUsageBentoCard(
    walletUsages: List<RecapWalletUsage>,
    currencySymbol: String,
    language: AppLanguage,
    palette: RecapCardPalette,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA

    RecapBentoCard(
        palette = palette,
        cornerRadius = 22.dp,
        horizontalPadding = 18.dp,
        verticalPadding = 18.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_recap_wallet_usage_card")
    ) {
        if (walletUsages.isEmpty()) {
            Text(
                text = AppStrings.t("no_wallet_activity_this_month", language),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            walletUsages.forEachIndexed { index, wallet ->
                if (index > 0) {
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = palette.divider,
                        modifier = Modifier.padding(vertical = 13.dp)
                    )
                }

                val accentColor = parseHexColorOrDefault(wallet.colorHex, RecapRoyalBlue)
                val iconVec = resolveWalletIcon(wallet.accountType, wallet.iconName)
                val txCountStr = CurrencyFormatter.formatNumber(wallet.transactionCount, language)
                val txSubtitle = if (isBn) {
                    "$txCountStr টি লেনদেন"
                } else {
                    val unit = if (wallet.transactionCount == 1) "transaction" else "transactions"
                    "$txCountStr $unit"
                }

                val hasSpend = wallet.spendTotal > 0.0
                val amountText = if (hasSpend) {
                    CurrencyFormatter.format(wallet.spendTotal, currencySymbol, language = language)
                } else if (wallet.incomeTotal > 0.0) {
                    CurrencyFormatter.format(wallet.incomeTotal, currencySymbol, showSign = true, language = language)
                } else {
                    CurrencyFormatter.format(0.0, currencySymbol, language = language)
                }
                val amountSubLabel = if (hasSpend) {
                    AppStrings.t("spent_short", language)
                } else if (wallet.incomeTotal > 0.0) {
                    AppStrings.t("received_short", language)
                } else {
                    AppStrings.t("spent_short", language)
                }
                val amountColor = if (!hasSpend && wallet.incomeTotal > 0.0) {
                    if (palette.isDark) RecapEmeraldDarkText else RecapEmerald
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tinted 38.dp entity squircle icon
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = if (palette.isDark) 0.22f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVec,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = wallet.accountName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = txSubtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = amountText,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = amountSubLabel,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

internal data class LocalizedRecapInsight(
    val title: String,
    val description: String,
    val type: RecapInsightType
)

/**
 * Clean icon-free Monthly Insights Bento card using simple 6.dp colored status dots
 * and 14.dp vertical row padding separated by hairline dividers.
 */
@Composable
internal fun MonthlyInsightsBentoCard(
    insights: List<LocalizedRecapInsight>,
    palette: RecapCardPalette,
    modifier: Modifier = Modifier
) {
    RecapBentoCard(
        palette = palette,
        cornerRadius = 22.dp,
        horizontalPadding = 18.dp,
        verticalPadding = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_recap_insights_card")
    ) {
        insights.forEachIndexed { index, insight ->
            if (index > 0) {
                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = palette.divider
                )
            }

            val dotColor = when (insight.type) {
                RecapInsightType.SAVINGS_POSITIVE,
                RecapInsightType.SAVINGS_BALANCED ->
                    if (palette.isDark) RecapEmeraldDarkText else RecapEmerald
                RecapInsightType.SAVINGS_NEGATIVE ->
                    if (palette.isDark) RecapCoralDarkText else RecapCoral
                RecapInsightType.TOP_CATEGORY ->
                    if (palette.isDark) RecapRoyalBlueDarkText else RecapRoyalBlue
                RecapInsightType.INCOME_RECORDED ->
                    if (palette.isDark) RecapTealDarkText else RecapTeal
                RecapInsightType.EMPTY ->
                    if (palette.isDark) Color(0xFF94A3B8) else Color(0xFF94A3B8)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = insight.description,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

internal fun parseHexColorOrDefault(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

internal fun resolveWalletIcon(accountType: AccountType?, iconName: String): ImageVector {
    val normalized = iconName.lowercase(Locale.ENGLISH).trim()
    if (normalized.isNotBlank() && normalized != "category") {
        return getIconForName(normalized)
    }
    return when (accountType) {
        AccountType.BANK -> Icons.Rounded.AccountBalance
        AccountType.MFS -> Icons.Rounded.PhoneAndroid
        AccountType.CREDIT_CARD -> Icons.Rounded.CreditCard
        AccountType.CASH, AccountType.OTHER, null -> Icons.Rounded.AccountBalanceWallet
    }
}
