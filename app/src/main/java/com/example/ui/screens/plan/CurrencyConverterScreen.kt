package com.example.ui.screens.plan

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class CurrencyPickerTarget {
    FROM,
    TO
}

private val ALL_CURRENCIES_PRIORITY = listOf(
    "USD", "EUR", "GBP", "SAR", "AED", "INR",
    "MYR", "SGD", "QAR", "KWD", "OMR", "BHD",
    "CAD", "AUD", "JPY", "CNY", "CHF", "NZD",
    "HKD", "THB", "KRW", "TRY", "PKR", "LKR",
    "NPR", "BDT"
)

@Composable
fun CurrencyConverterScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.currencyConverterState.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val langCode = if (language == AppLanguage.BANGLA) "bn" else "en"
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primary = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        viewModel.ensureCurrencyRatesFresh()
    }

    var pickerTarget by remember { mutableStateOf<CurrencyPickerTarget?>(null) }
    var showCustomRateSheet by remember { mutableStateOf(false) }
    var swapClickCount by remember { mutableIntStateOf(0) }

    val swapRotation by animateFloatAsState(
        targetValue = swapClickCount * 180f,
        animationSpec = tween(durationMillis = 300),
        label = "currency_swap_rotation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin_transition")
    val refreshSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_spin_angle"
    )

    val fromCurrency = remember(uiState.fromCode) {
        SUPPORTED_CURRENCIES.firstOrNull { it.code == uiState.fromCode } ?: SUPPORTED_CURRENCIES.first()
    }
    val toCurrency = remember(uiState.toCode) {
        SUPPORTED_CURRENCIES.firstOrNull { it.code == uiState.toCode } ?: SUPPORTED_CURRENCIES[1]
    }

    val parsedAmount = remember(uiState.amountInput) {
        CurrencyFormatter.parseLocalizedDouble(uiState.amountInput) ?: 0.0
    }

    val (effectiveRate, isCustomRateActive) = remember(
        fromCurrency.code,
        toCurrency.code,
        uiState.ratesPerUsd,
        uiState.customOverrides
    ) {
        CurrencyRateRepository.getEffectivePairRate(
            fromCode = fromCurrency.code,
            toCode = toCurrency.code,
            ratesPerUsd = uiState.ratesPerUsd,
            customOverrides = uiState.customOverrides
        )
    }

    val convertedAmount = remember(parsedAmount, effectiveRate) {
        parsedAmount * effectiveRate
    }

    val updatedTimeText = remember(uiState.lastUpdatedMillis, language) {
        formatUpdatedTime(uiState.lastUpdatedMillis, language)
    }

    val allOtherCurrencies = remember(fromCurrency.code) {
        val byCode = SUPPORTED_CURRENCIES.associateBy { it.code }
        val prioritized = ALL_CURRENCIES_PRIORITY
            .filter { it != fromCurrency.code }
            .mapNotNull { byCode[it] }
        val remaining = SUPPORTED_CURRENCIES.filter { cur ->
            cur.code != fromCurrency.code && prioritized.none { it.code == cur.code }
        }
        prioritized + remaining
    }

    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF090D16), Color(0xFF111827), Color(0xFF0B132B))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFEAF4F8), Color(0xFFF0F5FA), Color(0xFFF4F2FA))
        )
    }

    val cardColor = if (isDark) CardBackgroundDark else Color.White
    val dividerColor = if (isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9)
    val listState = rememberLazyListState()
    val glassLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 12.dp,
        bottomNavClearance = 32.dp
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    backdropState = glassLayout.backdropState,
                    lazyListState = listState,
                    contentLayer = glassLayout.contentGraphicsLayer
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = glassLayout.contentPadding.calculateTopPadding(),
                bottom = glassLayout.contentPadding.calculateBottomPadding()
            )
        ) {
            // 1. Airy, Uncluttered Conversion Bento Card (12.dp directly below header)
            item(key = "conversion_bento_card") {
                ConversionBentoCard(
                    fromCurrency = fromCurrency,
                    toCurrency = toCurrency,
                    amountText = uiState.amountInput,
                    convertedAmount = convertedAmount,
                    effectiveRate = effectiveRate,
                    isCustomRateActive = isCustomRateActive,
                    swapRotation = swapRotation,
                    language = language,
                    langCode = langCode,
                    isDark = isDark,
                    cardColor = cardColor,
                    dividerColor = dividerColor,
                    onAmountChange = { viewModel.setCurrencyAmount(it) },
                    onSelectFromCurrency = { pickerTarget = CurrencyPickerTarget.FROM },
                    onSelectToCurrency = { pickerTarget = CurrencyPickerTarget.TO },
                    onSwap = {
                        swapClickCount += 1
                        viewModel.swapCurrencies()
                    },
                    onOpenCustomRate = { showCustomRateSheet = true },
                    onCopyConverted = {
                        val copyStr = formatSymbolAmount(toCurrency, convertedAmount, language)
                        copyToClipboard(context, copyStr)
                        viewModel.userNotificationMessage.value = AppStrings.t("copied_to_clipboard", language)
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. External "All currencies" Section Header
            item(key = "all_currencies_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.t("all_currencies", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "${AppStrings.t("updated_label", language)} $updatedTimeText",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 4. Single-Column Grouped "All currencies" Bento Card
            item(key = "all_currencies_list_card") {
                val referenceAmount = if (parsedAmount > 0.0) parsedAmount else 1.0
                AllCurrenciesBentoCard(
                    fromCurrency = fromCurrency,
                    selectedToCode = toCurrency.code,
                    currencies = allOtherCurrencies,
                    referenceAmount = referenceAmount,
                    ratesPerUsd = uiState.ratesPerUsd,
                    customOverrides = uiState.customOverrides,
                    language = language,
                    langCode = langCode,
                    isDark = isDark,
                    cardColor = cardColor,
                    dividerColor = dividerColor,
                    onSelectTargetCurrency = { code -> viewModel.setToCurrency(code) }
                )
            }
        }

        // Fixed Top Header matching Income Tracker & Calendar
        ScrollAwareFixedHeader(
            layoutState = glassLayout,
            modifier = Modifier.align(Alignment.TopCenter),
            testTag = "currency_converter_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularHeaderActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = AppStrings.t("back", language),
                    cardColor = cardColor,
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = onBack,
                    modifier = Modifier.testTag("currency_converter_back_button")
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = AppStrings.t("currency_converter", language),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                CircularHeaderActionButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = AppStrings.t("refresh_rates", language),
                    cardColor = cardColor,
                    tint = if (uiState.isRefreshing) primary else MaterialTheme.colorScheme.onSurface,
                    iconRotation = if (uiState.isRefreshing) refreshSpin else 0f,
                    onClick = { viewModel.refreshCurrencyRates(showToast = true) },
                    modifier = Modifier.testTag("currency_refresh_button")
                )
            }
        }
    }

    // Searchable Currency Picker Sheet (All 26 currencies)
    pickerTarget?.let { target ->
        val selectedCode = if (target == CurrencyPickerTarget.FROM) fromCurrency.code else toCurrency.code
        CurrencySearchBottomSheet(
            title = AppStrings.t("select_currency", language),
            selectedCode = selectedCode,
            referenceFromCode = fromCurrency.code,
            ratesPerUsd = uiState.ratesPerUsd,
            customOverrides = uiState.customOverrides,
            language = language,
            isDark = isDark,
            onSelect = { chosen ->
                if (target == CurrencyPickerTarget.FROM) {
                    viewModel.setFromCurrency(chosen.code)
                } else {
                    viewModel.setToCurrency(chosen.code)
                }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null }
        )
    }

    // Custom Exchange Rate Sheet (opened by tapping the rate line)
    if (showCustomRateSheet) {
        SetCustomRateBottomSheet(
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            ratesPerUsd = uiState.ratesPerUsd,
            customOverrides = uiState.customOverrides,
            language = language,
            isDark = isDark,
            onSaveCustomRate = { baseCode, quoteCode, rate ->
                viewModel.saveCustomExchangeRate(baseCode, quoteCode, rate)
                showCustomRateSheet = false
            },
            onResetToLive = { fromCode, toCode ->
                viewModel.resetCustomExchangeRate(fromCode, toCode)
                showCustomRateSheet = false
            },
            onDismiss = { showCustomRateSheet = false }
        )
    }
}

@Composable
private fun CircularHeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    cardColor: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRotation: Float = 0f
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = cardColor,
        shadowElevation = 0.dp,
        modifier = modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(iconRotation)
            )
        }
    }
}

@Composable
private fun ConversionBentoCard(
    fromCurrency: CurrencyInfo,
    toCurrency: CurrencyInfo,
    amountText: String,
    convertedAmount: Double,
    effectiveRate: Double,
    isCustomRateActive: Boolean,
    swapRotation: Float,
    language: AppLanguage,
    langCode: String,
    isDark: Boolean,
    cardColor: Color,
    dividerColor: Color,
    onAmountChange: (String) -> Unit,
    onSelectFromCurrency: () -> Unit,
    onSelectToCurrency: () -> Unit,
    onSwap: () -> Unit,
    onOpenCustomRate: () -> Unit,
    onCopyConverted: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // FROM currency selector with country flag
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSelectFromCurrency)
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .testTag("from_currency_selector"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyFlagBadge(
                    currency = fromCurrency,
                    isDark = isDark,
                    sizeDp = 34,
                    isCircle = true
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${fromCurrency.code} • ${fromCurrency.localizedName(langCode)}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = AppStrings.t("select_currency", language),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large, crisp numeric input directly on the white Bento surface
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ltrDisplaySymbol(fromCurrency),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        val cleaned = CurrencyFormatter.convertToEnglishDigits(raw)
                        val filtered = buildString {
                            var seenDot = false
                            for (ch in cleaned) {
                                if (ch in '0'..'9') append(ch)
                                else if (ch == '.' && !seenDot) {
                                    seenDot = true
                                    append(ch)
                                }
                            }
                        }.take(12)
                        onAmountChange(filtered)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(primary),
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (amountText.isEmpty()) {
                                Text(
                                    text = CurrencyFormatter.localizeDigits("0", language),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("converter_amount_input")
                )
            }

            // Clean divider + tappable rate line (left) & circular Swap pill (right)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = dividerColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenCustomRate)
                            .heightIn(min = 40.dp)
                            .padding(end = 12.dp)
                            .testTag("currency_rate_pill"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatReadableUnitRate(
                                from = fromCurrency,
                                to = toCurrency,
                                directRate = effectiveRate,
                                language = language
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCustomRateActive) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF78350F).copy(alpha = 0.65f) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = AppStrings.t("custom_badge", language),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onSwap,
                        shape = CircleShape,
                        color = primary,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("currency_swap_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.SwapVert,
                                contentDescription = AppStrings.t("swap_currencies", language),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(swapRotation)
                            )
                        }
                    }
                }
            }

            // TO currency selector with country flag
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSelectToCurrency)
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .testTag("to_currency_selector"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyFlagBadge(
                    currency = toCurrency,
                    isDark = isDark,
                    sizeDp = 34,
                    isCircle = true
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${toCurrency.code} • ${toCurrency.localizedName(langCode)}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = AppStrings.t("select_currency", language),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large converted amount — tap to copy directly
            Text(
                text = formatSymbolAmount(toCurrency, convertedAmount, language),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = primary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onCopyConverted)
                    .padding(vertical = 2.dp)
                    .testTag("converter_result_text")
            )
        }
    }
}

@Composable
private fun AllCurrenciesBentoCard(
    fromCurrency: CurrencyInfo,
    selectedToCode: String,
    currencies: List<CurrencyInfo>,
    referenceAmount: Double,
    ratesPerUsd: Map<String, Double>,
    customOverrides: Map<String, Double>,
    language: AppLanguage,
    langCode: String,
    isDark: Boolean,
    cardColor: Color,
    dividerColor: Color,
    onSelectTargetCurrency: (String) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            currencies.forEachIndexed { index, targetCur ->
                if (index > 0) {
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = dividerColor,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                val isSelectedTarget = targetCur.code == selectedToCode
                val (pairRate, isPairCustom) = CurrencyRateRepository.getEffectivePairRate(
                    fromCode = fromCurrency.code,
                    toCode = targetCur.code,
                    ratesPerUsd = ratesPerUsd,
                    customOverrides = customOverrides
                )
                val liveConverted = referenceAmount * pairRate
                val rowTint = if (isSelectedTarget) {
                    primary.copy(alpha = if (isDark) 0.14f else 0.06f).compositeOver(cardColor)
                } else {
                    Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowTint)
                        .clickable { onSelectTargetCurrency(targetCur.code) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("all_currency_row_${targetCur.code}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurrencyFlagBadge(
                        currency = targetCur,
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
                                text = targetCur.localizedName(langCode),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelectedTarget) primary else MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isPairCustom) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF78350F).copy(alpha = 0.65f) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = AppStrings.t("custom_badge", language),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = formatDirectUnitRate(
                                from = fromCurrency,
                                to = targetCur,
                                directRate = pairRate,
                                language = language
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = formatSymbolAmount(targetCur, liveConverted, language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelectedTarget) primary else MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun formatUpdatedTime(millis: Long, language: AppLanguage): String {
    if (millis <= 0L) return "—"
    val formatter = SimpleDateFormat("h:mm a", Locale.US)
    val raw = formatter.format(Date(millis))
    return if (language == AppLanguage.BANGLA) {
        CurrencyFormatter.localizeDigits(raw, language)
    } else {
        raw
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Converted Amount", text))
}
