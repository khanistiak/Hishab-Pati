package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.components.rememberReducedMotionEnabled

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Shared-element bounds modifier connecting Home screen wallet cards (Card Stack / Card Carousel)
 * seamlessly with the Account Details hero card using a unified `"wallet_card_${accountId}"` key.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.walletCardSharedBounds(
    accountId: String,
    cornerRadius: Dp = 24.dp
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalNavAnimatedVisibilityScope.current
    val reducedMotion = rememberReducedMotionEnabled()

    if (sharedScope == null || animatedScope == null || reducedMotion) {
        return this
    }

    return with(sharedScope) {
        this@walletCardSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = "wallet_card_$accountId"),
            animatedVisibilityScope = animatedScope,
            boundsTransform = { _, _ ->
                spring(dampingRatio = 0.85f, stiffness = 480f)
            },
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(cornerRadius)),
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds()
        )
    }
}
