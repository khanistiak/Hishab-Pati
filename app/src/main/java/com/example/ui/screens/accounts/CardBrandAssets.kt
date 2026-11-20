package com.example.ui.screens.accounts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

data class MfsProviderInfo(
    val name: String,
    val brandColorHex: String
)

/**
 * Active selectable providers for MFS account creation / editing.
 * Cellfin and Tap are removed per user requirements.
 */
val SelectableMfsProviders = listOf(
    MfsProviderInfo("bKash", "#E2136E"),
    MfsProviderInfo("Nagad", "#F26522"),
    MfsProviderInfo("Rocket", "#8C3494"),
    MfsProviderInfo("Upay", "#005BAC")
)

/**
 * Full list of 13 MFS providers displayed in the All Providers bottom sheet picker.
 */
val AllConfigurableMfsProviders = listOf(
    MfsProviderInfo("bKash", "#E2136E"),
    MfsProviderInfo("Rocket", "#8C3494"),
    MfsProviderInfo("Nagad", "#F26522"),
    MfsProviderInfo("MYCash", "#00833E"),
    MfsProviderInfo("Islami Bank mCash", "#00843D"),
    MfsProviderInfo("TAP (Trust Axiata Pay)", "#E31B23"),
    MfsProviderInfo("FirstPay", "#0054A6"),
    MfsProviderInfo("OK Banking", "#ED1C24"),
    MfsProviderInfo("Upay", "#005BAC"),
    MfsProviderInfo("Rupali Cash", "#008542"),
    MfsProviderInfo("TeleCash", "#0066B3"),
    MfsProviderInfo("Islamic Wallet", "#0B6623"),
    MfsProviderInfo("Meghna Bank Tap n Pay", "#E30613")
)

/**
 * All supported providers, preserving legacy Tap and CellFin for backward compatibility.
 */
val SupportedMfsProviders = (AllConfigurableMfsProviders + listOf(
    MfsProviderInfo("Tap", "#0084C7"),
    MfsProviderInfo("CellFin", "#008848")
)).distinctBy { it.name.lowercase() }

/**
 * Legacy Others MFS provider option for backward compatibility.
 */
val OthersMfsProvider = MfsProviderInfo("Others", "#4F46E5")

/**
 * Visible primary provider chips (bKash, Nagad, Rocket, Upay).
 */
val VisibleMfsProviderChoices: List<MfsProviderInfo> = SelectableMfsProviders

val SupportedCardNetworks = listOf("Visa", "Mastercard", "American Express", "UnionPay")

/**
 * Shared central source of truth for all supported banks in Bangladesh (61 banks).
 * Stored as a single flat list without category groupings per requirements.
 */
val PopularBanks = listOf(
    "Agrani Bank",
    "Bangladesh Development Bank",
    "BASIC Bank",
    "Janata Bank",
    "Rupali Bank",
    "Sonali Bank",
    "Sammilito Islami Bank",
    "Bangladesh Krishi Bank",
    "Rajshahi Krishi Unnayan Bank",
    "Probashi Kallyan Bank",
    "AB Bank PLC",
    "Bangladesh Commerce Bank Limited",
    "Bank Asia PLC",
    "Bengal Commercial Bank PLC",
    "BRAC Bank PLC",
    "Citizens Bank PLC",
    "City Bank PLC",
    "Community Bank Bangladesh PLC",
    "Dhaka Bank PLC",
    "Dutch-Bangla Bank PLC",
    "Eastern Bank PLC",
    "IFIC Bank PLC",
    "Jamuna Bank PLC",
    "Meghna Bank PLC",
    "Mercantile Bank PLC",
    "Midland Bank PLC",
    "Modhumoti Bank PLC",
    "Mutual Trust Bank PLC",
    "National Bank PLC",
    "National Credit & Commerce Bank PLC",
    "NRB Bank PLC",
    "NRBC Bank PLC",
    "ONE Bank PLC",
    "Padma Bank PLC",
    "Prime Bank PLC",
    "Pubali Bank PLC",
    "SBAC Bank PLC",
    "Shimanto Bank PLC",
    "Southeast Bank PLC",
    "The Premier Bank PLC",
    "Trust Bank PLC",
    "United Commercial Bank PLC",
    "Uttara Bank PLC",
    "Al-Arafah Islami Bank PLC",
    "ICB Islamic Bank PLC",
    "Islami Bank Bangladesh PLC",
    "Shahjalal Islami Bank PLC",
    "Standard Bank PLC",
    "Citibank N.A",
    "Commercial Bank of Ceylon PLC",
    "Habib Bank Limited",
    "HSBC",
    "National Bank of Pakistan",
    "Standard Chartered Bank",
    "State Bank of India",
    "Woori Bank",
    "Ansar VDP Unnayan Bank",
    "Grameen Bank",
    "Jubilee Bank",
    "Karmashangosthan Bank",
    "Palli Sanchay Bank"
)

/**
 * Pure helper to build and filter the searchable list of banks and institutions,
 * combining [PopularBanks] with any custom bank names already saved by the user.
 */
fun filterBanksAndInstitutions(
    query: String,
    customInstitutions: List<String> = emptyList()
): List<String> {
    val combined = LinkedHashSet<String>()
    PopularBanks.forEach { combined.add(it) }
    customInstitutions
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { custom ->
            if (combined.none { it.equals(custom, ignoreCase = true) }) {
                combined.add(custom)
            }
        }
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return combined.toList()
    return combined.filter { it.contains(trimmed, ignoreCase = true) }
}

/**
 * Pure helper to build and filter all MFS providers configured in the app
 * (including existing 13 providers plus any custom user MFS provider names).
 */
fun filterMfsProviders(
    query: String,
    customProviders: List<MfsProviderInfo> = emptyList()
): List<MfsProviderInfo> {
    val combined = mutableListOf<MfsProviderInfo>()
    AllConfigurableMfsProviders.forEach { prov ->
        if (combined.none { it.name.equals(prov.name, ignoreCase = true) }) {
            combined.add(prov)
        }
    }
    customProviders.forEach { custom ->
        val cleanName = custom.name.trim()
        if (cleanName.isNotEmpty() && combined.none { it.name.equals(cleanName, ignoreCase = true) }) {
            combined.add(custom.copy(name = cleanName))
        }
    }
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return combined
    return combined.filter { it.name.contains(trimmed, ignoreCase = true) }
}

enum class EmvChipStyle {
    GOLD,
    SILVER_GOLD
}

/**
 * Realistic Metallic EMV Smart Chip for Debit and Credit cards only.
 * Includes brushed gold (Credit) or brushed silver-gold (Bank/Debit) metallic gradient,
 * micro-beveled outline, and authentic etched circuit contact cutouts.
 */
@Composable
fun EmvChip(
    modifier: Modifier = Modifier,
    width: Dp = 34.dp,
    height: Dp = 24.dp,
    style: EmvChipStyle = EmvChipStyle.GOLD,
    cornerRadius: Dp = 5.dp
) {
    Canvas(
        modifier = modifier
            .size(width = width, height = height)
    ) {
        val corner = cornerRadius.toPx()

        // 1. Brushed metallic gradient base
        val chipBrush = when (style) {
            EmvChipStyle.GOLD -> Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFEEA0),
                    Color(0xFFE5B745),
                    Color(0xFFBA8618),
                    Color(0xFFF7DE88),
                    Color(0xFFCE9622)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            EmvChipStyle.SILVER_GOLD -> Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF8FAFC),
                    Color(0xFFE3DEC8),
                    Color(0xFFB5B09A),
                    Color(0xFFEDF0F5),
                    Color(0xFF9CA3AF)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        }

        drawRoundRect(
            brush = chipBrush,
            size = size,
            cornerRadius = CornerRadius(corner, corner)
        )

        // 2. Beveled outer border
        val borderMidColor = if (style == EmvChipStyle.GOLD) Color(0xFF8D6208) else Color(0xFF64748B)
        val borderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.82f),
                borderMidColor,
                Color.White.copy(alpha = 0.42f)
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )

        drawRoundRect(
            brush = borderBrush,
            size = size,
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 0.9.dp.toPx())
        )

        // 3. Etched micro-circuit grooves (ISO 7816 contact layout)
        val etchColor = if (style == EmvChipStyle.GOLD) {
            Color(0xFF6B4B04).copy(alpha = 0.85f)
        } else {
            Color(0xFF334155).copy(alpha = 0.72f)
        }
        val etchStroke = (size.height * 0.038f).coerceAtLeast(0.6.dp.toPx())

        val padLeft = size.width * 0.28f
        val padRight = size.width * 0.72f
        val padTop = size.height * 0.25f
        val padBottom = size.height * 0.75f

        // Center contact pad
        drawRoundRect(
            color = etchColor,
            topLeft = Offset(padLeft, padTop),
            size = Size(padRight - padLeft, padBottom - padTop),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = etchStroke)
        )

        // Horizontal contact divider
        drawLine(
            color = etchColor,
            start = Offset(0f, size.height * 0.5f),
            end = Offset(padLeft, size.height * 0.5f),
            strokeWidth = etchStroke
        )
        drawLine(
            color = etchColor,
            start = Offset(padRight, size.height * 0.5f),
            end = Offset(size.width, size.height * 0.5f),
            strokeWidth = etchStroke
        )

        // Vertical contact dividers
        drawLine(
            color = etchColor,
            start = Offset(size.width * 0.5f, 0f),
            end = Offset(size.width * 0.5f, padTop),
            strokeWidth = etchStroke
        )
        drawLine(
            color = etchColor,
            start = Offset(size.width * 0.5f, padBottom),
            end = Offset(size.width * 0.5f, size.height),
            strokeWidth = etchStroke
        )
    }
}

/**
 * Payment Network Logos (Visa, Mastercard, AMEX, UnionPay, Generic).
 * Rendered flat and clean using official assets.
 */
@Composable
fun CardNetworkLogo(
    network: String?,
    modifier: Modifier = Modifier
) {
    val net = network?.trim()?.lowercase() ?: ""

    when {
        net.contains("visa") -> {
            Image(
                painter = painterResource(id = R.drawable.ic_net_visa),
                contentDescription = "Visa",
                modifier = modifier.height(18.dp),
                contentScale = ContentScale.Fit
            )
        }
        net.contains("master") -> {
            Image(
                painter = painterResource(id = R.drawable.ic_net_mastercard),
                contentDescription = "Mastercard",
                modifier = modifier.height(20.dp),
                contentScale = ContentScale.Fit
            )
        }
        net.contains("american") || net.contains("amex") -> {
            Image(
                painter = painterResource(id = R.drawable.ic_net_amex),
                contentDescription = "American Express",
                modifier = modifier.height(22.dp),
                contentScale = ContentScale.Fit
            )
        }
        net.contains("union") -> {
            Image(
                painter = painterResource(id = R.drawable.ic_net_unionpay),
                contentDescription = "UnionPay",
                modifier = modifier.height(20.dp),
                contentScale = ContentScale.Fit
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = "Card",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = modifier.size(20.dp)
            )
        }
    }
}

/**
 * Clean, flat MFS provider logo for bKash, Nagad, Rocket, Upay, and legacy Tap / CellFin.
 * Free of drop-shadows, elevation, or decorative pill containers.
 */
@Composable
fun MfsProviderLogo(
    provider: String?,
    modifier: Modifier = Modifier
) {
    val prov = provider?.trim()?.lowercase() ?: ""

    when {
        prov.contains("bkash") -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = modifier
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_mfs_bkash),
                    contentDescription = "bKash Logo",
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "bKash",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 15.sp,
                        letterSpacing = (-0.5).sp
                    )
                )
            }
        }
        prov.contains("nagad") -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = modifier
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_mfs_nagad),
                    contentDescription = "Nagad Logo",
                    modifier = Modifier.size(30.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "নগদ",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                )
            }
        }
        prov.contains("rocket") -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = modifier
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_mfs_rocket),
                    contentDescription = "Rocket Logo",
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "Rocket",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                )
            }
        }
        prov.contains("upay") -> {
            Image(
                painter = painterResource(id = R.drawable.ic_mfs_upay),
                contentDescription = "Upay Logo",
                modifier = modifier
                    .height(32.dp)
                    .widthIn(max = 50.dp),
                contentScale = ContentScale.Fit
            )
        }
        prov.contains("tap") -> {
            // Legacy Tap provider
            Text(
                text = "tap",
                modifier = modifier,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 16.sp
                )
            )
        }
        prov.contains("cellfin") -> {
            // Legacy CellFin provider
            Text(
                text = "CellFin",
                modifier = modifier,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            )
        }
        else -> {
            val label = provider?.trim().orEmpty()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = modifier
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "MFS Wallet",
                    tint = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(18.dp)
                )
                if (label.isNotBlank()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}
