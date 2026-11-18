package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.ThemeColorOption

val IncomeGreen = Color(0xFF10B981)
val IncomeGreenLight = Color(0xFFD1FAE5)
val ExpenseRed = Color(0xFFEF4444)
val ExpenseRedLight = Color(0xFFFEE2E2)
val TransferBlue = Color(0xFF3B82F6)
val TransferBlueLight = Color(0xFFDBEAFE)

val CardBorderLight = Color(0x1F6366F1)
val CardBackgroundLight = Color(0xFFFFFFFF)
val CardBackgroundDark = Color(0xFF1E1E2E)

// Crystal Liquid Bottom Navigation Tokens (Sky Cyan / Aqua)
val CrystalSkyCyan = Color(0xFF0284C7)
val CrystalSkyCyanDeep = Color(0xFF0369A1)
val CrystalSkyCyanBright = Color(0xFF38BDF8)
val CrystalAquaDeep = Color(0xFF0EA5E9)
val CrystalSkyCyanLightText = Color(0xFF7DD3FC)

fun getPrimaryColor(option: ThemeColorOption): Color = Color(option.primaryHex)
fun getSecondaryColor(option: ThemeColorOption): Color = Color(option.secondaryHex)

val AppGradients = object {
    val AuroraLight = listOf(
        Color(0xFFE0E7FF),
        Color(0xFFF3E8FF),
        Color(0xFFE0F2FE),
        Color(0xFFFDF4FF)
    )
    val AuroraDark = listOf(
        Color(0xFF0F172A),
        Color(0xFF1E1B4B),
        Color(0xFF1E293B),
        Color(0xFF0F172A)
    )
    val OceanWavesLight = listOf(
        Color(0xFFE0F2FE),
        Color(0xFFBAE6FD),
        Color(0xFFE0E7FF)
    )
    val SunsetMeshLight = listOf(
        Color(0xFFFFEDD5),
        Color(0xFFFEE2E2),
        Color(0xFFEDE9FE)
    )
    val EmeraldGlowLight = listOf(
        Color(0xFFD1FAE5),
        Color(0xFFA7F3D0),
        Color(0xFFE0F2FE)
    )
    val VioletAuroraLight = listOf(
        Color(0xFFEDE9FE),
        Color(0xFFDDD6FE),
        Color(0xFFFCE7F3)
    )
}
