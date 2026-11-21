package com.example.ui.screens.transaction

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.util.AppStrings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tactile, elevated Bento text input container used for Transaction Name and Note (optional)
 * on the Add / Edit Transaction screen.
 *
 * Adapts smoothly to Light and Dark themes and animates a 1.5dp accent border matching the
 * active transaction type (Expense coral-red, Income emerald-green, Transfer royal-blue) on focus.
 */
@Composable
internal fun BentoTransactionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    iconTint: Color,
    badgeBackgroundColor: Color,
    badgeBorderColor: Color,
    activeAccentColor: Color,
    interactionSource: MutableInteractionSource,
    testTag: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    contentDescription: String? = null,
    onFocusGained: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var isFocused by remember { mutableStateOf(false) }

    val focusProgress by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "bento_input_focus_progress"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.5.dp else 1.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "bento_input_border_width"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isFocused) 6.dp else 4.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "bento_input_shadow"
    )
    val animatedIconTint by animateColorAsState(
        targetValue = if (isFocused) activeAccentColor else iconTint,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "bento_input_icon_tint"
    )

    val fieldShape = RoundedCornerShape(18.dp)
    val badgeShape = RoundedCornerShape(11.dp)

    val surfaceColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.White.copy(alpha = 0.94f)
    }

    val outlineMuted = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                lerp(Color.White.copy(alpha = 0.12f), activeAccentColor, focusProgress),
                lerp(Color.White.copy(alpha = 0.05f), activeAccentColor.copy(alpha = 0.85f), focusProgress),
                lerp(Color.White.copy(alpha = 0.02f), activeAccentColor.copy(alpha = 0.72f), focusProgress)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                lerp(Color.White, activeAccentColor, focusProgress),
                lerp(outlineMuted, activeAccentColor.copy(alpha = 0.88f), focusProgress)
            )
        )
    }

    val shadowSpotColor = if (isDark) {
        lerp(Color.Black.copy(alpha = 0.45f), activeAccentColor.copy(alpha = 0.24f), focusProgress)
    } else {
        lerp(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            activeAccentColor.copy(alpha = 0.16f),
            focusProgress
        )
    }
    val shadowAmbientColor = if (isDark) {
        Color.Black.copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 15.sp,
            fontWeight = if (singleLine) FontWeight.Medium else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(activeAccentColor),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocusGained()
                }
            }
            .testTag(testTag),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 58.dp)
                    .shadow(
                        elevation = shadowElevation,
                        shape = fieldShape,
                        ambientColor = shadowAmbientColor,
                        spotColor = shadowSpotColor
                    )
                    .clip(fieldShape)
                    .background(surfaceColor, fieldShape)
                    .border(width = borderWidth, brush = borderBrush, shape = fieldShape)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(badgeShape)
                            .background(
                                lerp(
                                    badgeBackgroundColor,
                                    activeAccentColor.copy(alpha = if (isDark) 0.22f else 0.12f),
                                    focusProgress
                                )
                            )
                            .border(
                                width = 0.75.dp,
                                color = lerp(
                                    badgeBorderColor,
                                    activeAccentColor.copy(alpha = if (isDark) 0.35f else 0.24f),
                                    focusProgress
                                ),
                                shape = badgeShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = contentDescription ?: placeholder,
                            tint = animatedIconTint,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            }
        }
    )
}

/**
 * Tactile, elevated Bento Date selector card with a soft sky-blue squircle calendar badge,
 * stacked "Date" micro-label above bold formatted date text, and a trailing chevron pill.
 */
@Composable
internal fun BentoTransactionDateField(
    label: String,
    formattedDate: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val fieldShape = RoundedCornerShape(18.dp)
    val badgeShape = RoundedCornerShape(11.dp)

    val skyBlue = Color(0xFF0EA5E9)
    val iconTint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val badgeBg = skyBlue.copy(alpha = if (isDark) 0.20f else 0.12f)
    val badgeBorder = skyBlue.copy(alpha = if (isDark) 0.30f else 0.20f)

    val surfaceColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.White.copy(alpha = 0.94f)
    }
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.02f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .shadow(
                elevation = 4.dp,
                shape = fieldShape,
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            )
            .clip(fieldShape)
            .background(surfaceColor, fieldShape)
            .border(width = 1.dp, brush = borderBrush, shape = fieldShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(badgeShape)
                    .background(badgeBg)
                    .border(width = 0.75.dp, color = badgeBorder, shape = badgeShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                    )
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.40f else 0.50f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Silky spring-animated segmented tab switcher (`↓ Expense` / `↑ Income` / `⇄ Transfer`)
 * powered by a single sliding elevated underlay capsule pill inside a frosted glass track.
 */
@Composable
internal fun TransactionTypeSegmentedTabs(
    selectedType: TransactionType,
    language: AppLanguage,
    onSelectType: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val selectedIndex = when (selectedType) {
        TransactionType.EXPENSE -> 0
        TransactionType.INCOME -> 1
        TransactionType.TRANSFER -> 2
        else -> 0
    }

    val expenseAccent = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
    val incomeAccent = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val transferAccent = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val mutedSlate = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val activeAccentColor by animateColorAsState(
        targetValue = when (selectedIndex) {
            0 -> Color(0xFFDC2626)
            1 -> Color(0xFF059669)
            else -> Color(0xFF2563EB)
        },
        animationSpec = tween(durationMillis = 220),
        label = "tabActiveAccentColor"
    )

    val trackBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
    } else {
        Color.White.copy(alpha = 0.55f)
    }
    val trackBorder = Color.White.copy(alpha = if (isDark) 0.14f else 0.45f)

    val pillSurface = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.White
    }
    val pillRimBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.24f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.40f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(trackBg, CircleShape)
            .border(width = 1.dp, color = trackBorder, shape = CircleShape)
            .padding(4.dp)
            .testTag("tx_type_segmented_tabs")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / 3
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "tabIndicatorOffset"
            )

            // Single elevated sliding underlay capsule pill
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = activeAccentColor.copy(alpha = if (isDark) 0.22f else 0.14f),
                        spotColor = activeAccentColor.copy(alpha = if (isDark) 0.30f else 0.22f)
                    )
                    .clip(CircleShape)
                    .background(pillSurface, CircleShape)
                    .border(width = 1.dp, brush = pillRimBrush, shape = CircleShape)
            )

            // Three foreground tab items with synchronized icon + label tint crossfade
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SegmentedTabItem(
                    label = AppStrings.t("expense", language),
                    icon = Icons.Default.ArrowDownward,
                    isSelected = selectedIndex == 0,
                    selectedColor = expenseAccent,
                    unselectedColor = mutedSlate,
                    onClick = { onSelectType(TransactionType.EXPENSE) },
                    testTag = "tx_type_tab_expense",
                    modifier = Modifier.weight(1f)
                )

                SegmentedTabItem(
                    label = AppStrings.t("income", language),
                    icon = Icons.Default.ArrowUpward,
                    isSelected = selectedIndex == 1,
                    selectedColor = incomeAccent,
                    unselectedColor = mutedSlate,
                    onClick = { onSelectType(TransactionType.INCOME) },
                    testTag = "tx_type_tab_income",
                    modifier = Modifier.weight(1f)
                )

                SegmentedTabItem(
                    label = AppStrings.t("transfer_label", language),
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    isSelected = selectedIndex == 2,
                    selectedColor = transferAccent,
                    unselectedColor = mutedSlate,
                    onClick = { onSelectType(TransactionType.TRANSFER) },
                    testTag = "tx_type_tab_transfer",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SegmentedTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val animatedTint by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        animationSpec = tween(durationMillis = 220),
        label = "tabItemTint_$testTag"
    )

    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = animatedTint,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = animatedTint
            ),
            maxLines = 1
        )
    }
}
