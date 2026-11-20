package com.example.ui.screens.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import java.util.Locale

internal data class CurrencyBadgeStyle(
    val bgLight: Color,
    val textLight: Color,
    val bgDark: Color,
    val textDark: Color
)

private val CURRENCY_BADGE_PALETTE = listOf(
    CurrencyBadgeStyle(Color(0xFFE0F2FE), Color(0xFF0369A1), Color(0xFF0C4A6E), Color(0xFF7DD3FC)), // Sky
    CurrencyBadgeStyle(Color(0xFFDCFCE7), Color(0xFF15803D), Color(0xFF14532D), Color(0xFF86EFAC)), // Emerald
    CurrencyBadgeStyle(Color(0xFFEDE9FE), Color(0xFF6D28D9), Color(0xFF4C1D95), Color(0xFFC4B5FD)), // Violet
    CurrencyBadgeStyle(Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFF78350F), Color(0xFFFCD34D)), // Amber
    CurrencyBadgeStyle(Color(0xFFFCE7F3), Color(0xFFBE185D), Color(0xFF831843), Color(0xFFF9A8D4)), // Rose
    CurrencyBadgeStyle(Color(0xFFCCFBF1), Color(0xFF0F766E), Color(0xFF134E4A), Color(0xFF5EEAD4)), // Teal
    CurrencyBadgeStyle(Color(0xFFE0E7FF), Color(0xFF4338CA), Color(0xFF312E81), Color(0xFFA5B4FC)), // Indigo
    CurrencyBadgeStyle(Color(0xFFFFEDD5), Color(0xFFC2410C), Color(0xFF7C2D12), Color(0xFFFDBA74))  // Coral/Orange
)

internal fun currencyBadgeStyleFor(code: String): CurrencyBadgeStyle {
    val index = SUPPORTED_CURRENCIES.indexOfFirst { it.code == code }.coerceAtLeast(0)
    return CURRENCY_BADGE_PALETTE[index % CURRENCY_BADGE_PALETTE.size]
}

/**
 * Clean LTR currency symbol/prefix so Arabic RTL symbols (﷼, د.إ) never reverse or clip.
 */
internal fun ltrDisplaySymbol(currency: CurrencyInfo): String {
    return when (currency.code) {
        "SAR" -> "SAR"
        "AED" -> "AED"
        "QAR" -> "QAR"
        "KWD" -> "KD"
        "OMR" -> "OMR"
        "BHD" -> "BD"
        "PKR" -> "PKR"
        "NPR" -> "NPR"
        "LKR" -> "LKR"
        else -> currency.symbol
    }
}

@Composable
internal fun CurrencyFlagBadge(
    currency: CurrencyInfo,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Int = 40,
    isCircle: Boolean = false
) {
    val style = remember(currency.code) { currencyBadgeStyleFor(currency.code) }
    val shape = if (isCircle) CircleShape else RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(shape)
            .background(if (isDark) style.bgDark.copy(alpha = 0.45f) else style.bgLight.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = currency.flag,
            fontSize = 22.sp,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrencySearchBottomSheet(
    title: String,
    selectedCode: String,
    referenceFromCode: String,
    ratesPerUsd: Map<String, Double>,
    customOverrides: Map<String, Double>,
    language: AppLanguage,
    isDark: Boolean,
    onSelect: (CurrencyInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val primary = MaterialTheme.colorScheme.primary
    val langCode = if (language == AppLanguage.BANGLA) "bn" else "en"

    val filteredCurrencies = remember(searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.US)
        if (q.isEmpty()) {
            SUPPORTED_CURRENCIES
        } else {
            SUPPORTED_CURRENCIES.filter { c ->
                c.code.lowercase(Locale.US).contains(q) ||
                    c.nameEn.lowercase(Locale.US).contains(q) ||
                    c.nameBn.lowercase(Locale.US).contains(q) ||
                    c.countryKeywords.lowercase(Locale.US).contains(q) ||
                    ltrDisplaySymbol(c).lowercase(Locale.US).contains(q)
            }
        }
    }

    val referenceCurrency = remember(referenceFromCode) {
        SUPPORTED_CURRENCIES.firstOrNull { it.code == referenceFromCode } ?: SUPPORTED_CURRENCIES.first()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF161D2F) else Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = CurrencyFormatter.formatNumber(SUPPORTED_CURRENCIES.size, language),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                shape = CircleShape,
                placeholder = {
                    Text(
                        text = AppStrings.t("search_currency_hint", language),
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = AppStrings.t("cancel", language),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currency_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredCurrencies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.t("no_matching_currencies", language),
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(filteredCurrencies, key = { _, item -> item.code }) { index, currency ->
                        if (index > 0) {
                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = if (isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9)
                            )
                        }
                        val isSelected = currency.code == selectedCode
                        val (pairRate, isCustom) = CurrencyRateRepository.getEffectivePairRate(
                            fromCode = referenceCurrency.code,
                            toCode = currency.code,
                            ratesPerUsd = ratesPerUsd,
                            customOverrides = customOverrides
                        )

                        val rowBg = if (isSelected) {
                            primary.copy(alpha = if (isDark) 0.14f else 0.07f)
                                .compositeOver(if (isDark) Color(0xFF161D2F) else Color.White)
                        } else {
                            Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(rowBg)
                                .clickable { onSelect(currency) }
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                                .testTag("currency_picker_item_${currency.code}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyFlagBadge(
                                currency = currency,
                                isDark = isDark,
                                sizeDp = 40,
                                isCircle = false
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${currency.code} • ${currency.localizedName(langCode)}",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isCustom) {
                                        Surface(
                                            shape = CircleShape,
                                            color = primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = AppStrings.t("custom_badge", language),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                if (currency.code != referenceCurrency.code) {
                                    Text(
                                        text = formatDirectUnitRate(
                                            from = referenceCurrency,
                                            to = currency,
                                            directRate = pairRate,
                                            language = language
                                        ),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = ltrDisplaySymbol(currency),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SetCustomRateBottomSheet(
    fromCurrency: CurrencyInfo,
    toCurrency: CurrencyInfo,
    ratesPerUsd: Map<String, Double>,
    customOverrides: Map<String, Double>,
    language: AppLanguage,
    isDark: Boolean,
    onSaveCustomRate: (baseCode: String, quoteCode: String, rate: Double) -> Unit,
    onResetToLive: (fromCode: String, toCode: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primary = MaterialTheme.colorScheme.primary

    val initialDirectRate = remember(fromCurrency.code, toCurrency.code, ratesPerUsd, customOverrides) {
        CurrencyRateRepository.getEffectivePairRate(
            fromCode = fromCurrency.code,
            toCode = toCurrency.code,
            ratesPerUsd = ratesPerUsd,
            customOverrides = customOverrides
        ).first
    }
    var isFlipped by remember(fromCurrency.code, toCurrency.code) {
        mutableStateOf(initialDirectRate > 0.0 && initialDirectRate < 0.1)
    }

    val baseCurrency = if (isFlipped) toCurrency else fromCurrency
    val quoteCurrency = if (isFlipped) fromCurrency else toCurrency

    val (effectiveRate, isCustomActive) = remember(
        baseCurrency.code,
        quoteCurrency.code,
        ratesPerUsd,
        customOverrides
    ) {
        CurrencyRateRepository.getEffectivePairRate(
            fromCode = baseCurrency.code,
            toCode = quoteCurrency.code,
            ratesPerUsd = ratesPerUsd,
            customOverrides = customOverrides
        )
    }

    val liveRate = remember(baseCurrency.code, quoteCurrency.code, ratesPerUsd) {
        CurrencyRateRepository.getLivePairRate(baseCurrency.code, quoteCurrency.code, ratesPerUsd)
    }

    var rateText by remember(baseCurrency.code, quoteCurrency.code, effectiveRate) {
        mutableStateOf(formatEditableRate(effectiveRate))
    }

    val parsedRate = CurrencyFormatter.parseLocalizedDouble(rateText)
    val isValidRate = parsedRate != null && parsedRate > 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF161D2F) else Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.t("set_custom_rate", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AppStrings.t("live_rate_hint", language),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = primary.copy(alpha = if (isDark) 0.2f else 0.1f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { isFlipped = !isFlipped }
                        .testTag("custom_rate_flip_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = AppStrings.t("flip_direction", language),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Equation Card: 1 [Base] = [Editable Field] [Quote]
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${CurrencyFormatter.formatNumber(1, language)} ${baseCurrency.code}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "=",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { input ->
                                val clean = CurrencyFormatter.convertToEnglishDigits(input)
                                val filtered = buildString {
                                    var seenDot = false
                                    for (ch in clean) {
                                        if (ch in '0'..'9') append(ch)
                                        else if (ch == '.' && !seenDot) {
                                            seenDot = true
                                            append(ch)
                                        }
                                    }
                                }.take(12)
                                rateText = filtered
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            suffix = {
                                Text(
                                    text = quoteCurrency.code,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primary
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primary,
                                unfocusedBorderColor = if (isDark) Color(0x28FFFFFF) else Color(0xFFE2E8F0),
                                focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                                unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_rate_value_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val liveDisplay = "${CurrencyFormatter.formatNumber(1, language)} ${baseCurrency.code} = ${formatSymbolAmount(quoteCurrency, liveRate, language, isRate = true)}"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${AppStrings.t("live_reference_rate", language)}: $liveDisplay",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isCustomActive) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = AppStrings.t("custom_badge", language),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val value = parsedRate ?: return@Button
                    onSaveCustomRate(baseCurrency.code, quoteCurrency.code, value)
                },
                enabled = isValidRate,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_custom_rate_button")
            ) {
                Text(
                    text = AppStrings.t("save_custom_rate", language),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { onResetToLive(fromCurrency.code, toCurrency.code) },
                shape = CircleShape,
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("reset_live_rate_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = AppStrings.t("reset_to_live_rate", language),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

internal fun formatConvertedAmount(amount: Double, language: AppLanguage): String {
    if (amount <= 0.0) {
        return CurrencyFormatter.localizeDigits("0.00", language)
    }
    val formatted = when {
        amount >= 100_000.0 -> String.format(Locale.US, "%,.0f", amount)
        amount >= 1.0 -> String.format(Locale.US, "%,.2f", amount)
        amount >= 0.01 -> String.format(Locale.US, "%.3f", amount)
        else -> String.format(Locale.US, "%.4f", amount)
    }
    return CurrencyFormatter.localizeDigits(formatted, language)
}

internal fun formatRateNumber(rate: Double, language: AppLanguage): String {
    val formatted = when {
        rate >= 1000.0 -> String.format(Locale.US, "%,.1f", rate)
        rate >= 1.0 -> String.format(Locale.US, "%.2f", rate)
        rate >= 0.1 -> String.format(Locale.US, "%.3f", rate)
        rate >= 0.01 -> String.format(Locale.US, "%.4f", rate)
        else -> String.format(Locale.US, "%.5f", rate)
    }
    return CurrencyFormatter.localizeDigits(formatted, language)
}

internal fun formatSymbolAmount(
    currency: CurrencyInfo,
    amount: Double,
    language: AppLanguage,
    isRate: Boolean = false
): String {
    val prefix = ltrDisplaySymbol(currency)
    val numberStr = if (isRate) formatRateNumber(amount, language) else formatConvertedAmount(amount, language)
    return "$prefix $numberStr"
}

internal fun formatDirectUnitRate(
    from: CurrencyInfo,
    to: CurrencyInfo,
    directRate: Double,
    language: AppLanguage
): String {
    if (directRate <= 0.0) return "–"
    val one = CurrencyFormatter.formatNumber(1, language)
    return "$one ${from.code} = ${formatRateNumber(directRate, language)} ${to.code}"
}

internal fun formatReadableUnitRate(
    from: CurrencyInfo,
    to: CurrencyInfo,
    directRate: Double,
    language: AppLanguage
): String {
    if (directRate <= 0.0) return "–"
    val one = CurrencyFormatter.formatNumber(1, language)
    return if (directRate < 0.1) {
        val inverse = 1.0 / directRate
        val sym = ltrDisplaySymbol(from)
        val sep = if (sym.length == 1) "" else " "
        "$one ${to.code} = $sym$sep${formatRateNumber(inverse, language)}"
    } else {
        val sym = ltrDisplaySymbol(to)
        val sep = if (sym.length == 1) "" else " "
        "$one ${from.code} = $sym$sep${formatRateNumber(directRate, language)}"
    }
}

private fun formatEditableRate(rate: Double): String {
    if (rate <= 0.0) return ""
    return when {
        rate >= 10.0 -> String.format(Locale.US, "%.2f", rate)
        rate >= 1.0 -> String.format(Locale.US, "%.3f", rate)
        else -> String.format(Locale.US, "%.4f", rate)
    }.trimEnd('0').trimEnd('.')
}
