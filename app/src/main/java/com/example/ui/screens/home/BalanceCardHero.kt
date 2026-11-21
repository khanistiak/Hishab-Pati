package com.example.ui.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.HomeCardStyle
import com.example.ui.screens.accounts.AnimatedGuillocheRosetteCanvas
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter

/**
 * Compact layout switcher icon button placed directly on the Home screen's
 * wallet header bar to cycle smoothly through Balance Card -> Card Stack -> Card Carousel.
 * On the Balance Card surface, renders as a crisp frosted white glass circle.
 */
@Composable
internal fun HomeCardViewSwitcherButton(
    currentStyle: HomeCardStyle,
    isBangla: Boolean,
    onClick: () -> Unit,
    onDarkSurface: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val language = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
    val buttonShape = CircleShape

    val icon = when (currentStyle) {
        HomeCardStyle.BALANCE_CARD -> Icons.Rounded.GridView
        HomeCardStyle.STACK -> Icons.Rounded.ViewCarousel
        HomeCardStyle.CAROUSEL -> Icons.Rounded.Layers
    }

    val containerColor = when {
        onDarkSurface -> Color.White.copy(alpha = 0.18f)
        isDarkTheme -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        else -> Color.White.copy(alpha = 0.88f)
    }

    val borderColor = when {
        onDarkSurface -> Color.White.copy(alpha = 0.28f)
        isDarkTheme -> Color.White.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    }

    val iconTint = if (onDarkSurface) Color.White else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(if (onDarkSurface) 36.dp else 34.dp)
            .clip(buttonShape)
            .background(containerColor, buttonShape)
            .border(width = 1.dp, color = borderColor, shape = buttonShape)
            .clickable(
                role = Role.Button,
                onClickLabel = AppStrings.t("switch_card_view", language)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .testTag("home_card_view_switcher_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = AppStrings.t("switch_card_view", language),
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Hishab Pati signature Royal Indigo-to-Electric Violet Balance Card hero.
 * Features a 3-stop diagonal Royal Indigo -> Indigo-Violet -> Electric Violet gradient matching
 * the Home screen `+` button, 3-layered continuous animated cubic-bezier silk ribbon waves,
 * luminous top-left white light-wash, crisp 1.2.dp specular bevel border, bold FontWeight.Black
 * hero balance typography, and sleek compact `THIS MONTH` capsule pills.
 * Tapping anywhere on the card navigates to the All Accounts screen while the inline privacy
 * eye toggle and top-right view-switcher button stay independent.
 */
@Composable
internal fun BalanceCardHero(
    totalBalance: Double,
    earnedThisMonth: Double,
    spentThisMonth: Double,
    accountCount: Int,
    isBalanceVisible: Boolean,
    currencySymbol: String,
    isBangla: Boolean,
    currentStyle: HomeCardStyle,
    onCardClick: () -> Unit,
    onToggleVisibility: () -> Unit,
    onSwitchViewMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
    val cardShape = RoundedCornerShape(24.dp)

    // Multi-stop diagonal Royal Indigo -> Deep Royal Violet -> Electric Violet gradient
    val indigoVioletGradientColors = remember {
        listOf(
            Color(0xFF4F46E5),
            Color(0xFF6D28D9),
            Color(0xFF7C3AED)
        )
    }

    // Crisp 1.2.dp white specular bevel border (55% top-left -> 15% bottom-right)
    val specularBevelBorderBrush = remember {
        Brush.linearGradient(
            0.00f to Color.White.copy(alpha = 0.55f),
            0.46f to Color.White.copy(alpha = 0.28f),
            1.00f to Color.White.copy(alpha = 0.15f),
            start = Offset(0f, 0f),
            end = Offset(940f, 700f)
        )
    }

    val hiddenTotalMask = "${currencySymbol}••••••"
    val hiddenPillMask = "${currencySymbol}••••"
    val formattedTotal = if (isBalanceVisible) {
        CurrencyFormatter.format(totalBalance, currencySymbol)
    } else {
        hiddenTotalMask
    }
    val formattedIncome = if (isBalanceVisible) {
        CurrencyFormatter.format(earnedThisMonth, currencySymbol)
    } else {
        hiddenPillMask
    }
    val formattedExpense = if (isBalanceVisible) {
        CurrencyFormatter.format(spentThisMonth, currencySymbol)
    } else {
        hiddenPillMask
    }

    var balanceFontSizeSp by remember(formattedTotal) { mutableFloatStateOf(38f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(186.dp)
            .shadow(
                elevation = 10.dp,
                shape = cardShape,
                ambientColor = Color(0xFF4F46E5).copy(alpha = 0.22f),
                spotColor = Color(0xFF4F46E5).copy(alpha = 0.34f)
            )
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(
                    colors = indigoVioletGradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(940f, 700f)
                )
            )
            .clickable(onClick = onCardClick)
            .testTag("home_balance_card_hero")
    ) {
        // 1. Rotating Geometric Guilloché / Spirograph Rosette anchored right-center
        AnimatedGuillocheRosetteCanvas(
            modifier = Modifier.matchParentSize()
        )

        // 2. Crisp ~35-degree diagonal white glossy light-reflection & physical glass crease shadow
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.24f),
                        0.38f to Color.White.copy(alpha = 0.12f),
                        0.48f to Color.White.copy(alpha = 0.02f),
                        0.52f to Color.Black.copy(alpha = 0.06f),
                        1.00f to Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.85f, h * 1.15f)
                )
            )

            // Subtle upper-left specular catch-light enhancing 3D bevel depth
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.12f),
                        0.46f to Color.White.copy(alpha = 0.03f),
                        1.00f to Color.Transparent
                    ),
                    center = Offset(w * 0.08f, h * 0.08f),
                    radius = maxOf(w, h) * 0.50f
                )
            )
        }

        // 3. Clean executive controls & typography evenly distributed in 186.dp height
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: "TOTAL BALANCE" + inline eye icon | clean 36.dp circular view-switcher button only
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = AppStrings.t("total_balance_caps", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.86f),
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.3.sp,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                role = Role.Button,
                                onClick = onToggleVisibility
                            )
                            .testTag("balance_card_visibility_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (isBalanceVisible) "Hide balance" else "Show balance",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HomeCardViewSwitcherButton(
                    currentStyle = currentStyle,
                    isBangla = isBangla,
                    onClick = onSwitchViewMode,
                    onDarkSurface = true
                )
            }

            // Middle Zone: Heavy bold 38.sp FontWeight.Black tabular hero balance
            val heroBalanceText = remember(formattedTotal, currencySymbol, balanceFontSizeSp) {
                buildAnnotatedString {
                    val symbolIdx = formattedTotal.indexOf(currencySymbol)
                    if (currencySymbol.isNotEmpty() && symbolIdx >= 0) {
                        if (symbolIdx > 0) append(formattedTotal.substring(0, symbolIdx))
                        withStyle(
                            SpanStyle(
                                fontSize = (balanceFontSizeSp - 5f).coerceAtLeast(20f).sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White.copy(alpha = 0.96f)
                            )
                        ) {
                            append(currencySymbol)
                        }
                        append(formattedTotal.substring(symbolIdx + currencySymbol.length))
                    } else {
                        append(formattedTotal)
                    }
                }
            }

            Text(
                text = heroBalanceText,
                style = MaterialTheme.typography.displaySmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = balanceFontSizeSp.sp,
                    lineHeight = (balanceFontSizeSp + 4f).sp,
                    letterSpacing = (-0.8).sp,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                onTextLayout = { result ->
                    if (result.didOverflowWidth && balanceFontSizeSp > 22f) {
                        balanceFontSizeSp = (balanceFontSizeSp - 2f).coerceAtLeast(22f)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom Zone: "THIS MONTH" micro-label + two sleek, trim content-adaptive capsule pills
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = AppStrings.t("this_month_caps", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.82f),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        fontSize = 10.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BalanceFlowCapsulePill(
                        amount = formattedIncome,
                        suffix = AppStrings.t("in_short", language),
                        icon = Icons.Rounded.SouthWest,
                        iconTint = Color(0xFF6EE7B7),
                        contentDescription = if (isBangla) "এই মাসের আয়" else "Monthly income",
                        modifier = Modifier.testTag("balance_card_income_pill")
                    )

                    BalanceFlowCapsulePill(
                        amount = formattedExpense,
                        suffix = AppStrings.t("out_short", language),
                        icon = Icons.Rounded.NorthEast,
                        iconTint = Color(0xFFFDA4AF),
                        contentDescription = if (isBangla) "এই মাসের খরচ" else "Monthly spent",
                        modifier = Modifier.testTag("balance_card_expense_pill")
                    )
                }
            }
        }

        // 4. Crisp 1.2.dp white specular bevel perimeter rim
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.2.dp,
                    brush = specularBevelBorderBrush,
                    shape = cardShape
                )
        )
    }
}

@Composable
private fun BalanceFlowCapsulePill(
    amount: String,
    suffix: String,
    icon: ImageVector,
    iconTint: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .wrapContentWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f), CircleShape)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.24f),
                shape = CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(13.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = amount,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    lineHeight = 15.sp,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = suffix,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
