package com.example.ui.screens.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.data.model.getDisplayName
import com.example.ui.components.CategoryIcon
import com.example.ui.screens.transaction.CategorySelectionSheetStyle
import com.example.ui.screens.transaction.DisableSheetForceDark
import com.example.ui.screens.transaction.SheetHeaderCircleActionButton
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter

/**
 * Resolves the appropriate category icon name and color hex for a transaction,
 * preferring user/default categories from the database and falling back to
 * semantic transaction-type defaults.
 */
fun resolveTransactionCategoryVisuals(
    tx: TransactionEntity,
    categories: List<CategoryEntity>
): Pair<String, String> {
    val isTransfer = tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT
    val isIncome = tx.type == TransactionType.INCOME || tx.type == TransactionType.BORROW

    if (isTransfer) {
        return "swap_horiz" to "#3B82F6"
    }

    val matchedCategory = categories.firstOrNull {
        it.name.equals(tx.category, ignoreCase = true) ||
            (it.nameBn.isNotBlank() && it.nameBn == tx.category)
    }
    if (matchedCategory != null) {
        return matchedCategory.iconName to matchedCategory.colorHex
    }

    val fallbackIcon = when (tx.category.lowercase().trim()) {
        "daily essentials", "groceries" -> "shopping_cart"
        "eating out", "food & dining", "dining", "food" -> "restaurant"
        "transportation", "transport" -> "directions_car"
        "health care", "healthcare", "medical" -> "medical_services"
        "rent & utilities", "housing", "bills & utilities" -> "home"
        "shopping" -> "shopping_bag"
        "entertainment" -> "movie"
        "education" -> "school"
        "salary", "income" -> "payments"
        "investment", "investments" -> "trending_up"
        "gifts", "gift" -> "card_giftcard"
        else -> if (isIncome) "payments" else "category"
    }

    val fallbackColor = when {
        isIncome -> "#10B981"
        else -> "#EF4444"
    }

    return fallbackIcon to fallbackColor
}

/**
 * Modal bottom sheet wrapper for displaying transaction details in All Transactions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsBottomSheet(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit,
    onDuplicate: (TransactionEntity) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sheetContainerColor = if (isDark) Color(0xFF141824) else Color(0xFFF8FAFC)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = CategorySelectionSheetStyle.SHEET_CORNER_RADIUS,
            topEnd = CategorySelectionSheetStyle.SHEET_CORNER_RADIUS
        ),
        containerColor = sheetContainerColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
            )
        }
    ) {
        DisableSheetForceDark()
        TransactionDetailsSheetContent(
            transaction = transaction,
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            language = language,
            isDark = isDark,
            onDismiss = onDismiss,
            onDuplicate = onDuplicate,
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

/**
 * Pure content composable for the redesigned Transaction Details bottom sheet.
 * Extracted so it can be rendered directly inside ModalBottomSheet as well as in
 * Robolectric Compose UI tests.
 */
@Composable
fun TransactionDetailsSheetContent(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage = AppLanguage.ENGLISH,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    onDismiss: () -> Unit,
    onDuplicate: (TransactionEntity) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    val isExpense = transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.LEND
    val isIncome = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.BORROW
    val isTransfer = transaction.type == TransactionType.TRANSFER ||
        transaction.type == TransactionType.CC_PAYMENT ||
        !transaction.toAccountId.isNullOrBlank()

    val sourceAccount = remember(transaction.accountId, accounts) {
        accounts.firstOrNull { it.id == transaction.accountId }
    }
    val destinationAccount = remember(transaction.toAccountId, accounts) {
        if (!transaction.toAccountId.isNullOrBlank()) {
            accounts.firstOrNull { it.id == transaction.toAccountId }
        } else {
            null
        }
    }

    val (categoryIconName, categoryColorHex) = remember(transaction, categories) {
        resolveTransactionCategoryVisuals(transaction, categories)
    }

    val typeAccentColor = when {
        isIncome -> if (isDark) Color(0xFF34D399) else IncomeGreen
        isTransfer -> if (isDark) Color(0xFF60A5FA) else TransferBlue
        else -> if (isDark) Color(0xFFF87171) else ExpenseRed
    }

    val typeBadgeBgColor = when {
        isIncome -> IncomeGreen.copy(alpha = if (isDark) 0.18f else 0.12f)
        isTransfer -> TransferBlue.copy(alpha = if (isDark) 0.20f else 0.12f)
        else -> ExpenseRed.copy(alpha = if (isDark) 0.18f else 0.12f)
    }

    val typeIcon: ImageVector = when {
        isIncome -> Icons.Default.SouthWest
        isTransfer -> Icons.Default.SwapHoriz
        else -> Icons.Default.NorthEast
    }

    val typeDisplayLabel = transaction.type.getDisplayName(language)

    val amountPrefix = when {
        isIncome -> "+ "
        isExpense -> "- "
        else -> ""
    }
    val formattedAmount = remember(transaction.amount, currencySymbol, language, amountPrefix) {
        amountPrefix + CurrencyFormatter.format(transaction.amount, currencySymbol, language = language)
    }

    val formattedDateTime = remember(transaction.timestamp, language) {
        DateFormatter.formatFull(transaction.timestamp, language)
    }

    val displayTitle = remember(transaction.title, transaction.category, typeDisplayLabel) {
        when {
            transaction.title.isNotBlank() -> transaction.title
            transaction.category.isNotBlank() -> transaction.category
            else -> typeDisplayLabel
        }
    }

    val showCategoryRow = remember(transaction.category, isTransfer) {
        transaction.category.isNotBlank() &&
            !(isTransfer && transaction.category.equals("Transfer", ignoreCase = true))
    }

    val showSubcategoryRow = remember(transaction.subcategory) {
        transaction.subcategory.isNotBlank()
    }

    val cardSurfaceColor = if (isDark) Color(0xFF1E2436) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF2E364F) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val dividerColor = if (isDark) Color(0xFF2A324B) else Color(0xFFF1F5F9)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 4.dp, bottom = 24.dp)
            .testTag("transaction_details_sheet")
    ) {
        // 1. Sheet Header Row: Title + High-Contrast Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBn) "লেনদেনের বিবরণ" else "Transaction Details",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = textPrimary
                )
            )

            SheetHeaderCircleActionButton(
                icon = CategorySelectionSheetStyle.HighContrastCloseIcon,
                contentDescription = if (isBn) "বন্ধ করুন" else "Close",
                isDark = isDark,
                onClick = onDismiss,
                testTag = "tx_details_close_button"
            )
        }

        // 2. Hero Summary Card: Category Icon, Type Pill, Transaction Title, and Prominent Amount
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardSurfaceColor,
            border = BorderStroke(1.dp, cardBorderColor),
            shadowElevation = if (isDark) 0.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_details_hero_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CategoryIcon(
                        iconName = categoryIconName,
                        colorHex = categoryColorHex,
                        size = 48.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Transaction Type Pill Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = typeBadgeBgColor,
                        border = BorderStroke(1.dp, typeAccentColor.copy(alpha = 0.28f)),
                        modifier = Modifier.testTag("tx_details_type_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = typeAccentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = typeDisplayLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = typeAccentColor
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transaction Name / Title
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textPrimary
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("tx_details_title")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Prominent Formatted Amount
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = typeAccentColor
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("tx_details_amount")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Prominent Wallet / Account Section Card (Single Wallet or Transfer Source -> Destination)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardSurfaceColor,
            border = BorderStroke(1.dp, cardBorderColor),
            shadowElevation = if (isDark) 0.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_details_wallet_section")
        ) {
            if (isTransfer) {
                TransferWalletsCardContent(
                    sourceAccount = sourceAccount,
                    destinationAccount = destinationAccount,
                    currencySymbol = currencySymbol,
                    language = language,
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    dividerColor = dividerColor
                )
            } else {
                SingleWalletCardContent(
                    account = sourceAccount,
                    currencySymbol = currencySymbol,
                    language = language,
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Structured Transaction Details Card (Category, Subcategory when present, Date & Time, Payee when present)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardSurfaceColor,
            border = BorderStroke(1.dp, cardBorderColor),
            shadowElevation = if (isDark) 0.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_details_info_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                if (showCategoryRow) {
                    DetailInfoRow(
                        icon = Icons.Default.Category,
                        iconTint = parseColorOrDefault(categoryColorHex, MaterialTheme.colorScheme.primary),
                        label = if (isBn) "ক্যাটাগরি" else "Category",
                        value = transaction.category,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        testTag = "tx_details_category_row"
                    )
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                }

                if (showSubcategoryRow) {
                    DetailInfoRow(
                        icon = Icons.Default.SubdirectoryArrowRight,
                        iconTint = parseColorOrDefault(categoryColorHex, MaterialTheme.colorScheme.primary),
                        label = if (isBn) "সাবক্যাটাগরি" else "Subcategory",
                        value = transaction.subcategory,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        testTag = "tx_details_subcategory_row"
                    )
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                }

                DetailInfoRow(
                    icon = Icons.Default.CalendarToday,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = if (isBn) "তারিখ ও সময়" else "Date & Time",
                    value = formattedDateTime,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    testTag = "tx_details_datetime_row"
                )

                if (transaction.payeeOrPayer.isNotBlank()) {
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                    DetailInfoRow(
                        icon = Icons.Default.Person,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        label = if (isIncome) {
                            if (isBn) "প্রদানকারী" else "Payer"
                        } else {
                            if (isBn) "প্রাপক" else "Payee"
                        },
                        value = transaction.payeeOrPayer,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        testTag = "tx_details_payee_row"
                    )
                }
            }
        }

        // 5. Note / Description Card — ONLY rendered when a note actually exists
        if (transaction.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardSurfaceColor,
                border = BorderStroke(1.dp, cardBorderColor),
                shadowElevation = if (isDark) 0.dp else 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_details_notes_section")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isBn) "নোট" else "Note",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = transaction.notes,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            lineHeight = 20.sp,
                            color = textPrimary
                        ),
                        modifier = Modifier.testTag("tx_details_notes_text")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6. Action Buttons Row: Duplicate, Edit, and Visually Distinct Delete
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_details_actions_row"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Duplicate Button
            val duplicateBg = if (isDark) Color(0xFF262E45) else Color(0xFFF1F5F9)
            val duplicateBorder = if (isDark) Color(0xFF3B4666) else Color(0xFFCBD5E1)
            Surface(
                onClick = { onDuplicate(transaction) },
                shape = RoundedCornerShape(15.dp),
                color = duplicateBg,
                border = BorderStroke(1.dp, duplicateBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tx_details_duplicate_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "ডুপ্লিকেট" else "Duplicate",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = textPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Edit Button
            val primaryColor = MaterialTheme.colorScheme.primary
            val editBg = primaryColor.copy(alpha = if (isDark) 0.22f else 0.12f)
            val editBorder = primaryColor.copy(alpha = if (isDark) 0.48f else 0.32f)
            val editTint = if (isDark) Color(0xFF93C5FD).let {
                if (primaryColor.luminance() > 0.35f) primaryColor else Color(0xFF60A5FA)
            } else primaryColor

            Surface(
                onClick = { onEdit(transaction) },
                shape = RoundedCornerShape(15.dp),
                color = editBg,
                border = BorderStroke(1.dp, editBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tx_details_edit_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = editTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "সম্পাদনা" else "Edit",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = editTint
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Delete Button (Visually distinct destructive style)
            val deleteTint = if (isDark) Color(0xFFF87171) else ExpenseRed
            val deleteBg = ExpenseRed.copy(alpha = if (isDark) 0.18f else 0.10f)
            val deleteBorder = ExpenseRed.copy(alpha = if (isDark) 0.42f else 0.28f)

            Surface(
                onClick = { onDelete(transaction) },
                shape = RoundedCornerShape(15.dp),
                color = deleteBg,
                border = BorderStroke(1.dp, deleteBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tx_details_delete_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = deleteTint,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isBn) "মুছুন" else "Delete",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = deleteTint
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleWalletCardContent(
    account: AccountEntity?,
    currencySymbol: String,
    language: AppLanguage,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color
) {
    val isBn = language == AppLanguage.BANGLA
    val walletName = account?.name?.takeIf { it.isNotBlank() }
        ?: if (isBn) "ওয়ালেট" else "Wallet"
    val walletTypeLabel = account?.type?.getDisplayName(language)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = if (isBn) "ওয়ালেট / অ্যাকাউন্ট" else "Wallet / Account",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = textSecondary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        WalletEndpointRow(
            account = account,
            fallbackName = walletName,
            typeBadgeLabel = walletTypeLabel,
            currencySymbol = currencySymbol,
            language = language,
            isDark = isDark,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            testTag = "tx_details_single_wallet_row"
        )
    }
}

@Composable
private fun TransferWalletsCardContent(
    sourceAccount: AccountEntity?,
    destinationAccount: AccountEntity?,
    currencySymbol: String,
    language: AppLanguage,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color
) {
    val isBn = language == AppLanguage.BANGLA
    val fromWalletName = sourceAccount?.name?.takeIf { it.isNotBlank() }
        ?: if (isBn) "উৎস ওয়ালেট" else "Source Wallet"
    val toWalletName = destinationAccount?.name?.takeIf { it.isNotBlank() }
        ?: if (isBn) "গন্তব্য ওয়ালেট" else "Destination Wallet"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("tx_details_transfer_wallets_section")
    ) {
        // Source Wallet
        Text(
            text = if (isBn) "উৎস ওয়ালেট (From)" else "From Wallet",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = textSecondary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        WalletEndpointRow(
            account = sourceAccount,
            fallbackName = fromWalletName,
            typeBadgeLabel = sourceAccount?.type?.getDisplayName(language),
            currencySymbol = currencySymbol,
            language = language,
            isDark = isDark,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            testTag = "tx_details_source_wallet"
        )

        // Transfer Connector Divider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor,
                thickness = 1.dp
            )
            Surface(
                shape = CircleShape,
                color = TransferBlue.copy(alpha = if (isDark) 0.22f else 0.12f),
                border = BorderStroke(1.dp, TransferBlue.copy(alpha = 0.30f)),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = if (isBn) "স্থানান্তর" else "Transferred to",
                        tint = if (isDark) Color(0xFF60A5FA) else TransferBlue,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor,
                thickness = 1.dp
            )
        }

        // Destination Wallet
        Text(
            text = if (isBn) "গন্তব্য ওয়ালেট (To)" else "To Wallet",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = textSecondary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        WalletEndpointRow(
            account = destinationAccount,
            fallbackName = toWalletName,
            typeBadgeLabel = destinationAccount?.type?.getDisplayName(language),
            currencySymbol = currencySymbol,
            language = language,
            isDark = isDark,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            testTag = "tx_details_destination_wallet"
        )
    }
}

@Composable
private fun WalletEndpointRow(
    account: AccountEntity?,
    fallbackName: String,
    typeBadgeLabel: String?,
    currencySymbol: String,
    language: AppLanguage,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    testTag: String
) {
    val isBn = language == AppLanguage.BANGLA
    val rowBg = if (isDark) Color(0xFF161B29) else Color(0xFFF8FAFC)
    val rowBorder = if (isDark) Color(0xFF2A324B) else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = rowBg,
        border = BorderStroke(1.dp, rowBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (account != null) {
                CategoryIcon(
                    iconName = account.iconName.ifBlank { defaultAccountIconName(account.type) },
                    colorHex = account.colorHex.ifBlank { "#4F46E5" },
                    size = 40.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = account?.name?.takeIf { it.isNotBlank() } ?: fallbackName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!typeBadgeLabel.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF283149) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = typeBadgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                ),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (account != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val balancePrefix = if (account.type == AccountType.CREDIT_CARD) {
                        if (isBn) "বকেয়া ব্যালেন্স: " else "Balance: "
                    } else {
                        if (isBn) "বর্তমান ব্যালেন্স: " else "Balance: "
                    }
                    Text(
                        text = balancePrefix + CurrencyFormatter.format(
                            account.balance,
                            currencySymbol,
                            language = language
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = textSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(0.42f)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = textSecondary
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = textPrimary
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

private fun defaultAccountIconName(type: AccountType): String {
    return when (type) {
        AccountType.BANK -> "account_balance"
        AccountType.CREDIT_CARD -> "credit_card"
        AccountType.CASH -> "payments"
        AccountType.MFS -> "phone_android"
        AccountType.OTHER -> "account_balance_wallet"
    }
}

private fun parseColorOrDefault(colorHex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        fallback
    }
}
