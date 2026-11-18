package com.example.ui.screens.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.ui.components.CategoryIcon
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter

/**
 * Horizontally scrollable Wallet/Account selector row with "View Wallet" action.
 */
@Composable
fun WalletRowSelector(
    label: String,
    selectedAccountId: String,
    accounts: List<AccountEntity>,
    currencySymbol: String,
    onSelectAccount: (AccountEntity) -> Unit,
    onViewAllWallets: () -> Unit,
    modifier: Modifier = Modifier,
    excludedAccountId: String? = null,
    testTagPrefix: String = "wallet"
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header: Label + "View Wallet" Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            // "View Wallet" Action Button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onViewAllWallets)
                    .testTag("${testTagPrefix}_view_wallet_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (CurrencyFormatter.currentLanguage == com.example.data.model.AppLanguage.BANGLA) "ওয়ালেট দেখুন" else "View Wallet",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val activeAccounts = accounts.filter { !it.isHidden }

        if (activeAccounts.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (CurrencyFormatter.currentLanguage == com.example.data.model.AppLanguage.BANGLA) "কোন সক্রিয় ওয়ালেট নেই। প্রথমে একটি ওয়ালেট তৈরি করুন।" else "No wallets available. Create a wallet first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Horizontal swipeable row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                items(activeAccounts, key = { it.id }) { acc ->
                    val isSelected = acc.id == selectedAccountId
                    val isExcluded = excludedAccountId != null && acc.id == excludedAccountId

                    WalletCardItem(
                        account = acc,
                        currencySymbol = currencySymbol,
                        isSelected = isSelected,
                        isExcluded = isExcluded,
                        onClick = {
                            if (!isExcluded) {
                                onSelectAccount(acc)
                            }
                        },
                        testTag = "${testTagPrefix}_item_${acc.id}"
                    )
                }
            }
        }
    }
}

/**
 * Individual wallet card in the horizontal swipeable row.
 */
@Composable
private fun WalletCardItem(
    account: AccountEntity,
    currencySymbol: String,
    isSelected: Boolean,
    isExcluded: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val walletColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = when {
                isSelected -> walletColor
                isExcluded -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            }
        ),
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = !isExcluded, onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        isSelected -> walletColor.copy(alpha = 0.12f)
                        isExcluded -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    }
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryIcon(
                    iconName = account.iconName,
                    colorHex = account.colorHex,
                    size = 34.dp
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(walletColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else if (isExcluded) {
                    Text(
                        text = if (CurrencyFormatter.currentLanguage == com.example.data.model.AppLanguage.BANGLA) "উৎস" else "Source",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = account.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isExcluded) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = CurrencyFormatter.format(account.balance, currencySymbol),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = when {
                        isExcluded -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        account.balance >= 0 -> IncomeGreen
                        else -> ExpenseRed
                    }
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Clean, modern bottom sheet for viewing and selecting from all available wallets,
 * styled consistently with the Edit Budget category picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSelectionBottomSheet(
    title: String = "Select Wallet",
    selectedAccountId: String,
    accounts: List<AccountEntity>,
    currencySymbol: String,
    onSelectAccount: (AccountEntity) -> Unit,
    onDismiss: () -> Unit,
    excludedAccountId: String? = null,
    onAddAccount: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeAccounts = accounts.filter { !it.isHidden }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBgUnselected = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val cardBgSelected = if (isDark) Color(0xFF0F253E) else Color(0xFFEFF6FF)
    val borderUnselected = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val buttonBg = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
    val accentBlue = Color(0xFF0284C7)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
            )
        }
    ) {
        DisableSheetForceDark()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .testTag("wallet_selection_sheet")
        ) {
            // Header Row: Title & Subtitle + High-Contrast Circular Plus (+) and Close (×) Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = if (CurrencyFormatter.currentLanguage == com.example.data.model.AppLanguage.BANGLA)
                            "এই লেনদেনের জন্য একটি অ্যাকাউন্ট নির্বাচন করুন"
                        else
                            "Choose an account for this transaction",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onAddAccount != null) {
                        SheetHeaderCircleActionButton(
                            onClick = onAddAccount,
                            icon = CategorySelectionSheetStyle.HighContrastPlusIcon,
                            contentDescription = "Add Account",
                            isDark = isDark,
                            testTag = "btn_add_wallet_account"
                        )
                    }

                    SheetHeaderCircleActionButton(
                        onClick = onDismiss,
                        icon = CategorySelectionSheetStyle.HighContrastCloseIcon,
                        contentDescription = "Close",
                        isDark = isDark,
                        testTag = "btn_close_wallet_sheet"
                    )
                }
            }

            if (activeAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (CurrencyFormatter.currentLanguage == com.example.data.model.AppLanguage.BANGLA)
                            "কোন সক্রিয় ওয়ালেট নেই"
                        else
                            "No wallets available",
                        fontSize = 14.sp,
                        color = textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("wallet_selection_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp)
                ) {
                    items(activeAccounts, key = { it.id }) { acc ->
                        val isSelected = acc.id == selectedAccountId
                        val isExcluded = excludedAccountId != null && acc.id == excludedAccountId
                        val rowShape = RoundedCornerShape(14.dp)

                        Surface(
                            shape = rowShape,
                            color = when {
                                isExcluded -> if (isDark) cardBgUnselected.copy(alpha = 0.5f) else Color(0xFFF1F5F9).copy(alpha = 0.6f)
                                isSelected -> cardBgSelected
                                else -> cardBgUnselected
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = when {
                                    isExcluded -> borderUnselected.copy(alpha = 0.5f)
                                    isSelected -> accentBlue
                                    else -> borderUnselected
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(rowShape)
                                .clickable(enabled = !isExcluded) {
                                    onSelectAccount(acc)
                                    onDismiss()
                                }
                                .testTag("wallet_sheet_item_${acc.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIcon(
                                    iconName = acc.iconName,
                                    colorHex = acc.colorHex,
                                    size = 40.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = acc.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = if (isExcluded) textSecondary.copy(alpha = 0.6f) else textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val institutionText = when {
                                        isExcluded -> if (CurrencyFormatter.currentLanguage == com.example.data.model.AppLanguage.BANGLA)
                                            "উৎস ওয়ালেট হিসেবে নির্বাচিত"
                                        else
                                            "Selected as source wallet"
                                        acc.institution.isNotBlank() -> acc.institution
                                        else -> acc.type.name
                                    }

                                    Text(
                                        text = institutionText,
                                        fontSize = 12.sp,
                                        color = if (isExcluded) Color(0xFFEF4444) else textSecondary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.format(acc.balance, currencySymbol),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = when {
                                            isExcluded -> textSecondary.copy(alpha = 0.6f)
                                            acc.balance >= 0 -> IncomeGreen
                                            else -> ExpenseRed
                                        }
                                    )

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(accentBlue),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
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
}
