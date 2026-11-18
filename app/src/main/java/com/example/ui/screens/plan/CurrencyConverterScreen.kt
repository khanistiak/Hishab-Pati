package com.example.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.TranslucentCard
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CurrencyRateInfo(
    val code: String,
    val name: String,
    val flag: String,
    val symbol: String,
    val rateAgainstBdt: Double // 1 BDT in foreign currency
)

@Composable
fun CurrencyConverterScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH

    val currencies = remember {
        listOf(
            CurrencyRateInfo("BDT", "Bangladeshi Taka", "🇧🇩", "৳", 1.0),
            CurrencyRateInfo("USD", "US Dollar", "🇺🇸", "$", 0.0081),
            CurrencyRateInfo("EUR", "Euro", "🇪🇺", "€", 0.0076),
            CurrencyRateInfo("GBP", "British Pound", "🇬🇧", "£", 0.0064),
            CurrencyRateInfo("JPY", "Japanese Yen", "🇯🇵", "¥", 1.25),
            CurrencyRateInfo("INR", "Indian Rupee", "🇮🇳", "₹", 0.69),
            CurrencyRateInfo("CAD", "Canadian Dollar", "🇨🇦", "C$", 0.0112),
            CurrencyRateInfo("AUD", "Australian Dollar", "🇦🇺", "A$", 0.0125),
            CurrencyRateInfo("CHF", "Swiss Franc", "🇨🇭", "CHF", 0.0072),
            CurrencyRateInfo("CNY", "Chinese Yuan", "🇨🇳", "¥", 0.058),
            CurrencyRateInfo("SAR", "Saudi Riyal", "🇸🇦", "SR", 0.0305),
            CurrencyRateInfo("AED", "UAE Dirham", "🇦🇪", "AED", 0.0298),
            CurrencyRateInfo("SGD", "Singapore Dollar", "🇸🇬", "S$", 0.0108),
            CurrencyRateInfo("MYR", "Malaysian Ringgit", "🇲🇾", "RM", 0.035),
            CurrencyRateInfo("THB", "Thai Baht", "🇹🇭", "฿", 0.278),
            CurrencyRateInfo("KRW", "South Korean Won", "🇰🇷", "₩", 11.2)
        )
    }

    var fromCurrency by remember { mutableStateOf(currencies[0]) } // BDT
    var toCurrency by remember { mutableStateOf(currencies[1]) }   // USD
    var amountInput by remember { mutableStateOf("1000") }

    var showFromMenu by remember { mutableStateOf(false) }
    var showToMenu by remember { mutableStateOf(false) }

    val inputAmt = amountInput.toDoubleOrNull() ?: 0.0
    // Conversion: amount in BDT * (toRate / fromRate)
    val convertedAmt = if (fromCurrency.rateAgainstBdt > 0) {
        (inputAmt / fromCurrency.rateAgainstBdt) * toCurrency.rateAgainstBdt
    } else 0.0

    val unitRate = if (fromCurrency.rateAgainstBdt > 0) {
        (1.0 / fromCurrency.rateAgainstBdt) * toCurrency.rateAgainstBdt
    } else 0.0

    val updatedTimeStr = remember {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date())
    }

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.CURRENCY_CONVERTER
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 66.dp,
        extraTopGap = 8.dp,
        bottomNavClearance = 30.dp
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    scrollLayout.backdropState,
                    listState,
                    scrollLayout.contentGraphicsLayer
                )
                .testTag("currency_converter_lazy_column"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = scrollLayout.contentPadding.calculateTopPadding(),
                bottom = scrollLayout.contentPadding.calculateBottomPadding()
            )
        ) {
            // Main Conversion Card
            item {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = AppStrings.t("from", language),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // From Currency Selector Pill
                        Box {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showFromMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = fromCurrency.flag, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${fromCurrency.code} · ${fromCurrency.name}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showFromMenu,
                                onDismissRequest = { showFromMenu = false }
                            ) {
                                currencies.forEach { cur ->
                                    DropdownMenuItem(
                                        text = { Text("${cur.flag} ${cur.code} - ${cur.name}") },
                                        onClick = {
                                            fromCurrency = cur
                                            showFromMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Amount Input Box
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { amountInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("converter_amount_input"),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                placeholder = {
                                    Text(
                                        text = "0.00",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Swap Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4F46E5),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val temp = fromCurrency
                                    fromCurrency = toCurrency
                                    toCurrency = temp
                                },
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap Currencies",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = AppStrings.t("to", language),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // To Currency Selector Pill
                        Box {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showToMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = toCurrency.flag, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${toCurrency.code} · ${toCurrency.name}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showToMenu,
                                onDismissRequest = { showToMenu = false }
                            ) {
                                currencies.forEach { cur ->
                                    DropdownMenuItem(
                                        text = { Text("${cur.flag} ${cur.code} - ${cur.name}") },
                                        onClick = {
                                            toCurrency = cur
                                            showToMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Converted Result Display
                        val formattedConverted = String.format(Locale.US, "%.2f", convertedAmt)
                        Text(
                            text = "${toCurrency.symbol} $formattedConverted",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4F46E5)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "1 ${fromCurrency.code} = ${String.format(Locale.US, "%.4f", unitRate)} ${toCurrency.code}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Text(
                            text = "Updated $updatedTimeStr",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            // Quick amount section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = AppStrings.t("quick_amount", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1", "10", "100", "1000", "10000").forEach { pillAmount ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { amountInput = pillAmount },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (language == AppLanguage.BANGLA) CurrencyFormatter.convertToBanglaDigits(pillAmount) else pillAmount,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Popular currencies section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = AppStrings.t("popular_currencies", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2-column list of popular currencies
            val popularList = currencies.filter { it.code != "BDT" }
            val chunkedPopular = popularList.chunked(2)

            items(chunkedPopular.size) { rowIndex ->
                val pair = chunkedPopular[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { cur ->
                        val isSelected = toCurrency.code == cur.code
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) Color(0xFF6366F1) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { toCurrency = cur }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = cur.flag, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = cur.code,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "1 ${fromCurrency.code} → ${cur.code}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        }
                    }

                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "currency_converter_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.size(42.dp),
                    onClick = onBack
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = AppStrings.t("currency_converter", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }
    }
}
