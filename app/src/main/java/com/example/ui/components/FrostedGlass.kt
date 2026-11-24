package com.example.ui.components

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.ui.theme.LocalIsDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

class FrostedBackdropState(
    val graphicsLayer: GraphicsLayer? = null,
    val backgroundGraphicsLayer: GraphicsLayer? = null
) {
    var layerOriginInRoot: Offset by mutableStateOf(Offset.Zero)
    var backgroundOriginInRoot: Offset by mutableStateOf(Offset.Zero)
    var hasRecordedContent: Boolean by mutableStateOf(false)
    var hasRecordedBackground: Boolean by mutableStateOf(false)
    var lazyListState: LazyListState? by mutableStateOf(null)
    var scrollTick: Int by mutableIntStateOf(0)
    var activeContentLayer: GraphicsLayer? by mutableStateOf(null)
    private val recordedContentLayers = HashSet<GraphicsLayer>()
    var targetGlassProgress: Float by mutableFloatStateOf(0f)

    fun markLayerRecorded(layer: GraphicsLayer) {
        recordedContentLayers.add(layer)
    }

    fun isLayerRecorded(layer: GraphicsLayer): Boolean = recordedContentLayers.contains(layer)
}

val LocalFrostedBackdropState = staticCompositionLocalOf<FrostedBackdropState?> { null }

fun Modifier.recordFrostedBackground(backdropState: FrostedBackdropState?): Modifier {
    val bgLayer = backdropState?.backgroundGraphicsLayer ?: return this
    return this
        .onGloballyPositioned { coords ->
            backdropState.backgroundOriginInRoot = coords.positionInRoot()
        }
        .drawWithContent {
            bgLayer.record {
                this@drawWithContent.drawContent()
            }
            backdropState.hasRecordedBackground = true
            drawLayer(bgLayer)
        }
}

fun Modifier.recordFrostedBackdrop(
    backdropState: FrostedBackdropState?,
    lazyListState: LazyListState? = null,
    contentLayer: GraphicsLayer? = null
): Modifier {
    val targetLayer = contentLayer ?: backdropState?.graphicsLayer ?: return this
    return this
        .onGloballyPositioned { coords ->
            backdropState?.layerOriginInRoot = coords.positionInRoot()
            backdropState?.lazyListState = lazyListState
            backdropState?.activeContentLayer = targetLayer
        }
        .drawWithContent {
            targetLayer.record {
                this@drawWithContent.drawContent()
            }
            backdropState?.markLayerRecorded(targetLayer)
            backdropState?.hasRecordedContent = true
            backdropState?.activeContentLayer = targetLayer
            backdropState?.scrollTick = ((backdropState?.scrollTick ?: 0) + 1) % 100000
            drawLayer(targetLayer)
        }
}

fun Modifier.crystalOuterShadow(
    shape: Shape,
    color: Color = Color(0x18000000),
    blurSpread: Dp = 8.dp,
    offsetY: Dp = 3.dp
): Modifier = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    clipPath(path, ClipOp.Difference) {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            frameworkPaint.isAntiAlias = true
            frameworkPaint.color = android.graphics.Color.TRANSPARENT
            frameworkPaint.setShadowLayer(
                blurSpread.toPx(),
                0f,
                offsetY.toPx(),
                color.toArgb()
            )
            canvas.drawPath(path, paint)
        }
    }
}

class ScrollAwareGlassLayoutState(
    val headerHeightPxState: MutableIntState,
    val contentPadding: PaddingValues,
    val targetGlassProgress: Float,
    val backdropState: FrostedBackdropState?,
    val contentGraphicsLayer: GraphicsLayer
)

@Composable
fun rememberScrollAwareGlassLayout(
    listState: LazyListState,
    defaultHeaderHeight: Dp = 72.dp,
    extraTopGap: Dp = 4.dp,
    bottomNavClearance: Dp = 104.dp
): ScrollAwareGlassLayoutState {
    val frostedBackdropState = LocalFrostedBackdropState.current
    val tabContentGraphicsLayer = rememberGraphicsLayer()
    val density = LocalDensity.current
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val headerHeightPxState = remember { mutableIntStateOf(0) }
    val measuredHeaderHeightDp = with(density) {
        if (headerHeightPxState.intValue > 0) {
            headerHeightPxState.intValue.toDp()
        } else {
            statusBarTopPadding + defaultHeaderHeight
        }
    }

    val topContentPadding = measuredHeaderHeightDp + extraTopGap
    val bottomContentPadding = navBarClearanceValue(navBarBottomPadding, bottomNavClearance)

    val rampDistancePx = with(density) { 48.dp.toPx() }.coerceAtLeast(1f)
    val targetGlassProgress by remember(listState, rampDistancePx) {
        derivedStateOf {
            val rawScrollPx = if (listState.firstVisibleItemIndex > 0) {
                rampDistancePx
            } else {
                listState.firstVisibleItemScrollOffset.toFloat()
            }
            if (rawScrollPx <= 2f) {
                0f
            } else if (rawScrollPx >= rampDistancePx) {
                1f
            } else {
                val fraction = (rawScrollPx / rampDistancePx).coerceIn(0f, 1f)
                val progress = (fraction * 0.88f + 0.12f).coerceIn(0f, 1f)
                // Quantize to 0.04f steps during the short 48dp ramp to avoid per-pixel recompositions
                ((progress * 25f).toInt() / 25f)
            }
        }
    }

    LaunchedEffect(targetGlassProgress, listState, tabContentGraphicsLayer) {
        frostedBackdropState?.targetGlassProgress = targetGlassProgress
        frostedBackdropState?.lazyListState = listState
        frostedBackdropState?.activeContentLayer = tabContentGraphicsLayer
    }

    return remember(
        headerHeightPxState,
        topContentPadding,
        bottomContentPadding,
        targetGlassProgress,
        frostedBackdropState,
        tabContentGraphicsLayer
    ) {
        ScrollAwareGlassLayoutState(
            headerHeightPxState = headerHeightPxState,
            contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding),
            targetGlassProgress = targetGlassProgress,
            backdropState = frostedBackdropState,
            contentGraphicsLayer = tabContentGraphicsLayer
        )
    }
}

@Composable
fun rememberScrollAwareGlassLayout(
    scrollState: ScrollState,
    defaultHeaderHeight: Dp = 56.dp,
    extraTopGap: Dp = 4.dp,
    bottomNavClearance: Dp = 24.dp
): ScrollAwareGlassLayoutState {
    val frostedBackdropState = LocalFrostedBackdropState.current
    val tabContentGraphicsLayer = rememberGraphicsLayer()
    val density = LocalDensity.current
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val headerHeightPxState = remember { mutableIntStateOf(0) }
    val measuredHeaderHeightDp = with(density) {
        if (headerHeightPxState.intValue > 0) {
            headerHeightPxState.intValue.toDp()
        } else {
            statusBarTopPadding + defaultHeaderHeight
        }
    }

    val topContentPadding = measuredHeaderHeightDp + extraTopGap
    val bottomContentPadding = navBarClearanceValue(navBarBottomPadding, bottomNavClearance)

    val rampDistancePx = with(density) { 48.dp.toPx() }.coerceAtLeast(1f)
    val targetGlassProgress by remember(scrollState, rampDistancePx) {
        derivedStateOf {
            val rawScrollPx = scrollState.value.toFloat()
            if (rawScrollPx <= 2f) {
                0f
            } else if (rawScrollPx >= rampDistancePx) {
                1f
            } else {
                val fraction = (rawScrollPx / rampDistancePx).coerceIn(0f, 1f)
                val progress = (fraction * 0.88f + 0.12f).coerceIn(0f, 1f)
                ((progress * 25f).toInt() / 25f)
            }
        }
    }

    LaunchedEffect(targetGlassProgress, tabContentGraphicsLayer) {
        frostedBackdropState?.targetGlassProgress = targetGlassProgress
        frostedBackdropState?.lazyListState = null
        frostedBackdropState?.activeContentLayer = tabContentGraphicsLayer
    }

    return remember(
        headerHeightPxState,
        topContentPadding,
        bottomContentPadding,
        targetGlassProgress,
        frostedBackdropState,
        tabContentGraphicsLayer
    ) {
        ScrollAwareGlassLayoutState(
            headerHeightPxState = headerHeightPxState,
            contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding),
            targetGlassProgress = targetGlassProgress,
            backdropState = frostedBackdropState,
            contentGraphicsLayer = tabContentGraphicsLayer
        )
    }
}

private fun navBarClearanceValue(navBarBottomPadding: Dp, bottomNavClearance: Dp): Dp =
    navBarBottomPadding + bottomNavClearance

@Composable
fun BoxScope.ScrollAwareFixedHeader(
    layoutState: ScrollAwareGlassLayoutState,
    modifier: Modifier = Modifier,
    testTag: String = "shared_fixed_header",
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDarkHeader = LocalIsDarkTheme.current ?: (surfaceColor.luminance() < 0.5f)
    val headerBaseBg = remember(isDarkHeader, surfaceColor, backgroundColor) {
        if (isDarkHeader) {
            backgroundColor.copy(alpha = 0.22f)
        } else {
            surfaceColor.copy(alpha = 0.16f)
        }
    }
    val headerOverlayBrush = remember(isDarkHeader) {
        if (isDarkHeader) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF0F172A).copy(alpha = 0.36f),
                    Color(0xFF1E293B).copy(alpha = 0.28f),
                    Color.Transparent
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.32f),
                    Color.White.copy(alpha = 0.22f),
                    Color.Transparent
                )
            )
        }
    }

    FrostedGlassSurface(
        modifier = modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .onSizeChanged { layoutState.headerHeightPxState.intValue = it.height }
            .testTag(testTag),
        shape = RoundedCornerShape(0.dp),
        baseBackgroundColor = headerBaseBg,
        overlayBrush = headerOverlayBrush,
        restingTintBrush = null,
        blurRadius = 10.dp,
        glassProgressOverride = layoutState.targetGlassProgress,
        border = null,
        featherBottomEdge = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            content()
        }
    }
}

@Composable
fun FrostedGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    baseBackgroundColor: Color = MaterialTheme.colorScheme.background,
    overlayBrush: Brush? = null,
    restingTintBrush: Brush? = null,
    blurRadius: Dp = 22.dp,
    glassProgressOverride: Float? = null,
    border: BorderStroke? = null,
    featherBottomEdge: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val backdropState = LocalFrostedBackdropState.current
    val isDark = LocalIsDarkTheme.current ?: (MaterialTheme.colorScheme.surface.luminance() < 0.5f)
    var surfaceOffsetInRoot by remember(isDark) { mutableStateOf(Offset.Zero) }
    val reducedMotion = rememberReducedMotionEnabled()

    val rawTargetProgress = glassProgressOverride ?: backdropState?.targetGlassProgress ?: 1f
    val effectiveGlassProgress by animateFloatAsState(
        targetValue = rawTargetProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 140),
        label = "FrostedGlassProgress"
    )

    val supportsRenderEffectBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val currentBlurDp = blurRadius * effectiveGlassProgress
    val backdropLayerAlpha = effectiveGlassProgress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                surfaceOffsetInRoot = coords.positionInRoot()
            }
            .clip(shape)
            .then(if (border != null && effectiveGlassProgress > 0.05f) Modifier.border(border, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Blurred backdrop layer + tint (keyed by isDark so theme switches never reuse cached dark backdrop layers)
        key(isDark) {
        if (effectiveGlassProgress > 0.005f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (featherBottomEdge) {
                            Modifier
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    val currentIsDark = isDark
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.0f to Color.Black,
                                            0.72f to Color.Black,
                                            1.0f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        } else {
                            Modifier
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val currentIsDark = isDark
                            alpha = backdropLayerAlpha
                            val blurPx = currentBlurDp.toPx()
                            renderEffect = if (supportsRenderEffectBlur && blurPx > 0.5f) {
                                BlurEffect(blurPx, blurPx, TileMode.Clamp)
                            } else {
                                null
                            }
                        }
                        .drawBehind {
                            val currentIsDark = isDark
                            val listState = backdropState?.lazyListState
                            val activeLayer = backdropState?.activeContentLayer ?: backdropState?.graphicsLayer
                            val scrollRead = (backdropState?.scrollTick ?: 0) +
                                (listState?.firstVisibleItemIndex ?: 0) +
                                (listState?.firstVisibleItemScrollOffset ?: 0) +
                                (if (currentIsDark) 1 else 0)
                            if (scrollRead >= 0) {
                                drawRect(color = baseBackgroundColor)
                                val bgLayer = backdropState?.backgroundGraphicsLayer
                                if (bgLayer != null && backdropState.hasRecordedBackground) {
                                    val bgDx = backdropState.backgroundOriginInRoot.x - surfaceOffsetInRoot.x
                                    val bgDy = backdropState.backgroundOriginInRoot.y - surfaceOffsetInRoot.y
                                    translate(left = bgDx, top = bgDy) {
                                        drawLayer(bgLayer)
                                    }
                                }
                                if (activeLayer != null && (backdropState?.hasRecordedContent == true || backdropState?.isLayerRecorded(activeLayer) == true)) {
                                    val dx = (backdropState?.layerOriginInRoot?.x ?: 0f) - surfaceOffsetInRoot.x
                                    val dy = (backdropState?.layerOriginInRoot?.y ?: 0f) - surfaceOffsetInRoot.y
                                    translate(left = dx, top = dy) {
                                        drawLayer(activeLayer)
                                    }
                                }
                            }
                        }
                )

                if (overlayBrush != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { 
                                val currentIsDark = isDark
                                alpha = effectiveGlassProgress 
                            }
                            .background(overlayBrush)
                    )
                }
            }
        } else if (restingTintBrush != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(restingTintBrush)
            )
        }
        }

        content()
    }
}
