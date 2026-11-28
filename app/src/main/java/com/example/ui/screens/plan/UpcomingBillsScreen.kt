package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.DebtType
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.util.CurrencyFormatter
import kotlin.math.abs

@Composable
fun UpcomingBillsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val debts by viewModel.debts.collectAsState(initial = emptyList())
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val isDark = LocalIsDarkTheme.current ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)

    val scrollState = rememberLazyListState()
    val glassLayout = rememberScrollAwareGlassLayout(scrollState)

    // Filter credit cards with bills / dues
    val creditCardBills = remember(accounts, isBn) {
        accounts.filter {
            !it.isHidden &&
                it.type == AccountType.CREDIT_CARD &&
                (it.paymentDueDate.isNotBlank() || it.balance < 0.0 || it.minPayment > 0.0)
        }.map { cc ->
            val outstanding = if (cc.balance < 0.0) abs(cc.balance) else cc.minPayment
            Triple(cc, outstanding, if (cc.paymentDueDate.isNotBlank()) cc.paymentDueDate else (if (isBn) "পরিশোধযোগ্য" else "Due soon"))
        }
    }

    // Filter active borrowed debts (to pay)
    val borrowedDebts = remember(debts, isBn) {
        debts.filter { it.type == DebtType.BORROWED && !it.isSettled && it.amount > 0.0 }
            .map { debt ->
                val remaining = calculateDebtRemaining(debt)
                val dueStr = debt.tentativeCloseDateStr.ifBlank { debt.dueDate }
                Triple(debt, remaining, dueStr.ifBlank { if (isBn) "নির্ধারিত তারিখ নেই" else "No due date" })
            }
    }

    val totalCcAmount = creditCardBills.sumOf { it.second }
    val totalDebtAmount = borrowedDebts.sumOf { it.second }
    val grandTotal = totalCcAmount + totalDebtAmount

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .recordFrostedBackdrop(glassLayout.backdropState),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = glassLayout.contentPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Top Summary Bento Box
                item(key = "summary_bento") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color.White,
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = if (isBn) "মোট আসন্ন পরিশোধযোগ্য" else "Total Upcoming Payable",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.format(grandTotal, currencySymbol, false, language),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) "ক্রেডিট কার্ড বিল" else "Credit Cards",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyFormatter.format(totalCcAmount, currencySymbol, false, language),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = if (isBn) "ধার নেওয়া (পরিশোধযোগ্য)" else "Borrowed Dues",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyFormatter.format(totalDebtAmount, currencySymbol, false, language),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Bento Box 1 — Upcoming Credit Card Bills
                item(key = "section_cc_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBn) "আসন্ন ক্রেডিট কার্ড বিল" else "Upcoming Credit Card Bills",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                            )
                        )
                        Text(
                            text = "${creditCardBills.size}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (creditCardBills.isEmpty()) {
                    item(key = "cc_empty") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color.White,
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isBn) "কোনো আসন্ন ক্রেডিট কার্ড বিল নেই" else "No upcoming credit card bills",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                } else {
                    items(creditCardBills, key = { "cc_${it.first.id}" }) { (cc, amount, dueText) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.openAccountDetail(cc.id) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color.White,
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF0369A1).copy(alpha = 0.3f) else Color(0xFFE0F2FE),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cc.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isBn) "বিল তারিখ: $dueText" else "Due: $dueText",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.format(amount, currencySymbol, false, language),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF64748B) else Color(0xFFCBD5E1),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Bento Box 2 — Borrowed (To Pay)
                item(key = "section_borrowed_header") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBn) "ধার নেওয়া (পরিশোধযোগ্য)" else "Borrowed (To Pay)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                            )
                        )
                        Text(
                            text = if (isBn) "পরিচালনা" else "Manage",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.openSubScreen(SubScreen.MANAGE_DEBTS) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (borrowedDebts.isEmpty()) {
                    item(key = "borrowed_empty") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color.White,
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isBn) "কোনো পরিশোধযোগ্য ঋণ নেই" else "No active borrowed dues",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                } else {
                    items(borrowedDebts, key = { "debt_${it.first.id}" }) { (debt, remaining, dueText) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.openSubScreen(SubScreen.MANAGE_DEBTS) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color.White,
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF9F1239).copy(alpha = 0.3f) else Color(0xFFFFE4E6),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = debt.personName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isBn) "তারিখ: $dueText" else "Due: $dueText",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.format(remaining, currencySymbol, false, language),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isBn) "পরিশোধ করুন" else "Payable",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fixed Frosted Top Header
            ScrollAwareFixedHeader(
                layoutState = glassLayout,
                testTag = "upcoming_bills_fixed_header"
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
                            modifier = Modifier.size(40.dp),
                            onClick = onBack
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isBn) "আসন্ন বিলসমূহ" else "Upcoming Bills",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                )
                            )
                            Text(
                                text = if (isBn) "ক্রেডিট কার্ড ও ধার নেওয়া" else "Credit Cards & Borrowed",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
