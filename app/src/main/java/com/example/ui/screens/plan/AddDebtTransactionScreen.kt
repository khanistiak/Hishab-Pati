package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.DebtType
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.HishabPatiDatePickerDialog
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun sanitizeDebtAmountInput(raw: String): String {
    val ascii = raw.map { ch ->
        when (ch) {
            '০' -> '0'
            '১' -> '1'
            '২' -> '2'
            '৩' -> '3'
            '৪' -> '4'
            '৫' -> '5'
            '৬' -> '6'
            '৭' -> '7'
            '৮' -> '8'
            '৯' -> '9'
            else -> ch
        }
    }.joinToString("")

    val cleaned = StringBuilder()
    var hasDot = false
    var decimalDigits = 0
    var integerDigits = 0

    for (ch in ascii) {
        when {
            ch.isDigit() -> {
                if (hasDot) {
                    if (decimalDigits < 2) {
                        cleaned.append(ch)
                        decimalDigits++
                    }
                } else {
                    if (integerDigits < 12) {
                        cleaned.append(ch)
                        integerDigits++
                    }
                }
            }
            ch == '.' && !hasDot -> {
                if (cleaned.isEmpty()) {
                    cleaned.append('0')
                    integerDigits = 1
                }
                cleaned.append('.')
                hasDot = true
            }
        }
    }
    return cleaned.toString()
}

@Composable
fun AddDebtTransactionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSubScreen by viewModel.currentSubScreen.collectAsState()
    BackHandler(
        enabled = currentSubScreen == SubScreen.ADD_DEBT_TRANSACTION,
        onBack = onBack
    )

    val selectedDebt by viewModel.selectedDebtForTransaction.collectAsState()
    val fallbackSelectedDebt by viewModel.selectedDebt.collectAsState()
    val txType by viewModel.debtTransactionType.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val liveDebt = selectedDebt ?: fallbackSelectedDebt
    var lastKnownDebt by remember { mutableStateOf(liveDebt) }
    if (liveDebt != null) {
        lastKnownDebt = liveDebt
    }
    val debt = liveDebt ?: lastKnownDebt

    if (debt == null) {
        LaunchedEffect(currentSubScreen) {
            if (currentSubScreen == SubScreen.ADD_DEBT_TRANSACTION) {
                onBack()
            }
        }
        return
    }

    val isPayable = debt.type == DebtType.BORROWED
    val initial = debt.personName.trim().take(1).uppercase(Locale.US).ifEmpty { "?" }
    val displayTotal = if (debt.totalAmount > 0.0) debt.totalAmount else (debt.amount + debt.paidAmount)
    val remainingAmount = calculateDebtRemaining(debt)

    var selectedTxType by remember(debt.id, txType) { mutableStateOf(txType) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var selectedDateMillis by remember(debt.id, txType) { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val normalizedTxKey = selectedTxType.trim().uppercase(Locale.ENGLISH).replace(" ", "_")
    val isInterestTx = normalizedTxKey == "ADD_INTEREST" || normalizedTxKey == "INTEREST"

    // Emerald (#059669) for inflow / Lent / Received; Coral-Rose (#DC2626) for outflow / Repay Debt / Borrow More
    val isInflowAction = when (normalizedTxKey) {
        "BORROW_MORE", "BORROWED_MORE" -> false
        "LEND_MORE", "LENT_MORE" -> true
        "INCREASE", "ADD_INTEREST", "INTEREST" -> !isPayable
        else -> !isPayable
    }
    val actionAccentColor = if (isInflowAction) Color(0xFF059669) else Color(0xFFDC2626)
    val actionIconTint = if (isDark) {
        if (isInflowAction) Color(0xFF34D399) else Color(0xFFF87171)
    } else {
        actionAccentColor
    }

    fun resolveTxTypeLabel(keyRaw: String): String {
        val norm = keyRaw.trim().uppercase(Locale.ENGLISH).replace(" ", "_")
        return when (norm) {
            "BORROW_MORE", "BORROWED_MORE", "INCREASE" -> {
                if (isPayable) {
                    if (isBn) "আরও ঋণ গ্রহণ" else "Borrow More"
                } else {
                    if (isBn) "আরও ঋণ প্রদান" else "Lend More"
                }
            }
            "LEND_MORE", "LENT_MORE" -> if (isBn) "আরও ঋণ প্রদান" else "Lend More"
            "ADD_INTEREST", "INTEREST" -> if (isBn) "সুদ যুক্ত করুন" else "Add Interest"
            else -> {
                if (isPayable) AppStrings.t("repay_debt", language)
                else AppStrings.t("receive_payment", language)
            }
        }
    }

    fun resolveDefaultTxName(keyRaw: String): String {
        val norm = keyRaw.trim().uppercase(Locale.ENGLISH).replace(" ", "_")
        return when (norm) {
            "BORROW_MORE", "BORROWED_MORE", "INCREASE" -> {
                if (isPayable) {
                    if (isBn) "আরও ঋণ গ্রহণ" else "Borrowed More"
                } else {
                    if (isBn) "আরও ঋণ প্রদান" else "Lent More"
                }
            }
            "LEND_MORE", "LENT_MORE" -> if (isBn) "আরও ঋণ প্রদান" else "Lent More"
            "ADD_INTEREST", "INTEREST" -> if (isBn) "সুদ" else "Interest"
            else -> {
                if (isPayable) (if (isBn) "ঋণ পরিশোধ" else "Debt Repayment")
                else (if (isBn) "আদায় গ্রহণ" else "Payment Received")
            }
        }
    }

    val localizedTxTypeLabel = resolveTxTypeLabel(selectedTxType)
    val defaultTxName = resolveDefaultTxName(selectedTxType)
    val txTypeOptions = remember(isPayable) {
        if (isPayable) {
            listOf("Record Payment", "Borrow More", "Add Interest")
        } else {
            listOf("Receive Payment", "Lend More", "Add Interest")
        }
    }

    var transactionName by remember(debt.id, txType) { mutableStateOf(defaultTxName) }
    var hasCustomName by remember(debt.id, txType) { mutableStateOf(false) }
    var amountInput by remember(debt.id, txType) { mutableStateOf("") }
    var amountError by remember(debt.id, txType) { mutableStateOf<String?>(null) }
    var isAmountFocused by remember { mutableStateOf(false) }
    var isNameFocused by remember { mutableStateOf(false) }
    var isNotesFocused by remember { mutableStateOf(false) }
    var selectedAccount by remember(debt.id, txType) {
        val initialAcc = if (!isInterestTx && debt.bindToWallet && debt.accountId.isNotBlank()) {
            accounts.firstOrNull { it.id == debt.accountId }
        } else {
            null
        }
        mutableStateOf<AccountEntity?>(initialAcc)
    }
    var notesInput by remember(debt.id, txType) { mutableStateOf("") }
    val tagsInput by remember(debt.id, txType) { mutableStateOf("") }

    val amountFocusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }
    val notesFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val listState = viewModel.rememberScreenLazyListState(
        subKey = "${debt.id}_$txType",
        screenOverride = SubScreen.ADD_DEBT_TRANSACTION
    )

    val dateStr = remember(selectedDateMillis, language) {
        val raw = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date(selectedDateMillis))
        DateFormatter.localizeText(raw, language)
    }
    val paidPct = if (displayTotal > 0) ((debt.paidAmount / displayTotal) * 100).toInt().coerceIn(0, 100) else 0
    val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0

    val cardBg = if (isDark) CardBackgroundDark else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.09f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val debtColor = try {
        Color(android.graphics.Color.parseColor(debt.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val submitTransaction: () -> Unit = {
        val amt = amountInput.toDoubleOrNull() ?: 0.0
        if (amt > 0.0) {
            amountError = null
            keyboardController?.hide()
            viewModel.saveDebtTransaction(
                debt = debt,
                type = selectedTxType,
                amount = amt,
                account = if (isInterestTx) null else selectedAccount,
                notes = notesInput,
                tags = tagsInput,
                customTitle = transactionName.ifBlank { defaultTxName },
                timestamp = selectedDateMillis
            )
        } else {
            amountError = AppStrings.t("invalid_amount_error", language)
            coroutineScope.launch {
                listState.animateScrollToItem(1)
                runCatching { amountFocusRequester.requestFocus() }
                keyboardController?.show()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier.imePadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("debt_tx_back_button"),
                    onClick = {
                        keyboardController?.hide()
                        onBack()
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AppStrings.t("back", language),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = AppStrings.t("add_transaction", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            val primaryColor = MaterialTheme.colorScheme.primary
            val primaryDeep = lerp(primaryColor, Color(0xFF0F172A), 0.24f)
            val ctaGradient = Brush.horizontalGradient(
                colors = listOf(primaryColor, primaryDeep)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = primaryColor.copy(alpha = 0.30f),
                            spotColor = primaryColor.copy(alpha = 0.38f)
                        )
                        .clip(CircleShape)
                        .background(ctaGradient, CircleShape)
                        .clickable(onClick = submitTransaction)
                        .testTag("debt_tx_done_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.t("done", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("add_debt_transaction_lazy_column"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Hero Debt Summary Card
            item {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = debtColor.copy(alpha = if (isDark) 0.22f else 0.16f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initial,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = debtColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = debt.personName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${AppStrings.t("total_amount", language)}: ${CurrencyFormatter.format(displayTotal, currencySymbol, language = language)}",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }

                            val badgeAccent = if (isPayable) ExpenseRed else IncomeGreen
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = badgeAccent.copy(alpha = if (isDark) 0.18f else 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isPayable) {
                                            "↓ ${AppStrings.t("borrowed_tab", language)}"
                                        } else {
                                            "↑ ${AppStrings.t("lent_tab", language)}"
                                        },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = badgeAccent
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = CurrencyFormatter.format(debt.paidAmount, currencySymbol, language = language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                                Text(
                                    text = if (isBn) {
                                        "পরিশোধিত ${CurrencyFormatter.convertToBanglaDigits(paidPct.toString())}%"
                                    } else {
                                        "Paid $paidPct%"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.format(remainingAmount, currencySymbol, language = language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isPayable) ExpenseRed else IncomeGreen,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                                Text(
                                    text = if (isBn) "বাকি আছে" else "Remaining",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }

            // 1. Elevated Interactive Amount Bento Card
            item {
                Spacer(modifier = Modifier.height(14.dp))
                DebtBentoInputCard(
                    leadingIcon = Icons.Rounded.Payments,
                    iconTint = actionIconTint,
                    badgeTint = actionAccentColor,
                    activeAccentColor = actionAccentColor,
                    isFocused = isAmountFocused,
                    isError = amountError != null,
                    iconContentDescription = AppStrings.t("amount", language),
                    testTag = "debt_tx_amount_card",
                    onClick = {
                        amountFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.t("amount", language),
                            fontSize = 11.5.sp,
                            color = when {
                                amountError != null -> ExpenseRed
                                isAmountFocused -> actionAccentColor
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        if (parsedAmount > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = actionAccentColor.copy(alpha = if (isDark) 0.20f else 0.11f)
                            ) {
                                Text(
                                    text = CurrencyFormatter.format(parsedAmount, currencySymbol, language = language),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = actionIconTint,
                                    style = TextStyle(fontFeatureSettings = "tnum"),
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("debt_tx_amount_formatted_preview")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currencySymbol,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAmountFocused) actionIconTint else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        BasicTextField(
                            value = amountInput,
                            onValueChange = { raw ->
                                amountInput = sanitizeDebtAmountInput(raw)
                                if (amountError != null) {
                                    amountError = null
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFeatureSettings = "tnum"
                            ),
                            cursorBrush = SolidColor(actionAccentColor),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitTransaction() }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(amountFocusRequester)
                                .onFocusChanged { isAmountFocused = it.isFocused }
                                .testTag("debt_tx_amount_input"),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (amountInput.isEmpty()) {
                                        Text(
                                            text = if (isBn) "০.০০" else "0.00",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                            style = TextStyle(fontFeatureSettings = "tnum")
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
                if (amountError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = amountError ?: "",
                        color = ExpenseRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .testTag("debt_tx_amount_error")
                    )
                }
            }

            // 2. Elevated Transaction Type Bento Card
            item {
                Spacer(modifier = Modifier.height(10.dp))
                val indigoTint = Color(0xFF4F46E5)
                Box(modifier = Modifier.fillMaxWidth()) {
                    DebtBentoInputCard(
                        leadingIcon = Icons.Rounded.SwapHoriz,
                        iconTint = if (isDark) Color(0xFFA5B4FC) else indigoTint,
                        badgeTint = indigoTint,
                        activeAccentColor = actionAccentColor,
                        isFocused = showTypeMenu,
                        iconContentDescription = AppStrings.t("transaction_type", language),
                        testTag = "debt_tx_type_card",
                        onClick = {
                            keyboardController?.hide()
                            showTypeMenu = true
                        },
                        trailingContent = {
                            DebtTrailingChevronPill(
                                icon = Icons.Rounded.ExpandMore,
                                iconSize = 18.dp,
                                contentDescription = AppStrings.t("transaction_type", language)
                            )
                        }
                    ) {
                        Text(
                            text = AppStrings.t("transaction_type", language),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = localizedTxTypeLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        txTypeOptions.forEach { optionKey ->
                            val optionLabel = resolveTxTypeLabel(optionKey)
                            val isSelectedOption = optionLabel == localizedTxTypeLabel
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = optionLabel,
                                        fontWeight = if (isSelectedOption) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelectedOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = if (isSelectedOption) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    selectedTxType = optionKey
                                    if (!hasCustomName) {
                                        transactionName = resolveDefaultTxName(optionKey)
                                    }
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // 3. Elevated Date Bento Card
            item {
                Spacer(modifier = Modifier.height(10.dp))
                val skyBlue = Color(0xFF0EA5E9)
                DebtBentoInputCard(
                    leadingIcon = Icons.Rounded.CalendarToday,
                    iconTint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    badgeTint = skyBlue,
                    activeAccentColor = actionAccentColor,
                    iconContentDescription = AppStrings.t("date", language),
                    testTag = "debt_tx_date_card",
                    onClick = {
                        keyboardController?.hide()
                        showDatePicker = true
                    },
                    trailingContent = {
                        DebtTrailingChevronPill(
                            icon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            iconSize = 12.dp,
                            contentDescription = AppStrings.t("date", language)
                        )
                    }
                ) {
                    Text(
                        text = AppStrings.t("date", language),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateStr,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 4. Elevated Name Bento Card
            item {
                Spacer(modifier = Modifier.height(10.dp))
                val violetTint = Color(0xFF7C3AED)
                DebtBentoInputCard(
                    leadingIcon = Icons.Rounded.EditNote,
                    iconTint = if (isDark) Color(0xFFC4B5FD) else violetTint,
                    badgeTint = violetTint,
                    activeAccentColor = actionAccentColor,
                    isFocused = isNameFocused,
                    iconContentDescription = AppStrings.t("name", language),
                    testTag = "debt_tx_name_card",
                    onClick = {
                        nameFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                ) {
                    Text(
                        text = AppStrings.t("name", language),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isNameFocused) actionAccentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    BasicTextField(
                        value = transactionName,
                        onValueChange = {
                            transactionName = it
                            hasCustomName = it.isNotBlank()
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(actionAccentColor),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester)
                            .onFocusChanged { isNameFocused = it.isFocused }
                            .testTag("debt_tx_name_input"),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (transactionName.isEmpty()) {
                                    Text(
                                        text = AppStrings.t("transaction_name_placeholder", language),
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
            }

            // Wallet Section (for cash/bank transactions; hidden for Interest)
            if (!isInterestTx && accounts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = AppStrings.t("wallet", language),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(accounts, key = { it.id }) { acc ->
                            val isSelected = selectedAccount?.id == acc.id
                            val selectedTileBg = if (acc.name.contains("Cash", ignoreCase = true)) {
                                IncomeGreen.copy(alpha = if (isDark) 0.22f else 0.16f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) selectedTileBg else cardBg,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else cardBorder
                                ),
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedAccount = if (isSelected) null else acc
                                    }
                                    .testTag("debt_tx_wallet_${acc.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (acc.type) {
                                            AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                            AccountType.BANK -> Icons.Default.AccountBalance
                                            else -> Icons.Default.Wallet
                                        },
                                        contentDescription = acc.name,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = acc.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(acc.balance, currencySymbol, language = language),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                            style = TextStyle(fontFeatureSettings = "tnum")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Elevated Other Notes Bento Card
            item {
                Spacer(modifier = Modifier.height(12.dp))
                val slateTint = Color(0xFF64748B)
                DebtBentoInputCard(
                    leadingIcon = Icons.Rounded.Notes,
                    iconTint = if (isDark) Color(0xFF94A3B8) else slateTint,
                    badgeTint = slateTint,
                    activeAccentColor = actionAccentColor,
                    isFocused = isNotesFocused,
                    iconContentDescription = AppStrings.t("other_notes", language),
                    testTag = "debt_tx_notes_card",
                    onClick = {
                        notesFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                ) {
                    Text(
                        text = AppStrings.t("other_notes", language),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isNotesFocused) actionAccentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    BasicTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        maxLines = 4,
                        textStyle = TextStyle(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(actionAccentColor),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(notesFocusRequester)
                            .onFocusChanged { isNotesFocused = it.isFocused }
                            .testTag("debt_tx_notes_input"),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (notesInput.isEmpty()) {
                                    Text(
                                        text = AppStrings.t("debt_notes_placeholder", language),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showDatePicker) {
        HishabPatiDatePickerDialog(
            initialSelectedDateMillis = selectedDateMillis,
            onDateSelected = { millis ->
                selectedDateMillis = millis
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false },
            language = language
        )
    }
}
