package com.example.ui.screens.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountSortOption
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.screens.home.resolveAccountCardColor
import com.example.ui.theme.ExpenseRed
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val sortOption by viewModel.accountSortOption.collectAsState()
    val groupByType by viewModel.accountGroupByType.collectAsState()
    val debts by viewModel.debts.collectAsState()

    val currencySymbol = settings?.currencySymbol ?: "৳"
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBangla = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val netWorth = remember(accounts, debts) {
        viewModel.calculateNetWorth(accounts, debts)
    }

    var selectedAccountForAction by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToAdjustBalance by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showReorderSheet by remember { mutableStateOf(false) }

    BackHandler {
        when {
            showSortSheet -> showSortSheet = false
            showReorderSheet -> showReorderSheet = false
            selectedAccountForAction != null -> selectedAccountForAction = null
            accountToAdjustBalance != null -> accountToAdjustBalance = null
            accountToDelete != null -> accountToDelete = null
            else -> onBack()
        }
    }

    val sortedAccounts = remember(accounts, sortOption) {
        val nonHidden = accounts.filter { !it.isHidden }
        when (sortOption) {
            AccountSortOption.CUSTOM -> nonHidden.sortedWith(compareBy({ it.sortOrder }, { it.name.lowercase() }))
            AccountSortOption.BALANCE_DESC -> nonHidden.sortedByDescending { it.balance }
            AccountSortOption.BALANCE_ASC -> nonHidden.sortedBy { it.balance }
            AccountSortOption.NAME_ASC -> nonHidden.sortedBy { it.name.lowercase() }
            AccountSortOption.NAME_DESC -> nonHidden.sortedByDescending { it.name.lowercase() }
            AccountSortOption.TYPE -> nonHidden.sortedWith(compareBy({ it.type.ordinal }, { it.sortOrder }))
        }
    }

    val accountsByType = remember(sortedAccounts) {
        sortedAccounts.groupBy { it.type }
    }
    val archivedAccounts = remember(accounts) {
        accounts.filter { it.isHidden }
    }

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

    val emeraldColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val coralColor = if (isDark) Color(0xFFFB7185) else Color(0xFFDC2626)
    val bentoCardBg = if (isDark) Color(0xFF192234) else Color.White
    val pillBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.80f)
    val headerBtnBg = if (isDark) Color(0xFF192234) else Color.White
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.08f else 0.06f)

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.ACCOUNTS_MANAGEMENT
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 0.dp,
        bottomNavClearance = 32.dp
    )

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
                .testTag("accounts_lazy_column"),
            contentPadding = PaddingValues(
                top = scrollLayout.contentPadding.calculateTopPadding() + 14.dp,
                bottom = 32.dp
            )
        ) {
            // 1. Open-Canvas Net Worth Summary + Ratio Bar
            item(key = "accounts_net_worth_hero") {
                OpenNetWorthSummary(
                    accountCount = sortedAccounts.size,
                    netWorth = netWorth.netWorth,
                    availableAmount = netWorth.totalAssets,
                    owingAmount = netWorth.totalLiabilities,
                    currencySymbol = currencySymbol,
                    language = language,
                    emeraldColor = emeraldColor,
                    coralColor = coralColor
                )
            }

            // 2. Control Strip (Sort pill left; Reorder + Grouped / Single List pills right)
            item(key = "accounts_utility_bar") {
                Spacer(modifier = Modifier.height(14.dp))
                AccountsControlStrip(
                    sortOption = sortOption,
                    groupByType = groupByType,
                    language = language,
                    pillBg = pillBg,
                    onSortClick = { showSortSheet = true },
                    onReorderClick = { showReorderSheet = true },
                    onToggleViewMode = { viewModel.setAccountGroupByType(!groupByType) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Accounts List (Grouped by category or SPEND FROM vs CREDIT CARDS)
            if (sortedAccounts.isEmpty()) {
                item(key = "accounts_empty_card") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = bentoCardBg,
                        shadowElevation = if (isDark) 0.dp else 1.5.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isBangla) "কোনো অ্যাকাউন্ট যুক্ত করা হয়নি" else "No accounts added yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isBangla) "আপনার প্রথম ব্যাংক, এমএফএস বা ক্যাশ অ্যাকাউন্ট যোগ করতে ওপরের '+' বাটনে চাপুন" else "Tap '+' at the top right to add your first Bank, MFS, or Cash account",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (groupByType) {
                val groupOrder = listOf(
                    AccountType.BANK,
                    AccountType.MFS,
                    AccountType.CASH,
                    AccountType.CREDIT_CARD,
                    AccountType.OTHER
                )
                val nonEmptyGroups = groupOrder.mapNotNull { type ->
                    val list = accountsByType[type].orEmpty()
                    if (list.isNotEmpty()) type to list else null
                }

                nonEmptyGroups.forEachIndexed { groupIdx, (type, accsInType) ->
                    val isCreditGroup = type == AccountType.CREDIT_CARD
                    val groupTotal = if (isCreditGroup) {
                        accsInType.sumOf { abs(it.balance) }
                    } else {
                        accsInType.sumOf { it.balance }
                    }
                    val totalText = if (isCreditGroup && groupTotal > 0.0) {
                        "-${CurrencyFormatter.format(groupTotal, currencySymbol, language = language)}"
                    } else {
                        CurrencyFormatter.format(groupTotal, currencySymbol, language = language)
                    }

                    item(key = "accounts_group_${type.name}") {
                        AccountSectionBentoBlock(
                            headerTitle = (if (isBangla) type.displayNameBn else type.displayName).uppercase(),
                            headerTotalText = totalText,
                            headerTotalColor = if (isCreditGroup && groupTotal > 0.0) coralColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                            accounts = accsInType,
                            currencySymbol = currencySymbol,
                            language = language,
                            cardBg = bentoCardBg,
                            dividerColor = dividerColor,
                            emeraldColor = emeraldColor,
                            coralColor = coralColor,
                            isDark = isDark,
                            onAccountClick = { acc -> viewModel.openAccountDetail(acc.id) }
                        )
                        if (groupIdx < nonEmptyGroups.lastIndex || archivedAccounts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            } else {
                // Single List Mode: SPEND FROM and CREDIT CARDS / OWING
                val spendFromAccounts = sortedAccounts.filter { it.type != AccountType.CREDIT_CARD && it.balance >= 0.0 }
                val creditAndOwingAccounts = sortedAccounts.filter { it.type == AccountType.CREDIT_CARD || it.balance < 0.0 }

                if (spendFromAccounts.isNotEmpty()) {
                    val availableTotal = spendFromAccounts.sumOf { it.balance }
                    item(key = "accounts_spend_from_section") {
                        AccountSectionBentoBlock(
                            headerTitle = AppStrings.t("spend_from_header", language),
                            headerTotalText = CurrencyFormatter.format(availableTotal, currencySymbol, language = language),
                            headerTotalColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            accounts = spendFromAccounts,
                            currencySymbol = currencySymbol,
                            language = language,
                            cardBg = bentoCardBg,
                            dividerColor = dividerColor,
                            emeraldColor = emeraldColor,
                            coralColor = coralColor,
                            isDark = isDark,
                            onAccountClick = { acc -> viewModel.openAccountDetail(acc.id) }
                        )
                        if (creditAndOwingAccounts.isNotEmpty() || archivedAccounts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }

                if (creditAndOwingAccounts.isNotEmpty()) {
                    val creditTotal = creditAndOwingAccounts.sumOf { abs(it.balance) }
                    val onlyCreditCards = creditAndOwingAccounts.all { it.type == AccountType.CREDIT_CARD }
                    val headerKey = if (onlyCreditCards) "credit_cards_header" else "owing_header"
                    item(key = "accounts_owing_section") {
                        AccountSectionBentoBlock(
                            headerTitle = AppStrings.t(headerKey, language),
                            headerTotalText = if (creditTotal > 0.0) {
                                "-${CurrencyFormatter.format(creditTotal, currencySymbol, language = language)}"
                            } else {
                                CurrencyFormatter.format(0.0, currencySymbol, language = language)
                            },
                            headerTotalColor = if (creditTotal > 0.0) coralColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                            accounts = creditAndOwingAccounts,
                            currencySymbol = currencySymbol,
                            language = language,
                            cardBg = bentoCardBg,
                            dividerColor = dividerColor,
                            emeraldColor = emeraldColor,
                            coralColor = coralColor,
                            isDark = isDark,
                            onAccountClick = { acc -> viewModel.openAccountDetail(acc.id) }
                        )
                        if (archivedAccounts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            }

            // 4. Archived Wallets Section (if any accounts are archived)
            if (archivedAccounts.isNotEmpty()) {
                item(key = "accounts_archived_section") {
                    val countText = if (isBangla) {
                        CurrencyFormatter.convertToBanglaDigits(archivedAccounts.size.toString())
                    } else {
                        archivedAccounts.size.toString()
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "আর্কাইভ করা ওয়ালেট ($countText)" else "ARCHIVED WALLETS ($countText)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                    letterSpacing = 0.8.sp,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = bentoCardBg,
                            shadowElevation = if (isDark) 0.dp else 1.5.dp
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                archivedAccounts.forEachIndexed { idx, acc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            MiniAccountCardThumbnail(
                                                account = acc,
                                                isArchived = true,
                                                currencySymbol = currencySymbol
                                            )

                                            Column {
                                                Text(
                                                    text = acc.name,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${CurrencyFormatter.format(acc.balance, currencySymbol, language = language)} • ${AppStrings.t("archived_badge", language)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                                    )
                                                )
                                            }
                                        }

                                        TextButton(
                                            onClick = { viewModel.unarchiveAccount(acc.id) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Unarchive,
                                                contentDescription = AppStrings.t("restore_label", language),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(AppStrings.t("restore_label", language))
                                        }
                                    }
                                    if (idx < archivedAccounts.lastIndex) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(0.6.dp)
                                                .background(dividerColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Single-Row Top Header Bar (Matching Calendar, Income tracker, and Settings)
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "accounts_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = headerBtnBg,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("accounts_back_button"),
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

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = AppStrings.t("accounts", language),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = headerBtnBg,
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("accounts_transfer_button"),
                        onClick = { viewModel.openTransferFromAccount(null) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = AppStrings.t("transfer_label", language),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = headerBtnBg,
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("accounts_add_button"),
                        onClick = { viewModel.openAddAccount() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = AppStrings.t("add_account", language),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Account Action Sheet (Edit, Adjust Balance, Delete)
    selectedAccountForAction?.let { acc ->
        ModalBottomSheet(
            onDismissRequest = { selectedAccountForAction = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(text = acc.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "Current Balance: ${CurrencyFormatter.format(acc.balance, currencySymbol, language = language)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (acc.institution.isNotBlank()) Text("Institution: ${acc.institution}", fontSize = 13.sp)

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        val toAdjust = acc
                        selectedAccountForAction = null
                        viewModel.openAdjustBalance(toAdjust)
                    }) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adjust Balance")
                    }

                    TextButton(onClick = {
                        val toEdit = acc
                        selectedAccountForAction = null
                        viewModel.openEditAccount(toEdit)
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Details")
                    }

                    TextButton(onClick = {
                        accountToDelete = acc
                        selectedAccountForAction = null
                    }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = ExpenseRed)
                    }
                }
            }
        }
    }

    // Adjust Balance Dialog
    accountToAdjustBalance?.let { acc ->
        var newBalStr by remember { mutableStateOf(acc.balance.toString()) }

        AlertDialog(
            onDismissRequest = { accountToAdjustBalance = null },
            title = { Text("Adjust Balance") },
            text = {
                Column {
                    Text("Enter the actual current balance for ${acc.name}. An adjustment record will be created automatically.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBalStr,
                        onValueChange = { newBalStr = it },
                        label = { Text("New Balance ($currencySymbol)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newBal = newBalStr.toDoubleOrNull()
                        if (newBal != null) {
                            viewModel.adjustAccountBalance(acc.id, newBal)
                        }
                        accountToAdjustBalance = null
                    }
                ) {
                    Text("Adjust", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToAdjustBalance = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Account Confirmation
    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete ${acc.name}? Its transactions will be preserved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(acc.id)
                        accountToDelete = null
                    }
                ) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        AccountSortBottomSheet(
            currentSort = sortOption,
            groupByType = groupByType,
            onSortSelected = { option -> viewModel.setAccountSortOption(option) },
            onGroupByTypeChanged = { grouped -> viewModel.setAccountGroupByType(grouped) },
            onOpenCustomReorder = {
                showSortSheet = false
                showReorderSheet = true
            },
            onApplyCurrentAsCustom = { viewModel.applyOrderAsCustom(sortedAccounts) },
            onDismiss = { showSortSheet = false },
            language = language
        )
    }

    // Reorder Accounts Sheet
    if (showReorderSheet) {
        AccountReorderBottomSheet(
            accounts = accounts,
            currencySymbol = currencySymbol,
            onSaveOrder = { newOrder ->
                viewModel.reorderAccountsList(newOrder)
            },
            onDismiss = { showReorderSheet = false }
        )
    }
}

@Composable
private fun OpenNetWorthSummary(
    accountCount: Int,
    netWorth: Double,
    availableAmount: Double,
    owingAmount: Double,
    currencySymbol: String,
    language: AppLanguage,
    emeraldColor: Color,
    coralColor: Color
) {
    val isBangla = language == AppLanguage.BANGLA
    val countText = if (isBangla) {
        CurrencyFormatter.convertToBanglaDigits(accountCount.toString())
    } else {
        accountCount.toString()
    }
    val heroSubtitle = if (isBangla) {
        "${AppStrings.t("net_worth", language)} • $countText ${AppStrings.t("accounts_count_suffix", language)}"
    } else {
        "NET WORTH • $countText ACCOUNTS"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("accounts_editorial_hero")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = heroSubtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f),
                        fontSize = 10.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CurrencyFormatter.format(netWorth, currencySymbol, language = language),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFeatureSettings = "tnum",
                        letterSpacing = (-0.6).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "+${CurrencyFormatter.format(availableAmount, currencySymbol, language = language)} ${AppStrings.t("available_suffix", language)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = emeraldColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1
                )
                Text(
                    text = "${CurrencyFormatter.format(owingAmount, currencySymbol, language = language)} ${AppStrings.t("owing_suffix", language)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = coralColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Slim 4.dp dual-color Assets vs Liabilities ratio bar
        val totalVolume = (availableAmount.coerceAtLeast(0.0) + owingAmount.coerceAtLeast(0.0))
        val assetFraction = if (totalVolume > 0.0) {
            (availableAmount.coerceAtLeast(0.0) / totalVolume).toFloat().coerceIn(0f, 1f)
        } else {
            1f
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (assetFraction > 0.005f) {
                Box(
                    modifier = Modifier
                        .weight(assetFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(emeraldColor)
                )
            }
            if (assetFraction > 0.005f && (1f - assetFraction) > 0.005f) {
                Spacer(modifier = Modifier.width(3.dp))
            }
            if ((1f - assetFraction) > 0.005f) {
                Box(
                    modifier = Modifier
                        .weight(1f - assetFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(coralColor)
                )
            }
        }
    }
}

@Composable
private fun AccountsControlStrip(
    sortOption: AccountSortOption,
    groupByType: Boolean,
    language: AppLanguage,
    pillBg: Color,
    onSortClick: () -> Unit,
    onReorderClick: () -> Unit,
    onToggleViewMode: () -> Unit
) {
    val sortShortName = when (sortOption) {
        AccountSortOption.CUSTOM -> if (language == AppLanguage.BANGLA) "কাস্টম" else "Custom"
        else -> sortOption.getDisplayName(language)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Sort: Custom ⌄ pill (34.dp height)
        Surface(
            modifier = Modifier
                .weight(1f, fill = false)
                .height(34.dp)
                .testTag("accounts_sort_pill"),
            shape = CircleShape,
            color = if (sortOption != AccountSortOption.CUSTOM) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                pillBg
            },
            onClick = onSortClick
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "${AppStrings.t("sort_short_prefix", language)}: $sortShortName",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (sortOption != AccountSortOption.CUSTOM) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                        },
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (sortOption != AccountSortOption.CUSTOM) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Reorder pill + Grouped / Single List toggle pill
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .height(34.dp)
                    .testTag("accounts_reorder_pill"),
                shape = CircleShape,
                color = pillBg,
                onClick = onReorderClick
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Reorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = AppStrings.t("reorder_short", language),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .height(34.dp)
                    .testTag("accounts_view_mode_toggle_pill"),
                shape = CircleShape,
                color = pillBg,
                onClick = onToggleViewMode
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (groupByType) Icons.Default.ViewAgenda else Icons.Default.FormatListBulleted,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (groupByType) {
                            AppStrings.t("grouped_short", language)
                        } else {
                            AppStrings.t("single_list_short", language)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Bespoke 54.dp x 36.dp mini wallet/bank card thumbnail synced with [HishabPatiAccountCard] rules:
 * mini EMV chip ONLY on Bank/Credit cards; clean geometric/linen artwork on MFS & Cash wallets.
 */
@Composable
private fun MiniAccountCardThumbnail(
    account: AccountEntity,
    isArchived: Boolean = false,
    currencySymbol: String = "৳",
    modifier: Modifier = Modifier
) {
    HishabPatiMiniAccountCard(
        account = account,
        isArchived = isArchived,
        currencySymbol = currencySymbol,
        modifier = modifier
    )
}

@Composable
private fun AccountSectionBentoBlock(
    headerTitle: String,
    headerTotalText: String,
    headerTotalColor: Color,
    accounts: List<AccountEntity>,
    currencySymbol: String,
    language: AppLanguage,
    cardBg: Color,
    dividerColor: Color,
    emeraldColor: Color,
    coralColor: Color,
    isDark: Boolean,
    onAccountClick: (AccountEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headerTitle,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f),
                    letterSpacing = 0.8.sp,
                    fontSize = 11.sp
                )
            )
            Text(
                text = headerTotalText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = headerTotalColor,
                    fontSize = 12.5.sp,
                    fontFeatureSettings = "tnum"
                )
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            shadowElevation = if (isDark) 0.dp else 1.5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                accounts.forEachIndexed { index, acc ->
                    AccountRowItem(
                        acc = acc,
                        currencySymbol = currencySymbol,
                        language = language,
                        emeraldColor = emeraldColor,
                        coralColor = coralColor,
                        isDark = isDark,
                        onClick = { onAccountClick(acc) }
                    )
                    if (index < accounts.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.6.dp)
                                .background(dividerColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRowItem(
    acc: AccountEntity,
    currencySymbol: String,
    language: AppLanguage,
    emeraldColor: Color,
    coralColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val isCC = acc.type == AccountType.CREDIT_CARD
    val ccOutstanding = if (acc.balance < 0) abs(acc.balance) else acc.balance

    val subtitle = remember(acc, language) {
        val masked = if (acc.accountNumberLast4.isNotBlank()) "••${acc.accountNumberLast4}" else null
        val inst = acc.institution.takeIf { it.isNotBlank() }
        val parts = listOfNotNull(masked, inst)
        when {
            parts.isNotEmpty() -> parts.joinToString(" • ")
            acc.type == AccountType.CASH -> AppStrings.t("physical_cash", language)
            else -> if (language == AppLanguage.BANGLA) acc.type.displayNameBn else acc.type.displayName
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
            .testTag("account_row_${acc.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom 54.dp x 36.dp Mini Card Thumbnail matched by category
            MiniAccountCardThumbnail(
                account = acc,
                currencySymbol = currencySymbol
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = acc.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (isCC) {
                    Text(
                        text = AppStrings.t("outstanding_caps", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = coralColor,
                            letterSpacing = 0.6.sp
                        )
                    )
                }
                Text(
                    text = CurrencyFormatter.format(
                        amount = if (isCC) ccOutstanding else acc.balance,
                        symbol = currencySymbol,
                        language = language
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                        color = when {
                            isCC && ccOutstanding > 0.0 -> coralColor
                            acc.balance < 0.0 -> coralColor
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    ),
                    maxLines = 1
                )
            }
        }

        // Credit Card slim 4.dp utilization progress bar
        if (isCC && acc.creditLimit > 0.0) {
            val usedAmt = abs(acc.balance)
            val usedPct = ((usedAmt / acc.creditLimit) * 100.0).toInt().coerceIn(0, 999)
            val isHealthy = usedPct <= 30
            val pctText = if (language == AppLanguage.BANGLA) {
                CurrencyFormatter.convertToBanglaDigits(usedPct.toString())
            } else {
                usedPct.toString()
            }
            val limitText = CurrencyFormatter.format(acc.creditLimit, currencySymbol, language = language)
            val usageLabel = if (language == AppLanguage.BANGLA) {
                "$limitText সীমার $pctText% ব্যবহৃত"
            } else {
                "$pctText% of $limitText limit used"
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (usedAmt / acc.creditLimit).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = if (isHealthy) emeraldColor else coralColor,
                trackColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = usageLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
                    )
                )
                Text(
                    text = if (isHealthy) AppStrings.t("healthy_usage", language) else AppStrings.t("high_usage", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHealthy) emeraldColor else coralColor
                    )
                )
            }
        }
    }
}
