package com.example.ui.screens.accounts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountType
import com.example.ui.components.rememberReducedMotionEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI_F = (2.0 * PI).toFloat()

/**
 * Computes a dynamic multi-stop 3D light-source gradient directly from the card's [baseColor]:
 * Brighter top-left light-catch tint -> rich true mid-tone -> deep saturated bottom-right shadow tone.
 * Preserves hue fidelity while ensuring crisp >= 4.5:1 contrast for white typography.
 */
internal fun buildAccountCardGradient(
    baseColor: Color,
    type: AccountType? = null,
    alpha: Float = 1f
): List<Color> {
    val anchoredBase = if (baseColor.luminance() > 0.44f) {
        lerp(baseColor, Color(0xFF0F172A), 0.25f)
    } else {
        baseColor
    }

    val lightCatch = when (type) {
        AccountType.CREDIT_CARD -> lerp(anchoredBase, Color.White, 0.14f)
        AccountType.BANK -> lerp(anchoredBase, Color.White, 0.18f)
        AccountType.MFS -> lerp(anchoredBase, Color.White, 0.16f)
        AccountType.CASH -> lerp(anchoredBase, Color.White, 0.15f)
        AccountType.OTHER, null -> lerp(anchoredBase, Color.White, 0.17f)
    }

    val deepShadow = when (type) {
        AccountType.CREDIT_CARD -> lerp(anchoredBase, Color(0xFF050810), 0.50f)
        AccountType.BANK -> lerp(anchoredBase, Color(0xFF070E1E), 0.42f)
        AccountType.MFS -> lerp(anchoredBase, Color(0xFF090D1A), 0.38f)
        AccountType.CASH -> lerp(anchoredBase, Color(0xFF061210), 0.36f)
        AccountType.OTHER, null -> lerp(anchoredBase, Color(0xFF0A0F24), 0.44f)
    }

    val richMid = lerp(anchoredBase, deepShadow, 0.12f)

    return listOf(
        lightCatch.copy(alpha = alpha),
        anchoredBase.copy(alpha = alpha),
        richMid.copy(alpha = alpha),
        deepShadow.copy(alpha = alpha)
    )
}

/**
 * Crisp 3D physical bevel & specular edge rim brush transitioning diagonally from
 * bright top-left specular white (0.38f) -> soft mid-edge (0.12f) -> subtle dark bottom-right bevel (0.20f).
 */
internal fun build3DSpecularRimBrush(): Brush = Brush.linearGradient(
    0.00f to Color.White.copy(alpha = 0.38f),
    0.34f to Color.White.copy(alpha = 0.14f),
    0.58f to Color.White.copy(alpha = 0.05f),
    0.80f to Color.Black.copy(alpha = 0.08f),
    1.00f to Color.Black.copy(alpha = 0.20f),
    start = Offset(0f, 0f),
    end = Offset(900f, 620f)
)

/**
 * Dimensional 3D physical card studio lighting & diagonal white glass reflection sheen:
 * - Realistic diagonal white specular reflection band from top-left (0, 0) to bottom-right (w, h)
 *   with stops 0.0f -> 0.16f, 0.38f -> 0.08f, 0.52f -> 0.01f, 1.0f -> Transparent
 * - Subtle top-left catch-light & bottom-right ambient vignette for real 3D physical depth
 */
@Composable
internal fun Card3DLightingOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Realistic diagonal white glass specular reflection band (top-left -> bottom-right)
        drawRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.16f),
                    0.38f to Color.White.copy(alpha = 0.08f),
                    0.52f to Color.White.copy(alpha = 0.01f),
                    1.00f to Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Soft radial top-left catch-light glow
        val topLightCenter = Offset(w * 0.14f, h * 0.06f)
        val topLightRadius = maxOf(w, h) * 0.64f
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.12f),
                    0.48f to Color.White.copy(alpha = 0.04f),
                    1.00f to Color.Transparent
                ),
                center = topLightCenter,
                radius = topLightRadius
            )
        )

        // Subtle bottom-right ambient vignette for physical 3D depth
        val bottomShadowCenter = Offset(w * 0.94f, h * 0.96f)
        val bottomShadowRadius = maxOf(w, h) * 0.72f
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color.Black.copy(alpha = 0.16f),
                    0.48f to Color.Black.copy(alpha = 0.06f),
                    1.00f to Color.Transparent
                ),
                center = bottomShadowCenter,
                radius = bottomShadowRadius
            )
        )
    }
}

/**
 * Premium rotating Geometric Guilloché / Spirograph Rosette artwork used EXCLUSIVELY on
 * BalanceCardHero. Anchored on the right-center side of the card (0.78w, 0.52h), renders
 * 20 concentric, progressively rotated hairline elliptical petal loops (0.9.dp strokes)
 * transitioning from luminous white (14% alpha) to soft cyan-lavender (#A5F3FC, 16% alpha)
 * with a serene 24s rotational cycle and subtle 0.97..1.03 breathing scale pulse.
 */
@Composable
internal fun AnimatedGuillocheRosetteCanvas(
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()

    val rotationDeg: Float
    val breathScale: Float

    if (!reducedMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "balance_card_guilloche_rosette")
        val rot by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 24000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "guilloche_rosette_rotation"
        )
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "guilloche_rosette_breath"
        )
        rotationDeg = rot
        breathScale = scale
    } else {
        rotationDeg = 12f
        breathScale = 1.0f
    }

    val innerPetalColor = remember { Color.White.copy(alpha = 0.14f) }
    val outerPetalColor = remember { Color(0xFFA5F3FC).copy(alpha = 0.16f) }
    val accentCyanGlow = remember { Color(0xFFA5F3FC).copy(alpha = 0.10f) }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val center = Offset(w * 0.78f, h * 0.52f)
        val strokeWidthPx = 0.9.dp.toPx()
        val fineStrokePx = 0.65.dp.toPx()
        val primaryStroke = Stroke(width = strokeWidthPx)
        val fineStroke = Stroke(width = fineStrokePx)

        // Subtle luminous halo centered behind the security rosette
        val haloRadius = h * 0.96f * breathScale
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to accentCyanGlow,
                    0.52f to Color.White.copy(alpha = 0.04f),
                    1.00f to Color.Transparent
                ),
                center = center,
                radius = haloRadius
            ),
            radius = haloRadius,
            center = center
        )

        val loopCount = 20
        val angleStepDeg = 180f / loopCount

        // Outer & mid woven spirograph rosette (20 concentric rotated elliptical petal loops)
        withTransform({
            scale(scaleX = breathScale, scaleY = breathScale, pivot = center)
            rotate(degrees = rotationDeg, pivot = center)
        }) {
            for (i in 0 until loopCount) {
                val progress = i.toFloat() / (loopCount - 1).toFloat()
                // Smooth radial envelope creates an authentic engraved bank-note petal weave
                val rx = h * (0.50f + progress * 0.42f)
                val ry = h * (0.21f + progress * 0.24f)
                val petalColor = lerp(innerPetalColor, outerPetalColor, progress)

                withTransform({
                    rotate(degrees = i * angleStepDeg, pivot = center)
                }) {
                    drawOval(
                        color = petalColor,
                        topLeft = Offset(center.x - rx, center.y - ry),
                        size = Size(rx * 2f, ry * 2f),
                        style = primaryStroke
                    )
                }
            }
        }

        // Subtle counter-rotating inner guilloché filigree core for fine engraved depth
        val coreLoopCount = 10
        val coreAngleStep = 180f / coreLoopCount
        withTransform({
            scale(scaleX = breathScale, scaleY = breathScale, pivot = center)
            rotate(degrees = -rotationDeg * 0.5f, pivot = center)
        }) {
            val coreRx = h * 0.34f
            val coreRy = h * 0.14f
            for (i in 0 until coreLoopCount) {
                withTransform({
                    rotate(degrees = i * coreAngleStep, pivot = center)
                }) {
                    drawOval(
                        color = Color.White.copy(alpha = 0.11f),
                        topLeft = Offset(center.x - coreRx, center.y - coreRy),
                        size = Size(coreRx * 2f, coreRy * 2f),
                        style = fineStroke
                    )
                }
            }
        }

        // Delicate outer guilloché guide ring framing the rosette cleanly
        drawCircle(
            color = Color(0xFFA5F3FC).copy(alpha = 0.11f),
            radius = h * 0.92f * breathScale,
            center = center,
            style = fineStroke
        )
    }
}

/**
 * Renders clean, static category-specific bespoke surface artwork for individual account cards:
 * - CREDIT_CARD: Static fine-line Guilloché concentric arc mesh sweeping from bottom-right
 * - BANK: Static smooth dual-layered silk cubic Bezier ribbon curves
 * - MFS: Static faceted origami / low-poly translucent geometric light planes
 * - CASH / OTHER: Static soft organic flowing silk/linen curves
 */
@Composable
internal fun AccountCardPatternCanvas(
    type: AccountType,
    isMini: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pathA = remember { Path() }
    val pathB = remember { Path() }
    val pathC = remember { Path() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        when (type) {
            AccountType.CREDIT_CARD -> {
                // Static fine-line Guilloché concentric elliptical arc mesh from bottom-right
                val lineStroke = (if (isMini) 0.55.dp else 0.95.dp).toPx()
                val ringCount = if (isMini) 5 else 9
                val anchorX = w * 0.92f
                val anchorY = h * 0.96f

                for (i in 1..ringCount) {
                    val t = i.toFloat() / ringCount
                    val rx = w * (0.22f + t * 0.72f)
                    val ry = h * (0.26f + t * 0.85f)
                    val alpha = if (i % 2 == 0) 0.14f else 0.08f
                    pathA.reset()
                    pathA.moveTo(anchorX - rx, anchorY)
                    pathA.cubicTo(
                        anchorX - rx * 0.92f,
                        anchorY - ry * 0.72f,
                        anchorX - rx * 0.34f,
                        anchorY - ry,
                        anchorX + rx * 0.2f,
                        anchorY - ry * 0.92f
                    )
                    drawPath(
                        path = pathA,
                        color = Color.White.copy(alpha = if (isMini) alpha * 1.15f else alpha),
                        style = Stroke(width = lineStroke)
                    )
                }
            }

            AccountType.BANK -> {
                // Static smooth dual-layered silk curve ribbons
                pathA.reset()
                pathA.moveTo(-w * 0.05f, h * 0.78f)
                pathA.cubicTo(
                    w * 0.28f, h * 0.36f,
                    w * 0.62f, h * 1.05f,
                    w * 1.08f, h * 0.42f
                )
                pathA.lineTo(w * 1.08f, h * 1.05f)
                pathA.lineTo(-w * 0.05f, h * 1.05f)
                pathA.close()
                drawPath(
                    path = pathA,
                    color = Color.White.copy(alpha = if (isMini) 0.16f else 0.11f)
                )

                pathB.reset()
                pathB.moveTo(w * 0.15f, h * 1.02f)
                pathB.cubicTo(
                    w * 0.42f, h * 0.46f,
                    w * 0.78f, h * 0.82f,
                    w * 1.05f, h * 0.16f
                )
                pathB.lineTo(w * 1.05f, h * 1.05f)
                pathB.close()
                drawPath(
                    path = pathB,
                    color = Color.White.copy(alpha = if (isMini) 0.10f else 0.07f)
                )

                if (!isMini) {
                    pathC.reset()
                    pathC.moveTo(-w * 0.05f, h * 0.78f)
                    pathC.cubicTo(
                        w * 0.28f, h * 0.36f,
                        w * 0.62f, h * 1.05f,
                        w * 1.08f, h * 0.42f
                    )
                    drawPath(
                        path = pathC,
                        color = Color.White.copy(alpha = 0.14f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            AccountType.MFS -> {
                // Static faceted origami / low-poly translucent geometric light planes
                pathA.reset()
                pathA.moveTo(w * 0.56f, 0f)
                pathA.lineTo(w, 0f)
                pathA.lineTo(w, h * 0.66f)
                pathA.close()
                drawPath(
                    path = pathA,
                    color = Color.White.copy(alpha = if (isMini) 0.15f else 0.10f)
                )

                pathB.reset()
                pathB.moveTo(w * 0.40f, h)
                pathB.lineTo(w * 0.78f, h * 0.32f)
                pathB.lineTo(w, h * 0.66f)
                pathB.lineTo(w, h)
                pathB.close()
                drawPath(
                    path = pathB,
                    color = Color.White.copy(alpha = if (isMini) 0.20f else 0.15f)
                )

                pathC.reset()
                pathC.moveTo(w * 0.56f, 0f)
                pathC.lineTo(w * 0.78f, h * 0.32f)
                pathC.lineTo(w * 0.48f, h * 0.48f)
                pathC.close()
                drawPath(
                    path = pathC,
                    color = Color.White.copy(alpha = if (isMini) 0.11f else 0.08f)
                )

                if (!isMini) {
                    val creaseStroke = 0.85.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.14f),
                        start = Offset(w * 0.78f, h * 0.32f),
                        end = Offset(w * 0.40f, h),
                        strokeWidth = creaseStroke
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = Offset(w * 0.56f, 0f),
                        end = Offset(w * 0.78f, h * 0.32f),
                        strokeWidth = creaseStroke
                    )
                }
            }

            AccountType.CASH, AccountType.OTHER -> {
                // Static soft organic flowing silk/linen curves
                pathA.reset()
                pathA.moveTo(0f, h * 0.70f)
                pathA.cubicTo(
                    w * 0.30f, h * 0.44f,
                    w * 0.66f, h * 0.92f,
                    w, h * 0.52f
                )
                pathA.lineTo(w, h)
                pathA.lineTo(0f, h)
                pathA.close()
                drawPath(
                    path = pathA,
                    color = Color.White.copy(alpha = if (isMini) 0.15f else 0.12f)
                )

                pathB.reset()
                pathB.moveTo(0f, h * 0.86f)
                pathB.cubicTo(
                    w * 0.36f, h * 0.66f,
                    w * 0.72f, h * 1.02f,
                    w, h * 0.74f
                )
                pathB.lineTo(w, h)
                pathB.lineTo(0f, h)
                pathB.close()
                drawPath(
                    path = pathB,
                    color = Color.White.copy(alpha = if (isMini) 0.10f else 0.07f)
                )

                if (!isMini) {
                    pathC.reset()
                    pathC.moveTo(0f, h * 0.56f)
                    pathC.cubicTo(
                        w * 0.32f, h * 0.32f,
                        w * 0.64f, h * 0.78f,
                        w, h * 0.38f
                    )
                    drawPath(
                        path = pathC,
                        color = Color.White.copy(alpha = 0.11f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Subtle holographic iridescent security foil badge placed beside Credit Card network badges.
 */
@Composable
internal fun IridescentSecurityFoil(
    modifier: Modifier = Modifier
) {
    val foilShape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .size(width = 18.dp, height = 13.dp)
            .clip(foilShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE0E7FF).copy(alpha = 0.52f),
                        Color(0xFFFDE68A).copy(alpha = 0.44f),
                        Color(0xFFA7F3D0).copy(alpha = 0.48f),
                        Color(0xFFFBCFE8).copy(alpha = 0.42f)
                    )
                )
            )
            .border(
                width = 0.6.dp,
                color = Color.White.copy(alpha = 0.42f),
                shape = foilShape
            )
    )
}

/**
 * Tactile embossed currency watermark seal for Cash wallets.
 */
@Composable
internal fun CashCurrencyWatermarkSeal(
    symbol: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.32f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol.ifBlank { "৳" },
            style = MaterialTheme.typography.labelLarge.copy(
                color = Color.White.copy(alpha = 0.92f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        )
    }
}
