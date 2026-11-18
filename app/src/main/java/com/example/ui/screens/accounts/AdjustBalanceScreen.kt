package com.example.ui.screens.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountType
import com.example.ui.MainViewModel
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter
import kotlin.math.abs

@Composable
fun AdjustBalanceScreen(
    viewModel: MainViewModel,
    account: AccountEntity?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    if (account == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No wallet selected for balance adjustment.")
        }
        return
    }

    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currencySymbol ?: "৳"

    // Format current balance for display & editing
    val isCreditCard = account.type == AccountType.CREDIT_CARD
    val currentBalance = account.balance

    var amountInput by remember {
        val displayVal = if (isCreditCard) {
            val out = if (currentBalance < 0) abs(currentBalance) else currentBalance
            if (out % 1.0 == 0.0) out.toLong().toString() else out.toString()
        } else {
            if (currentBalance % 1.0 == 0.0) currentBalance.toLong().toString() else currentBalance.toString()
        }
        mutableStateOf(displayVal)
    }

    var recordAsTransaction by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Parse input
    val parsedAmount = amountInput.trim().toDoubleOrNull()
    val targetBalance = if (parsedAmount != null) {
        if (isCreditCard) -abs(parsedAmount) else parsedAmount
    } else null

    val diff = if (targetBalance != null) targetBalance - currentBalance else 0.0

    val cardColor = remember(account.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(account.colorHex))
        } catch (_: Exception) {
            Color(0xFF2563EB)
        }
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
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            if (parsedAmount == null || parsedAmount < 0) {
                                errorMessage = "Please enter a valid non-negative amount."
                                return@Button
                            }
                            isSaving = true
                            viewModel.adjustAccountBalance(
                                id = account.id,
                                newBalance = targetBalance ?: currentBalance,
                                recordAsTransaction = recordAsTransaction
                            )
                            onBack()
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_adjusted_balance_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isSaving) "Saving..." else "Save Adjustment",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val listState = viewModel.rememberScreenLazyListState(
            subKey = account?.id ?: "default",
            screenOverride = com.example.ui.SubScreen.ADJUST_BALANCE
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("adjust_balance_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header (Back, "Adjust Balance")
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(42.dp),
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

                    Text(
                        text = "Adjust Balance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }

            // 2. Wallet Summary Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            cardColor.copy(alpha = 0.12f),
                                            cardColor.copy(alpha = 0.04f)
                                        )
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = cardColor,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when (account.type) {
                                                    AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                                    AccountType.BANK -> Icons.Default.AccountBalance
                                                    AccountType.CASH -> Icons.Default.Payments
                                                    AccountType.MFS -> Icons.Default.PhoneAndroid
                                                    else -> Icons.Default.CreditCard
                                                },
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = account.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (account.institution.isNotBlank()) account.institution else account.type.name,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isCreditCard) "CURRENT OUTSTANDING" else "CURRENT BALANCE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.8.sp
                                        )
                                    )
                                    val displayCurrent = if (isCreditCard) abs(currentBalance) else currentBalance
                                    Text(
                                        text = CurrencyFormatter.format(displayCurrent, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Current Amount / Corrected Balance Section
            item {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    cornerRadius = 22.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isCreditCard) "Corrected Outstanding Amount" else "Corrected Balance",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        Text(
                            text = "Enter the actual balance currently in this wallet. The system will compute any discrepancy.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = {
                                amountInput = it
                                errorMessage = null
                            },
                            prefix = {
                                Text(
                                    text = "$currencySymbol ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            },
                            placeholder = { Text("0.00") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("adjust_balance_input"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            isError = errorMessage != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = ExpenseRed,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }

                        // Discrepancy / Difference indicator
                        if (parsedAmount != null && abs(diff) > 0.0001) {
                            val isPositiveDiff = diff > 0
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPositiveDiff) IncomeGreen.copy(alpha = 0.12f) else ExpenseRed.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isPositiveDiff) "Adjustment Difference (Increase)" else "Adjustment Difference (Decrease)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isPositiveDiff) IncomeGreen else ExpenseRed,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "${if (isPositiveDiff) "+" else "-"}${CurrencyFormatter.format(abs(diff), currencySymbol)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPositiveDiff) IncomeGreen else ExpenseRed
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Record Adjustment as Transaction Toggle
            item {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    cornerRadius = 22.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Record Adjustment as Transaction",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (recordAsTransaction) {
                                    "Creates an adjustment transaction so your cash flow and Activity screen accurately reflect this change."
                                } else {
                                    "Updates wallet balance directly without creating a transaction in your history."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Switch(
                            checked = recordAsTransaction,
                            onCheckedChange = { recordAsTransaction = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("record_adjustment_transaction_switch")
                        )
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
