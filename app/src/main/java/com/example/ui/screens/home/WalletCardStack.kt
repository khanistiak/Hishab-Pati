package com.example.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.ui.HomeCardStyle
import com.example.ui.components.TranslucentCard
import com.example.ui.components.rememberReducedMotionEnabled
import com.example.ui.navigation.walletCardSharedBounds
import com.example.ui.screens.accounts.HishabPatiAccountCard
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.util.CurrencyFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MAX_VISIBLE_STACK_CARDS = 5
private const val Z_SWAP_PROGRESS = 0.45f

private enum class StackSwipeDirection {
    NONE,
    FORWARD_RIGHT,
    BACKWARD_LEFT
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}

/**
 * Tight, crisp vertical peek offsets for the top 3 cards + 2 slim text-free depth strips:
 * - pos 0: 0.dp (full front card)
 * - pos 1: -26.dp (2nd card peeking header)
 * - pos 2: -50.dp (3rd card peeking header)
 * - pos 3: -56.dp (4th slim colored/shadow depth strip)
 * - pos 4: -61.dp (5th slim colored/shadow depth strip)
 */
private fun slotYDp(slot: Int): Dp = when (slot) {
    0 -> 0.dp
    1 -> (-26).dp
    2 -> (-50).dp
    3 -> (-56).dp
    else -> (-61).dp
}

private fun slotScale(slot: Int): Float = when (slot) {
    0 -> 1.00f
    1 -> 0.96f
    2 -> 0.92f
    3 -> 0.885f
    else -> 0.85f
}

private fun slotAlpha(slot: Int): Float = when (slot) {
    0 -> 1.00f
    1 -> 0.92f
    2 -> 0.92f
    3 -> 0.90f
    else -> 0.88f
}

private fun slotShadowElevation(slot: Int): Dp = when (slot) {
    0 -> 12.dp
    1 -> 8.dp
    2 -> 6.dp
    3 -> 4.dp
    else -> 3.dp
}

private fun nextIndexForDirection(currentIndex: Int, dir: StackSwipeDirection, count: Int): Int {
    if (count <= 1) return 0
    return when (dir) {
        StackSwipeDirection.FORWARD_RIGHT -> (currentIndex + 1) % count
        StackSwipeDirection.BACKWARD_LEFT -> (currentIndex - 1 + count) % count
        StackSwipeDirection.NONE -> currentIndex
    }
}

private data class RenderedDeckCard(
    val renderKey: Any,
    val account: AccountEntity,
    val testPos: Int,
    val translationXPx: Float,
    val translationYDp: Dp,
    val scale: Float,
    val alpha: Float,
    val zIndex: Float,
    val shadowElevation: Dp,
    val peekingHeaderOnly: Boolean,
    val hideContent: Boolean,
    val isFrontInteractive: Boolean,
    val tapSlotOffset: Int
)

@Composable
internal fun StackedAccountCards(
    accounts: List<AccountEntity>,
    isBalanceVisible: Boolean,
    currencySymbol: String,
    isBangla: Boolean = false,
    isHomeActive: Boolean = true,
    currentCardStyle: HomeCardStyle = HomeCardStyle.STACK,
    showHeader: Boolean = true,
    isDarkTheme: Boolean = LocalIsDarkTheme.current ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f),
    onSwitchViewMode: (() -> Unit)? = null,
    onAccountClick: (AccountEntity) -> Unit,
    onAllAccountsClick: () -> Unit
) {
    val visibleAccounts = remember(accounts) { accounts.filter { !it.isHidden } }
    if (visibleAccounts.isEmpty()) {
        TranslucentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("home_wallet_card_stack"),
            cornerRadius = 24.dp,
            isDarkTheme = isDarkTheme
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (showHeader && onSwitchViewMode != null) {
                    HomeCardViewSwitcherButton(
                        currentStyle = currentCardStyle,
                        isBangla = isBangla,
                        onClick = onSwitchViewMode,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isBangla) "কোনো অ্যাকাউন্ট নেই" else "No accounts yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBangla) "লেনদেন শুরু করতে একটি অ্যাকাউন্ট যোগ করুন" else "Add your Bank, MFS, or Cash account to get started",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAllAccountsClick,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(text = if (isBangla) "অ্যাকাউন্ট যোগ করুন" else "Add Account")
                }
                }
            }
        }
        return
    }

    val n = visibleAccounts.size
    var rawActiveIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeIndex = ((rawActiveIndex % n) + n) % n

    LaunchedEffect(n) {
        if (rawActiveIndex >= n || rawActiveIndex < 0) {
            rawActiveIndex = ((rawActiveIndex % n) + n) % n
        }
    }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val reducedMotion = rememberReducedMotionEnabled()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Zero-lag 1:1 horizontal finger drag tracking + snap-back job
    var liveDragOffsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val snapBackAnimatable = remember { Animatable(0f) }
    var snapBackJob by remember { mutableStateOf<Job?>(null) }

    // Single atomic transition Animatable(0f -> 1f) driving all cards in lockstep
    val transitionProgress = remember { Animatable(0f) }
    var activeTransitionDir by remember { mutableStateOf(StackSwipeDirection.NONE) }
    var transitionStartX by remember { mutableFloatStateOf(0f) }
    var transitionJob by remember { mutableStateOf<Job?>(null) }

    // Entry & Home-return staggered fan-out unfold animation
    var fanReplayTick by remember { mutableIntStateOf(1) }
    var wasHomeActive by remember { mutableStateOf(isHomeActive) }
    var hasSeenInitialResume by remember { mutableStateOf(false) }

    LaunchedEffect(isHomeActive) {
        if (isHomeActive && !wasHomeActive) {
            fanReplayTick++
        }
        wasHomeActive = isHomeActive
    }

    DisposableEffect(lifecycleOwner, isHomeActive) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasSeenInitialResume && isHomeActive) {
                    fanReplayTick++
                }
                hasSeenInitialResume = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val fanAnimatables = remember {
        List(MAX_VISIBLE_STACK_CARDS) { Animatable(if (reducedMotion) 1f else 0f) }
    }

    LaunchedEffect(fanReplayTick, reducedMotion) {
        if (reducedMotion) {
            fanAnimatables.forEach { it.snapTo(1f) }
        } else {
            fanAnimatables.forEach { it.snapTo(0f) }
            for (pos in 0 until MAX_VISIBLE_STACK_CARDS) {
                launch {
                    delay(pos * 40L)
                    fanAnimatables[pos].animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        }
    }

    val countPillBgColor = remember(isDarkTheme) {
        if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.58f)
        else Color.White.copy(alpha = 0.64f)
    }
    val countPillBorderColor = remember(isDarkTheme) {
        if (isDarkTheme) Color.White.copy(alpha = 0.14f)
        else Color.White.copy(alpha = 0.78f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (showHeader) 6.dp else 0.dp, bottom = if (showHeader) 8.dp else 0.dp)
            .testTag("home_wallet_card_stack")
    ) {
        if (showHeader) {
            // Lightweight floating header row: frosted glass total account count pill on left,
            // "All accounts >" + circular frosted view-switcher button on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val accountCountText = if (isBangla) {
                    "${CurrencyFormatter.convertToBanglaDigits(n.toString())}টি অ্যাকাউন্ট"
                } else {
                    if (n == 1) "1 account" else "$n accounts"
                }

                key(isDarkTheme) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                color = countPillBgColor,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = countPillBorderColor,
                                shape = CircleShape
                            )
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = accountCountText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onAllAccountsClick() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("stack_all_accounts_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isBangla) "সকল অ্যাকাউন্ট" else "All accounts",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                    }

                    if (onSwitchViewMode != null) {
                        HomeCardViewSwitcherButton(
                            currentStyle = currentCardStyle,
                            isBangla = isBangla,
                            onClick = onSwitchViewMode,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        val visibleCount = minOf(n, MAX_VISIBLE_STACK_CARDS)
        val backSlot = (visibleCount - 1).coerceAtLeast(0)
        val cardHeight = 180.dp
        val maxTopPeekDp = -slotYDp(backSlot)
        val totalStackHeight = cardHeight + maxTopPeekDp

            val dismissThresholdPx = with(density) { 68.dp.toPx() }
            val currentActiveIndex by rememberUpdatedState(activeIndex)
            val currentTransitionDir by rememberUpdatedState(activeTransitionDir)

            // Synchronously commits any in-flight swipe before starting a new drag/swipe so
            // rapid 4-5+ consecutive swipes NEVER leave cards stuck or the front slot empty.
            fun commitActiveTransitionImmediately(): Int {
                val inFlightDir = currentTransitionDir
                if (inFlightDir != StackSwipeDirection.NONE) {
                    transitionJob?.cancel()
                    transitionJob = null
                    val committedIdx = nextIndexForDirection(currentActiveIndex, inFlightDir, n)
                    rawActiveIndex = committedIdx
                    activeTransitionDir = StackSwipeDirection.NONE
                    transitionStartX = 0f
                    liveDragOffsetX = 0f
                    scope.launch { transitionProgress.snapTo(0f) }
                    return committedIdx
                }
                return currentActiveIndex
            }

            fun startAtomicTransition(
                direction: StackSwipeDirection,
                startX: Float,
                startProgress: Float = 0f,
                initialVelocityProgress: Float = 0f
            ) {
                if (n <= 1 || direction == StackSwipeDirection.NONE) return
                snapBackJob?.cancel()
                snapBackJob = null

                val baseIdx = commitActiveTransitionImmediately()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                if (reducedMotion) {
                    rawActiveIndex = nextIndexForDirection(baseIdx, direction, n)
                    liveDragOffsetX = 0f
                    return
                }

                transitionStartX = startX
                liveDragOffsetX = 0f
                activeTransitionDir = direction

                transitionJob = scope.launch {
                    try {
                        if (direction == StackSwipeDirection.BACKWARD_LEFT) {
                            transitionProgress.snapTo(startProgress.coerceIn(0f, 1f))
                            transitionProgress.animateTo(
                                targetValue = 1f,
                                initialVelocity = initialVelocityProgress.coerceIn(0f, 8f),
                                animationSpec = spring(
                                    dampingRatio = 0.84f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        } else {
                            transitionProgress.snapTo(0f)
                            transitionProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 260,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                        // Atomic handoff: update index and clear direction together in one frame
                        rawActiveIndex = nextIndexForDirection(baseIdx, direction, n)
                        activeTransitionDir = StackSwipeDirection.NONE
                        transitionStartX = 0f
                        transitionProgress.snapTo(0f)
                    } catch (_: CancellationException) {
                        // Interrupted by a rapid follow-up swipe; already committed cleanly
                    }
                }
            }

            fun startSnapBack(fromX: Float, velocityX: Float) {
                snapBackJob?.cancel()
                snapBackJob = scope.launch {
                    try {
                        snapBackAnimatable.snapTo(fromX)
                        val snapSpec = if (fromX < 0f) {
                            spring<Float>(
                                dampingRatio = 0.84f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        } else {
                            spring<Float>(
                                dampingRatio = 0.80f,
                                stiffness = 420f
                            )
                        }
                        snapBackAnimatable.animateTo(
                            targetValue = 0f,
                            initialVelocity = velocityX,
                            animationSpec = snapSpec
                        ) {
                            liveDragOffsetX = value
                        }
                        liveDragOffsetX = 0f
                    } catch (_: CancellationException) {
                        // Interrupted by new drag
                    }
                }
            }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(totalStackHeight)
        ) {
                val cardWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(300f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(n, cardWidthPx) {
                            if (n > 1) {
                                val velocityTracker = VelocityTracker()
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        velocityTracker.resetTracking()
                                        snapBackJob?.cancel()
                                        snapBackJob = null
                                        commitActiveTransitionImmediately()
                                        isDragging = true
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                                        // Zero-lag synchronous 1:1 horizontal tracking
                                        liveDragOffsetX = (liveDragOffsetX + dragAmount)
                                            .coerceIn(-cardWidthPx * 1.05f, cardWidthPx * 1.05f)
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        val velocityX = velocityTracker.calculateVelocity().x
                                        val currentX = liveDragOffsetX
                                        val rtlProgress = (-currentX / cardWidthPx).coerceIn(0f, 1f)
                                        val shouldCommitRight =
                                            currentX > dismissThresholdPx ||
                                                (velocityX > 450f && currentX >= -24f)
                                        val shouldCommitLeft =
                                            rtlProgress > 0.25f ||
                                                (velocityX < -500f && currentX <= 24f)

                                        when {
                                            shouldCommitRight -> {
                                                startAtomicTransition(
                                                    direction = StackSwipeDirection.FORWARD_RIGHT,
                                                    startX = currentX
                                                )
                                            }
                                            shouldCommitLeft -> {
                                                startAtomicTransition(
                                                    direction = StackSwipeDirection.BACKWARD_LEFT,
                                                    startX = currentX,
                                                    startProgress = rtlProgress,
                                                    initialVelocityProgress = (-velocityX / cardWidthPx).coerceAtLeast(0f)
                                                )
                                            }
                                            else -> {
                                                startSnapBack(currentX, velocityX)
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        startSnapBack(liveDragOffsetX, 0f)
                                    }
                                )
                            }
                        }
                ) {
                    val p = if (activeTransitionDir == StackSwipeDirection.NONE) {
                        0f
                    } else {
                        transitionProgress.value.coerceIn(0f, 1f)
                    }
                    val rightEdgeMarginPx = with(density) { 24.dp.toPx() }

                    fun MutableList<RenderedDeckCard>.appendBackwardLeftCards(
                        rtlP: Float,
                        isSettlingCommit: Boolean
                    ) {
                        val incomingIdx = (activeIndex - 1 + n) % n
                        val incomingAcc = visibleAccounts[incomingIdx]

                        // 1. Existing cards stay visible and smoothly step backward 1 slot in place.
                        //    Never hide/remove the incoming back card before the swipe completes.
                        for (pos in (visibleCount - 1) downTo 0) {
                            val idx = (activeIndex + pos) % n
                            val acc = visibleAccounts[idx]
                            val fan = fanAnimatables[pos.coerceIn(0, MAX_VISIBLE_STACK_CARDS - 1)].value
                            val baseTy = lerpDp(0.dp, slotYDp(pos), fan)
                            val baseScale = lerpFloat(0.94f, slotScale(pos), fan)

                            if (idx == incomingIdx) {
                                // Keep back-slot card visible while drag begins, fading gently only
                                // once the incoming front copy glides across the stack from the right.
                                val backFade = 1f - ((rtlP - 0.35f) / 0.65f).coerceIn(0f, 1f)
                                add(
                                    RenderedDeckCard(
                                        renderKey = "receding_back_${acc.id}",
                                        account = acc,
                                        testPos = pos,
                                        translationXPx = 0f,
                                        translationYDp = baseTy,
                                        scale = baseScale,
                                        alpha = (fan * slotAlpha(pos) * backFade).coerceIn(0f, 1f),
                                        zIndex = (8 - pos).toFloat().coerceAtLeast(1f),
                                        shadowElevation = slotShadowElevation(pos),
                                        peekingHeaderOnly = pos in 1..2,
                                        hideContent = pos >= 3,
                                        isFrontInteractive = false,
                                        tapSlotOffset = pos
                                    )
                                )
                            } else {
                                val targetSlot = pos + 1
                                if (targetSlot < MAX_VISIBLE_STACK_CARDS) {
                                    add(
                                        RenderedDeckCard(
                                            renderKey = acc.id,
                                            account = acc,
                                            testPos = if (isSettlingCommit) targetSlot else pos,
                                            translationXPx = 0f,
                                            translationYDp = lerpDp(baseTy, slotYDp(targetSlot), rtlP),
                                            scale = lerpFloat(baseScale, slotScale(targetSlot), rtlP),
                                            alpha = (fan * lerpFloat(slotAlpha(pos), slotAlpha(targetSlot), rtlP)).coerceIn(0f, 1f),
                                            zIndex = (8 - targetSlot).toFloat().coerceAtLeast(2f),
                                            shadowElevation = lerpDp(
                                                slotShadowElevation(pos),
                                                slotShadowElevation(targetSlot),
                                                rtlP
                                            ),
                                            peekingHeaderOnly = when (targetSlot) {
                                                1 -> rtlP >= 0.55f
                                                2 -> true
                                                else -> false
                                            },
                                            hideContent = when {
                                                targetSlot >= 3 -> rtlP >= 0.45f || pos >= 3
                                                else -> false
                                            },
                                            isFrontInteractive = false,
                                            tapSlotOffset = targetSlot
                                        )
                                    )
                                } else {
                                    // 5th card softly fading behind when n > 5
                                    add(
                                        RenderedDeckCard(
                                            renderKey = acc.id,
                                            account = acc,
                                            testPos = MAX_VISIBLE_STACK_CARDS - 1,
                                            translationXPx = 0f,
                                            translationYDp = slotYDp(MAX_VISIBLE_STACK_CARDS - 1),
                                            scale = slotScale(MAX_VISIBLE_STACK_CARDS - 1),
                                            alpha = (fan * slotAlpha(MAX_VISIBLE_STACK_CARDS - 1) * (1f - rtlP)).coerceIn(0f, 1f),
                                            zIndex = 2f,
                                            shadowElevation = slotShadowElevation(MAX_VISIBLE_STACK_CARDS - 1),
                                            peekingHeaderOnly = false,
                                            hideContent = true,
                                            isFrontInteractive = false,
                                            tapSlotOffset = MAX_VISIBLE_STACK_CARDS - 1
                                        )
                                    )
                                }
                            }
                        }

                        // 2. Incoming card (lastIndex) glides smoothly from the right edge
                        //    ((cardWidth + 24.dp) * (1f - p)) at highest zIndex = 10f onto the front of the stack.
                        val incomingTx = (cardWidthPx + rightEdgeMarginPx) * (1f - rtlP)
                        add(
                            RenderedDeckCard(
                                renderKey = incomingAcc.id,
                                account = incomingAcc,
                                testPos = 0,
                                translationXPx = incomingTx,
                                translationYDp = lerpDp((-4).dp, 0.dp, rtlP),
                                scale = 1.0f,
                                alpha = 1f,
                                zIndex = 10f,
                                shadowElevation = lerpDp(16.dp, slotShadowElevation(0), rtlP),
                                peekingHeaderOnly = false,
                                hideContent = false,
                                isFrontInteractive = isSettlingCommit && rtlP >= Z_SWAP_PROGRESS,
                                tapSlotOffset = 0
                            )
                        )
                    }

                    val renderedCards = buildList {
                        when (activeTransitionDir) {
                            StackSwipeDirection.NONE -> {
                                val isPullingFromRight = liveDragOffsetX < -0.5f && n > 1
                                if (isPullingFromRight) {
                                    val rtlProgress = (-liveDragOffsetX / cardWidthPx).coerceIn(0f, 1f)
                                    appendBackwardLeftCards(
                                        rtlP = rtlProgress,
                                        isSettlingCommit = false
                                    )
                                } else {
                                    // Idle & Left-to-Right 1:1 dragging: front card slides right, background cards stay stationary
                                    for (pos in (visibleCount - 1) downTo 0) {
                                        val idx = (activeIndex + pos) % n
                                        val acc = visibleAccounts[idx]
                                        val fan = fanAnimatables[pos.coerceIn(0, MAX_VISIBLE_STACK_CARDS - 1)].value
                                        val isFront = pos == 0
                                        val txPx = if (isFront) liveDragOffsetX else 0f
                                        val liftDp = if (isFront && txPx != 0f) {
                                            (-8f * (abs(txPx) / cardWidthPx).coerceIn(0f, 1f)).dp
                                        } else {
                                            0.dp
                                        }
                                        add(
                                            RenderedDeckCard(
                                                renderKey = acc.id,
                                                account = acc,
                                                testPos = pos,
                                                translationXPx = txPx,
                                                translationYDp = lerpDp(0.dp, slotYDp(pos), fan) + liftDp,
                                                scale = lerpFloat(0.94f, slotScale(pos), fan),
                                                alpha = (fan * slotAlpha(pos)).coerceIn(0f, 1f),
                                                zIndex = (10 - pos).toFloat(),
                                                shadowElevation = slotShadowElevation(pos),
                                                peekingHeaderOnly = pos in 1..2,
                                                hideContent = pos >= 3,
                                                isFrontInteractive = isFront,
                                                tapSlotOffset = pos
                                            )
                                        )
                                    }
                                }
                            }

                            StackSwipeDirection.FORWARD_RIGHT -> {
                                // Outgoing front card (pos = 0): arcs right, drops behind at p >= 0.45f, tucks into back slot
                                val outgoingAcc = visibleAccounts[activeIndex]
                                val peakRightX = maxOf(
                                    transitionStartX + cardWidthPx * 0.12f,
                                    cardWidthPx * 0.68f
                                ).coerceAtMost(cardWidthPx * 0.94f)
                                val startLiftDp = (-8f * (abs(transitionStartX) / cardWidthPx).coerceIn(0f, 1f)).dp
                                val isBehind = p >= Z_SWAP_PROGRESS

                                val outTx: Float
                                val outTy: Dp
                                val outScale: Float
                                val outAlpha: Float
                                if (!isBehind) {
                                    val phase = FastOutSlowInEasing.transform((p / Z_SWAP_PROGRESS).coerceIn(0f, 1f))
                                    outTx = lerpFloat(transitionStartX, peakRightX, phase)
                                    outTy = lerpDp(startLiftDp, (-16).dp, phase)
                                    outScale = lerpFloat(1.0f, 0.94f, phase)
                                    outAlpha = 1f
                                } else {
                                    val phase = FastOutSlowInEasing.transform(((p - Z_SWAP_PROGRESS) / (1f - Z_SWAP_PROGRESS)).coerceIn(0f, 1f))
                                    outTx = lerpFloat(peakRightX, 0f, phase)
                                    outTy = lerpDp((-16).dp, slotYDp(backSlot), phase)
                                    outScale = lerpFloat(0.94f, slotScale(backSlot), phase)
                                    outAlpha = if (n > MAX_VISIBLE_STACK_CARDS) {
                                        lerpFloat(1f, 0f, phase)
                                    } else {
                                        lerpFloat(1f, slotAlpha(backSlot), phase)
                                    }
                                }

                                add(
                                    RenderedDeckCard(
                                        renderKey = outgoingAcc.id,
                                        account = outgoingAcc,
                                        testPos = backSlot,
                                        translationXPx = outTx,
                                        translationYDp = outTy,
                                        scale = outScale,
                                        alpha = outAlpha,
                                        zIndex = if (isBehind) 1f else 12f,
                                        shadowElevation = if (isBehind) slotShadowElevation(backSlot) else 14.dp,
                                        peekingHeaderOnly = isBehind && backSlot in 1..2,
                                        hideContent = isBehind && backSlot >= 3,
                                        isFrontInteractive = false,
                                        tapSlotOffset = 0
                                    )
                                )

                                // Simultaneously promote pos 1..visibleCount-1 forward by 1 slot
                                for (pos in (visibleCount - 1) downTo 1) {
                                    val idx = (activeIndex + pos) % n
                                    val acc = visibleAccounts[idx]
                                    val targetSlot = pos - 1
                                    add(
                                        RenderedDeckCard(
                                            renderKey = acc.id,
                                            account = acc,
                                            testPos = targetSlot,
                                            translationXPx = 0f,
                                            translationYDp = lerpDp(slotYDp(pos), slotYDp(targetSlot), p),
                                            scale = lerpFloat(slotScale(pos), slotScale(targetSlot), p),
                                            alpha = lerpFloat(slotAlpha(pos), slotAlpha(targetSlot), p),
                                            zIndex = (10 - targetSlot).toFloat(),
                                            shadowElevation = lerpDp(
                                                slotShadowElevation(pos),
                                                slotShadowElevation(targetSlot),
                                                p
                                            ),
                                            peekingHeaderOnly = when (targetSlot) {
                                                0 -> p < Z_SWAP_PROGRESS
                                                1, 2 -> true
                                                else -> false
                                            },
                                            hideContent = when {
                                                targetSlot == 0 || targetSlot == 1 -> false
                                                targetSlot == 2 -> p < 0.35f
                                                else -> true
                                            },
                                            isFrontInteractive = targetSlot == 0,
                                            tapSlotOffset = targetSlot
                                        )
                                    )
                                }

                                // If more than 5 accounts exist, fade the next rear depth strip smoothly into slot 4
                                if (n > MAX_VISIBLE_STACK_CARDS) {
                                    val incomingRearIdx = (activeIndex + MAX_VISIBLE_STACK_CARDS) % n
                                    add(
                                        RenderedDeckCard(
                                            renderKey = visibleAccounts[incomingRearIdx].id,
                                            account = visibleAccounts[incomingRearIdx],
                                            testPos = MAX_VISIBLE_STACK_CARDS - 1,
                                            translationXPx = 0f,
                                            translationYDp = slotYDp(MAX_VISIBLE_STACK_CARDS - 1),
                                            scale = slotScale(MAX_VISIBLE_STACK_CARDS - 1),
                                            alpha = p * slotAlpha(MAX_VISIBLE_STACK_CARDS - 1),
                                            zIndex = 2f,
                                            shadowElevation = slotShadowElevation(MAX_VISIBLE_STACK_CARDS - 1),
                                            peekingHeaderOnly = false,
                                            hideContent = true,
                                            isFrontInteractive = false,
                                            tapSlotOffset = MAX_VISIBLE_STACK_CARDS - 1
                                        )
                                    )
                                }
                            }

                            StackSwipeDirection.BACKWARD_LEFT -> {
                                appendBackwardLeftCards(
                                    rtlP = p,
                                    isSettlingCommit = true
                                )
                            }
                        }
                    }

                    renderedCards.forEach { card ->
                        key(card.renderKey) {
                            val sharedBoundsModifier = if (card.isFrontInteractive) {
                                Modifier.walletCardSharedBounds(
                                    accountId = card.account.id,
                                    cornerRadius = 22.dp
                                )
                            } else {
                                Modifier
                            }

                            StackedDeckCardSurface(
                                account = card.account,
                                currencySymbol = currencySymbol,
                                isBalanceVisible = isBalanceVisible,
                                isBangla = isBangla,
                                shadowElevation = card.shadowElevation,
                                peekingHeaderOnly = card.peekingHeaderOnly,
                                hideContent = card.hideContent,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(cardHeight)
                                    .zIndex(card.zIndex)
                                    .offset {
                                        val yPx = with(density) { card.translationYDp.toPx() }
                                        IntOffset(card.translationXPx.roundToInt(), yPx.roundToInt())
                                    }
                                    .graphicsLayer {
                                        scaleX = card.scale
                                        scaleY = card.scale
                                        alpha = card.alpha.coerceIn(0f, 1f)
                                        rotationZ = 0f // Strictly horizontal (0° rotation at all times)
                                        transformOrigin = TransformOrigin(0.5f, 0f)
                                    }
                                    .then(sharedBoundsModifier)
                                    .clickable {
                                        if (!isDragging && abs(liveDragOffsetX) < 12f) {
                                            if (card.isFrontInteractive) {
                                                commitActiveTransitionImmediately()
                                                onAccountClick(card.account)
                                            } else if (n > 1 && card.tapSlotOffset > 0) {
                                                if (card.tapSlotOffset == 1) {
                                                    startAtomicTransition(StackSwipeDirection.FORWARD_RIGHT, 0f)
                                                } else if (card.tapSlotOffset == n - 1) {
                                                    startAtomicTransition(StackSwipeDirection.BACKWARD_LEFT, 0f)
                                                } else {
                                                    val base = commitActiveTransitionImmediately()
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    rawActiveIndex = (base + card.tapSlotOffset) % n
                                                }
                                            }
                                        }
                                    }
                                    .testTag("wallet_stack_card_${card.testPos}")
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun StackedDeckCardSurface(
    account: AccountEntity,
    currencySymbol: String,
    isBalanceVisible: Boolean,
    isBangla: Boolean,
    shadowElevation: Dp,
    peekingHeaderOnly: Boolean,
    hideContent: Boolean,
    modifier: Modifier = Modifier
) {
    val language = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
    val cardShape = RoundedCornerShape(22.dp)
    val cardHue = remember(account) { resolveAccountCardColor(account) }

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = cardShape,
                ambientColor = cardHue.copy(alpha = 0.28f),
                spotColor = cardHue.copy(alpha = 0.36f)
            )
            .clip(cardShape)
    ) {
        HishabPatiAccountCard(
            account = account,
            currencySymbol = currencySymbol,
            isBalanceVisible = isBalanceVisible,
            language = language,
            cornerRadius = 22.dp,
            shadowElevation = 0.dp,
            peekingHeaderOnly = peekingHeaderOnly,
            hideContent = hideContent,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Resolves account card color matching account creation and detail screens.
 * Uses saved colorHex, or falls back to official brand colors (bKash #E2136E, Nagad #F26522, etc.).
 */
internal fun resolveAccountCardColor(account: AccountEntity): Color {
    val hex = account.colorHex.trim()
    if (hex.isNotBlank() && hex != "#000000" && hex != "0") {
        try {
            return Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {}
    }
    return if (account.type == AccountType.MFS) {
        val prov = (account.institution.ifBlank { account.name }).lowercase()
        when {
            prov.contains("bkash") -> Color(0xFFE2136E)
            prov.contains("nagad") -> Color(0xFFF26522)
            prov.contains("rocket") -> Color(0xFF8C3494)
            prov.contains("upay") -> Color(0xFF005BAC)
            prov.contains("tap") -> Color(0xFF0084C7)
            prov.contains("cellfin") -> Color(0xFF008848)
            else -> Color(0xFF00897B)
        }
    } else when (account.type) {
        AccountType.CREDIT_CARD -> Color(0xFF1E293B)
        AccountType.BANK -> Color(0xFF2563EB)
        AccountType.CASH -> Color(0xFF059669)
        else -> Color(0xFF4F46E5)
    }
}
