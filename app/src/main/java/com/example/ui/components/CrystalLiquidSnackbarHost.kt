package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

enum class ToastType {
    SUCCESS,
    WARNING,
    INFO
}

object CrystalLiquidToastClassifier {
    private val warningKeywords = listOf(
        "fail", "error", "invalid", "couldn't", "could not", "cannot", "can't",
        "over budget", "overspent", "missing", "required", "please", "denied",
        "wrong", "empty", "no readable", "offline", "warning", "unavailable",
        "ব্যর্থ", "ত্রুটি", "ভুল", "যায়নি", "পাওয়া যায়নি", "প্রয়োজন", "অবশ্যই", "অনুগ্রহ"
    )

    private val infoKeywords = listOf(
        "no notifications", "no cloud backup", "copied", "checking", "scanning",
        "already", "info", "notice",
        "কোন নতুন বিজ্ঞপ্তি", "কপি", "যাচাই হচ্ছে"
    )

    private val successKeywords = listOf(
        "saved", "restored", "up to date", "updated", "created", "added",
        "deleted", "removed", "settled", "recorded", "enabled", "disabled",
        "completed", "synced", "exported", "imported", "signed in", "signed out",
        "সংরক্ষিত", "সংরক্ষণ", "আপটুডেট", "আপডেট", "তৈরি", "যোগ", "মুছে",
        "পরিশোধ", "রেকর্ড", "সম্পন্ন", "সফল"
    )

    fun resolve(message: String): ToastType {
        val lower = message.lowercase(Locale.ROOT)
        return when {
            warningKeywords.any { lower.contains(it) } -> ToastType.WARNING
            infoKeywords.any { lower.contains(it) } -> ToastType.INFO
            successKeywords.any { lower.contains(it) } -> ToastType.SUCCESS
            else -> ToastType.SUCCESS
        }
    }
}

@Composable
fun CrystalLiquidSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val currentData = hostState.currentSnackbarData
    var activeData by remember { mutableStateOf<SnackbarData?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(currentData) {
        if (currentData != null) {
            activeData = currentData
            visible = true
            val timeoutMs = if (currentData.visuals.actionLabel != null) 4000L else 2500L
            delay(timeoutMs)
            visible = false
            delay(220L)
            currentData.dismiss()
        } else {
            visible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 84.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Keep underlying SnackbarHost state machine wired while rendering custom animated capsule
        SnackbarHost(
            hostState = hostState,
            modifier = Modifier.size(0.dp),
            snackbar = {}
        )

        AnimatedVisibility(
            visible = visible && activeData != null,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> (fullHeight * 0.65f).toInt() },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + scaleIn(
                initialScale = 0.94f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> (fullHeight * 0.4f).toInt() },
                animationSpec = tween(180)
            ) + scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(180)
            ) + fadeOut(animationSpec = tween(160))
        ) {
            activeData?.let { data ->
                CrystalLiquidToastPill(
                    message = data.visuals.message,
                    actionLabel = data.visuals.actionLabel,
                    onActionClick = { data.performAction() }
                )
            }
        }
    }
}

@Composable
fun CrystalLiquidToastPill(
    message: String,
    modifier: Modifier = Modifier,
    toastType: ToastType = CrystalLiquidToastClassifier.resolve(message),
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primary = MaterialTheme.colorScheme.primary
    val isMultiLine = message.length > 44 || message.contains('\n')
    val capsuleShape = if (isMultiLine) RoundedCornerShape(28.dp) else CircleShape

    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
    } else {
        primary.copy(alpha = 0.06f)
            .compositeOver(Color.White.copy(alpha = 0.92f))
    }

    val specularBorderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.07f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White,
                primary.copy(alpha = 0.22f)
            )
        )
    }

    val isNotificationInfo = message.lowercase(Locale.ROOT).contains("notification") ||
        message.contains("বিজ্ঞপ্তি")

    val (badgeBgColor, badgeIconColor, badgeIcon) = when (toastType) {
        ToastType.SUCCESS -> Triple(
            if (isDark) Color(0xFF064E3B).copy(alpha = 0.75f) else Color(0xFFDCFCE7),
            if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
            Icons.Rounded.Check
        )
        ToastType.WARNING -> Triple(
            if (isDark) Color(0xFF78350F).copy(alpha = 0.75f) else Color(0xFFFEF3C7),
            if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
            Icons.Rounded.PriorityHigh
        )
        ToastType.INFO -> Triple(
            primary.copy(alpha = if (isDark) 0.24f else 0.14f),
            primary,
            if (isNotificationInfo) Icons.Rounded.Notifications else Icons.Rounded.Info
        )
    }

    Surface(
        shape = capsuleShape,
        color = containerColor,
        shadowElevation = 10.dp,
        tonalElevation = 2.dp,
        modifier = modifier
            .wrapContentWidth()
            .widthIn(min = 180.dp, max = 340.dp)
            .heightIn(min = 46.dp)
            .border(width = 1.dp, brush = specularBorderBrush, shape = capsuleShape)
            .testTag("crystal_liquid_toast")
    ) {
        Row(
            modifier = Modifier.padding(
                start = 10.dp,
                end = if (actionLabel != null) 10.dp else 16.dp,
                top = 9.dp,
                bottom = 9.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(badgeBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeIconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onActionClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = primary.copy(alpha = 0.14f),
                        contentColor = primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 30.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = primary
                        )
                    )
                }
            }
        }
    }
}
