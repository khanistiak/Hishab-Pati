package com.example.ui.screens.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.ui.screens.home.resolveAccountCardColor
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import kotlin.math.abs

/**
 * Shared visual data contract for [HishabPatiAccountCard] across:
 * Home Card Stack, Home Card Carousel, Account Details Hero Card, and Add/Edit Live Card Preview.
 */
data class AccountCardVisualModel(
    val name: String,
    val institution: String,
    val type: AccountType,
    val balance: Double,
    val baseColor: Color,
    val last4: String = "",
    val network: String = "",
    val subtype: String = "",
    val location: String = "",
    val tier: String = "",
    val billingDate: String = ""
)

fun AccountEntity.toCardVisualModel(): AccountCardVisualModel {
    val parts = notes.split("|")
    val network = parts.firstOrNull { it.startsWith("Network:") }
        ?.removePrefix("Network:")?.trim().orEmpty()
    val subtype = parts.firstOrNull { it.startsWith("Subtype:") }
        ?.removePrefix("Subtype:")?.trim().orEmpty()
    val location = parts.firstOrNull { it.startsWith("Location:") }
        ?.removePrefix("Location:")?.trim().orEmpty()
    val tier = parts.firstOrNull { it.startsWith("Tier:") }
        ?.removePrefix("Tier:")?.trim().orEmpty()

    val displayBalance = if (type == AccountType.CREDIT_CARD) {
        if (balance < 0) abs(balance) else balance
    } else {
        balance
    }

    return AccountCardVisualModel(
        name = name,
        institution = institution,
        type = type,
        balance = displayBalance,
        baseColor = resolveAccountCardColor(this),
        last4 = accountNumberLast4,
        network = network,
        subtype = subtype,
        location = location,
        tier = tier,
        billingDate = billingDate
    )
}

/**
 * Unified bespoke wallet/account card component for Hishab Pati.
 * Preserves user-chosen colors ([AccountCardVisualModel.baseColor]) while applying
 * category-specific surface artwork, hardware elements (EMV chip + NFC ONLY on Bank/Credit),
 * and brand/network emblems.
 */
@Composable
fun HishabPatiAccountCard(
    model: AccountCardVisualModel,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    isBalanceVisible: Boolean = true,
    language: AppLanguage = AppLanguage.ENGLISH,
    cornerRadius: Dp = 22.dp,
    shadowElevation: Dp = 6.dp,
    peekingHeaderOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val isBangla = language == AppLanguage.BANGLA
    val isCredit = model.type == AccountType.CREDIT_CARD
    val isBank = model.type == AccountType.BANK
    val isMfs = model.type == AccountType.MFS
    val isCash = model.type == AccountType.CASH
    val hasHardwareChip = isCredit || isBank

    val gradientColors = remember(model.baseColor, model.type) {
        buildAccountCardGradient(model.baseColor, model.type)
    }

    val cardShape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.28f),
            Color.White.copy(alpha = 0.08f)
        )
    )

    val displayName = model.name.ifBlank {
        when (model.type) {
            AccountType.CREDIT_CARD -> AppStrings.t("credit_card_label", language)
            AccountType.BANK -> AppStrings.t("debit_card_label", language)
            AccountType.CASH -> AppStrings.t("cash", language)
            AccountType.MFS -> if (model.institution.isNotBlank()) "${model.institution} Wallet" else AppStrings.t("mobile_wallet", language)
            AccountType.OTHER -> AppStrings.t("other", language)
        }
    }

    val topSubtitle = when (model.type) {
        AccountType.CREDIT_CARD -> model.institution.ifBlank { AppStrings.t("credit_card_label", language) }.uppercase()
        AccountType.BANK -> model.institution.ifBlank { AppStrings.t("debit_card_label", language) }.uppercase()
        AccountType.MFS -> model.institution.ifBlank { AppStrings.t("mobile_wallet", language) }
        AccountType.CASH -> model.location.ifBlank { AppStrings.t("physical_cash", language) }
        AccountType.OTHER -> model.institution.ifBlank { AppStrings.t("other", language) }
    }

    val formattedBalance = if (isBalanceVisible) {
        CurrencyFormatter.format(amount = model.balance, symbol = currencySymbol, language = language)
    } else {
        "••••••••"
    }

    val formattedLast4 = remember(model.last4, isBangla) {
        if (model.last4.isBlank()) ""
        else if (isBangla) CurrencyFormatter.convertToBanglaDigits(model.last4)
        else model.last4
    }

    val shadowColor = model.baseColor.copy(alpha = 0.28f)
    val containerModifier = modifier
        .shadow(
            elevation = shadowElevation,
            shape = cardShape,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .clip(cardShape)
        .background(Brush.linearGradient(gradientColors))
        .border(BorderStroke(1.dp, borderBrush), cardShape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Box(modifier = containerModifier) {
        // 1. Category-specific bespoke Canvas surface pattern
        AccountCardPatternCanvas(
            type = model.type,
            isMini = false,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Subtle top-left specular glass sheen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent,
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(520f, 360f)
                    )
                )
        )

        // 3. Card Typography & Hardware Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = if (peekingHeaderOnly) 7.dp else 16.dp,
                    bottom = 16.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (peekingHeaderOnly) 8.dp else 10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.20f),
                        modifier = Modifier.size(if (peekingHeaderOnly) 24.dp else 34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (model.type) {
                                    AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                    AccountType.BANK -> Icons.Default.AccountBalance
                                    AccountType.CASH -> Icons.Default.Payments
                                    AccountType.MFS -> Icons.Default.PhoneAndroid
                                    AccountType.OTHER -> Icons.Default.AccountBalance
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (peekingHeaderOnly) 13.5.dp else 18.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (peekingHeaderOnly) 13.sp else 14.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!peekingHeaderOnly) {
                            Text(
                                text = topSubtitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontSize = 10.sp,
                                    letterSpacing = if (hasHardwareChip) 0.7.sp else 0.1.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (peekingHeaderOnly) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = formattedBalance,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.96f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1
                        )
                        if (hasHardwareChip) {
                            Icon(
                                imageVector = Icons.Default.Contactless,
                                contentDescription = "Contactless",
                                tint = Color.White.copy(alpha = 0.84f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                } else {
                    when {
                        hasHardwareChip -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contactless,
                                    contentDescription = "Contactless",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(19.dp)
                                )
                                EmvChip(
                                    width = 34.dp,
                                    height = 24.dp,
                                    style = if (isCredit) EmvChipStyle.GOLD else EmvChipStyle.SILVER_GOLD,
                                    cornerRadius = 5.dp
                                )
                            }
                        }
                        isMfs -> {
                            // Clean mobile-wallet badge (NO chip, NO NFC)
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.18f),
                                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.28f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = "Mobile Wallet",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    if (model.tier.isNotBlank()) {
                                        Text(
                                            text = model.tier,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        isCash -> {
                            // Translucent frosted banknote icon pill (NO chip, NO NFC)
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.20f),
                                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.28f)),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = "Cash",
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MIDDLE ROW: Micro-label + Large Tabular Balance
            Column(modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    text = if (isCredit) {
                        AppStrings.t("outstanding_caps", language)
                    } else {
                        AppStrings.t("balance_caps", language)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.74f),
                        fontSize = 10.sp,
                        letterSpacing = 1.25.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedBalance,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // BOTTOM ROW: Category-tailored details (left) + Emblems / Foils / Watermark (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bottomLeftText = when (model.type) {
                    AccountType.CREDIT_CARD -> {
                        val net = model.network.ifBlank { AppStrings.t("credit_card_label", language) }
                        if (formattedLast4.isNotBlank()) "$net •••• $formattedLast4"
                        else if (model.billingDate.isNotBlank()) "$net • Cycle ${model.billingDate}"
                        else net
                    }
                    AccountType.BANK -> {
                        val sub = model.subtype.ifBlank { AppStrings.t("debit_card_label", language) }
                        if (formattedLast4.isNotBlank()) "$sub •••• $formattedLast4" else sub
                    }
                    AccountType.MFS -> {
                        val walletLabel = AppStrings.t("mobile_wallet", language)
                        if (formattedLast4.isNotBlank()) "$walletLabel •••• $formattedLast4" else walletLabel
                    }
                    AccountType.CASH -> {
                        model.location.ifBlank { AppStrings.t("cash_in_hand", language) }
                    }
                    AccountType.OTHER -> {
                        if (formattedLast4.isNotBlank()) "•••• $formattedLast4" else model.type.displayName
                    }
                }

                Text(
                    text = bottomLeftText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (model.type) {
                        AccountType.CREDIT_CARD -> {
                            IridescentSecurityFoil()
                            if (model.network.isNotBlank()) {
                                CardNetworkLogo(
                                    network = model.network,
                                    modifier = Modifier.height(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = "Credit Card",
                                    tint = Color.White.copy(alpha = 0.86f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        AccountType.BANK -> {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Bank Crest",
                                tint = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier.size(15.dp)
                            )
                            if (model.network.isNotBlank()) {
                                CardNetworkLogo(
                                    network = model.network,
                                    modifier = Modifier.height(18.dp)
                                )
                            }
                        }
                        AccountType.MFS -> {
                            val providerName = model.institution.ifBlank { model.name }
                            MfsProviderLogo(provider = providerName)
                        }
                        AccountType.CASH -> {
                            CashCurrencyWatermarkSeal(symbol = currencySymbol)
                        }
                        AccountType.OTHER -> {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Convenience overload accepting [AccountEntity] directly.
 */
@Composable
fun HishabPatiAccountCard(
    account: AccountEntity,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    isBalanceVisible: Boolean = true,
    language: AppLanguage = AppLanguage.ENGLISH,
    cornerRadius: Dp = 22.dp,
    shadowElevation: Dp = 6.dp,
    peekingHeaderOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val model = remember(account) { account.toCardVisualModel() }
    HishabPatiAccountCard(
        model = model,
        currencySymbol = currencySymbol,
        modifier = modifier,
        isBalanceVisible = isBalanceVisible,
        language = language,
        cornerRadius = cornerRadius,
        shadowElevation = shadowElevation,
        peekingHeaderOnly = peekingHeaderOnly,
        onClick = onClick
    )
}

/**
 * Compact 54.dp × 36.dp mini bank/wallet thumbnail for the Accounts List screen.
 * Mirrors the exact category textures and user-selected tint:
 * - CREDIT_CARD & BANK show mini EMV chip (Gold vs Silver-Gold)
 * - MFS & CASH strictly omit the EMV chip and show geometric origami / organic linen accents
 */
@Composable
fun HishabPatiMiniAccountCard(
    account: AccountEntity,
    isArchived: Boolean = false,
    currencySymbol: String = "৳",
    modifier: Modifier = Modifier
) {
    val baseColor = resolveAccountCardColor(account)
    val effectiveAlpha = if (isArchived) 0.55f else 1f
    val gradientColors = remember(baseColor, account.type, effectiveAlpha) {
        buildAccountCardGradient(baseColor, account.type, effectiveAlpha)
    }

    val cardShape = RoundedCornerShape(8.dp)
    val showChip = account.type == AccountType.CREDIT_CARD || account.type == AccountType.BANK

    Box(
        modifier = modifier
            .size(width = 54.dp, height = 36.dp)
            .clip(cardShape)
            .background(Brush.linearGradient(colors = gradientColors))
            .border(
                width = 0.6.dp,
                color = Color.White.copy(alpha = if (account.type == AccountType.CREDIT_CARD) 0.24f else 0.28f),
                shape = cardShape
            )
    ) {
        // Category-matched miniature Canvas pattern
        AccountCardPatternCanvas(
            type = account.type,
            isMini = true,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle diagonal glass specular sheen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(120f, 90f)
                    )
                )
        )

        when {
            showChip -> {
                EmvChip(
                    width = 11.dp,
                    height = 8.dp,
                    style = if (account.type == AccountType.CREDIT_CARD) {
                        EmvChipStyle.GOLD
                    } else {
                        EmvChipStyle.SILVER_GOLD
                    },
                    cornerRadius = 2.dp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 7.dp)
                )
                if (account.type == AccountType.CREDIT_CARD) {
                    // Tiny iridescent foil dot on bottom-right for mini credit cards
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 5.dp, bottom = 5.dp)
                            .size(width = 6.dp, height = 4.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(alpha = 0.45f))
                    )
                }
            }
            account.type == AccountType.MFS -> {
                // Clean digital wallet dot/mobile indicator — NO EMV chip
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 6.dp, bottom = 5.dp)
                        .size(11.dp)
                )
            }
            account.type == AccountType.CASH -> {
                // Subtle currency watermark seal — NO EMV chip
                Text(
                    text = currencySymbol.ifBlank { "৳" },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 4.dp)
                )
            }
        }
    }
}
