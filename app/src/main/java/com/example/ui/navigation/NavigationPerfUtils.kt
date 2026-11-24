package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.components.rememberReducedMotionEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Returns `false` on Frame 1 so a destination screen can paint its header, hero KPIs, and
 * primary Bento layout instantaneously during the 160–190ms route transition, then flips to `true`
 * once the transition settles so heavy Canvas charts and secondary sections mount smoothly.
 */
@Composable
fun rememberTransitionSettled(
    key: Any? = Unit,
    settleDelayMillis: Long = 140L
): Boolean {
    val reducedMotion = rememberReducedMotionEnabled()
    var settled by remember(key, reducedMotion) { mutableStateOf(reducedMotion) }

    LaunchedEffect(key, reducedMotion) {
        if (reducedMotion || settled) {
            settled = true
            return@LaunchedEffect
        }
        // Let the first frame of the destination screen commit immediately
        withFrameNanos { }
        if (settleDelayMillis > 0L) {
            delay(settleDelayMillis)
        }
        settled = true
    }
    return settled
}

/**
 * Executes an expensive pure computation off the Main Thread ([Dispatchers.Default]) and retains
 * the most recent computed value across key updates so UI screens never flash blank when switching
 * filters, periods, or tabs. On initial mount, yields one frame ([withFrameNanos]) when
 * [deferFirstFrame] is true so navigation transitions begin instantaneously without jank.
 */
@Composable
fun <T> rememberDeferredComputation(
    vararg keys: Any?,
    initialValue: T,
    deferFirstFrame: Boolean = true,
    computation: () -> T
): State<T> {
    val state = remember { mutableStateOf(initialValue) }
    var hasCommittedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(keys = keys) {
        if (deferFirstFrame && !hasCommittedOnce) {
            withFrameNanos { }
        }
        val computed = withContext(Dispatchers.Default) {
            computation()
        }
        state.value = computed
        hasCommittedOnce = true
    }

    return state
}

/**
 * Lightweight Bento Glass placeholder matching the active theme's translucent card surface,
 * used briefly (~140ms) while heavy chart cards wait for the screen transition to settle.
 */
@Composable
fun DeferredBentoChartPlaceholder(
    height: Dp,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 8.dp
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.50f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
    val borderColor = if (isDark) {
        Color(0x28A5B4FC)
    } else {
        Color(0x1A6366F1)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .height(height),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth())
    }
}
