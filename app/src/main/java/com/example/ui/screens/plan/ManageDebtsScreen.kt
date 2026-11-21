package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DebtEntity
import com.example.data.model.AppLanguage
import com.example.data.model.DebtType
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.navigation.rememberNavigationScopeKey
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

fun isDebtAdditionTransaction(
    tx: com.example.data.local.entity.TransactionEntity,
    isPayable: Boolean
): Boolean {
    val isExplicitAddition = tx.type == com.example.data.model.TransactionType.BORROW ||
        tx.type == com.example.data.model.TransactionType.LEND ||
        tx.subcategory.equals("Initial Debt", ignoreCase = true) ||
        tx.subcategory.equals("Borrow More", ignoreCase = true) ||
        tx.subcategory.equals("Lend More", ignoreCase = true) ||
        tx.subcategory.equals("Interest", ignoreCase = true) ||
        tx.category.equals("Interest", ignoreCase = true) ||
        tx.category.equals("Debt / Borrow", ignoreCase = true) ||
        tx.category.equals("Debt / Lend", ignoreCase = true)
    if (isExplicitAddition) return true

    val isExplicitPayment = tx.subcategory.equals("Installment", ignoreCase = true) ||
        tx.subcategory.equals("Final Settlement", ignoreCase = true) ||
        tx.category.equals("Debt Repayment", ignoreCase = true) ||
        tx.category.equals("Debt Collection", ignoreCase = true) ||
        (if (isPayable) {
            tx.type == com.example.data.model.TransactionType.EXPENSE
        } else {
            tx.type == com.example.data.model.TransactionType.INCOME
        })
    return !isExplicitPayment
}

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
    val currentSubScreen by viewModel.currentSubScreen.collectAsState()
    BackHandler(enabled = currentSubScreen == com.example.ui.SubScreen.MANAGE_DEBTS, onBack = onBack)

    val debts by viewModel.debts.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val filterType by viewModel.debtFilterType.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val showSettledDebts by viewModel.showSettledDebts.collectAsState()

    val borrowedCount = remember(debts, showSettledDebts) {
        debts.count { it.type == DebtType.BORROWED && (showSettledDebts || !it.isSettled) }
    }
    val lentCount = remember(debts, showSettledDebts) {
        debts.count { it.type == DebtType.LENT && (showSettledDebts || !it.isSettled) }
    }
    val filteredDebts = remember(debts, filterType, showSettledDebts) {
        debts.filter { it.type == filterType && (showSettledDebts || !it.isSettled) }
    }
    val isPayable = filterType == DebtType.BORROWED

    // Totals exclude settled debts regardless of visibility toggle
    val totalBorrowed = remember(debts) {
        debts.filter { it.type == DebtType.BORROWED && !it.isSettled }
            .sumOf { calculateDebtRemaining(it) }
    }
    val totalLent = remember(debts) {
        debts.filter { it.type == DebtType.LENT && !it.isSettled }
            .sumOf { calculateDebtRemaining(it) }
    }
    val netDebtPosition = totalLent - totalBorrowed
    val totalOutstanding = if (isPayable) totalBorrowed else totalLent

    // Keep a single stable LazyListState so swapping Borrowed/Lent tabs never swaps
    // LazyColumn's state instance (which can stall recomposition of LazyColumn items),
    // while still persisting/restoring per-tab scroll position in scrollStateStore.
    val navScopeKey = rememberNavigationScopeKey(
        viewModel = viewModel,
        screenOverride = com.example.ui.SubScreen.MANAGE_DEBTS
    )
    val tabScrollKey = "$navScopeKey|LAZY:${filterType.name}"
    val listState = remember(navScopeKey) {
        val initial = viewModel.scrollStateStore.getLazyPosition(tabScrollKey)
        LazyListState(
            firstVisibleItemIndex = initial.index,
            firstVisibleItemScrollOffset = initial.offset
        )
    }
    LaunchedEffect(listState, tabScrollKey) {
        val saved = viewModel.scrollStateStore.getLazyPosition(tabScrollKey)
        if (listState.firstVisibleItemIndex != saved.index ||
            listState.firstVisibleItemScrollOffset != saved.offset
        ) {
            listState.scrollToItem(saved.index, saved.offset)
        }
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                viewModel.scrollStateStore.saveLazyPosition(tabScrollKey, index, offset)
            }
    }
    DisposableEffect(listState, tabScrollKey) {
        onDispose {
            viewModel.scrollStateStore.saveLazyPosition(
                tabScrollKey,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }
    }
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 6.dp
    )

    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF090D16),
                    Color(0xFF111827),
                    Color(0xFF0B132B)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFEAF4F8),
                    Color(0xFFF4F2FA)
                )
            )
        }
    }

    val cardColor = if (isDark) CardBackgroundDark else MaterialTheme.colorScheme.surfaceContainerLowest
    val cardBorderColor = if (isDark) Color(0x22FFFFFF) else Color(0x146366F1)

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDebt(filterType) },
                containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2A303C),
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
                        contentDescription = if (isBn) "নতুন ঋণ" else "Add Debt",
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
    ) { _ ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                // 1. Debt Overview Hero Bento Card
                item(key = "debt_overview_hero_card") {
                    DebtOverviewHeroCard(
                        netDebtPosition = netDebtPosition,
                        totalLent = totalLent,
                        totalBorrowed = totalBorrowed,
                        currencySymbol = currencySymbol,
                        language = language,
                        cardColor = cardColor,
                        borderColor = cardBorderColor,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 2. Crystal Liquid Segmented Tab Pill (Borrowed / Lent)
                item(key = "debt_type_selector") {
                    DebtSegmentedTabPill(
                        selectedType = filterType,
                        borrowedCount = borrowedCount,
                        lentCount = lentCount,
                        language = language,
                        isDark = isDark,
                        onSelect = { viewModel.debtFilterType.value = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 3. Section Header Row
                item(key = "debt_summary_bar_${filterType.name}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val countText = if (isBn) {
                                "${CurrencyFormatter.convertToBanglaDigits(filteredDebts.size.toString())} টি রেকর্ড"
                            } else {
                                "${filteredDebts.size} ${if (filteredDebts.size == 1) "record" else "records"}"
                            }
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                modifier = Modifier.testTag("debt_summary_count")
                            )
                            if (showSettledDebts) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = IncomeGreen.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = AppStrings.t("incl_settled", language),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        val totalLabel = if (isPayable) {
                            "${AppStrings.t("total_borrowed", language)}: "
                        } else {
                            "${AppStrings.t("total_lent", language)}: "
                        }
                        Text(
                            text = "$totalLabel${CurrencyFormatter.format(totalOutstanding, currencySymbol, language = language)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isPayable) ExpenseRed else IncomeGreen,
                                fontSize = 12.5.sp
                            ),
                            modifier = Modifier.testTag("debt_summary_total")
                        )
                    }
                }

                // 4. Debt Items or Empty State
                if (filteredDebts.isEmpty()) {
                    item(key = "empty_debts_${filterType.name}") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cardColor,
                            border = BorderStroke(1.dp, cardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = (if (isPayable) ExpenseRed else IncomeGreen).copy(alpha = 0.12f),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isPayable) ExpenseRed else IncomeGreen,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isPayable) {
                                        if (isBn) "কোনো ধার নেওয়া ঋণ নেই" else "No borrowed debts"
                                    } else {
                                        if (isBn) "কোনো ধার দেওয়া ঋণ নেই" else "No lent debts"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBn) {
                                        "নতুন ঋণ যোগ করতে নিচের '+ নতুন ঋণ' বাটনে চাপ দিন"
                                    } else {
                                        "Tap '+ Add Debt' below to track dues and repayments"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredDebts,
                        key = { "${filterType.name}_${it.id}" },
                        contentType = { "debt_card" }
                    ) { debt ->
                        val boundAcc = accounts.find { it.id == debt.accountId }
                        CompactDebtCard(
                            debt = debt,
                            boundAccountName = if (debt.bindToWallet) boundAcc?.name else null,
                            currencySymbol = currencySymbol,
                            language = language,
                            cardColor = cardColor,
                            borderColor = cardBorderColor,
                            onClick = { viewModel.openDebtDetails(debt) },
                            onRecordPayment = {
                                viewModel.openAddDebtTransaction(
                                    debt,
                                    if (debt.type == DebtType.BORROWED) "Record Payment" else "Receive Payment"
                                )
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item(key = "bottom_fab_spacer") {
                    Spacer(modifier = Modifier.height(84.dp))
                }
            }

            // Fixed scroll-aware frosted header matching Accounts and Calendar screens
            ScrollAwareFixedHeader(
                layoutState = scrollLayout,
                testTag = "manage_debts_fixed_header"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            } else {
                                Color.White.copy(alpha = 0.85f)
                            },
                            shadowElevation = if (isDark) 0.dp else 1.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("manage_debts_back_button"),
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

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = AppStrings.t("manage_debts", language),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Text(
                                text = AppStrings.t("borrowed_and_lent_tracking", language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    // Show Settled Debts Pill
                    Surface(
                        shape = CircleShape,
                        color = if (showSettledDebts) {
                            IncomeGreen.copy(alpha = 0.16f)
                        } else if (isDark) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        } else {
                            Color.White.copy(alpha = 0.85f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (showSettledDebts) IncomeGreen.copy(alpha = 0.4f) else cardBorderColor
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { viewModel.showSettledDebts.value = !showSettledDebts }
                            .testTag("toggle_show_settled_debts")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showSettledDebts) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                contentDescription = if (isBn) "পরিশোধিত ঋণ দেখান" else "Show Settled Debts",
                                tint = if (showSettledDebts) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isBn) "পরিশোধিত" else "Settled",
                                fontSize = 12.sp,
                                fontWeight = if (showSettledDebts) FontWeight.Bold else FontWeight.Medium,
                                color = if (showSettledDebts) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtOverviewHeroCard(
    netDebtPosition: Double,
    totalLent: Double,
    totalBorrowed: Double,
    currencySymbol: String,
    language: AppLanguage,
    cardColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val totalVolume = totalLent + totalBorrowed
    val lentFraction = if (totalVolume > 0.0) (totalLent / totalVolume).toFloat() else 0.5f
    val borrowedFraction = if (totalVolume > 0.0) (totalBorrowed / totalVolume).toFloat() else 0.5f

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardColor,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("debt_overview_hero_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = AppStrings.t("net_debt_position", language),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    letterSpacing = 0.9.sp,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = CurrencyFormatter.format(netDebtPosition, currencySymbol, language = language),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = when {
                        netDebtPosition > 0.001 -> IncomeGreen
                        netDebtPosition < -0.001 -> ExpenseRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Two-tone split progress bar comparing Total Lent (#10B981) vs Total Borrowed (#EF4444)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(lentFraction.coerceAtLeast(0.02f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (totalVolume > 0.0) IncomeGreen else IncomeGreen.copy(alpha = 0.25f))
                )
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .weight(borrowedFraction.coerceAtLeast(0.02f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (totalVolume > 0.0) ExpenseRed else ExpenseRed.copy(alpha = 0.25f))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom dot-indicator legend columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppStrings.t("lent_to_receive", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = CurrencyFormatter.format(totalLent, currencySymbol, language = language),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.5.sp
                        )
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppStrings.t("borrowed_to_pay", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = CurrencyFormatter.format(totalBorrowed, currencySymbol, language = language),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.5.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtSegmentedTabPill(
    selectedType: DebtType,
    borrowedCount: Int,
    lentCount: Int,
    language: AppLanguage,
    isDark: Boolean,
    onSelect: (DebtType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val selectedIndex = if (selectedType == DebtType.BORROWED) 0 else 1

    val trackColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
        Color.White.copy(alpha = 0.65f)
    }
    val activePillColor = if (isDark) CardBackgroundDark else Color.White

    Surface(
        shape = CircleShape,
        color = trackColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0x22FFFFFF) else Color(0x26FFFFFF)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val tabWidth = maxWidth / 2
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "debt_tab_pill_offset"
            )

            // Sliding white active pill
            Surface(
                shape = CircleShape,
                color = activePillColor,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
            ) {}

            Row(modifier = Modifier.fillMaxSize()) {
                val borrowedCountStr = if (isBn) {
                    CurrencyFormatter.convertToBanglaDigits(borrowedCount.toString())
                } else borrowedCount.toString()
                val lentCountStr = if (isBn) {
                    CurrencyFormatter.convertToBanglaDigits(lentCount.toString())
                } else lentCount.toString()

                // Borrowed Tab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onSelect(DebtType.BORROWED) }
                        .testTag("debt_type_borrowed")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (selectedIndex == 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "ধার নেওয়া ($borrowedCountStr)" else "Borrowed ($borrowedCountStr)",
                            fontWeight = if (selectedIndex == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp,
                            color = if (selectedIndex == 0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                            },
                            maxLines = 1
                        )
                    }
                }

                // Lent Tab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onSelect(DebtType.LENT) }
                        .testTag("debt_type_lent")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (selectedIndex == 1) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "ধার দেওয়া ($lentCountStr)" else "Lent ($lentCountStr)",
                            fontWeight = if (selectedIndex == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp,
                            color = if (selectedIndex == 1) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                            },
                            maxLines = 1
                        )
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
    language: AppLanguage = AppLanguage.ENGLISH,
    isBn: Boolean = language == AppLanguage.BANGLA,
    cardColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    borderColor: Color = Color(0x146366F1),
    onClick: () -> Unit,
    onRecordPayment: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isPayable = debt.type == DebtType.BORROWED
    val effectiveLanguage = if (isBn) AppLanguage.BANGLA else language

    val displayDate = if (debt.createdDateStr.isNotBlank()) {
        debt.createdDateStr
    } else {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        sdf.format(Date(debt.createdAt))
    }

    val remainingAmount = calculateDebtRemaining(debt)
    val totalOriginal = maxOf(
        debt.totalAmount,
        remainingAmount + debt.paidAmount,
        if (debt.isSettled) debt.amount else 0.0
    ).coerceAtLeast(0.0)
    val clearedAmount = if (debt.isSettled) {
        totalOriginal
    } else {
        (totalOriginal - remainingAmount).coerceAtLeast(debt.paidAmount)
    }
    val progressFraction = if (debt.isSettled) {
        1f
    } else if (totalOriginal > 0.0) {
        (clearedAmount / totalOriginal).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val paidPct = (progressFraction * 100f).roundToInt().coerceIn(0, 100)

    val accentTint = if (debt.isSettled) IncomeGreen else if (isPayable) ExpenseRed else IncomeGreen
    val displayAmount = if (debt.isSettled) totalOriginal else remainingAmount

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        shadowElevation = 1.5.dp,
        border = BorderStroke(1.dp, borderColor),
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("debt_item_${debt.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 42.dp tinted squircle icon container (12.dp radius)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentTint.copy(alpha = 0.13f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (debt.isSettled) {
                                Icons.Default.CheckCircle
                            } else if (isPayable) {
                                Icons.Default.ArrowDownward
                            } else {
                                Icons.Default.ArrowUpward
                            },
                            contentDescription = debt.personName,
                            tint = accentTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            debt.reason.isNotBlank() -> debt.reason
                            debt.notes.isNotBlank() -> debt.notes
                            isPayable -> if (isBn) "ধার নেওয়া টাকা" else "Borrowed money"
                            else -> if (isBn) "ধার দেওয়া টাকা" else "Lent money"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.format(displayAmount, currencySymbol, language = effectiveLanguage),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = accentTint
                        )
                    )
                    Text(
                        text = if (isBn) {
                            "মোট ${CurrencyFormatter.format(totalOriginal, currencySymbol, language = effectiveLanguage)}"
                        } else {
                            "of ${CurrencyFormatter.format(totalOriginal, currencySymbol, language = effectiveLanguage)}"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Repayment Progress Bar + Status Pill
            LinearProgressIndicator(
                progress = { progressFraction },
                color = accentTint,
                trackColor = accentTint.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pctStr = if (isBn) CurrencyFormatter.convertToBanglaDigits(paidPct.toString()) else paidPct.toString()
                val clearedFormatted = CurrencyFormatter.format(clearedAmount, currencySymbol, language = effectiveLanguage)
                Text(
                    text = if (isBn) {
                        "$pctStr% পরিশোধ • $clearedFormatted পরিশোধিত"
                    } else {
                        "$pctStr% paid • $clearedFormatted cleared"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                when {
                    debt.isSettled -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IncomeGreen.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = AppStrings.t("settled", effectiveLanguage),
                                color = IncomeGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                            )
                        }
                    }
                    debt.tentativeCloseDateStr.isNotBlank() -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Due: ${debt.tentativeCloseDateStr}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                            )
                        }
                    }
                    debt.dueDate.isNotBlank() -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Due: ${debt.dueDate}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Footer Action Row: Created Date (+ Wallet chip) & + Record Payment Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    )

                    if (!boundAccountName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (!debt.isSettled && onRecordPayment != null) {
                    Surface(
                        shape = CircleShape,
                        color = accentTint.copy(alpha = 0.13f),
                        onClick = onRecordPayment,
                        modifier = Modifier.testTag("record_payment_${debt.id}")
                    ) {
                        Text(
                            text = AppStrings.t("record_payment_short", effectiveLanguage),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentTint,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
