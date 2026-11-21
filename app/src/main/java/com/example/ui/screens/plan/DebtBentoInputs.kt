package com.example.ui.screens.plan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExpenseRed

/**
 * Elevated, tactile Bento input card container for the Debt Add Transaction screen.
 *
 * - Light Mode: Luminous frosted-white surface (`White 94%`), `1.dp` specular gradient border,
 *   and soft ambient elevation shadow (`4.dp`, lifting to `6.dp` on focus).
 * - Dark Mode: Sleek tonal `surfaceContainerHigh` Bento card with a `1.dp` top-left glass rim
 *   (`White 12%`) and soft ambient elevation shadow.
 * - Focus / Error Highlight: Smoothly animates a `1.5.dp` border using the active action accent
 *   (emerald `#059669` for inflow / Lent / Received, coral-rose `#DC2626` for outflow / Repay / Borrow).
 */
@Composable
internal fun DebtBentoInputCard(
    leadingIcon: ImageVector,
    iconTint: Color,
    badgeTint: Color,
    activeAccentColor: Color,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    isError: Boolean = false,
    iconContentDescription: String? = null,
    testTag: String? = null,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardShape = RoundedCornerShape(18.dp)
    val badgeShape = RoundedCornerShape(11.dp)

    val highlightAccent = if (isError) ExpenseRed else activeAccentColor
    val highlightTarget = if (isFocused || isError) 1f else 0f

    val highlightProgress by animateFloatAsState(
        targetValue = highlightTarget,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "debt_bento_focus_progress"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused || isError) 1.5.dp else 1.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "debt_bento_border_width"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isFocused || isError) 6.dp else 4.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "debt_bento_shadow_elevation"
    )
    val animatedIconTint by animateColorAsState(
        targetValue = if (isFocused || isError) highlightAccent else iconTint,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "debt_bento_icon_tint"
    )

    val surfaceColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.White.copy(alpha = 0.94f)
    }

    val outlineMuted = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                lerp(Color.White.copy(alpha = 0.12f), highlightAccent, highlightProgress),
                lerp(Color.White.copy(alpha = 0.05f), highlightAccent.copy(alpha = 0.85f), highlightProgress),
                lerp(Color.White.copy(alpha = 0.02f), highlightAccent.copy(alpha = 0.72f), highlightProgress)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                lerp(Color.White, highlightAccent, highlightProgress),
                lerp(outlineMuted, highlightAccent.copy(alpha = 0.88f), highlightProgress)
            )
        )
    }

    val shadowSpotColor = if (isDark) {
        lerp(Color.Black.copy(alpha = 0.44f), highlightAccent.copy(alpha = 0.24f), highlightProgress)
    } else {
        lerp(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            highlightAccent.copy(alpha = 0.16f),
            highlightProgress
        )
    }
    val shadowAmbientColor = if (isDark) {
        Color.Black.copy(alpha = 0.36f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
    }

    val badgeBg = lerp(
        badgeTint.copy(alpha = if (isDark) 0.20f else 0.11f),
        highlightAccent.copy(alpha = if (isDark) 0.24f else 0.13f),
        highlightProgress
    )
    val badgeBorder = lerp(
        badgeTint.copy(alpha = if (isDark) 0.30f else 0.20f),
        highlightAccent.copy(alpha = if (isDark) 0.36f else 0.26f),
        highlightProgress
    )

    var cardModifier = modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 64.dp)
        .shadow(
            elevation = shadowElevation,
            shape = cardShape,
            ambientColor = shadowAmbientColor,
            spotColor = shadowSpotColor
        )
        .clip(cardShape)
        .background(surfaceColor, cardShape)
        .border(width = borderWidth, brush = borderBrush, shape = cardShape)

    if (onClick != null) {
        cardModifier = cardModifier.clickable(onClick = onClick)
    }
    if (testTag != null) {
        cardModifier = cardModifier.testTag(testTag)
    }

    Box(modifier = cardModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = verticalAlignment
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(badgeShape)
                    .background(badgeBg)
                    .border(width = 0.75.dp, color = badgeBorder, shape = badgeShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = iconContentDescription,
                    tint = animatedIconTint,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                content = content
            )

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(10.dp))
                trailingContent()
            }
        }
    }
}

@Composable
internal fun DebtTrailingChevronPill(
    icon: ImageVector,
    iconSize: Dp = 14.dp,
    contentDescription: String? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.42f else 0.50f),
        modifier = Modifier.size(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
