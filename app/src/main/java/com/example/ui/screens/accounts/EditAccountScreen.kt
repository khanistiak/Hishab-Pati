package com.example.ui.screens.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountType
import com.example.ui.MainViewModel
import com.example.ui.components.HishabPatiDatePickerDialog
import com.example.ui.components.HishabPatiDatePickerStyle
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.TranslucentCard
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.ExpenseRed
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CardColorPreset(
    val name: String,
    val hex: String
)

val CardColorPresets = listOf(
    CardColorPreset("Burnt Orange", "#EA580C"),
    CardColorPreset("Sapphire Blue", "#2563EB"),
    CardColorPreset("Royal Violet", "#7C3AED"),
    CardColorPreset("Midnight Slate", "#1E293B"),
    CardColorPreset("Crimson Red", "#DC2626"),
    CardColorPreset("Emerald Green", "#059669"),
    CardColorPreset("Sunset Amber", "#D97706"),
    CardColorPreset("Rose Pink", "#E11D48"),
    CardColorPreset("Deep Purple", "#4F46E5"),
    CardColorPreset("Teal Cyan", "#0D9488")
)

val ComprehensiveColorPalette = listOf(
    CardColorPreset("Burnt Orange", "#EA580C"),
    CardColorPreset("Sapphire Blue", "#2563EB"),
    CardColorPreset("Royal Violet", "#7C3AED"),
    CardColorPreset("Midnight Slate", "#1E293B"),
    CardColorPreset("Crimson Red", "#DC2626"),
    CardColorPreset("Emerald Green", "#059669"),
    CardColorPreset("Sunset Amber", "#D97706"),
    CardColorPreset("Rose Pink", "#E11D48"),
    CardColorPreset("Deep Purple", "#4F46E5"),
    CardColorPreset("Teal Cyan", "#0D9488"),
    CardColorPreset("Indigo Navy", "#1E3A8A"),
    CardColorPreset("Electric Blue", "#0284C7"),
    CardColorPreset("Ocean Teal", "#0E7490"),
    CardColorPreset("Forest Pine", "#15803D"),
    CardColorPreset("Jade Green", "#047857"),
    CardColorPreset("Fresh Mint", "#10B981"),
    CardColorPreset("Goldenrod", "#CA8A04"),
    CardColorPreset("Warm Amber", "#B45309"),
    CardColorPreset("Fiery Coral", "#F97316"),
    CardColorPreset("Terracotta", "#C2410C"),
    CardColorPreset("Ruby Red", "#B91C1C"),
    CardColorPreset("Fuchsia Pink", "#C026D3"),
    CardColorPreset("Hot Magenta", "#DB2777"),
    CardColorPreset("Deep Plum", "#581C87"),
    CardColorPreset("Charcoal Black", "#334155"),
    CardColorPreset("Dark Obsidian", "#0F172A"),
    CardColorPreset("bKash Pink", "#E2136E"),
    CardColorPreset("Nagad Orange", "#F26522")
)

val CardNetworks = SupportedCardNetworks

val BankSubtypes = listOf("Salary Account", "Savings Account", "Current Account", "DPS Account", "Fixed Deposit (FDR)")

/**
 * Pure helper resolving the live wallet preview title and subline when an MFS provider is selected.
 */
fun resolveMfsWalletPreviewText(
    walletName: String,
    providerName: String
): Pair<String, String> {
    val cleanName = walletName.trim()
    val cleanProvider = providerName.trim()
    val title = if (cleanName.isNotEmpty()) {
        cleanName
    } else if (cleanProvider.isNotEmpty()) {
        "$cleanProvider Account"
    } else {
        "MFS Wallet"
    }
    val subline = if (cleanProvider.isNotEmpty()) cleanProvider else "Mobile Wallet"
    return title to subline
}

@Composable
fun EditAccountScreen(
    viewModel: MainViewModel,
    account: AccountEntity?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToAdjustBalance: (AccountEntity) -> Unit = {}
) {
    BackHandler { onBack() }

    val isNewAccount = account == null
    val settings by viewModel.settings.collectAsState()
    val allAccounts by viewModel.accounts.collectAsState()
    val currencySymbol = settings?.currencySymbol ?: "৳"

    val listState = viewModel.rememberScreenLazyListState(
        subKey = account?.id ?: "new",
        screenOverride = com.example.ui.SubScreen.EDIT_ACCOUNT
    )
    val glassLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 6.dp,
        bottomNavClearance = 96.dp
    )

    // Common state - start completely clean for fresh-install / new account
    var selectedType by remember { mutableStateOf(account?.type ?: AccountType.BANK) }
    var name by remember {
        mutableStateOf(account?.name ?: "")
    }
    var institution by remember { mutableStateOf(account?.institution ?: "") }
    var selectedColorHex by remember {
        mutableStateOf(
            account?.colorHex ?: when (selectedType) {
                AccountType.CREDIT_CARD -> "#1E293B"
                AccountType.CASH -> "#059669"
                AccountType.MFS -> "#E2136E"
                else -> "#2563EB"
            }
        )
    }
    var includeInTotal by remember { mutableStateOf(account?.includeInTotal ?: true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showColorPaletteDialog by remember { mutableStateOf(false) }
    var showBankPicker by remember { mutableStateOf(false) }
    var showMfsProviderPicker by remember { mutableStateOf(false) }

    val customBankInstitutions = remember(allAccounts) {
        allAccounts
            .filter { it.type == AccountType.BANK || it.type == AccountType.CREDIT_CARD }
            .mapNotNull { it.institution.trim().takeIf { inst -> inst.isNotEmpty() } }
            .distinct()
    }

    val customMfsProviders = remember(allAccounts) {
        allAccounts
            .filter { it.type == AccountType.MFS }
            .mapNotNull { acc ->
                val inst = acc.institution.trim()
                if (inst.isNotEmpty()) MfsProviderInfo(inst, acc.colorHex) else null
            }
    }

    // Category-specific states (clean fields, no personal or sample defaults)
    // 1. Credit Card specific states
    var creditLimitText by remember {
        val lim = account?.creditLimit ?: 0.0
        mutableStateOf(if (lim > 0.0) {
            if (lim % 1.0 == 0.0) lim.toLong().toString() else lim.toString()
        } else "")
    }
    var billingDateText by remember {
        mutableStateOf(account?.billingDate ?: "")
    }
    var paymentDueDateText by remember {
        mutableStateOf(account?.paymentDueDate ?: "")
    }
    var showBillingDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    val parseDateToMillis: (String) -> Long = { str ->
        val trimmed = str.trim()
        if (trimmed.isEmpty()) {
            System.currentTimeMillis()
        } else if (trimmed.all { it.isDigit() }) {
            val day = trimmed.toIntOrNull() ?: 1
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
            }.timeInMillis
        } else {
            HishabPatiDatePickerStyle.parseManualDateInput(trimmed) ?: System.currentTimeMillis()
        }
    }

    val formatDateToString: (Long) -> String = { millis ->
        val sdf = SimpleDateFormat("d MMM", Locale.US)
        DateFormatter.localizeText(sdf.format(Date(millis)), settings?.language ?: com.example.data.model.AppLanguage.ENGLISH)
    }

    var cardNetwork by remember {
        val stored = account?.notes?.split("|")?.firstOrNull { it.startsWith("Network:") }?.removePrefix("Network:")?.trim()
        mutableStateOf(stored ?: "")
    }
    var outstandingText by remember {
        val bal = account?.balance
        mutableStateOf(
            if (bal != null) {
                val outVal = if (bal < 0) kotlin.math.abs(bal) else bal
                if (outVal % 1.0 == 0.0) outVal.toLong().toString() else outVal.toString()
            } else ""
        )
    }

    // 2. Debit Card specific states
    var bankSubtype by remember {
        val stored = account?.notes?.split("|")?.firstOrNull { it.startsWith("Subtype:") }?.removePrefix("Subtype:")?.trim()
        mutableStateOf(stored ?: "")
    }
    var bankBalanceText by remember {
        val bal = account?.balance
        mutableStateOf(
            if (bal != null) {
                if (bal % 1.0 == 0.0) bal.toLong().toString() else bal.toString()
            } else ""
        )
    }
    var bankAccountLast4 by remember { mutableStateOf(account?.accountNumberLast4 ?: "") }
    var debitCardLast4 by remember {
        val stored = account?.notes?.split("|")?.firstOrNull { it.startsWith("DebitCard:") }?.removePrefix("DebitCard:")?.trim()
        mutableStateOf(stored ?: "")
    }

    // 3. Cash specific states
    var cashLocation by remember {
        val stored = account?.notes?.split("|")?.firstOrNull { it.startsWith("Location:") }?.removePrefix("Location:")?.trim()
        mutableStateOf(stored ?: "")
    }
    var cashBalanceText by remember {
        val bal = account?.balance
        mutableStateOf(
            if (bal != null) {
                if (bal % 1.0 == 0.0) bal.toLong().toString() else bal.toString()
            } else ""
        )
    }

    // 4. MFS specific states
    var mfsTier by remember {
        val stored = account?.notes?.split("|")?.firstOrNull { it.startsWith("Tier:") }?.removePrefix("Tier:")?.trim()
        mutableStateOf(stored ?: "Personal")
    }
    var mfsNumberLast4 by remember { mutableStateOf(account?.accountNumberLast4 ?: "") }
    var mfsBalanceText by remember {
        val bal = account?.balance
        mutableStateOf(
            if (bal != null) {
                if (bal % 1.0 == 0.0) bal.toLong().toString() else bal.toString()
            } else ""
        )
    }

    // Color preset resolution
    val currentColorName = ComprehensiveColorPalette.firstOrNull { it.hex.equals(selectedColorHex, ignoreCase = true) }?.name ?: "Custom Color"
    val previewColor = remember(selectedColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (_: Exception) {
            Color(0xFF2563EB)
        }
    }

    // Computed display values for live preview
    val previewAmount = when (selectedType) {
        AccountType.CREDIT_CARD -> outstandingText.toDoubleOrNull() ?: 0.0
        AccountType.BANK -> bankBalanceText.toDoubleOrNull() ?: 0.0
        AccountType.CASH -> cashBalanceText.toDoubleOrNull() ?: 0.0
        AccountType.MFS -> mfsBalanceText.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    val previewSubline = when (selectedType) {
        AccountType.CREDIT_CARD -> if (institution.isNotBlank()) institution else "Credit Card"
        AccountType.BANK -> if (institution.isNotBlank()) institution else "Debit Card"
        AccountType.CASH -> if (cashLocation.isNotBlank()) cashLocation else "Cash on Hand"
        AccountType.MFS -> if (institution.isNotBlank()) institution else "Mobile Wallet"
        else -> "Hishab Pati"
    }

    val previewLast4 = when (selectedType) {
        AccountType.CREDIT_CARD -> bankAccountLast4
        AccountType.BANK -> if (bankAccountLast4.isNotBlank()) bankAccountLast4 else debitCardLast4
        AccountType.CASH -> ""
        AccountType.MFS -> mfsNumberLast4
        else -> ""
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            val trimmedInstitution = institution.trim()
                            val notesBuilder = mutableListOf<String>()
                            val finalBalance: Double
                            val finalLast4: String
                            val finalLimit: Double
                            val finalBillingDate: String
                            val finalDueDate: String
                            val finalMinPayment: Double
                            val finalIcon: String

                            when (selectedType) {
                                AccountType.CREDIT_CARD -> {
                                    val out = outstandingText.toDoubleOrNull() ?: 0.0
                                    finalBalance = -kotlin.math.abs(out)
                                    finalLast4 = bankAccountLast4.take(4)
                                    finalLimit = creditLimitText.toDoubleOrNull() ?: 0.0
                                    finalBillingDate = billingDateText
                                    finalDueDate = paymentDueDateText
                                    finalMinPayment = account?.minPayment ?: 0.0
                                    finalIcon = "credit_card"
                                    if (cardNetwork.isNotBlank()) notesBuilder.add("Network:$cardNetwork")
                                    account?.notes?.split("|")?.firstOrNull { it.startsWith("APR:") }?.let { notesBuilder.add(it) }
                                }
                                AccountType.BANK -> {
                                    finalBalance = bankBalanceText.toDoubleOrNull() ?: 0.0
                                    finalLast4 = if (bankAccountLast4.isNotBlank()) bankAccountLast4.take(4) else debitCardLast4.take(4)
                                    finalLimit = 0.0
                                    finalBillingDate = ""
                                    finalDueDate = ""
                                    finalMinPayment = 0.0
                                    finalIcon = "account_balance"
                                    if (bankSubtype.isNotBlank()) notesBuilder.add("Subtype:$bankSubtype")
                                    if (cardNetwork.isNotBlank()) notesBuilder.add("Network:$cardNetwork")
                                    if (debitCardLast4.isNotBlank()) notesBuilder.add("DebitCard:$debitCardLast4")
                                }
                                AccountType.CASH -> {
                                    finalBalance = cashBalanceText.toDoubleOrNull() ?: 0.0
                                    finalLast4 = ""
                                    finalLimit = 0.0
                                    finalBillingDate = ""
                                    finalDueDate = ""
                                    finalMinPayment = 0.0
                                    finalIcon = "payments"
                                    if (cashLocation.isNotBlank()) notesBuilder.add("Location:$cashLocation")
                                }
                                AccountType.MFS -> {
                                    finalBalance = mfsBalanceText.toDoubleOrNull() ?: 0.0
                                    finalLast4 = if (mfsNumberLast4.length >= 4) mfsNumberLast4.takeLast(4) else mfsNumberLast4
                                    finalLimit = 0.0
                                    finalBillingDate = ""
                                    finalDueDate = ""
                                    finalMinPayment = 0.0
                                    finalIcon = "phone_android"
                                    if (mfsTier.isNotBlank()) notesBuilder.add("Tier:$mfsTier")
                                    if (mfsNumberLast4.isNotBlank()) notesBuilder.add("Mobile:$mfsNumberLast4")
                                }
                                else -> {
                                    finalBalance = bankBalanceText.toDoubleOrNull() ?: 0.0
                                    finalLast4 = bankAccountLast4.take(4)
                                    finalLimit = 0.0
                                    finalBillingDate = ""
                                    finalDueDate = ""
                                    finalMinPayment = 0.0
                                    finalIcon = "account_balance_wallet"
                                }
                            }

                            viewModel.saveAccount(
                                id = account?.id,
                                name = name.trim().ifBlank {
                                    when (selectedType) {
                                        AccountType.CREDIT_CARD -> if (trimmedInstitution.isNotBlank()) "$trimmedInstitution Credit Card" else "Credit Card"
                                        AccountType.BANK -> if (trimmedInstitution.isNotBlank()) "$trimmedInstitution Debit Card" else "Debit Card"
                                        AccountType.CASH -> "Cash"
                                        AccountType.MFS -> if (trimmedInstitution.isNotBlank()) "$trimmedInstitution Wallet" else "Mobile Wallet"
                                        else -> "Account"
                                    }
                                },
                                type = selectedType,
                                institution = trimmedInstitution,
                                balance = finalBalance,
                                accountNumberLast4 = finalLast4,
                                colorHex = selectedColorHex,
                                iconName = finalIcon,
                                creditLimit = finalLimit,
                                billingDate = finalBillingDate,
                                paymentDueDate = finalDueDate,
                                minPayment = finalMinPayment,
                                notes = notesBuilder.joinToString("|"),
                                includeInTotal = includeInTotal,
                                isHidden = account?.isHidden ?: false
                            )
                            viewModel.userNotificationMessage.value = if (isNewAccount) "Account created successfully!" else "Account updated successfully!"
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("update_account_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isNewAccount) "Create account" else "Update account",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .recordFrostedBackdrop(
                        backdropState = glassLayout.backdropState,
                        lazyListState = listState,
                        contentLayer = glassLayout.contentGraphicsLayer
                    )
                    .testTag("edit_account_lazy_column"),
                contentPadding = glassLayout.contentPadding
            ) {
                // 1. Live Bespoke Category Card Preview
                item {
                    val liveCardModel = AccountCardVisualModel(
                        name = name,
                        institution = institution,
                        type = selectedType,
                        balance = previewAmount,
                        baseColor = previewColor,
                        last4 = previewLast4,
                        network = cardNetwork,
                        subtype = bankSubtype,
                        location = cashLocation,
                        tier = mfsTier,
                        billingDate = billingDateText
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        HishabPatiAccountCard(
                            model = liveCardModel,
                            currencySymbol = currencySymbol,
                            language = settings?.language ?: com.example.data.model.AppLanguage.ENGLISH,
                            cornerRadius = 22.dp,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(186.dp)
                                .testTag("edit_account_live_preview_card")
                        )
                    }
                }

            // 3. Category Selector Tabs: [Debit Card] [Credit Card] [Cash] [MFS]
            item {
                val types = listOf(
                    AccountType.BANK to "Debit Card",
                    AccountType.CREDIT_CARD to "Credit Card",
                    AccountType.CASH to "Cash",
                    AccountType.MFS to "MFS"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { (type, label) ->
                        val isSelected = selectedType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 3.dp else 1.dp,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ) else null,
                            onClick = {
                                selectedType = type
                                if (isNewAccount) {
                                    // Update default color based on category only if currently using another category's default
                                    val mfsDefaultColors = AllConfigurableMfsProviders.map { it.brandColorHex }
                                    when (type) {
                                        AccountType.BANK -> if (selectedColorHex == "#1E293B" || selectedColorHex == "#059669" || selectedColorHex in mfsDefaultColors) selectedColorHex = "#2563EB"
                                        AccountType.CREDIT_CARD -> if (selectedColorHex == "#2563EB" || selectedColorHex == "#059669" || selectedColorHex in mfsDefaultColors) selectedColorHex = "#1E293B"
                                        AccountType.CASH -> if (selectedColorHex == "#2563EB" || selectedColorHex == "#1E293B" || selectedColorHex in mfsDefaultColors) selectedColorHex = "#059669"
                                        AccountType.MFS -> if (selectedColorHex == "#2563EB" || selectedColorHex == "#1E293B" || selectedColorHex == "#059669") {
                                            if (institution.isBlank()) institution = "bKash"
                                            selectedColorHex = AllConfigurableMfsProviders.firstOrNull { it.name.equals(institution, ignoreCase = true) }?.brandColorHex ?: "#E2136E"
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 4. CATEGORY-SPECIFIC INPUT FORMS (DIFFERENT FOR EVERY CATEGORY)
            when (selectedType) {
                AccountType.CREDIT_CARD -> {
                    // --- CREDIT CARD INPUT SECTION ---
                    item {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Credit Card Information",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )

                                // Card Name
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Card Name") },
                                    placeholder = { Text("e.g. Standard Chartered Platinum") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("account_name_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                // Issuing Bank / Institution with search icon (editable + searchable picker)
                                OutlinedTextField(
                                    value = institution,
                                    onValueChange = { institution = it },
                                    label = { Text("Issuing Bank / Institution") },
                                    placeholder = { Text("e.g. Standard Chartered Bank") },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { showBankPicker = true },
                                            modifier = Modifier.testTag("bank_search_icon_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Select Bank or Institution",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bank_institution_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                // Card Network Chips
                                Column {
                                    Text(
                                        text = "Card Network",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(SupportedCardNetworks) { network ->
                                            val isSelected = cardNetwork.equals(network, ignoreCase = true)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { cardNetwork = if (isSelected) "" else network },
                                                label = { Text(network) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                // Card Last 4 Digits
                                OutlinedTextField(
                                    value = bankAccountLast4,
                                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) bankAccountLast4 = it },
                                    label = { Text("Card Number (Last 4 digits)") },
                                    placeholder = { Text("e.g. 4821") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }
                    }

                    // Credit Card Limits & Dates Card (CRITICAL USER REQUIREMENT)
                    item {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Credit Limits & Billing Dates",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )

                                // Credit Limit
                                OutlinedTextField(
                                    value = creditLimitText,
                                    onValueChange = { creditLimitText = it },
                                    label = { Text("Credit Limit") },
                                    prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Bill Date
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showBillingDatePicker = true }
                                    ) {
                                        OutlinedTextField(
                                            value = billingDateText,
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = false,
                                            label = { Text("Bill / Cycle Date") },
                                            placeholder = { Text("e.g. 20 Nov") },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarMonth,
                                                    contentDescription = "Select Bill Date",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("billing_date_field"),
                                            shape = RoundedCornerShape(14.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }

                                    // Due Date
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showDueDatePicker = true }
                                    ) {
                                        OutlinedTextField(
                                            value = paymentDueDateText,
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = false,
                                            label = { Text("Due Date") },
                                            placeholder = { Text("e.g. 10 Dec") },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarMonth,
                                                    contentDescription = "Select Due Date",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("due_date_field"),
                                            shape = RoundedCornerShape(14.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }



                    // Credit Card Outstanding Section (CRITICAL USER REQUIREMENT)
                    item {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Current Outstanding",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ExpenseRed.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Amount Owed",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = ExpenseRed,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Enter the current outstanding amount spent on this card.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                OutlinedTextField(
                                    value = outstandingText,
                                    onValueChange = { outstandingText = it },
                                    prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, color = ExpenseRed) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }
                    }
                }

                AccountType.BANK -> {
                    // --- DEBIT CARD / BANK ACCOUNT INPUT SECTION ---
                    item {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Debit Card & Bank Information",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )

                                // Account Name
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Account Name") },
                                    placeholder = { Text("e.g. City Bank Salary Account") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("account_name_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                // Institution (editable + searchable bottom sheet trigger)
                                OutlinedTextField(
                                    value = institution,
                                    onValueChange = { institution = it },
                                    label = { Text("Bank / Institution") },
                                    placeholder = { Text("e.g. City Bank PLC") },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { showBankPicker = true },
                                            modifier = Modifier.testTag("bank_search_icon_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Select Bank or Institution",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bank_institution_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                // Account Subtype Chips
                                Column {
                                    Text(
                                        text = "Account Subtype",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(BankSubtypes) { subtype ->
                                            val isSelected = bankSubtype.equals(subtype, ignoreCase = true)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { bankSubtype = if (isSelected) "" else subtype },
                                                label = { Text(subtype) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                // Card Network Chips (Requirement 2: Debit Card Card Network selection)
                                Column {
                                    Text(
                                        text = "Card Network",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(SupportedCardNetworks) { network ->
                                            val isSelected = cardNetwork.equals(network, ignoreCase = true)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { cardNetwork = if (isSelected) "" else network },
                                                label = { Text(network) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Debit Card Identifiers
                    item {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Account & Card Identifiers",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = bankAccountLast4,
                                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) bankAccountLast4 = it },
                                        label = { Text("Account (Last 4)", maxLines = 1) },
                                        placeholder = { Text("e.g. 1234") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        shape = RoundedCornerShape(14.dp),
                                        singleLine = true,
                                        maxLines = 1,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )

                                    OutlinedTextField(
                                        value = debitCardLast4,
                                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) debitCardLast4 = it },
                                        label = { Text("Debit Card (Last 4)", maxLines = 1) },
                                        placeholder = { Text("e.g. 5678") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        shape = RoundedCornerShape(14.dp),
                                        singleLine = true,
                                        maxLines = 1,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Opening Balance
                    item {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Current / Opening Balance",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )

                                OutlinedTextField(
                                    value = bankBalanceText,
                                    onValueChange = { bankBalanceText = it },
                                    prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                                    placeholder = { Text("0.00") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }
                    }
                }

                AccountType.CASH -> {
                    // --- CASH INPUT SECTION ---
                    item {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Cash Wallet Information",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Cash Name") },
                                    placeholder = { Text("e.g. Wallet Cash, Home Safe") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                OutlinedTextField(
                                    value = cashLocation,
                                    onValueChange = { cashLocation = it },
                                    label = { Text("Storage Location / Notes") },
                                    placeholder = { Text("e.g. Pocket Wallet, Office Drawer") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }
                    }

                    // Cash on hand
                    item {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Cash on Hand / Balance",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )

                                OutlinedTextField(
                                    value = cashBalanceText,
                                    onValueChange = { cashBalanceText = it },
                                    prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                                    placeholder = { Text("0.00") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }
                    }
                }

                AccountType.MFS, AccountType.OTHER -> {
                    // --- MFS (MOBILE FINANCIAL SERVICES) INPUT SECTION ---
                    item {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Mobile Financial Services (MFS)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )

                                // MFS Provider Selection Area + Visible Chips (bKash, Nagad, Rocket, Upay + All Providers dropdown)
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { showMfsProviderPicker = true }
                                            .padding(vertical = 4.dp)
                                            .testTag("mfs_provider_picker_trigger"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Select Provider",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = if (institution.isNotBlank() && SelectableMfsProviders.none { it.name.equals(institution, ignoreCase = true) }) {
                                                    institution
                                                } else {
                                                    "All providers"
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Open MFS provider sheet",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val isNonPrimaryMfsSelected = institution.isNotBlank() &&
                                        SelectableMfsProviders.none { it.name.equals(institution, ignoreCase = true) }
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.testTag("mfs_provider_chips_row")
                                    ) {
                                        items(SelectableMfsProviders) { prov ->
                                            val isSelected = institution.equals(prov.name, ignoreCase = true)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    institution = prov.name
                                                    selectedColorHex = prov.brandColorHex
                                                },
                                                label = { Text(prov.name) },
                                                modifier = Modifier.testTag("mfs_chip_${prov.name.lowercase()}"),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                        item {
                                            val isAllProvidersSelected = isNonPrimaryMfsSelected
                                            val allProvidersLabel = if (isAllProvidersSelected) {
                                                "All Providers ($institution)"
                                            } else {
                                                "All Providers"
                                            }
                                            FilterChip(
                                                selected = isAllProvidersSelected,
                                                onClick = {
                                                    showMfsProviderPicker = true
                                                },
                                                label = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(allProvidersLabel)
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowDown,
                                                            contentDescription = "All Providers dropdown",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.testTag("mfs_chip_all_providers"),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Wallet Name") },
                                    placeholder = { Text(if (institution.isNotBlank()) "e.g. $institution Account" else "e.g. Personal bKash") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                OutlinedTextField(
                                    value = mfsNumberLast4,
                                    onValueChange = { if (it.length <= 11 && it.all { c -> c.isDigit() }) mfsNumberLast4 = it },
                                    label = { Text("Mobile / Wallet Number (Last 4 or Full)") },
                                    placeholder = { Text("e.g. 01712345678 or 1875") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                // Wallet Tier
                                Column {
                                    Text(
                                        text = "Account Tier",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val tiers = listOf("Personal", "Merchant", "Agent", "DPS")
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(tiers) { tier ->
                                            val isSelected = mfsTier == tier
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { mfsTier = tier },
                                                label = { Text(tier) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // MFS Balance
                    item {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Current MFS Balance",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )

                                OutlinedTextField(
                                    value = mfsBalanceText,
                                    onValueChange = { mfsBalanceText = it },
                                    prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                                    placeholder = { Text("0.00") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 5. Card Appearance (Color Picker Palette)
            item {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Card Appearance",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )

                            Text(
                                text = currentColorName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(CardColorPresets) { preset ->
                                val color = try {
                                    Color(android.graphics.Color.parseColor(preset.hex))
                                } catch (_: Exception) {
                                    Color(0xFF2563EB)
                                }
                                val isSelected = selectedColorHex.equals(preset.hex, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColorHex = preset.hex }
                                        .then(
                                            if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .clickable { showColorPaletteDialog = true }
                                        .testTag("show_all_colors_button"),
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "Show all colors",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Show all",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5b. Include in Total Balance Card Toggle
            item {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    cornerRadius = 20.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 16.dp)
                        ) {
                            Text(
                                text = "Include in Total Balance",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Include wallet in calculation of total balance",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Switch(
                            checked = includeInTotal,
                            onCheckedChange = { includeInTotal = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // 6. Delete account action (Archive is in the overflow menu)
            if (!isNewAccount && account != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delete account",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }

            if (showBillingDatePicker) {
                HishabPatiDatePickerDialog(
                    initialSelectedDateMillis = parseDateToMillis(billingDateText),
                    onDateSelected = { millis ->
                        billingDateText = formatDateToString(millis)
                        showBillingDatePicker = false
                    },
                    onDismissRequest = { showBillingDatePicker = false },
                    language = settings?.language ?: com.example.data.model.AppLanguage.ENGLISH,
                    titleText = "Select Bill / Cycle Date"
                )
            }

            if (showDueDatePicker) {
                HishabPatiDatePickerDialog(
                    initialSelectedDateMillis = parseDateToMillis(paymentDueDateText),
                    onDateSelected = { millis ->
                        paymentDueDateText = formatDateToString(millis)
                        showDueDatePicker = false
                    },
                    onDismissRequest = { showDueDatePicker = false },
                    language = settings?.language ?: com.example.data.model.AppLanguage.ENGLISH,
                    titleText = "Select Payment Due Date"
                )
            }

            // Fixed, scroll-aware frosted-glass top header matching Home and Plan tabs
            ScrollAwareFixedHeader(
                layoutState = glassLayout,
                testTag = "add_account_fixed_header"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        border = BorderStroke(
                            0.75.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("add_account_close_button"),
                        onClick = onBack
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = if (isNewAccount) "Add account" else "Edit account",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isNewAccount && account != null) {
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                                border = BorderStroke(
                                    0.75.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.size(42.dp),
                                onClick = { showOverflowMenu = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Adjust Balance") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onNavigateToAdjustBalance(account)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Archive Wallet") },
                                    onClick = {
                                        showOverflowMenu = false
                                        showArchiveConfirm = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Archive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Searchable Bank / Institution Bottom Sheet Picker
    if (showBankPicker) {
        BankInstitutionPickerSheet(
            currentInstitution = institution,
            customInstitutions = customBankInstitutions,
            onSelectBank = { selectedBank ->
                institution = selectedBank
                showBankPicker = false
            },
            onDismiss = { showBankPicker = false }
        )
    }

    // Searchable MFS Provider Bottom Sheet Picker
    if (showMfsProviderPicker) {
        MfsProviderPickerSheet(
            currentProviderName = institution,
            customProviders = customMfsProviders,
            onSelectProvider = { prov ->
                institution = prov.name
                selectedColorHex = prov.brandColorHex
                showMfsProviderPicker = false
            },
            onDismiss = { showMfsProviderPicker = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm && account != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete \"${account.name}\"? Transactions linked to this account may lose their association.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(account.id)
                        showDeleteConfirm = false
                        onBack()
                    }
                ) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Archive Confirmation Dialog
    if (showArchiveConfirm && account != null) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Archive Wallet") },
            text = { Text("Are you sure you want to archive \"${account.name}\"? It will be hidden from active wallet lists and transactions, but all balances and transaction history remain completely safe.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.archiveAccount(account.id)
                        showArchiveConfirm = false
                        onBack()
                    }
                ) {
                    Text("Archive", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Redesigned Wallet Color Picker Dialog
    if (showColorPaletteDialog) {
        WalletColorPickerDialog(
            selectedColorHex = selectedColorHex,
            currentColorName = currentColorName,
            onColorSelected = { selectedColorHex = it },
            onDismiss = { showColorPaletteDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankInstitutionPickerSheet(
    currentInstitution: String,
    customInstitutions: List<String> = emptyList(),
    onSelectBank: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val filteredBanks = remember(searchQuery, customInstitutions) {
        filterBanksAndInstitutions(searchQuery, customInstitutions)
    }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("bank_institution_picker_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select Bank / Institution",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Search the list or type any custom bank name in the field",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search bank or institution...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bank_picker_search_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.45f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.25f else 0.35f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            val trimmedQuery = searchQuery.trim()
            val hasExactMatch = filteredBanks.any { it.equals(trimmedQuery, ignoreCase = true) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (trimmedQuery.isNotEmpty() && !hasExactMatch) {
                    item(key = "custom_bank_entry") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectBank(trimmedQuery) }
                                .testTag("bank_picker_use_custom_row"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.10f),
                            border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Use \"$trimmedQuery\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = "Custom bank / institution name",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                items(filteredBanks, key = { it }) { bank ->
                    val isSelected = currentInstitution.trim().equals(bank, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectBank(bank) }
                            .testTag("bank_picker_item_$bank"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.20f else 0.10f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.28f else 0.42f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.65f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = bank,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
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
fun MfsProviderPickerSheet(
    currentProviderName: String,
    customProviders: List<MfsProviderInfo> = emptyList(),
    onSelectProvider: (MfsProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val filteredProviders = remember(searchQuery, customProviders) {
        filterMfsProviders(searchQuery, customProviders)
    }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("mfs_provider_picker_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select MFS Provider",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose from ${AllConfigurableMfsProviders.size} MFS providers",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search MFS provider...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mfs_picker_search_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.45f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.25f else 0.35f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            val trimmedQuery = searchQuery.trim()
            val hasExactMatch = filteredProviders.any { it.name.equals(trimmedQuery, ignoreCase = true) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (trimmedQuery.isNotEmpty() && !hasExactMatch) {
                    item(key = "custom_mfs_entry") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onSelectProvider(MfsProviderInfo(trimmedQuery, "#4F46E5"))
                                }
                                .testTag("mfs_picker_use_custom_row"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.10f),
                            border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Use \"$trimmedQuery\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = "Custom MFS provider name",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                items(filteredProviders, key = { it.name }) { prov ->
                    val isSelected = currentProviderName.trim().equals(prov.name, ignoreCase = true)
                    val brandColor = try {
                        Color(android.graphics.Color.parseColor(prov.brandColorHex))
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectProvider(prov) }
                            .testTag("mfs_picker_item_${prov.name.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.20f else 0.10f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.28f else 0.42f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.65f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = brandColor,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = prov.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = if (prov.name.equals("Others", ignoreCase = true)) {
                                            "Other Mobile Financial Service"
                                        } else {
                                            "${prov.name} Mobile Wallet"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalletColorPickerDialog(
    selectedColorHex: String,
    currentColorName: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialColorHex = remember { selectedColorHex }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val activePreviewColor = remember(selectedColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (_: Exception) {
            Color(0xFF2563EB)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .testTag("wallet_color_picker_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 18.dp,
            border = BorderStroke(
                width = 1.25.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = if (isDark) 0.50f else 0.72f
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                // Header: Title + Selected Color Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.20f else 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Wallet Color",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Choose a color for your wallet card",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = activePreviewColor.copy(alpha = if (isDark) 0.22f else 0.14f),
                        border = BorderStroke(1.dp, activePreviewColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(activePreviewColor)
                            )
                            Text(
                                text = currentColorName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Color Swatch Grid: true 1:1 circles centered in each grid cell with clear spacing
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 310.dp)
                        .testTag("wallet_color_swatch_grid")
                ) {
                    items(ComprehensiveColorPalette, key = { it.hex }) { preset ->
                        val swatchColor = try {
                            Color(android.graphics.Color.parseColor(preset.hex))
                        } catch (_: Exception) {
                            Color(0xFF2563EB)
                        }
                        val isSelected = selectedColorHex.equals(preset.hex, ignoreCase = true)

                        // Outer centering Box prevents GridCells.Fixed(4) from stretching the circle into an oval
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) {
                                            swatchColor.copy(alpha = 0.20f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                width = 2.25.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onColorSelected(preset.hex) }
                                    .testTag("wallet_color_swatch_${preset.hex}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 38.dp else 44.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = 1.dp,
                                            color = if (isDark) {
                                                Color.White.copy(alpha = 0.22f)
                                            } else {
                                                Color.Black.copy(alpha = 0.10f)
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected ${preset.name}",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons: Cancel & Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onColorSelected(initialColorHex)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
