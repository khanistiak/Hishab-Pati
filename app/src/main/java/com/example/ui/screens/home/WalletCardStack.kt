package com.example.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.ui.components.TranslucentCard
import com.example.ui.screens.accounts.HishabPatiAccountCard
import com.example.ui.navigation.walletCardSharedBounds
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.theme.CardBackgroundLight
import com.example.ui.theme.CardBorderLight
import com.example.ui.util.CurrencyFormatter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MAX_VISIBLE_INFO_CARDS = 3

private enum class StackTransitionPhase {
    IDLE,
    TUCKING_TO_BACK,
    PULLING_FROM_BACK_OUT,
    LANDING_AT_FRONT
}

private data class StackCardSpec(
    val account: AccountEntity,
    val stackPosition: Int, // 0, 1, 2 for visible stack; 3 for rear transition slot when N > 3
    val targetY: Dp,
    val targetScale: Float,
    val targetAlpha: Float,
    val zIndex: Float,
    val isFrontCard: Boolean,
    val isTrackedByDrag: Boolean,
    val isTrackedByTransition: Boolean
)

@Composable
internal fun StackedAccountCards(
    accounts: List<AccountEntity>,
    isBalanceVisible: Boolean,
    currencySymbol: String,
    isBangla: Boolean = false,
    onAccountClick: (AccountEntity) -> Unit,
    onAllAccountsClick: () -> Unit
) {
    val visibleAccounts = remember(accounts) { accounts.filter { !it.isHidden } }
    if (visibleAccounts.isEmpty()) {
        TranslucentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            cornerRadius = 26.dp
        ) {
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

    // 1:1 finger tracking for active front card (zero lag via snapTo while dragging)
    val dragOffsetX = remember { Animatable(0f) }
    // Independent horizontal trajectory for the card actively tucking behind or pulling out from behind
    val transitionOffsetX = remember { Animatable(0f) }

    var isDragging by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    var transitionCardId by remember { mutableStateOf<String?>(null) }
    var transitionPhase by remember { mutableStateOf(StackTransitionPhase.IDLE) }

    val cancelSpring = remember { spring<Float>(dampingRatio = 0.84f, stiffness = 340f) }
    val commitSpring = remember { spring<Float>(dampingRatio = 0.86f, stiffness = 300f) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerBg = if (isDark) CardBackgroundDark else CardBackgroundLight
    val containerBorder = if (isDark) Color(0x22FFFFFF) else CardBorderLight
    val containerShape = RoundedCornerShape(26.dp)

    // Non-clipping surface container so cards glide cleanly past the edge during swap without slicing
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .background(containerBg, containerShape)
            .border(1.dp, containerBorder, containerShape)
            .testTag("home_wallet_card_stack")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Container Header: "+N more" and "All accounts >"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val remainingCount = (n - 1).coerceAtLeast(0)
                val remainingText = if (remainingCount > 0) {
                    if (isBangla) {
                        "+${CurrencyFormatter.convertToBanglaDigits(remainingCount.toString())} আরও"
                    } else {
                        "+$remainingCount more"
                    }
                } else {
                    if (isBangla) "১টি অ্যাকাউন্ট" else "1 account"
                }
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontSize = 13.5.sp
                    )
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAllAccountsClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("stack_all_accounts_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isBangla) "সকল অ্যাকাউন্ট" else "All accounts",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val visibleCount = minOf(n, MAX_VISIBLE_INFO_CARDS)
            val cardHeight = 188.dp
            val topPeekHeightDp = when {
                n >= 5 -> 80f
                n == 4 -> 74f
                n == 3 -> 66f
                n == 2 -> 34f
                else -> 0f
            }
            val topHeadroom = 10.dp
            val totalStackHeight = cardHeight + topPeekHeightDp.dp + topHeadroom

            fun slotYDp(pos: Int): Dp = when (pos) {
                0 -> 0.dp
                1 -> (-34).dp
                2 -> (-66).dp
                3 -> (-74).dp
                else -> (-80).dp
            }

            fun slotScale(pos: Int): Float = when (pos) {
                0 -> 1.0f
                1 -> 0.96f
                2 -> 0.92f
                3 -> 0.88f
                else -> 0.84f
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalStackHeight)
            ) {
                val cardWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(300f)
                val swipeThresholdPx = cardWidthPx * 0.35f
                val outwardApexPx = cardWidthPx * 0.85f

                // Commit Right Swipe (Front card goes outward right, flips behind stack, glides into rear slot)
                suspend fun performRightCommit() {
                    val outgoingAccount = visibleAccounts[activeIndex]
                    // 1. Slide front card smoothly past right edge
                    dragOffsetX.animateTo(
                        targetValue = outwardApexPx,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    )
                    // 2. Commit moment: flip zIndex behind stack, advance activeIndex so remaining cards step forward
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    transitionCardId = outgoingAccount.id
                    transitionPhase = StackTransitionPhase.TUCKING_TO_BACK
                    transitionOffsetX.snapTo(dragOffsetX.value)
                    rawActiveIndex = (rawActiveIndex + 1) % n
                    dragOffsetX.snapTo(0f)

                    // 3. Smoothly glide departing card inward into rear stack slot
                    transitionOffsetX.animateTo(
                        targetValue = 0f,
                        animationSpec = commitSpring
                    )
                    transitionPhase = StackTransitionPhase.IDLE
                    transitionCardId = null
                }

                // Commit Left Swipe (Rear-most card pulls smoothly outward left behind stack, flips to front, lands at pos 0)
                suspend fun performLeftCommit(targetAccountIdx: Int = (activeIndex - 1 + n) % n) {
                    val incomingAccount = visibleAccounts[targetAccountIdx]
                    transitionCardId = incomingAccount.id
                    transitionPhase = StackTransitionPhase.PULLING_FROM_BACK_OUT
                    transitionOffsetX.snapTo(0f)

                    // 1. Return dragged front card cleanly to 0f while pulling target rear card outward to -0.85 * cardWidth
                    coroutineScope {
                        if (abs(dragOffsetX.value) > 0.5f) {
                            launch {
                                dragOffsetX.animateTo(0f, animationSpec = commitSpring)
                            }
                        }
                        launch {
                            transitionOffsetX.animateTo(
                                targetValue = -outwardApexPx,
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                            )
                        }
                    }

                    // 2. Apex commit moment: elevate zIndex to front (10f) & update activeIndex so previous front steps back to pos 1
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    transitionPhase = StackTransitionPhase.LANDING_AT_FRONT
                    rawActiveIndex = targetAccountIdx

                    // 3. Glide incoming card smoothly into front slot (pos = 0)
                    transitionOffsetX.animateTo(
                        targetValue = 0f,
                        animationSpec = commitSpring
                    )
                    transitionPhase = StackTransitionPhase.IDLE
                    transitionCardId = null
                }

                // Tapping peeking card pos = 1 or pos = 2 smoothly brings that card to front
                fun onPeekingCardTapped(targetStackPos: Int) {
                    if (isAnimating || isDragging || n <= 1 || targetStackPos <= 0) return
                    scope.launch {
                        isAnimating = true
                        try {
                            if (targetStackPos == 1) {
                                performRightCommit()
                            } else {
                                val targetIdx = (activeIndex + targetStackPos) % n
                                performLeftCommit(targetAccountIdx = targetIdx)
                            }
                        } finally {
                            isAnimating = false
                        }
                    }
                }

                // Build the exact list of cards to render (top 3 + active transition card if outside top 3)
                val cardSpecs = remember(
                    visibleAccounts,
                    activeIndex,
                    visibleCount,
                    n,
                    transitionCardId,
                    transitionPhase
                ) {
                    val specs = mutableListOf<StackCardSpec>()
                    val includedIds = HashSet<String>()

                    for (pos in (visibleCount - 1) downTo 0) {
                        val idx = (activeIndex + pos) % n
                        val acc = visibleAccounts[idx]
                        includedIds.add(acc.id)

                        val isTransitionTarget = acc.id == transitionCardId
                        val cardZIndex = when {
                            isTransitionTarget && transitionPhase == StackTransitionPhase.TUCKING_TO_BACK -> -1f
                            isTransitionTarget && transitionPhase == StackTransitionPhase.PULLING_FROM_BACK_OUT -> -1f
                            isTransitionTarget && transitionPhase == StackTransitionPhase.LANDING_AT_FRONT -> 10f
                            pos == 0 -> 5f
                            else -> (visibleCount - pos).toFloat()
                        }

                        specs.add(
                            StackCardSpec(
                                account = acc,
                                stackPosition = pos,
                                targetY = slotYDp(pos),
                                targetScale = slotScale(pos),
                                targetAlpha = 1f,
                                zIndex = cardZIndex,
                                isFrontCard = (pos == 0),
                                isTrackedByDrag = (pos == 0 && !isTransitionTarget),
                                isTrackedByTransition = isTransitionTarget
                            )
                        )
                    }

                    // Include transitioning card when N > 3 and it is outside the top 3 (slot 3 rear tuck / pull)
                    val activeTransId = transitionCardId
                    if (activeTransId != null && !includedIds.contains(activeTransId)) {
                        val transAcc = visibleAccounts.firstOrNull { it.id == activeTransId }
                        if (transAcc != null) {
                            val isPullingOut = transitionPhase == StackTransitionPhase.PULLING_FROM_BACK_OUT
                            specs.add(
                                StackCardSpec(
                                    account = transAcc,
                                    stackPosition = 3,
                                    targetY = slotYDp(3),
                                    targetScale = slotScale(3),
                                    targetAlpha = if (isPullingOut) 1f else 0.40f,
                                    zIndex = -1f,
                                    isFrontCard = false,
                                    isTrackedByDrag = false,
                                    isTrackedByTransition = true
                                )
                            )
                        }
                    }

                    specs
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(n, activeIndex, cardWidthPx) {
                            if (n > 1) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        if (!isAnimating) {
                                            isDragging = true
                                        }
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        if (isAnimating) return@detectHorizontalDragGestures
                                        change.consume()
                                        isDragging = true
                                        val next = (dragOffsetX.value + dragAmount)
                                            .coerceIn(-cardWidthPx * 0.95f, cardWidthPx * 0.95f)
                                        scope.launch {
                                            // Zero-lag 1:1 finger tracking; background deck stays 100% stationary
                                            dragOffsetX.snapTo(next)
                                        }
                                    },
                                    onDragEnd = {
                                        if (isAnimating) return@detectHorizontalDragGestures
                                        isDragging = false
                                        scope.launch {
                                            isAnimating = true
                                            try {
                                                val currentX = dragOffsetX.value
                                                when {
                                                    // Right swipe past threshold: Front card goes to back
                                                    currentX >= swipeThresholdPx -> {
                                                        performRightCommit()
                                                    }
                                                    // Left swipe past threshold: Rear card comes to front
                                                    currentX <= -swipeThresholdPx -> {
                                                        performLeftCommit()
                                                    }
                                                    // Released before threshold: smooth rubber-band cancel back to 0f
                                                    else -> {
                                                        dragOffsetX.animateTo(
                                                            targetValue = 0f,
                                                            animationSpec = cancelSpring
                                                        )
                                                    }
                                                }
                                            } finally {
                                                isAnimating = false
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        if (isAnimating) return@detectHorizontalDragGestures
                                        isDragging = false
                                        scope.launch {
                                            isAnimating = true
                                            try {
                                                dragOffsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = cancelSpring
                                                )
                                            } finally {
                                                isAnimating = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    // Subtle depth-peek shadow strips strictly when accounts.size > 3 (no text)
                    if (n >= 5) {
                        val hint2Color = resolveAccountCardColor(visibleAccounts[(activeIndex + 4) % n])
                        DepthHintCardLayer(
                            tintColor = hint2Color,
                            isDark = isDark,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(cardHeight)
                                .zIndex(-4f)
                                .offset(y = (-80).dp)
                                .graphicsLayer {
                                    scaleX = 0.84f
                                    scaleY = 0.84f
                                    rotationZ = 0f
                                    alpha = 0.22f
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                }
                                .testTag("wallet_stack_depth_hint_2")
                        )
                    }

                    if (n >= 4) {
                        val hint1Color = resolveAccountCardColor(visibleAccounts[(activeIndex + 3) % n])
                        DepthHintCardLayer(
                            tintColor = hint1Color,
                            isDark = isDark,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(cardHeight)
                                .zIndex(-3f)
                                .offset(y = (-74).dp)
                                .graphicsLayer {
                                    scaleX = 0.88f
                                    scaleY = 0.88f
                                    rotationZ = 0f
                                    alpha = 0.40f
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                }
                                .testTag("wallet_stack_depth_hint_1")
                        )
                    }

                    // Active & peeking cards — background cards remain 100% stationary during drag and animate smoothly ONLY on commit
                    cardSpecs.forEach { spec ->
                        key(spec.account.id) {
                            AnimatedStackCardItem(
                                spec = spec,
                                dragOffsetXProvider = { dragOffsetX.value },
                                transitionOffsetXProvider = { transitionOffsetX.value },
                                cardWidthPx = cardWidthPx,
                                cardHeight = cardHeight,
                                currencySymbol = currencySymbol,
                                isBalanceVisible = isBalanceVisible,
                                isBangla = isBangla,
                                onCardTap = {
                                    if (!isDragging && !isAnimating && abs(dragOffsetX.value) < 8f) {
                                        if (spec.stackPosition == 0) {
                                            onAccountClick(spec.account)
                                        } else {
                                            onPeekingCardTapped(spec.stackPosition)
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedStackCardItem(
    spec: StackCardSpec,
    dragOffsetXProvider: () -> Float,
    transitionOffsetXProvider: () -> Float,
    cardWidthPx: Float,
    cardHeight: Dp,
    currencySymbol: String,
    isBalanceVisible: Boolean,
    isBangla: Boolean,
    onCardTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Only animate Y, Scale, and Alpha on commit (when target slot changes); strictly stationary during drag
    val animatedY by animateDpAsState(
        targetValue = spec.targetY,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 300f),
        label = "stack_card_y_${spec.account.id}"
    )
    val animatedScale by animateFloatAsState(
        targetValue = spec.targetScale,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 300f),
        label = "stack_card_scale_${spec.account.id}"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = spec.targetAlpha,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 300f),
        label = "stack_card_alpha_${spec.account.id}"
    )

    val liftMaxPx = with(density) { 10.dp.toPx() }

    val sharedBoundsModifier = if (spec.isFrontCard) {
        Modifier.walletCardSharedBounds(accountId = spec.account.id, cornerRadius = 22.dp)
    } else {
        Modifier
    }

    StackedWalletCard(
        account = spec.account,
        currencySymbol = currencySymbol,
        isBalanceVisible = isBalanceVisible,
        isBangla = isBangla,
        isFrontCard = spec.isFrontCard,
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .zIndex(spec.zIndex)
            .offset {
                val txPx = when {
                    spec.isTrackedByTransition -> transitionOffsetXProvider()
                    spec.isTrackedByDrag -> dragOffsetXProvider()
                    else -> 0f
                }
                val liftPx = if (txPx != 0f) {
                    -liftMaxPx * (abs(txPx) / cardWidthPx).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val baseYPx = with(density) { animatedY.toPx() }
                IntOffset(txPx.roundToInt(), (baseYPx + liftPx).roundToInt())
            }
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                rotationZ = 0f
                alpha = animatedAlpha
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .then(sharedBoundsModifier)
            .clickable(onClick = onCardTap)
            .testTag("wallet_stack_card_${spec.stackPosition}")
    )
}

@Composable
private fun DepthHintCardLayer(
    tintColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = tintColor,
        shadowElevation = if (isDark) 1.dp else 2.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            tintColor,
                            Color(0xFF0F172A).copy(alpha = 0.35f)
                        )
                    )
                )
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

@Composable
private fun StackedWalletCard(
    account: AccountEntity,
    currencySymbol: String,
    isBalanceVisible: Boolean,
    isBangla: Boolean,
    isFrontCard: Boolean,
    modifier: Modifier = Modifier
) {
    val language = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
    HishabPatiAccountCard(
        account = account,
        currencySymbol = currencySymbol,
        modifier = modifier,
        isBalanceVisible = isBalanceVisible,
        language = language,
        cornerRadius = 22.dp,
        shadowElevation = if (isFrontCard) 6.dp else 2.dp,
        peekingHeaderOnly = !isFrontCard
    )
}
