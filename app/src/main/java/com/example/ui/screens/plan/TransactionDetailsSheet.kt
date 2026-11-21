package com.example.ui.screens.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIcon
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SheetRoyalIndigo = Color(0xFF4F46E5)
private val SheetElectricViolet = Color(0xFF7C3AED)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsSheet(
    transaction: TransactionEntity,
    budgetName: String,
    categoryIcon: String,
    categoryColorHex: String,
    currencySymbol: String,
    accounts: List<AccountEntity>,
    language: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val sheetBg = if (isDark) Color(0xFF0F1424) else Color(0xFFF6F7FE)
    val bentoCardColor = if (isDark) CardBackgroundDark else Color.White
    val bentoInnerColor = if (isDark) Color(0xFF161D31) else Color(0xFFF8F9FE)
    val bentoBorder = if (isDark) Color(0x28A5B4FC) else Color(0x1A6366F1)

    val account = remember(transaction.accountId, accounts) {
        accounts.firstOrNull { it.id == transaction.accountId }
    }

    val formattedDate = remember(transaction.timestamp, language) {
        val raw = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(transaction.timestamp))
        DateFormatter.localizeText(raw, language)
    }

    val accountTypeLabel = when (account?.type) {
        AccountType.CREDIT_CARD -> AppStrings.t("credit_card", language)
        AccountType.CASH -> AppStrings.t("cash", language)
        AccountType.BANK -> AppStrings.t("bank", language)
        AccountType.MFS -> AppStrings.t("mobile_banking", language)
        AccountType.OTHER -> AppStrings.t("other", language)
        null -> AppStrings.t("wallet", language)
    }

    val balanceLabel = if (account?.type == AccountType.CREDIT_CARD) {
        AppStrings.t("outstanding_balance", language)
    } else {
        AppStrings.t("current_balance", language)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("transaction_details_sheet")
        ) {
            // Header Row: Title & Royal Indigo-Violet Edit Pill Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.t("transaction_details", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(SheetRoyalIndigo, SheetElectricViolet)
                            )
                        )
                        .clickable {
                            onDismiss()
                            onEditTransaction(transaction)
                        }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                        .testTag("edit_transaction_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = AppStrings.t("edit", language),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppStrings.t("edit", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Transaction Identity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIcon(
                    iconName = categoryIcon,
                    colorHex = categoryColorHex,
                    size = 48.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title.ifBlank { transaction.category },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val subTitleText = buildString {
                        append(transaction.category)
                        if (transaction.subcategory.isNotBlank()) {
                            append(" • ")
                            append(transaction.subcategory)
                        }
                    }
                    Text(
                        text = subTitleText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SheetRoyalIndigo.copy(alpha = if (isDark) 0.22f else 0.10f)
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFC7D2FE) else SheetRoyalIndigo,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Card 1: Wallet Details Bento Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = bentoCardColor,
                border = BorderStroke(1.dp, bentoBorder),
                shadowElevation = if (isDark) 0.dp else 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = AppStrings.t("wallet_details", language),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = bentoInnerColor,
                        border = BorderStroke(1.dp, bentoBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SheetRoyalIndigo.copy(alpha = if (isDark) 0.24f else 0.12f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (account?.type) {
                                                AccountType.BANK -> Icons.Default.AccountBalance
                                                AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                                else -> Icons.Default.Payments
                                            },
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFFA5B4FC) else SheetRoyalIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = account?.name ?: AppStrings.t("cash", language),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SheetElectricViolet.copy(alpha = if (isDark) 0.22f else 0.12f)
                                        ) {
                                            Text(
                                                text = accountTypeLabel,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color(0xFFDDD6FE) else SheetElectricViolet,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "$balanceLabel: ${CurrencyFormatter.format(account?.balance ?: 0.0, currencySymbol, language = language)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (transaction.type == TransactionType.INCOME) {
                                    "+ ${CurrencyFormatter.format(transaction.amount, currencySymbol, language = language)}"
                                } else {
                                    "- ${CurrencyFormatter.format(transaction.amount, currencySymbol, language = language)}"
                                },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (transaction.type == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card 2: Affected Budgets Bento Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = bentoCardColor,
                border = BorderStroke(1.dp, bentoBorder),
                shadowElevation = if (isDark) 0.dp else 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = AppStrings.t("affected_budgets", language),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SheetRoyalIndigo.copy(alpha = if (isDark) 0.18f else 0.08f),
                        border = BorderStroke(1.dp, SheetRoyalIndigo.copy(alpha = if (isDark) 0.35f else 0.22f)),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            CategoryIcon(
                                iconName = categoryIcon,
                                colorHex = categoryColorHex,
                                size = 22.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = budgetName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Card 3: Notes (if present)
            if (transaction.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = bentoCardColor,
                    border = BorderStroke(1.dp, bentoBorder),
                    shadowElevation = if (isDark) 0.dp else 1.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = AppStrings.t("notes", language),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = transaction.notes,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
