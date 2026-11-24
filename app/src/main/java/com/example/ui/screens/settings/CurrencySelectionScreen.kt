package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.CurrencyOption
import com.example.data.model.SupportedCurrencies
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout

@Composable
fun CurrencySelectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBangla = language == AppLanguage.BANGLA
    val currentCode = settings?.currencyCode ?: "BDT"
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var searchQuery by remember { mutableStateOf("") }
    var selectedOption by remember(currentCode) {
        mutableStateOf(
            SupportedCurrencies.allCurrencies.find { it.code.equals(currentCode, ignoreCase = true) }
                ?: SupportedCurrencies.allCurrencies.first()
        )
    }

    val filteredCurrencies = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            SupportedCurrencies.allCurrencies
        } else {
            SupportedCurrencies.allCurrencies.filter {
                it.code.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.nameBn.contains(searchQuery, ignoreCase = true) ||
                it.symbol.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.CURRENCY_SELECTION
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 118.dp,
        extraTopGap = 4.dp
    )
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        // Currency List scrolling edge-to-edge underneath fixed frosted header & search bar
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    scrollLayout.backdropState,
                    listState,
                    scrollLayout.contentGraphicsLayer
                )
                .testTag("currency_lazy_column"),
            contentPadding = PaddingValues(
                top = scrollLayout.contentPadding.calculateTopPadding(),
                bottom = navBarBottomPadding + 84.dp
            )
        ) {
            if (filteredCurrencies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "কোন মুদ্রা পাওয়া যায়নি" else "No matching currencies found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredCurrencies, key = { it.code }) { item ->
                    val isSelected = selectedOption.code.equals(item.code, ignoreCase = true)

                    CurrencyRowItem(
                        item = item,
                        isSelected = isSelected,
                        isDark = isDark,
                        isBangla = isBangla,
                        onClick = {
                            selectedOption = item
                        }
                    )
                }
            }
        }

        // Shared Fixed Scroll-Aware Frosted Header containing Title AND Search Bar
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "currency_fixed_header"
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header row: Back button + Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF262B3D) else Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("currency_back_button"),
                        onClick = onBack
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (isBangla) "ডিফল্ট মুদ্রা" else "Default currency",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = if (isBangla)
                                "সমস্ত লেনদেন ও অ্যাকাউন্টের প্রদর্শন মুদ্রা নির্বাচন করুন"
                            else
                                "Select display currency for transactions & accounts",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Search Input Field integrated directly inside the fixed frosted panel
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isBangla) "মুদ্রার নাম বা কোড খুঁজুন..." else "Search by name or code...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        focusedContainerColor = if (isDark) Color(0xFF1E2230) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0xFF1E2230) else Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 10.dp)
                        .testTag("currency_search_input")
                )
            }
        }

        // Bottom Action Bar with "Set as Default" button (always accessible)
        Surface(
            color = (if (isDark) Color(0xFF161922) else Color(0xFFF8FAFC)).copy(alpha = 0.94f),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.setCurrency(selectedOption.code, selectedOption.symbol)
                        onBack()
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF4F46E5), Color(0xFF6366F1))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .testTag("confirm_currency_btn")
                ) {
                    Text(
                        text = if (isBangla)
                            "ডিফল্ট হিসেবে নিশ্চিত করুন (${selectedOption.code} ${selectedOption.symbol})"
                        else
                            "Set as Default (${selectedOption.code} ${selectedOption.symbol})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyRowItem(
    item: CurrencyOption,
    isSelected: Boolean,
    isDark: Boolean,
    isBangla: Boolean,
    onClick: () -> Unit
) {
    val highlightColor = Color(0xFF4F46E5)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) {
                    if (isDark) Color(0xFF263354).copy(alpha = 0.45f) else Color(0xFFEFF6FF)
                } else {
                    Color.Transparent
                }
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flag or Symbol Badge (Compact & consistent size)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) {
                        highlightColor.copy(alpha = 0.16f)
                    } else {
                        if (isDark) Color(0xFF262B3D) else Color(0xFFF1F5F9)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.flag.isNotBlank()) {
                Text(
                    text = item.flag,
                    fontSize = 20.sp
                )
            } else {
                Text(
                    text = item.symbol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = highlightColor
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Code, Symbol & Name
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.code,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFF262B3D) else Color(0xFFF1F5F9),
                    modifier = Modifier.padding(1.dp)
                ) {
                    Text(
                        text = item.symbol,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = highlightColor
                        ),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = if (isBangla) item.nameBn else item.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Selection Radio/Check Indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(highlightColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        CircleShape
                    )
            )
        }
    }
}
