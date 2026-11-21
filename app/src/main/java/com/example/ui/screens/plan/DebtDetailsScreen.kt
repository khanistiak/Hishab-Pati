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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.TranslucentCard
import com.example.ui.screens.transaction.CategorySelectionSheetStyle
import com.example.ui.screens.transaction.DisableSheetForceDark
import com.example.ui.screens.transaction.SheetHeaderCircleActionButton
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DebtHistoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateStr: String,
    val amount: Double,
    val isPositive: Boolean,
    val accountName: String,
    val accountColorHex: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSubScreen by viewModel.currentSubScreen.collectAsState()
    androidx.activity.compose.BackHandler(
        enabled = currentSubScreen == SubScreen.DEBT_DETAILS,
        onBack = onBack
    )
    val debts by viewModel.debts.collectAsState()
    val selectedDebtState by viewModel.selectedDebt.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val liveDebt = debts.find { it.id == selectedDebtState?.id } ?: selectedDebtState
    var lastKnownDebt by remember { mutableStateOf(liveDebt) }
    if (liveDebt != null) {
        lastKnownDebt = liveDebt
    }
    val debt = liveDebt ?: lastKnownDebt

    if (debt == null) {
        LaunchedEffect(currentSubScreen) {
            if (currentSubScreen == SubScreen.DEBT_DETAILS) {
                onBack()
            }
        }
        return
    }

    var showMenu by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showAddInterestDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedHistoryItemForDetails by remember { mutableStateOf<DebtHistoryItem?>(null) }
    var itemToDelete by remember { mutableStateOf<DebtHistoryItem?>(null) }

    val isPayable = debt.type == DebtType.BORROWED
    val initial = debt.personName.trim().take(1).uppercase(Locale.US).ifEmpty { "?" }
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA

    val boundAccount = accounts.find { it.id == debt.accountId }

    // REQUIREMENT 6: STRICT FILTERING BY DEBT ID ONLY
    val matchingTransactions = remember(allTransactions, debt.id) {
        allTransactions.filter { tx ->
            tx.debtId == debt.id ||
            tx.id.startsWith("tx_debt_${debt.id}") ||
            tx.id.startsWith("tx_repay_${debt.id}") ||
            tx.id == "tx_debt_init_${debt.id}" ||
            tx.notes.contains("[Debt:${debt.id}]")
        }.sortedBy { it.timestamp }
    }

    val hasInitialTx = remember(matchingTransactions, debt.id) {
        matchingTransactions.any {
            it.subcategory == "Initial Debt" || it.id == "tx_debt_init_${debt.id}"
        }
    }

    val repaymentsFromTxs = remember(matchingTransactions, isPayable) {
        matchingTransactions.filter { tx ->
            !isDebtAdditionTransaction(tx, isPayable)
        }.sumOf { it.amount }
    }

    val additionsFromTxs = remember(matchingTransactions, isPayable) {
        matchingTransactions.filter { tx ->
            isDebtAdditionTransaction(tx, isPayable)
        }.sumOf { it.amount }
    }

    val baseDebtTotal = if (debt.totalAmount > 0.0) debt.totalAmount else (debt.amount + debt.paidAmount)
    val displayTotal = if (matchingTransactions.isNotEmpty()) {
        if (hasInitialTx) additionsFromTxs else maxOf(baseDebtTotal, additionsFromTxs)
    } else {
        baseDebtTotal
    }

    val displayPaid = if (matchingTransactions.isNotEmpty()) {
        if (hasInitialTx) repaymentsFromTxs else maxOf(debt.paidAmount, repaymentsFromTxs)
    } else {
        debt.paidAmount
    }

    val displayRemaining = (displayTotal - displayPaid).coerceAtLeast(0.0)
    val displayIsSettled = (displayTotal > 0.0 && displayRemaining <= 0.001) ||
        (displayTotal <= 0.0 && debt.isSettled)
    val paidPct = if (displayTotal > 0) ((displayPaid / displayTotal) * 100).toInt().coerceIn(0, 100) else 0

    LaunchedEffect(debt.id) {
        viewModel.recalculateDebt(debt.id)
    }

    // Build complete chronological debt history
    val historyItems = remember(matchingTransactions, debt, accounts, language) {
        val list = mutableListOf<DebtHistoryItem>()
        val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        // If no explicit initial transaction recorded in DB, synthesize the initial debt entry
        if (!hasInitialTx) {
            val initialDateStr = if (debt.createdDateStr.isNotBlank()) debt.createdDateStr
            else dateFormatter.format(Date(debt.createdAt))

            // Compute initial debt amount by subtracting recorded additions from totalAmount
            val additionalAdditions = matchingTransactions
                .filter {
                    isDebtAdditionTransaction(it, isPayable) &&
                        !it.subcategory.equals("Initial Debt", ignoreCase = true)
                }
                .sumOf { it.amount }
            val initialDebtAmount = (displayTotal - additionalAdditions).coerceAtLeast(0.0)

            val initialTitle = if (isPayable) {
                if (isBn) "প্রাথমিক ঋণ গ্রহণ" else "Initial Debt (Borrowed)"
            } else {
                if (isBn) "প্রাথমিক ঋণ প্রদান" else "Initial Debt (Lent)"
            }

            val boundName = if (debt.bindToWallet && boundAccount != null) boundAccount.name
            else AppStrings.t("direct_unbound", language)

            list.add(
                DebtHistoryItem(
                    id = "init_${debt.id}",
                    title = initialTitle,
                    subtitle = if (debt.reason.isNotBlank()) debt.reason else (if (isPayable) "Borrowed from ${debt.personName}" else "Lent to ${debt.personName}"),
                    dateStr = if (isBn) CurrencyFormatter.convertToBanglaDigits(initialDateStr) else initialDateStr,
                    amount = if (initialDebtAmount > 0) initialDebtAmount else displayTotal,
                    isPositive = true,
                    accountName = boundName,
                    accountColorHex = if (debt.bindToWallet) boundAccount?.colorHex else null
                )
            )
        }

        // Add all recorded transactions for this specific debt
        matchingTransactions.forEach { tx ->
            val dateStr = dateFormatter.format(Date(tx.timestamp))
            val matchedAcc = accounts.find { it.id == tx.accountId }
            val accName = matchedAcc?.name ?: AppStrings.t("direct_unbound", language)
            val isAddition = isDebtAdditionTransaction(tx, isPayable)
            val isPayment = !isAddition

            val displayTitle = when {
                tx.subcategory.equals("Initial Debt", ignoreCase = true) -> {
                    if (isPayable) (if (isBn) "প্রাথমিক ঋণ গ্রহণ" else "Initial Debt (Borrowed)")
                    else (if (isBn) "প্রাথমিক ঋণ প্রদান" else "Initial Debt (Lent)")
                }
                tx.subcategory.equals("Borrow More", ignoreCase = true) -> {
                    if (isBn) "আরও ঋণ গ্রহণ" else "Borrowed More"
                }
                tx.subcategory.equals("Lend More", ignoreCase = true) -> {
                    if (isBn) "আরও ঋণ প্রদান" else "Lent More"
                }
                tx.subcategory.equals("Interest", ignoreCase = true) || tx.category.equals("Interest", ignoreCase = true) -> {
                    if (isBn) "সুদ" else "Interest"
                }
                tx.subcategory.equals("Final Settlement", ignoreCase = true) -> {
                    if (isBn) "চূড়ান্ত পরিশোধ" else "Final Settlement"
                }
                isPayment -> if (isPayable) (if (isBn) "ঋণ পরিশোধ" else "Repayment") else (if (isBn) "আদায় গ্রহণ" else "Payment Received")
                else -> tx.title
            }

            // Both for Payable and Receivable:
            // Additions (Borrow More, Lend More, Initial Debt, Interest) increase the debt balance (+ green up arrow)
            // Payments (Repayments or Payments Received) reduce the debt balance (- red down arrow)
            val isPos = isAddition

            list.add(
                DebtHistoryItem(
                    id = tx.id,
                    title = displayTitle,
                    subtitle = tx.notes.replace(Regex("\\[Debt:[^\\]]+\\]"), "").trim().ifBlank { tx.category },
                    dateStr = if (isBn) CurrencyFormatter.convertToBanglaDigits(dateStr) else dateStr,
                    amount = tx.amount,
                    isPositive = isPos,
                    accountName = accName,
                    accountColorHex = matchedAcc?.colorHex
                )
            )
        }

        list.reversed() // Show newest first
    }

    val debtColor = try {
        Color(android.graphics.Color.parseColor(debt.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("debt_details_back_button"),
                    onClick = onBack
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AppStrings.t("back", language),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = if (isBn) "ঋণের বিস্তারিত" else "Debt Details",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isBn) "সম্পাদনা করুন" else "Edit Debt") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                viewModel.openAddDebt(debt.type, debt)
                            }
                        )
                        if (!displayIsSettled) {
                            DropdownMenuItem(
                                text = { Text(if (isBn) "সম্পূর্ণ পরিশোধিত চিহ্নিত করুন" else "Mark as Fully Paid") },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.markDebtAsPaid(debt.id)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (isBn) "মুছে ফেলুন" else "Delete Debt", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (!displayIsSettled) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        val bottomBtnLabel = if (isPayable) {
                            AppStrings.t("repay_debt", language)
                        } else {
                            AppStrings.t("receive_payment", language)
                        }
                        Button(
                            onClick = {
                                viewModel.openAddDebtTransaction(debt, if (isPayable) "Record Payment" else "Receive Payment")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("repay_debt_button"),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2A303C),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = bottomBtnLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { contentPadding ->
        val listState = viewModel.rememberScreenLazyListState(
            subKey = debt.id,
            screenOverride = SubScreen.DEBT_DETAILS
        )
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .testTag("debt_details_lazy_column"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Hero Summary Card
            item {
                TranslucentCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = debtColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initial,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = debtColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = debt.personName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                )
                                Text(
                                    text = "${if (isBn) "মোট" else "Total"}: ${CurrencyFormatter.format(displayTotal, currencySymbol, language = language)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        fontSize = 12.5.sp
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isPayable) ExpenseRed.copy(alpha = 0.12f) else IncomeGreen.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPayable) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (isPayable) ExpenseRed else IncomeGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPayable) (if (isBn) "ধার নেওয়া" else "Borrowed") else (if (isBn) "ধার দেওয়া" else "Lent"),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        color = if (isPayable) ExpenseRed else IncomeGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { (paidPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (displayIsSettled) IncomeGreen else debtColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Paid vs Remaining Amounts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = CurrencyFormatter.format(displayPaid, currencySymbol, language = language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBn) "পরিশোধিত ${CurrencyFormatter.convertToBanglaDigits(paidPct.toString())}%" else "Paid $paidPct%",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.format(displayRemaining, currencySymbol, language = language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (displayIsSettled) IncomeGreen else if (isPayable) ExpenseRed else IncomeGreen
                                )
                                Text(
                                    text = if (displayIsSettled) (if (isBn) "সম্পূর্ণ পরিশোধিত" else "Fully Settled") else (if (isBn) "বাকি আছে" else "Remaining"),
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // Meta details: Dates, Bound Wallet, Balance status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val rawCreated = if (debt.createdDateStr.isNotBlank()) debt.createdDateStr
                            else SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(debt.createdAt))
                            val createdStr = if (isBn) CurrencyFormatter.convertToBanglaDigits(rawCreated) else rawCreated

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${if (isBn) "শুরু" else "Start"}: $createdStr",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            if (debt.tentativeCloseDateStr.isNotBlank()) {
                                Text(
                                    text = "${if (isBn) "মেয়াদ" else "Due"}: ${debt.tentativeCloseDateStr}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (debt.bindToWallet && boundAccount != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wallet,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = boundAccount.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Action Buttons: Add Interest (when unsettled) & Borrow/Lend More
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!displayIsSettled) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showAddInterestDialog = true }
                                        .testTag("debt_add_interest_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = "Add Interest",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "সুদ যোগ" else "Add Interest",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            val borrowMoreLabel = if (isPayable) {
                                if (isBn) "আরও ঋণ গ্রহণ" else "Borrow More"
                            } else {
                                if (isBn) "আরও ঋণ প্রদান" else "Lend More"
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.openAddDebtTransaction(debt, if (isPayable) "Borrow More" else "Lend More")
                                    }
                                    .testTag("debt_borrow_or_lend_more_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = borrowMoreLabel,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = borrowMoreLabel,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transactions & History Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "ঋণের ইতিহাস ও লেনদেন" else "Debt History",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "${historyItems.size}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Render Chronological History Items (Filtered specifically for this debt ID)
            items(historyItems, key = { it.id }) { item ->
                CompactDebtHistoryRow(
                    item = item,
                    currencySymbol = currencySymbol,
                    language = language,
                    onClick = { selectedHistoryItemForDetails = item }
                )
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // Hishab Pati Bento Bottom Sheet for Debt History Item Details & Editing
    selectedHistoryItemForDetails?.let { item ->
        val sheetBg = if (isDark) Color(0xFF141824) else Color(0xFFF8FAFC)
        val bentoCardBg = if (isDark) Color(0xFF1E2433).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.92f)
        val bentoBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x186366F1)
        val accentColor = if (item.isPositive) IncomeGreen else ExpenseRed
        val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        val primaryTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        val dividerColor = if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFE2E8F0)
        val walletDotColor = remember(item.accountColorHex, isDark) {
            item.accountColorHex?.let { hex ->
                try {
                    Color(android.graphics.Color.parseColor(hex))
                } catch (_: Exception) {
                    null
                }
            } ?: if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        }
        val hasNote = item.subtitle.isNotBlank() && item.subtitle != item.title

        ModalBottomSheet(
            onDismissRequest = { selectedHistoryItemForDetails = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = sheetBg,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 6.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.22f) else Color(0xFFCBD5E1))
                )
            }
        ) {
            DisableSheetForceDark()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 6.dp, bottom = 22.dp)
                    .testTag("debt_history_detail_sheet")
            ) {
                // Header & Hero Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = accentColor.copy(alpha = if (isDark) 0.18f else 0.13f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.30f else 0.22f)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (item.isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = item.title,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = primaryTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("debt_history_sheet_title")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (item.isPositive) "+" else "-"} ${CurrencyFormatter.format(item.amount, currencySymbol, language = language)}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = accentColor,
                            modifier = Modifier.testTag("debt_history_sheet_amount")
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    SheetHeaderCircleActionButton(
                        icon = CategorySelectionSheetStyle.HighContrastCloseIcon,
                        contentDescription = AppStrings.t("close", language),
                        isDark = isDark,
                        onClick = { selectedHistoryItemForDetails = null },
                        testTag = "debt_history_sheet_close_button"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Clean Bento Info Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = bentoCardBg,
                    border = BorderStroke(1.dp, bentoBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_history_sheet_bento_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppStrings.t("date", language),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.5.sp
                                ),
                                color = secondaryTextColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.dateStr,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = primaryTextColor,
                                textAlign = TextAlign.End,
                                modifier = Modifier.testTag("debt_history_sheet_date")
                            )
                        }

                        HorizontalDivider(color = dividerColor, thickness = 0.75.dp)

                        // Wallet / Account Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppStrings.t("wallet", language),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.5.sp
                                ),
                                color = secondaryTextColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(walletDotColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Wallet,
                                    contentDescription = null,
                                    tint = secondaryTextColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = item.accountName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    ),
                                    color = primaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("debt_history_sheet_wallet")
                                )
                            }
                        }

                        // Note / Details Row (multi-line wrapped, guaranteed spacing)
                        if (hasNote) {
                            HorizontalDivider(color = dividerColor, thickness = 0.75.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = AppStrings.t("note_details", language),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp
                                    ),
                                    color = secondaryTextColor,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp
                                    ),
                                    color = primaryTextColor,
                                    textAlign = TextAlign.End,
                                    softWrap = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("debt_history_sheet_details")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons Row (48.dp pill buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val historyItem = item
                            selectedHistoryItemForDetails = null
                            itemToDelete = historyItem
                        },
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = if (isDark) 0.35f else 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ExpenseRed.copy(alpha = if (isDark) 0.18f else 0.11f),
                            contentColor = ExpenseRed
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("delete_debt_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = AppStrings.t("delete", language),
                            tint = ExpenseRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppStrings.t("delete", language),
                            color = ExpenseRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }

                    Button(
                        onClick = {
                            val historyItem = item
                            selectedHistoryItemForDetails = null
                            val existingTx = allTransactions.find { it.id == historyItem.id }
                            if (existingTx != null) {
                                viewModel.transactionToEdit.value = existingTx
                                viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
                            } else {
                                val isPayableDebt = debt.type == DebtType.BORROWED
                                val fallbackTx = TransactionEntity(
                                    id = historyItem.id,
                                    type = if (isPayableDebt) TransactionType.BORROW else TransactionType.LEND,
                                    amount = historyItem.amount,
                                    title = historyItem.title,
                                    category = if (isPayableDebt) "Debt / Borrow" else "Debt / Lend",
                                    subcategory = "Initial Debt",
                                    accountId = if (debt.bindToWallet) debt.accountId else "",
                                    timestamp = debt.createdAt,
                                    notes = debt.notes,
                                    payeeOrPayer = debt.personName,
                                    debtId = debt.id
                                )
                                viewModel.transactionToEdit.value = fallbackTx
                                viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("edit_debt_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = AppStrings.t("edit", language),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppStrings.t("edit", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }
                }
            }
        }
    }

    // Delete Single Transaction Confirmation Dialog
    itemToDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(if (isBn) "লেনদেন মুছবেন?" else "Delete Transaction?") },
            text = {
                Text(
                    if (isBn) "এই লেনদেনটি মুছে ফেলা হলে ঋণের মোট হিসাব ও ওয়ালেটের ব্যালেন্স আপডেট হবে।"
                    else "Deleting this transaction will update the debt total and wallet balance accordingly."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = toDelete.id
                        itemToDelete = null
                        viewModel.deleteTransaction(id)
                    }
                ) {
                    Text(
                        text = if (isBn) "মুছে ফেলুন" else "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Add Interest Dialog
    if (showAddInterestDialog) {
        var interestAmtStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddInterestDialog = false },
            title = { Text(if (isBn) "সুদ যুক্ত করুন" else "Add Interest to Debt") },
            text = {
                Column {
                    Text(
                        text = if (isBn) "মোট ঋণের সাথে যুক্ত করতে সুদের পরিমাণ লিখুন।"
                        else "Enter interest amount to add to total debt balance.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = interestAmtStr,
                        onValueChange = { interestAmtStr = it },
                        label = { Text(if (isBn) "সুদের পরিমাণ ($currencySymbol)" else "Interest Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = interestAmtStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.addDebtInterest(debt.id, amt)
                        }
                        showAddInterestDialog = false
                    }
                ) {
                    Text(if (isBn) "যোগ করুন" else "Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddInterestDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(if (isBn) "ঋণ মুছে ফেলবেন?" else "Delete Debt?") },
            text = {
                Text(
                    if (isBn) "এই ঋণের সকল তথ্য ও সম্পর্কিত লেনদেন মুছে ফেলা হবে।"
                    else "This debt record and any linked transactions will be removed."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteDebt(debt.id)
                        onBack()
                    }
                ) {
                    Text(if (isBn) "মুছে ফেলুন" else "Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun CompactDebtHistoryRow(
    item: DebtHistoryItem,
    currencySymbol: String,
    language: AppLanguage,
    onClick: () -> Unit
) {
    TranslucentCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_history_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (item.isPositive) IncomeGreen.copy(alpha = 0.12f) else ExpenseRed.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (item.isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (item.isPositive) IncomeGreen else ExpenseRed,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(11.dp))

            // Left: Transaction name on top, Date directly underneath
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.dateStr,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Amount aligned to right, Wallet name directly underneath
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (item.isPositive) "+" else "-"} ${CurrencyFormatter.format(item.amount, currencySymbol, language = language)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (item.isPositive) IncomeGreen else ExpenseRed,
                        fontSize = 14.5.sp,
                        fontFeatureSettings = "tnum"
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.accountName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
