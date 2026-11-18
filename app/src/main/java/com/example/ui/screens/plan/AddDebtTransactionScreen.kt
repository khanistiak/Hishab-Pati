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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.DebtType
import com.example.ui.MainViewModel
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddDebtTransactionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDebt by viewModel.selectedDebtForTransaction.collectAsState()
    val txType by viewModel.debtTransactionType.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val currencySymbol = settings?.currencySymbol ?: "৳"

    val debt = selectedDebt ?: viewModel.selectedDebt.value
    if (debt == null) {
        onBack()
        return
    }

    val isPayable = debt.type == DebtType.BORROWED
    val initial = debt.personName.trim().take(1).uppercase(Locale.US).ifEmpty { "?" }
    val remainingAmount = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0.0)

    val defaultTxName = when (txType) {
        "Borrow More" -> if (language == AppLanguage.BANGLA) "আরও ঋণ গ্রহণ" else "Borrowed More"
        "Lend More" -> if (language == AppLanguage.BANGLA) "আরও ঋণ প্রদান" else "Lent More"
        "Add Interest" -> if (language == AppLanguage.BANGLA) "সুদ" else "Interest"
        else -> if (isPayable) (if (language == AppLanguage.BANGLA) "ঋণ পরিশোধ" else "Debt Repayment")
                else (if (language == AppLanguage.BANGLA) "আদায় গ্রহণ" else "Payment Received")
    }
    var transactionName by remember { mutableStateOf(defaultTxName) }
    var amountInput by remember { mutableStateOf("") }
    var selectedAccount by remember {
        val initialAcc = if (debt.bindToWallet && debt.accountId.isNotBlank()) {
            accounts.firstOrNull { it.id == debt.accountId }
        } else {
            accounts.firstOrNull()
        }
        mutableStateOf(initialAcc)
    }
    var notesInput by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }

    val dateStr = remember {
        SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())
    }
    val paidPct = if (debt.totalAmount > 0) ((debt.paidAmount / debt.totalAmount) * 100).toInt().coerceIn(0, 100) else 0

    Scaffold(
        containerColor = Color.Transparent,
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
                    text = "Add Transaction",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            // Done Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                color = Color.Transparent
            ) {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.saveDebtTransaction(
                                debt = debt,
                                type = txType,
                                amount = amt,
                                account = selectedAccount,
                                notes = notesInput,
                                tags = tagsInput,
                                customTitle = transactionName
                            )
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("debt_tx_done_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = AppStrings.t("done", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        val listState = viewModel.rememberScreenLazyListState(
            subKey = "${debt.id}_$txType",
            screenOverride = com.example.ui.SubScreen.ADD_DEBT_TRANSACTION
        )
        LazyColumn(
            state = listState,
            modifier = modifier
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
                            // Initial Avatar
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE0E7FF),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initial,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = debt.personName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Total Amount: ${CurrencyFormatter.format(debt.totalAmount, currencySymbol, language = language)}",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }

                            // Badge: Payable / Receivable
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isPayable) "↓ Borrowed" else "↑ Lent",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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

                        // Paid vs Remaining
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = CurrencyFormatter.format(debt.paidAmount, currencySymbol, language = language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Paid $paidPct%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.format(remainingAmount, currencySymbol, language = language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = ExpenseRed
                                )
                                Text(
                                    text = "Remaining",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }

            // Transaction Type Card
            item {
                Spacer(modifier = Modifier.height(10.dp))
                FieldCard(label = "Transaction Type") {
                    Text(
                        text = txType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Date Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                FieldCard(label = "Date") {
                    Text(
                        text = dateStr,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Name Field Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                FieldCard(label = "Name") {
                    OutlinedTextField(
                        value = transactionName,
                        onValueChange = { transactionName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        placeholder = { Text("My Transaction") }
                    )
                }
            }

            // Wallet Section (for cash transactions)
            if (txType != "Add Interest") {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wallet",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "View all",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(accounts) { acc ->
                            val isSelected = selectedAccount?.id == acc.id
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) {
                                    if (acc.name.contains("Cash", ignoreCase = true)) Color(0xFFD1FAE5) else Color(0xFFDBEAFE)
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedAccount = acc }
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
                                        contentDescription = null,
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
                                            maxLines = 1
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(acc.balance, currencySymbol, language = language),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Amount Card
            item {
                Spacer(modifier = Modifier.height(14.dp))
                FieldCard(label = "Amount") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currencySymbol,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("debt_tx_amount_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            placeholder = { Text("0.00", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            // Other Notes Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                FieldCard(label = "Other Notes") {
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        placeholder = { Text("Insert your additional notes here") }
                    )
                }
            }



            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun FieldCard(
    label: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}
