package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DebtEntity
import com.example.data.model.AppLanguage
import com.example.data.model.DebtType
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.TranslucentCard
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Calculates the actual unpaid balance of a debt.
 * - Settled debts always return 0.0.
 * - Unsettled partially-paid debts return (totalAmount - paidAmount) or the updated remaining amount.
 * - Unsettled debts with no payments return the full amount.
 */
fun calculateDebtRemaining(debt: DebtEntity): Double {
    if (debt.isSettled) return 0.0
    val fromTotalAndPaid = if (debt.totalAmount > 0.0 && debt.paidAmount > 0.0) {
        (debt.totalAmount - debt.paidAmount).coerceAtLeast(0.0)
    } else {
        debt.amount.coerceAtLeast(0.0)
    }
    val remaining = if (debt.amount in 0.001..fromTotalAndPaid) debt.amount else fromTotalAndPaid
    return if (remaining <= 0.001) 0.0 else remaining
}

@Composable
fun ManageDebtsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val debts by viewModel.debts.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val filterType by viewModel.debtFilterType.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val isBn = settings?.language == AppLanguage.BANGLA

    val showSettledDebts by viewModel.showSettledDebts.collectAsState()

    val filteredDebts = debts.filter { it.type == filterType && (showSettledDebts || !it.isSettled) }
    val isPayable = filterType == DebtType.BORROWED

    // Calculate totals using unsettled debts only (excluding settled debts, even when visible in list)
    val totalBorrowed = debts
        .filter { it.type == DebtType.BORROWED && !it.isSettled }
        .sumOf { calculateDebtRemaining(it) }

    val totalLent = debts
        .filter { it.type == DebtType.LENT && !it.isSettled }
        .sumOf { calculateDebtRemaining(it) }

    val totalOutstanding = if (isPayable) totalBorrowed else totalLent

    val listState = viewModel.rememberScreenLazyListState(
        subKey = filterType.name,
        screenOverride = com.example.ui.SubScreen.MANAGE_DEBTS
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 4.dp
    )

    val backgroundBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFFE0F2FE), // Pastel cyan
                Color(0xFFEDE9FE), // Pastel lavender
                Color(0xFFF8FAFC)  // Clean soft slate
            )
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDebt(filterType) },
                containerColor = Color(0xFF2A303C),
                contentColor = Color.White,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("add_debt_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Debt",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "নতুন ঋণ" else "Add Debt",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    ) { scaffoldPadding ->
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
                        scrollLayout.backdropState,
                        listState,
                        scrollLayout.contentGraphicsLayer
                    )
                    .testTag("manage_debts_lazy_column"),
                contentPadding = scrollLayout.contentPadding
            ) {
                // 1. Segmented switch: Borrowed vs Lent
                item(key = "debt_type_selector") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Borrowed Pill
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isPayable) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    shadowElevation = if (isPayable) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { viewModel.debtFilterType.value = DebtType.BORROWED }
                                        .testTag("debt_type_borrowed")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Borrowed",
                                            tint = if (isPayable) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "ধার নেওয়া" else "Borrowed",
                                            fontWeight = if (isPayable) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.5.sp,
                                            color = if (isPayable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Lent Pill
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (!isPayable) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    shadowElevation = if (!isPayable) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { viewModel.debtFilterType.value = DebtType.LENT }
                                        .testTag("debt_type_lent")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Lent",
                                            tint = if (!isPayable) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "ধার দেওয়া" else "Lent",
                                            fontWeight = if (!isPayable) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.5.sp,
                                            color = if (!isPayable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Compact Summary Bar
                item(key = "debt_summary_bar") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isBn) {
                                    "${filteredDebts.size} টি নথিভুক্ত ঋণ"
                                } else {
                                    "${filteredDebts.size} ${if (filteredDebts.size == 1) "record" else "records"}"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            )
                            if (showSettledDebts) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = if (isBn) "পরিশোধিত সহ" else "incl. settled",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${if (isPayable) (if (isBn) "মোট ধার নেওয়া: " else "Total Borrowed: ") else (if (isBn) "মোট ধার দেওয়া: " else "Total Lent: ")}${CurrencyFormatter.format(totalOutstanding, currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isPayable) ExpenseRed else IncomeGreen,
                                fontSize = 12.5.sp
                            )
                        )
                    }
                }

                // 3. Debt Items or Empty State
                if (filteredDebts.isEmpty()) {
                    item(key = "empty_debts") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isPayable) (if (isBn) "কোনো ধার নেওয়া ঋণ নেই" else "No borrowed debts")
                                    else (if (isBn) "কোনো ধার দেওয়া ঋণ নেই" else "No lent debts"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBn) "নতুন ঋণ যোগ করতে নিচের '+ নতুন ঋণ' বাটনে চাপ দিন" else "Tap '+ Add Debt' below to track dues and loans",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    items(filteredDebts, key = { it.id }) { debt ->
                        val boundAcc = accounts.find { it.id == debt.accountId }
                        CompactDebtCard(
                            debt = debt,
                            boundAccountName = if (debt.bindToWallet) boundAcc?.name else null,
                            currencySymbol = currencySymbol,
                            isBn = isBn,
                            onClick = { viewModel.openDebtDetails(debt) }
                        )
                    }
                }

                item(key = "bottom_fab_spacer") {
                    Spacer(modifier = Modifier.height(84.dp))
                }
            }

            // Fixed scroll-aware frosted header matching Home and Plan tabs
            ScrollAwareFixedHeader(
                layoutState = scrollLayout,
                testTag = "manage_debts_fixed_header"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("manage_debts_back_button"),
                            onClick = onBack
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (isBn) "পেছনে" else "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = if (isBn) "ঋণ ব্যবস্থাপনা" else "Manage Debts",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Text(
                                text = if (isBn) "ধার নেওয়া ও ধার দেওয়া ট্র্যাকিং" else "Borrowed & Lent Tracking",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    // Redesigned "Show Settled Debts" Control
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (showSettledDebts) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (showSettledDebts) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.showSettledDebts.value = !showSettledDebts }
                            .testTag("toggle_show_settled_debts")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showSettledDebts) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                contentDescription = if (isBn) "পরিশোধিত ঋণ দেখান" else "Show Settled Debts",
                                tint = if (showSettledDebts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isBn) "পরিশোধিত" else "Settled",
                                fontSize = 12.sp,
                                fontWeight = if (showSettledDebts) FontWeight.Bold else FontWeight.Medium,
                                color = if (showSettledDebts) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactDebtCard(
    debt: DebtEntity,
    boundAccountName: String?,
    currencySymbol: String,
    isBn: Boolean = false,
    onClick: () -> Unit
) {
    val initial = debt.personName.trim().take(1).uppercase(Locale.US).ifEmpty { "?" }
    val isPayable = debt.type == DebtType.BORROWED

    val displayDate = if (debt.createdDateStr.isNotBlank()) {
        debt.createdDateStr
    } else {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        sdf.format(Date(debt.createdAt))
    }

    val debtColor = try {
        Color(android.graphics.Color.parseColor(debt.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val remainingAmount = calculateDebtRemaining(debt)
    val displayAmount = if (debt.isSettled) {
        if (debt.totalAmount > 0.0) debt.totalAmount else debt.amount
    } else {
        remainingAmount
    }

    TranslucentCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_item_${debt.id}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Avatar with initial letter
                Surface(
                    shape = CircleShape,
                    color = debtColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initial,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = debtColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(11.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (debt.reason.isNotBlank()) debt.reason
                        else if (debt.notes.isNotBlank()) debt.notes
                        else (if (isPayable) "Borrowed money" else "Lent money"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.format(displayAmount, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = if (debt.isSettled) IncomeGreen else if (isPayable) ExpenseRed else IncomeGreen
                        )
                    )
                    Text(
                        text = if (debt.isSettled) {
                            if (isBn) "পরিশোধিত" else "Paid"
                        } else if (debt.paidAmount > 0 && debt.totalAmount > 0) {
                            if (isPayable) {
                                if (isBn) "বাকি (${CurrencyFormatter.format(debt.paidAmount, currencySymbol)} দেওয়া)" else "Due (${CurrencyFormatter.format(debt.paidAmount, currencySymbol)} paid)"
                            } else {
                                if (isBn) "পাওনা (${CurrencyFormatter.format(debt.paidAmount, currencySymbol)} পাওয়া)" else "Remaining (${CurrencyFormatter.format(debt.paidAmount, currencySymbol)} paid)"
                            }
                        } else {
                            if (isPayable) (if (isBn) "বাকি আছে" else "Due") else (if (isBn) "পাওনা" else "Remaining")
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Badges Row: Date, Due date, Bound Wallet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 11.5.sp
                        )
                    )

                    if (!boundAccountName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = boundAccountName,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (debt.isSettled) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = if (isBn) "পরিশোধিত" else "Settled",
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                } else if (debt.tentativeCloseDateStr.isNotBlank()) {
                    Text(
                        text = "Due: ${debt.tentativeCloseDateStr}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}
