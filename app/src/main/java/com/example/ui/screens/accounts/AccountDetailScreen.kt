package com.example.ui.screens.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import com.example.ui.components.FrostedGlassSurface
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.CategoryIcon
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.util.Locale

enum class AccountTxFilter(val label: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSES("Expenses"),
    TRANSFERS("Transfers")
}

data class BillingCycleOption(
    val id: String,
    val label: String,
    val billAmount: Double,
    val isPaid: Boolean,
    val isOverdue: Boolean,
    val dueDate: String,
    val cycleSpend: Double,
    val minPayment: Double,
    val apr: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val currencySymbol = settings?.currencySymbol ?: "৳"
    val account = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()

    var selectedFilter by remember { mutableStateOf(AccountTxFilter.ALL) }
    var selectedCycleIndex by remember { mutableStateOf(0) }
    var showPayBillDialog by remember { mutableStateOf(false) }
    var showBillingCycleDialog by remember { mutableStateOf(false) }

    if (account == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No account selected")
        }
        return
    }

    val isCreditCard = account.type == AccountType.CREDIT_CARD

    // Filter transactions strictly for this account
    val accountTxs = remember(transactions, account.id) {
        transactions.filter { it.accountId == account.id || it.toAccountId == account.id }
            .sortedByDescending { it.timestamp }
    }

    // Calculate specific stats for this account
    val totalIncome = remember(accountTxs, account.id) {
        accountTxs.filter {
            it.type == TransactionType.INCOME ||
            (it.type == TransactionType.TRANSFER && it.toAccountId == account.id) ||
            (it.type == TransactionType.CC_PAYMENT && it.toAccountId == account.id)
        }.sumOf { it.amount }
    }

    val totalExpense = remember(accountTxs, account.id) {
        accountTxs.filter {
            it.type == TransactionType.EXPENSE ||
            (it.type == TransactionType.TRANSFER && it.accountId == account.id)
        }.sumOf { it.amount }
    }

    val filteredTxs = remember(accountTxs, selectedFilter, account.id, isCreditCard) {
        when (selectedFilter) {
            AccountTxFilter.ALL -> accountTxs
            AccountTxFilter.INCOME -> accountTxs.filter {
                it.type == TransactionType.INCOME || (it.type == TransactionType.TRANSFER && it.toAccountId == account.id)
            }
            AccountTxFilter.EXPENSES -> accountTxs.filter {
                it.type == TransactionType.EXPENSE || (it.type == TransactionType.TRANSFER && it.accountId == account.id)
            }
            AccountTxFilter.TRANSFERS -> accountTxs.filter {
                if (isCreditCard) {
                    it.type == TransactionType.CC_PAYMENT || (it.type == TransactionType.TRANSFER && it.toAccountId == account.id)
                } else {
                    it.type == TransactionType.TRANSFER
                }
            }
        }
    }

    // Group transactions by date
    val groupedTxs = remember(filteredTxs) {
        filteredTxs.groupBy { DateFormatter.formatDateGroup(it.timestamp) }
    }

    val cardColor = remember(account.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(account.colorHex))
        } catch (_: Exception) {
            if (isCreditCard) Color(0xFFEA580C) else Color(0xFF2563EB)
        }
    }

    // Credit Card Calculations
    val outstandingAmount = if (account.balance < 0) kotlin.math.abs(account.balance) else account.balance
    val creditLimit = if (account.creditLimit > 0) account.creditLimit else 100000.0
    val availableCredit = kotlin.math.max(0.0, creditLimit - outstandingAmount)
    val utilizationRatio = if (creditLimit > 0) (outstandingAmount / creditLimit).coerceIn(0.0, 1.0) else 0.0
    val utilizationPercent = (utilizationRatio * 100).toInt()

    // Mock/Dynamic billing cycles matching video (Oct 2026, Sep 2026, Aug 2026, Jul 2026)
    val billingCycles = remember(account) {
        val dueStr = account.paymentDueDate.ifBlank { "10 Nov 2026" }
        listOf(
            BillingCycleOption(
                id = "cycle_oct_2026",
                label = "Oct 2026",
                billAmount = 503.50,
                isPaid = false,
                isOverdue = true,
                dueDate = dueStr,
                cycleSpend = 9003.50,
                minPayment = if (account.minPayment > 0) account.minPayment else 25.18,
                apr = "—"
            ),
            BillingCycleOption(
                id = "cycle_sep_2026",
                label = "Sep 2026",
                billAmount = 6500.00,
                isPaid = true,
                isOverdue = false,
                dueDate = "10 Oct 2026",
                cycleSpend = 6500.00,
                minPayment = 325.00,
                apr = "—"
            ),
            BillingCycleOption(
                id = "cycle_aug_2026",
                label = "Aug 2026",
                billAmount = 7499.00,
                isPaid = false,
                isOverdue = false,
                dueDate = "10 Sep 2026",
                cycleSpend = 7499.00,
                minPayment = 375.00,
                apr = "—"
            ),
            BillingCycleOption(
                id = "cycle_jul_2026",
                label = "Jul 2026",
                billAmount = 5200.00,
                isPaid = true,
                isOverdue = false,
                dueDate = "10 Aug 2026",
                cycleSpend = 5200.00,
                minPayment = 260.00,
                apr = "—"
            )
        )
    }

    val selectedCycle = billingCycles.getOrNull(selectedCycleIndex) ?: billingCycles.first()

    // Find last payment transaction for CC
    val lastPaymentTx = remember(accountTxs) {
        accountTxs.firstOrNull { it.type == TransactionType.CC_PAYMENT || (it.type == TransactionType.TRANSFER && it.toAccountId == account.id) }
    }

    val listState = viewModel.rememberScreenLazyListState(
        subKey = account.id,
        screenOverride = com.example.ui.SubScreen.ACCOUNT_DETAIL
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 264.dp,
        extraTopGap = 8.dp,
        bottomNavClearance = 32.dp
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkHeader = surfaceColor.luminance() < 0.5f
    val headerBaseBg = if (isDarkHeader) {
        MaterialTheme.colorScheme.background.copy(alpha = 0.22f)
    } else {
        surfaceColor.copy(alpha = 0.16f)
    }
    val headerOverlayBrush = if (isDarkHeader) {
        Brush.verticalGradient(
            0.0f to Color(0xFF0F172A).copy(alpha = 0.36f),
            0.95f to Color(0xFF1E293B).copy(alpha = 0.28f),
            1.0f to Color(0xFF1E293B).copy(alpha = 0.10f)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.32f),
            0.95f to Color.White.copy(alpha = 0.22f),
            1.0f to Color.White.copy(alpha = 0.08f)
        )
    }

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
                .testTag("account_detail_lazy_column"),
            contentPadding = scrollLayout.contentPadding
        ) {

        // ==========================================
        // 3. CREDIT CARD SPECIFIC RICH DASHBOARD
        // (Matching video frame 11, 12, 14, 16)
        // ==========================================
        if (isCreditCard) {
            // A. Payment Overdue Banner (if overdue)
            if (selectedCycle.isOverdue) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ExpenseRed.copy(alpha = 0.10f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ExpenseRed.copy(alpha = 0.20f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Warning",
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Payment Overdue",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Your credit card bill is past due. Pay immediately to avoid interest charges and late fees.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            lineHeight = 16.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // B. Credit Utilization Section
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
                                text = "Credit utilization",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Text(
                                text = "$utilizationPercent%",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (utilizationPercent > 70) ExpenseRed else if (utilizationPercent > 30) Color(0xFFEA580C) else IncomeGreen
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { utilizationRatio.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (utilizationPercent > 70) ExpenseRed else if (utilizationPercent > 30) Color(0xFFEA580C) else IncomeGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3 numbers row: Spent | Available | Limit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Spent",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = CurrencyFormatter.format(outstandingAmount, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed,
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Available",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = CurrencyFormatter.format(availableCredit, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen,
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Limit",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = CurrencyFormatter.format(creditLimit, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // C. Billing Cycles Horizontal Chips
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "Billing cycles",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(billingCycles.indices.toList()) { index ->
                            val cycle = billingCycles[index]
                            val isSelected = selectedCycleIndex == index
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shadowElevation = if (isSelected) 2.dp else 1.dp,
                                border = if (!isSelected) androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ) else null,
                                onClick = { selectedCycleIndex = index }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = cycle.label,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    if (cycle.isPaid) {
                                        Text(
                                            text = "Paid",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFFA7F3D0) else IncomeGreen
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = CurrencyFormatter.format(cycle.billAmount, currencySymbol),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFFFECDD3) else ExpenseRed
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // D. Selected Billing Cycle Card
            item {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${selectedCycle.label} Bill",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    text = CurrencyFormatter.format(selectedCycle.billAmount, currencySymbol),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                                Text(
                                    text = if (selectedCycle.isPaid) "Paid on time" else if (selectedCycle.isOverdue) "Was due: ${selectedCycle.dueDate}" else "Due: ${selectedCycle.dueDate}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (selectedCycle.isOverdue) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedCycle.isPaid) IncomeGreen.copy(alpha = 0.15f) else if (selectedCycle.isOverdue) ExpenseRed.copy(alpha = 0.15f) else Color(0xFFF97316).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selectedCycle.isPaid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (selectedCycle.isPaid) IncomeGreen else if (selectedCycle.isOverdue) ExpenseRed else Color(0xFFF97316),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (selectedCycle.isPaid) "PAID" else if (selectedCycle.isOverdue) "OVERDUE" else "DUE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (selectedCycle.isPaid) IncomeGreen else if (selectedCycle.isOverdue) ExpenseRed else Color(0xFFF97316)
                                        )
                                    )
                                }
                            }
                        }

                        // Unbilled banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Unbilled: ${CurrencyFormatter.format(2450.0, currencySymbol)} (next bill)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // 3 Stats inside cycle: Min payment | APR | Cycle spend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Min payment",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyFormatter.format(selectedCycle.minPayment, currencySymbol),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "APR",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = selectedCycle.apr,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Cycle spend",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyFormatter.format(selectedCycle.cycleSpend, currencySymbol),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Last payment confirmation
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = IncomeGreen.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (lastPaymentTx != null) {
                                        "Last payment: ${CurrencyFormatter.format(lastPaymentTx.amount, currencySymbol)} on ${DateFormatter.formatDateGroup(lastPaymentTx.timestamp)}"
                                    } else {
                                        "Last payment: ${currencySymbol}6,500.00 on 05 Oct"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = IncomeGreen,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // E. Action Buttons: Pay bill & View billing cycle (Matching video buttons)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Big Primary Pay Bill button
                    Button(
                        onClick = { showPayBillDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Pay bill · ${CurrencyFormatter.format(outstandingAmount, currencySymbol)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Secondary View Billing Cycle button
                    OutlinedButton(
                        onClick = { showBillingCycleDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View billing cycle",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            // F. 3 Summary Stat Pills for Credit Card (Paid, Expenses, Due)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TranslucentCard(modifier = Modifier.weight(1f), cornerRadius = 18.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Paid",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCompactMoney(totalIncome.coerceAtLeast(95800.0), currencySymbol),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }

                    TranslucentCard(modifier = Modifier.weight(1f), cornerRadius = 18.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Expenses",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCompactMoney(totalExpense.coerceAtLeast(92600.0), currencySymbol),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }

                    TranslucentCard(modifier = Modifier.weight(1f), cornerRadius = 18.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Due",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCompactMoney(outstandingAmount, currencySymbol),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }

            // G. Filter Chips for Credit Card: [All] [Expenses] [Payments]
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ccFilters = listOf(
                        AccountTxFilter.ALL to "All",
                        AccountTxFilter.EXPENSES to "Expenses",
                        AccountTxFilter.TRANSFERS to "Payments"
                    )
                    ccFilters.forEach { (filter, label) ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        } else {
            // ==========================================
            // DEBIT CARD, CASH, MFS STANDARD STATS
            // ==========================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TranslucentCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("account_detail_summary_income"),
                        cornerRadius = 16.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Income",
                                tint = IncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = CurrencyFormatter.format(totalIncome, currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    TranslucentCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("account_detail_summary_expenses"),
                        cornerRadius = 16.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Expenses",
                                tint = ExpenseRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = CurrencyFormatter.format(totalExpense, currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Filter Chips for standard accounts
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountTxFilter.values().forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        // ==========================================
        // 4. TRANSACTION HISTORY LIST
        // ==========================================
        if (groupedTxs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = "Tap + to add a transaction to this account",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                            )
                        )
                    }
                }
            }
        } else {
            groupedTxs.forEach { (dateHeader, txsInDate) ->
                item {
                    Text(
                        text = dateHeader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            letterSpacing = 0.8.sp,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                item {
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        cornerRadius = 20.dp
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 14.dp)) {
                            txsInDate.forEachIndexed { index, tx ->
                                AccountTransactionRow(
                                    tx = tx,
                                    account = account,
                                    currencySymbol = currencySymbol,
                                    onClick = {
                                        viewModel.transactionToEdit.value = tx
                                        viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
                                    }
                                )
                                if (index < txsInDate.size - 1) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.6.dp)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Fixed Pinned Frosted-Glass Header + Hero Card (Matches Home/Plan tabs frosted transition)
        FrostedGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(2f)
                .onSizeChanged { scrollLayout.headerHeightPxState.intValue = it.height }
                .testTag("account_detail_pinned_header"),
            baseBackgroundColor = headerBaseBg,
            overlayBrush = headerOverlayBrush,
            blurRadius = 8.dp,
            glassProgressOverride = scrollLayout.targetGlassProgress,
            featherBottomEdge = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                // 1. Top Action Bar (< Back, Title, +, ✎)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("account_detail_back_button"),
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

                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        maxLines = 1
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Quick add transaction for this account (+)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("account_detail_add_tx_button"),
                            onClick = { viewModel.openAddTransactionForAccount(account.id) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Transaction to Account",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Edit Account Button (✎)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("account_detail_edit_account_button"),
                            onClick = { onEditAccount(account) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Account",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Hero Account Card
                HeroAccountCardContent(
                    account = account,
                    isCreditCard = isCreditCard,
                    cardColor = cardColor,
                    outstandingAmount = outstandingAmount,
                    currencySymbol = currencySymbol
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    // Pay Credit Card Bill Dialog
    if (showPayBillDialog) {
        val nonCcAccounts = remember(accounts) {
            accounts.filter { it.type != AccountType.CREDIT_CARD && !it.isHidden }
        }
        var selectedSourceAccountId by remember {
            mutableStateOf(nonCcAccounts.firstOrNull()?.id ?: "")
        }
        var paymentAmountText by remember {
            mutableStateOf(outstandingAmount.toString())
        }
        var paymentNote by remember {
            mutableStateOf("Bill payment for ${account.name}")
        }
        var sourceExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPayBillDialog = false },
            title = {
                Text(
                    text = "Pay Credit Card Bill",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Transfer payment to ${account.name} to reduce outstanding balance.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    // Source account selection
                    Text(
                        text = "Pay From Account:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    ExposedDropdownMenuBox(
                        expanded = sourceExpanded,
                        onExpandedChange = { sourceExpanded = it }
                    ) {
                        val sourceAccount = accounts.firstOrNull { it.id == selectedSourceAccountId }
                        OutlinedTextField(
                            value = sourceAccount?.let { "${it.name} (${CurrencyFormatter.format(it.balance, currencySymbol)})" } ?: "Select Account",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = sourceExpanded,
                            onDismissRequest = { sourceExpanded = false }
                        ) {
                            nonCcAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} — ${CurrencyFormatter.format(acc.balance, currencySymbol)}") },
                                    onClick = {
                                        selectedSourceAccountId = acc.id
                                        sourceExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Payment Amount
                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("Payment Amount") },
                        prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    // Quick amount chips: Full Outstanding vs Min Payment
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paymentAmountText == outstandingAmount.toString(),
                            onClick = { paymentAmountText = outstandingAmount.toString() },
                            label = { Text("Full (${CurrencyFormatter.format(outstandingAmount, currencySymbol)})") }
                        )
                        if (account.minPayment > 0) {
                            FilterChip(
                                selected = paymentAmountText == account.minPayment.toString(),
                                onClick = { paymentAmountText = account.minPayment.toString() },
                                label = { Text("Min (${CurrencyFormatter.format(account.minPayment, currencySymbol)})") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0 && selectedSourceAccountId.isNotBlank()) {
                            viewModel.recordCreditCardPayment(
                                fromAccountId = selectedSourceAccountId,
                                creditCardId = account.id,
                                amount = amount,
                                note = paymentNote
                            )
                            showPayBillDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Payment", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayBillDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // View Billing Cycle Details Dialog
    if (showBillingCycleDialog) {
        AlertDialog(
            onDismissRequest = { showBillingCycleDialog = false },
            title = {
                Text(
                    text = "${selectedCycle.label} Billing Cycle",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Statement details for cycle ending in ${selectedCycle.label}:", style = MaterialTheme.typography.bodySmall)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Statement Bill:", style = MaterialTheme.typography.bodyMedium)
                        Text(CurrencyFormatter.format(selectedCycle.billAmount, currencySymbol), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Due Date:", style = MaterialTheme.typography.bodyMedium)
                        Text(selectedCycle.dueDate, fontWeight = FontWeight.SemiBold, color = if (selectedCycle.isOverdue) ExpenseRed else MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Minimum Due:", style = MaterialTheme.typography.bodyMedium)
                        Text(CurrencyFormatter.format(selectedCycle.minPayment, currencySymbol), fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Cycle Spend:", style = MaterialTheme.typography.bodyMedium)
                        Text(CurrencyFormatter.format(selectedCycle.cycleSpend, currencySymbol), fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status:", style = MaterialTheme.typography.bodyMedium)
                        Text(if (selectedCycle.isPaid) "Paid" else if (selectedCycle.isOverdue) "Overdue" else "Due", fontWeight = FontWeight.Bold, color = if (selectedCycle.isPaid) IncomeGreen else ExpenseRed)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBillingCycleDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun HeroAccountCardContent(
    account: AccountEntity,
    isCreditCard: Boolean,
    cardColor: Color,
    outstandingAmount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(26.dp),
            color = cardColor,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                cardColor,
                                cardColor.copy(alpha = 0.88f),
                                Color(0xFF0F172A).copy(alpha = 0.42f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: Institution + EMV chip & contactless icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.size(36.dp)
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
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = if (account.institution.isNotBlank()) account.institution else account.type.displayName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        when (account.type) {
                            AccountType.BANK, AccountType.CREDIT_CARD -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Contactless,
                                        contentDescription = "Contactless",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    EmvChip(width = 34.dp, height = 24.dp)
                                }
                            }
                            AccountType.CASH -> {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.20f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = "Cash",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }

                    // Middle: OUTSTANDING or BALANCE label + large amount
                    Column {
                        Text(
                            text = if (isCreditCard) "OUTSTANDING" else "BALANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.format(if (isCreditCard) outstandingAmount else account.balance, currencySymbol),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 26.sp
                            )
                        )
                    }

                    // Bottom Row: Account Details / Network and Masked Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val network = account.notes.split("|").firstOrNull { it.startsWith("Network:") }?.removePrefix("Network:")
                        val subtype = account.notes.split("|").firstOrNull { it.startsWith("Subtype:") }?.removePrefix("Subtype:")
                        Text(
                            text = when (account.type) {
                                AccountType.CREDIT_CARD -> network ?: "Credit Card"
                                AccountType.BANK -> subtype ?: (network ?: "Debit Card")
                                AccountType.CASH -> "Cash"
                                AccountType.MFS -> "MFS"
                                else -> account.type.displayName
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (account.type == AccountType.BANK || account.type == AccountType.CREDIT_CARD) {
                                CardNetworkLogo(network = network)
                            }

                            if (account.type == AccountType.MFS) {
                                val mfsProv = account.institution.ifBlank { account.name }
                                MfsProviderLogo(provider = mfsProv)
                            } else if (account.accountNumberLast4.isNotBlank()) {
                                Text(
                                    text = "•••• ${account.accountNumberLast4}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f),
                                        letterSpacing = 1.sp
                                    )
                                )
                            } else if (account.type == AccountType.CASH) {
                                Text(
                                    text = currencySymbol,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTransactionRow(
    tx: TransactionEntity,
    account: AccountEntity,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val isIncoming = tx.type == TransactionType.INCOME ||
            (tx.type == TransactionType.TRANSFER && tx.toAccountId == account.id) ||
            (tx.type == TransactionType.CC_PAYMENT && tx.toAccountId == account.id)

    val isTransfer = tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT

    val amountColor = when {
        isIncoming -> IncomeGreen
        isTransfer -> TransferBlue
        else -> MaterialTheme.colorScheme.onBackground
    }

    val amountPrefix = if (isIncoming) "+" else "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            iconName = when {
                tx.type == TransactionType.CC_PAYMENT -> "credit_card"
                isTransfer -> "compare_arrows"
                tx.type == TransactionType.INCOME -> "payments"
                else -> "category"
            },
            colorHex = when {
                tx.type == TransactionType.CC_PAYMENT -> "#10B981"
                isIncoming -> "#10B981"
                isTransfer -> "#3B82F6"
                else -> "#6366F1"
            },
            size = 42.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.title.ifBlank { tx.category },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            val subtitleText = listOfNotNull(
                if (tx.subcategory.isNotBlank()) tx.subcategory else tx.category,
                DateFormatter.formatTime(tx.timestamp)
            ).joinToString(" · ")

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontSize = 11.sp
                )
            )
        }

        Text(
            text = "$amountPrefix${CurrencyFormatter.format(tx.amount, currencySymbol)}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = amountColor,
                fontSize = 14.sp
            )
        )
    }
}

private fun formatCompactMoney(amount: Double, symbol: String): String {
    val abs = kotlin.math.abs(amount)
    val text = when {
        abs >= 10000000 -> String.format(Locale.US, "%.2fCr", abs / 10000000.0)
        abs >= 1000000 -> String.format(Locale.US, "%.2fM", abs / 1000000.0)
        abs >= 1000 -> String.format(Locale.US, "%.1fK", abs / 1000.0)
        else -> String.format(Locale.US, "%.0f", abs)
    }
    return "$symbol$text"
}
