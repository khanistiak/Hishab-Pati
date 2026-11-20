package com.example.ui.screens.accounts

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountType

/**
 * Computes a dynamic multi-stop gradient directly from the user's chosen [baseColor]
 * while preserving hue fidelity and ensuring crisp contrast for white typography.
 */
internal fun buildAccountCardGradient(
    baseColor: Color,
    type: AccountType,
    alpha: Float = 1f
): List<Color> {
    // Gently anchor ultra-bright user-selected colors so white text remains >= 4.5:1 legible
    val anchoredBase = if (baseColor.luminance() > 0.44f) {
        lerp(baseColor, Color(0xFF0F172A), 0.25f)
    } else {
        baseColor
    }

    return when (type) {
        AccountType.CREDIT_CARD -> listOf(
            lerp(anchoredBase, Color.White, 0.06f).copy(alpha = alpha),
            anchoredBase.copy(alpha = alpha),
            lerp(anchoredBase, Color(0xFF070B14), 0.46f).copy(alpha = alpha)
        )
        AccountType.BANK -> listOf(
            lerp(anchoredBase, Color.White, 0.10f).copy(alpha = alpha),
            anchoredBase.copy(alpha = alpha),
            lerp(anchoredBase, Color(0xFF0B1324), 0.36f).copy(alpha = alpha)
        )
        AccountType.MFS -> listOf(
            lerp(anchoredBase, Color.White, 0.09f).copy(alpha = alpha),
            anchoredBase.copy(alpha = alpha),
            lerp(anchoredBase, Color(0xFF0B1120), 0.30f).copy(alpha = alpha)
        )
        AccountType.CASH -> listOf(
            lerp(anchoredBase, Color.White, 0.07f).copy(alpha = alpha),
            anchoredBase.copy(alpha = alpha),
            lerp(anchoredBase, Color(0xFF0A1412), 0.28f).copy(alpha = alpha)
        )
        AccountType.OTHER -> listOf(
            lerp(anchoredBase, Color.White, 0.08f).copy(alpha = alpha),
            anchoredBase.copy(alpha = alpha),
            lerp(anchoredBase, Color(0xFF0F172A), 0.32f).copy(alpha = alpha)
        )
    }
}

/**
 * Renders category-specific bespoke surface artwork over the user's tinted card background:
 * - CREDIT_CARD: Fine-line Guilloché concentric wave mesh sweeping from bottom-right
 * - BANK: Smooth dual-layered silk cubic Bezier ribbons
 * - MFS: Faceted origami / low-poly translucent geometric light planes
 * - CASH: Soft, organic flowing linen/ribbon waves
 */
@Composable
internal fun AccountCardPatternCanvas(
    type: AccountType,
    isMini: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        when (type) {
            AccountType.CREDIT_CARD -> {
                // Fine-line Guilloché concentric elliptical wave mesh from bottom-right
                val lineStroke = (if (isMini) 0.55.dp else 0.95.dp).toPx()
                val ringCount = if (isMini) 5 else 9
                val anchorX = w * 0.92f
                val anchorY = h * 0.96f

                for (i in 1..ringCount) {
                    val t = i.toFloat() / ringCount
                    val rx = w * (0.22f + t * 0.72f)
                    val ry = h * (0.26f + t * 0.85f)
                    val alpha = if (i % 2 == 0) 0.14f else 0.08f
                    val path = Path().apply {
                        moveTo(anchorX - rx, anchorY)
                        cubicTo(
                            anchorX - rx * 0.92f,
                            anchorY - ry * 0.72f,
                            anchorX - rx * 0.34f,
                            anchorY - ry,
                            anchorX + rx * 0.2f,
                            anchorY - ry * 0.92f
                        )
                    }
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = if (isMini) alpha * 1.15f else alpha),
                        style = Stroke(width = lineStroke)
                    )
                }
            }

            AccountType.BANK -> {
                // Smooth dual-layered silk curve ribbons
                val primaryRibbon = Path().apply {
                    moveTo(-w * 0.05f, h * 0.78f)
                    cubicTo(
                        w * 0.28f, h * 0.36f,
                        w * 0.62f, h * 1.05f,
                        w * 1.08f, h * 0.42f
                    )
                    lineTo(w * 1.08f, h * 1.05f)
                    lineTo(-w * 0.05f, h * 1.05f)
                    close()
                }
                drawPath(
                    path = primaryRibbon,
                    color = Color.White.copy(alpha = if (isMini) 0.16f else 0.11f)
                )

                val secondaryRibbon = Path().apply {
                    moveTo(w * 0.15f, h * 1.02f)
                    cubicTo(
                        w * 0.42f, h * 0.46f,
                        w * 0.78f, h * 0.82f,
                        w * 1.05f, h * 0.16f
                    )
                    lineTo(w * 1.05f, h * 1.05f)
                    close()
                }
                drawPath(
                    path = secondaryRibbon,
                    color = Color.White.copy(alpha = if (isMini) 0.10f else 0.07f)
                )

                if (!isMini) {
                    val silkEdge = Path().apply {
                        moveTo(-w * 0.05f, h * 0.78f)
                        cubicTo(
                            w * 0.28f, h * 0.36f,
                            w * 0.62f, h * 1.05f,
                            w * 1.08f, h * 0.42f
                        )
                    }
                    drawPath(
                        path = silkEdge,
                        color = Color.White.copy(alpha = 0.14f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            AccountType.MFS -> {
                // Faceted origami / low-poly translucent geometric planes
                val facetA = Path().apply {
                    moveTo(w * 0.56f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h * 0.66f)
                    close()
                }
                drawPath(
                    path = facetA,
                    color = Color.White.copy(alpha = if (isMini) 0.15f else 0.10f)
                )

                val facetB = Path().apply {
                    moveTo(w * 0.40f, h)
                    lineTo(w * 0.78f, h * 0.32f)
                    lineTo(w, h * 0.66f)
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = facetB,
                    color = Color.White.copy(alpha = if (isMini) 0.20f else 0.15f)
                )

                val facetC = Path().apply {
                    moveTo(w * 0.56f, 0f)
                    lineTo(w * 0.78f, h * 0.32f)
                    lineTo(w * 0.48f, h * 0.48f)
                    close()
                }
                drawPath(
                    path = facetC,
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
                // Soft, organic flowing linen/ribbon waves
                val wave1 = Path().apply {
                    moveTo(0f, h * 0.70f)
                    cubicTo(
                        w * 0.30f, h * 0.44f,
                        w * 0.66f, h * 0.92f,
                        w, h * 0.52f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = wave1,
                    color = Color.White.copy(alpha = if (isMini) 0.15f else 0.12f)
                )

                val wave2 = Path().apply {
                    moveTo(0f, h * 0.86f)
                    cubicTo(
                        w * 0.36f, h * 0.66f,
                        w * 0.72f, h * 1.02f,
                        w, h * 0.74f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = wave2,
                    color = Color.White.copy(alpha = if (isMini) 0.10f else 0.07f)
                )

                if (!isMini) {
                    val linenStroke = Path().apply {
                        moveTo(0f, h * 0.56f)
                        cubicTo(
                            w * 0.32f, h * 0.32f,
                            w * 0.64f, h * 0.78f,
                            w, h * 0.38f
                        )
                    }
                    drawPath(
                        path = linenStroke,
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
    val foilShape = RoundedCornerShape(3.5.dp)
    Box(
        modifier = modifier
            .size(width = 20.dp, height = 14.dp)
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
