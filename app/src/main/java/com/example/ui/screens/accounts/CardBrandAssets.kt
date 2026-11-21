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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
 * Realistic Brushed-Metal EMV Smart Chip for Debit and Credit cards only.
 * Features a smooth diagonal brushed-gold (Credit) or brushed-silver (Bank/Debit) base,
 * delicate 0.6.dp bevel highlight, and ultra-fine etched 6-pad ISO 7816 contact grooves
 * paired with a subtle micro light-catch edge.
 */
@Composable
fun EmvChip(
    modifier: Modifier = Modifier,
    width: Dp = 34.dp,
    height: Dp = 25.dp,
    style: EmvChipStyle = EmvChipStyle.GOLD,
    cornerRadius: Dp = 5.dp
) {
    Canvas(
        modifier = modifier
            .size(width = width, height = height)
    ) {
        val w = size.width
        val h = size.height
        val corner = cornerRadius.toPx()
        val isMiniChip = width < 20.dp

        // 1. Smooth diagonal brushed metallic gradient base
        val chipBrush = when (style) {
            EmvChipStyle.GOLD -> Brush.linearGradient(
                0.00f to Color(0xFFF3D98B),
                0.48f to Color(0xFFD8B254),
                1.00f to Color(0xFFB99136),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
            EmvChipStyle.SILVER_GOLD -> Brush.linearGradient(
                0.00f to Color(0xFFECEFF4),
                0.48f to Color(0xFFCBD2DC),
                1.00f to Color(0xFFA3ADB9),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        }

        drawRoundRect(
            brush = chipBrush,
            size = size,
            cornerRadius = CornerRadius(corner, corner)
        )

        // Subtle metallic specular sheen across upper-left chip surface
        drawRoundRect(
            brush = Brush.linearGradient(
                0.00f to Color.White.copy(alpha = 0.24f),
                0.45f to Color.Transparent,
                1.00f to Color.Black.copy(alpha = 0.08f),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            size = size,
            cornerRadius = CornerRadius(corner, corner)
        )

        // 2. Ultra-fine etched 6-pad EMV contact layout (grooves + adjacent micro light catch)
        val grooveColor = if (style == EmvChipStyle.GOLD) {
            Color(0xFF6B4F1D).copy(alpha = 0.42f)
        } else {
            Color(0xFF334155).copy(alpha = 0.36f)
        }
        val lightCatchColor = Color.White.copy(alpha = 0.25f)
        val groovePx = (if (isMiniChip) 0.45.dp else 0.75.dp).toPx()
        val catchPx = (if (isMiniChip) 0.30.dp else 0.50.dp).toPx()
        val catchShift = groovePx * 0.85f

        val leftX = w * 0.31f
        val rightX = w * 0.69f
        val topSplitY = h * 0.34f
        val bottomSplitY = h * 0.66f
        val midY = h * 0.50f
        val edgeInset = groovePx * 0.6f

        fun drawEtchedLine(start: Offset, end: Offset, isHorizontal: Boolean) {
            // Subtle adjacent micro light catch
            if (!isMiniChip) {
                val dx = if (isHorizontal) 0f else catchShift
                val dy = if (isHorizontal) catchShift else 0f
                drawLine(
                    color = lightCatchColor,
                    start = Offset(start.x + dx, start.y + dy),
                    end = Offset(end.x + dx, end.y + dy),
                    strokeWidth = catchPx
                )
            }
            // Fine etched metallic groove
            drawLine(
                color = grooveColor,
                start = start,
                end = end,
                strokeWidth = groovePx
            )
        }

        // Left & right vertical column boundaries separating left/center/right contact pads
        drawEtchedLine(Offset(leftX, edgeInset), Offset(leftX, h - edgeInset), isHorizontal = false)
        drawEtchedLine(Offset(rightX, edgeInset), Offset(rightX, h - edgeInset), isHorizontal = false)

        // Left top / mid / bottom contact pad splits
        drawEtchedLine(Offset(edgeInset, topSplitY), Offset(leftX, topSplitY), isHorizontal = true)
        drawEtchedLine(Offset(edgeInset, bottomSplitY), Offset(leftX, bottomSplitY), isHorizontal = true)

        // Right top / mid / bottom contact pad splits
        drawEtchedLine(Offset(rightX, topSplitY), Offset(w - edgeInset, topSplitY), isHorizontal = true)
        drawEtchedLine(Offset(rightX, bottomSplitY), Offset(w - edgeInset, bottomSplitY), isHorizontal = true)

        // Center top & bottom bridge pad horizontal split
        drawEtchedLine(Offset(leftX, midY), Offset(rightX, midY), isHorizontal = true)

        // 3. Delicate 0.6.dp outer bevel highlight
        val outerBevelBrush = Brush.linearGradient(
            0.00f to Color.White.copy(alpha = 0.45f),
            0.50f to Color.White.copy(alpha = 0.16f),
            1.00f to grooveColor.copy(alpha = 0.40f),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        drawRoundRect(
            brush = outerBevelBrush,
            size = size,
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 0.6.dp.toPx())
        )
    }
}

/**
 * Authentic pure-white VISA brand vector emblem (aspect ratio 1000:324.68 ~= 3.08:1),
 * featuring the signature curved top-left wing/beak on the V, slanted geometric I,
 * flowing italic S, and sharp A with rounded top-left shoulder and triangular counter.
 */
private val VisaWhiteEmblem: ImageVector by lazy {
    val whiteFill = SolidColor(Color.White)
    ImageVector.Builder(
        name = "VisaWhiteEmblem",
        defaultWidth = 40.dp,
        defaultHeight = 13.dp,
        viewportWidth = 1000f,
        viewportHeight = 324.68f
    ).apply {
        // V with signature curved top-left wing/beak flowing into the left stem
        path(fill = whiteFill) {
            moveTo(293.60f, 5.29f)
            lineTo(378.40f, 5.29f)
            lineTo(249.10f, 319.39f)
            lineTo(161.30f, 319.39f)
            lineTo(96.50f, 67.50f)
            curveTo(88.80f, 37.50f, 61.20f, 22.80f, 0.0f, 10.60f)
            lineTo(0.0f, 5.29f)
            lineTo(134.20f, 5.29f)
            curveTo(157.80f, 5.29f, 173.40f, 19.60f, 178.90f, 45.20f)
            lineTo(209.80f, 218.50f)
            close()
        }

        // Slanted geometric I
        path(fill = whiteFill) {
            moveTo(412.19f, 5.29f)
            lineTo(495.59f, 5.29f)
            lineTo(428.87f, 319.39f)
            lineTo(345.47f, 319.39f)
            close()
        }

        // Flowing geometric italic S
        path(fill = whiteFill) {
            moveTo(730.20f, 13.80f)
            lineTo(715.80f, 80.50f)
            curveTo(695.00f, 70.80f, 667.50f, 64.50f, 643.00f, 64.50f)
            curveTo(614.00f, 64.50f, 598.50f, 76.20f, 598.50f, 91.50f)
            curveTo(598.50f, 108.50f, 616.50f, 118.80f, 651.50f, 136.20f)
            curveTo(697.00f, 158.80f, 718.50f, 182.50f, 714.50f, 221.50f)
            curveTo(708.50f, 286.50f, 651.50f, 324.68f, 574.50f, 324.68f)
            curveTo(537.50f, 324.68f, 503.00f, 316.50f, 482.50f, 306.80f)
            lineTo(497.50f, 237.80f)
            curveTo(520.50f, 249.50f, 549.50f, 258.50f, 579.50f, 258.50f)
            curveTo(610.50f, 258.50f, 629.00f, 245.50f, 629.00f, 227.50f)
            curveTo(629.00f, 211.00f, 612.50f, 200.50f, 577.50f, 183.00f)
            curveTo(534.50f, 161.50f, 514.00f, 136.50f, 517.80f, 97.50f)
            curveTo(523.50f, 36.50f, 576.50f, 0.0f, 652.00f, 0.0f)
            curveTo(685.50f, 0.0f, 712.50f, 6.20f, 730.20f, 13.80f)
            close()
        }

        // Sharp geometric A with rounded top-left shoulder, horizontal crossbar & triangular counter
        path(
            fill = whiteFill,
            pathFillType = PathFillType.EvenOdd
        ) {
            // Outer silhouette of A
            moveTo(708.50f, 319.39f)
            lineTo(830.50f, 24.50f)
            curveTo(836.50f, 11.20f, 846.80f, 5.29f, 862.50f, 5.29f)
            lineTo(936.00f, 5.29f)
            lineTo(1000.00f, 319.39f)
            lineTo(924.50f, 319.39f)
            lineTo(914.50f, 263.00f)
            lineTo(811.50f, 263.00f)
            lineTo(790.50f, 319.39f)
            close()
            // Inner triangular counter cutout
            moveTo(877.50f, 92.00f)
            lineTo(832.50f, 204.00f)
            lineTo(903.50f, 204.00f)
            close()
        }
    }.build()
}

/**
 * Payment Network Logos (Visa, Mastercard, AMEX, UnionPay, Generic).
 * Unified to a compact, consistent visual footprint across all networks, with VISA rendered
 * using the authentic pure-white vector emblem (40.dp x 13.dp) beside the security foil.
 */
@Composable
fun CardNetworkLogo(
    network: String?,
    modifier: Modifier = Modifier
) {
    val net = network?.trim()?.lowercase() ?: ""

    when {
        net.contains("visa") -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Image(
                    imageVector = VisaWhiteEmblem,
                    contentDescription = "Visa",
                    modifier = Modifier.size(width = 40.dp, height = 13.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        net.contains("master") -> {
            Image(
                painter = painterResource(id = R.drawable.ic_net_mastercard),
                contentDescription = "Mastercard",
                modifier = modifier
                    .height(16.dp)
                    .widthIn(max = 28.dp),
                contentScale = ContentScale.Fit
            )
        }
        net.contains("american") || net.contains("amex") -> {
            Image(
                painter = painterResource(id = R.drawable.ic_net_amex),
                contentDescription = "American Express",
                modifier = modifier
                    .height(16.dp)
                    .widthIn(max = 30.dp),
                contentScale = ContentScale.Fit
            )
        }
        net.contains("union") -> {
            Image(
                painter = painterResource(id = R.drawable.ic_net_unionpay),
                contentDescription = "UnionPay",
                modifier = modifier
                    .height(16.dp)
                    .widthIn(max = 28.dp),
                contentScale = ContentScale.Fit
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = "Card",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = modifier.size(16.dp)
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
