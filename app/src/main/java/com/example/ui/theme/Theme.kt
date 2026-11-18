package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.BackgroundTextureOption
import com.example.data.model.ThemeColorOption
import com.example.data.model.ThemeMode
import com.example.ui.components.FrostedBackdropState
import com.example.ui.components.LocalFrostedBackdropState
import com.example.ui.components.recordFrostedBackground

@Composable
fun MyWalletTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    themeColor: ThemeColorOption = ThemeColorOption.SKY_CYAN,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val primary = Color(themeColor.primaryHex)
    val secondary = Color(themeColor.secondaryHex)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            secondary = secondary,
            tertiary = IncomeGreen,
            background = Color(0xFF0B132B),
            surface = Color(0xFF1E293B),
            surfaceVariant = Color(0xFF334155),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF475569),
            outlineVariant = Color(0xFF334155)
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = secondary,
            tertiary = IncomeGreen,
            background = Color(0xFFF8FAFC),
            surface = Color.White,
            surfaceVariant = Color(0xFFF1F5F9),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            outlineVariant = Color(0xFFE2E8F0)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides colorScheme.onBackground
        ) {
            content()
        }
    }
}

@Composable
fun MyWalletBackground(
    modifier: Modifier = Modifier,
    texture: BackgroundTextureOption = BackgroundTextureOption.AURORA,
    themeColor: ThemeColorOption = ThemeColorOption.SKY_CYAN,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val primary = Color(themeColor.primaryHex)
    val secondary = Color(themeColor.secondaryHex)

    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF090D16),
                Color(0xFF111827),
                Color(0xFF0B132B)
            )
        )
    } else {
        when (texture) {
            BackgroundTextureOption.AURORA -> Brush.verticalGradient(
                listOf(
                    Color(0xFFE0E7FF), // soft indigo
                    Color(0xFFEFF6FF), // soft sky
                    Color(0xFFFAF5FF), // soft purple
                    Color(0xFFF8FAFC)  // crisp slate
                )
            )
            BackgroundTextureOption.WAVES -> Brush.linearGradient(
                listOf(
                    Color(0xFFE0F2FE),
                    Color(0xFFEDE9FE),
                    Color(0xFFF0FDF4)
                )
            )
            BackgroundTextureOption.COLOUR_MESH -> Brush.radialGradient(
                colors = listOf(
                    secondary.copy(alpha = 0.25f),
                    Color(0xFFF8FAFC)
                ),
                radius = 1200f
            )
            BackgroundTextureOption.PLAIN -> Brush.verticalGradient(
                listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))
            )
            else -> Brush.verticalGradient(
                listOf(
                    primary.copy(alpha = 0.12f),
                    secondary.copy(alpha = 0.08f),
                    Color(0xFFF8FAFC)
                )
            )
        }
    }

    val existingBackdropState = LocalFrostedBackdropState.current
    val contentGraphicsLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val backgroundGraphicsLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val backdropState = existingBackdropState ?: androidx.compose.runtime.remember(
        contentGraphicsLayer,
        backgroundGraphicsLayer
    ) {
        FrostedBackdropState(
            graphicsLayer = contentGraphicsLayer,
            backgroundGraphicsLayer = backgroundGraphicsLayer
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalFrostedBackdropState provides backdropState
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .recordFrostedBackground(backdropState)
            ) {
                drawRect(brush = bgGradient)

                // Draw subtle decorative ambient glows matching the screenshots
                if (!isDark) {
                    // Top-right soft pastel magenta/violet glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x28EC4899), Color.Transparent),
                            center = Offset(size.width * 0.9f, size.height * 0.05f),
                            radius = size.width * 0.6f
                        ),
                        radius = size.width * 0.6f,
                        center = Offset(size.width * 0.9f, size.height * 0.05f)
                    )
                    // Top-left soft sky/cyan glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3238BDF8), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.08f),
                            radius = size.width * 0.7f
                        ),
                        radius = size.width * 0.7f,
                        center = Offset(size.width * 0.1f, size.height * 0.08f)
                    )

                    // Optional subtle pattern overlays
                    if (texture == BackgroundTextureOption.DOTS) {
                        val spacing = 28f
                        val dotRadius = 1.5f
                        var x = 0f
                        while (x < size.width) {
                            var y = 0f
                            while (y < size.height) {
                                drawCircle(
                                    color = Color(0x106366F1),
                                                                    radius = dotRadius,
                                    center = Offset(x, y)
                                )
                                y += spacing
                            }
                            x += spacing
                        }
                    } else if (texture == BackgroundTextureOption.GRID) {
                        val gridSpacing = 40f
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = Color(0x0C6366F1),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                            x += gridSpacing
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = Color(0x0C6366F1),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += gridSpacing
                        }
                    }
                } else {
                    // Subtle dark ambient accent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.2f), Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.1f),
                            radius = size.width * 0.8f
                        ),
                        radius = size.width * 0.8f,
                        center = Offset(size.width * 0.8f, size.height * 0.1f)
                    )
                }
            }
            content()
        }
    }
}
