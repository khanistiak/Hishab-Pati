package com.example.ui.screens.accounts

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountSortOption
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.CategoryIcon
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.TranslucentCard
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter

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
    val isBangla = settings?.language == AppLanguage.BANGLA
    val netWorth = viewModel.calculateNetWorth(accounts, debts)

    var selectedAccountForAction by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToAdjustBalance by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showReorderSheet by remember { mutableStateOf(false) }

    BackHandler {
        if (showSortSheet) {
            showSortSheet = false
        } else if (showReorderSheet) {
            showReorderSheet = false
        } else if (selectedAccountForAction != null) {
            selectedAccountForAction = null
        } else if (showAddDialog) {
            showAddDialog = false
        } else if (accountToAdjustBalance != null) {
            accountToAdjustBalance = null
        } else if (accountToDelete != null) {
            accountToDelete = null
        } else {
            onBack()
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

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.ACCOUNTS_MANAGEMENT
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 66.dp,
        extraTopGap = 4.dp,
        bottomNavClearance = 80.dp
    )

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
                .testTag("accounts_lazy_column"),
            contentPadding = scrollLayout.contentPadding
        ) {
            // 2. Net Worth Card
        item {
            TranslucentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isBangla) "মোট সম্পদ" else "NET WORTH",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyFormatter.format(netWorth.netWorth, currencySymbol),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Segmented Bar (Assets vs Liabilities)
                    val totalVolume = (netWorth.totalAssets + netWorth.totalLiabilities).coerceAtLeast(1.0)
                    val assetRatio = (netWorth.totalAssets / totalVolume).toFloat()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(assetRatio.coerceAtLeast(0.01f))
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(IncomeGreen)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Box(
                            modifier = Modifier
                                .weight((1f - assetRatio).coerceAtLeast(0.01f))
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(ExpenseRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${if (isBangla) "সম্পদ" else "Assets"} ${CurrencyFormatter.format(netWorth.totalAssets, currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseRed))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${if (isBangla) "দায়" else "Liabilities"} ${CurrencyFormatter.format(netWorth.totalLiabilities, currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            )
                        }
                    }
                }
            }
        }

        // 2b. Sorting & View Controls Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Sort Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (sortOption != AccountSortOption.CUSTOM) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    onClick = { showSortSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = if (sortOption != AccountSortOption.CUSTOM) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "সাজান: ${sortOption.displayNameBn}" else "Sort: ${sortOption.displayName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (sortOption != AccountSortOption.CUSTOM) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Controls on right: Reorder (if Custom) and Grouped/Single view toggle
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (sortOption == AccountSortOption.CUSTOM) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            onClick = { showReorderSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Reorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "পুনঃক্রম" else "Reorder",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        onClick = { viewModel.setAccountGroupByType(!groupByType) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (groupByType) Icons.Default.ViewAgenda else Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (groupByType) (if (isBangla) "গ্রুপ করা" else "Grouped") else (if (isBangla) "একক তালিকা" else "Single List"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Accounts List (Grouped or Single List)
        if (accounts.isEmpty()) {
            item {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
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
            val groupOrder = listOf(AccountType.BANK, AccountType.MFS, AccountType.CASH, AccountType.CREDIT_CARD, AccountType.OTHER)

            groupOrder.forEach { type ->
                val accsInType = accountsByType[type] ?: emptyList()
                if (accsInType.isNotEmpty()) {
                    val groupTotal = accsInType.sumOf { it.balance }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (if (isBangla) type.displayNameBn else type.displayName).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                    letterSpacing = 0.8.sp,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = CurrencyFormatter.format(groupTotal, currencySymbol),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    item {
                        TranslucentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                accsInType.forEach { acc ->
                                    AccountRowItem(
                                        acc = acc,
                                        currencySymbol = currencySymbol,
                                        showTypeBadge = false,
                                        onClick = { viewModel.openAccountDetail(acc.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Single Flat List: All Accounts
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countStr = if (isBangla) CurrencyFormatter.convertToBanglaDigits(sortedAccounts.size.toString()) else sortedAccounts.size.toString()
                    Text(
                        text = if (isBangla) "সকল অ্যাকাউন্ট ($countStr)" else "ALL ACCOUNTS (${sortedAccounts.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            letterSpacing = 0.8.sp,
                            fontSize = 11.sp
                        )
                    )
                    val totalSum = sortedAccounts.sumOf { it.balance }
                    Text(
                        text = CurrencyFormatter.format(totalSum, currencySymbol),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    )
                }
            }

            item {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        sortedAccounts.forEach { acc ->
                            AccountRowItem(
                                acc = acc,
                                currencySymbol = currencySymbol,
                                showTypeBadge = true,
                                onClick = { viewModel.openAccountDetail(acc.id) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Archived Wallets Section (if any accounts are archived)
        val archivedAccounts = accounts.filter { it.isHidden }
        if (archivedAccounts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBangla) "আর্কাইভ করা ওয়ালেট (${archivedAccounts.size})" else "ARCHIVED WALLETS (${archivedAccounts.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            letterSpacing = 0.8.sp,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            item {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        archivedAccounts.forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val walletColor = try {
                                        Color(android.graphics.Color.parseColor(acc.colorHex))
                                    } catch (_: Exception) {
                                        Color(0xFF64748B)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(walletColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Archive,
                                            contentDescription = null,
                                            tint = walletColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = "${CurrencyFormatter.format(acc.balance, currencySymbol)} • Archived",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        viewModel.unarchiveAccount(acc.id)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Unarchive,
                                        contentDescription = "Restore",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore")
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // 1. Header (Back, "Accounts", Sort, '+')
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "accounts_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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

                Text(
                    text = if (isBangla) "অ্যাকাউন্টস" else "Accounts",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (sortOption != AccountSortOption.CUSTOM) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(42.dp),
                        onClick = { showSortSheet = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort Accounts",
                                tint = if (sortOption != AccountSortOption.CUSTOM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(42.dp),
                        onClick = { viewModel.openAddAccount() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Account",
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
                Text(text = "Current Balance: ${CurrencyFormatter.format(acc.balance, currencySymbol)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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

    // Add or Edit Account Dialog
    if (showAddDialog || accountToEdit != null) {
        val editing = accountToEdit
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var type by remember { mutableStateOf(editing?.type ?: AccountType.BANK) }
        var inst by remember { mutableStateOf(editing?.institution ?: "") }
        var balanceStr by remember { mutableStateOf(editing?.balance?.toString() ?: "0.0") }
        var last4 by remember { mutableStateOf(editing?.accountNumberLast4 ?: "") }
        var creditLimitStr by remember { mutableStateOf(editing?.creditLimit?.toString() ?: "0.0") }
        var dueDate by remember { mutableStateOf(editing?.paymentDueDate ?: "") }
        var colorHex by remember { mutableStateOf(editing?.colorHex ?: "#2563EB") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                accountToEdit = null
            },
            title = { Text(if (editing != null) "Edit Account" else "Add New Account") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name (e.g. City Bank)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inst,
                        onValueChange = { inst = it },
                        label = { Text("Financial Institution / Provider") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = balanceStr,
                        onValueChange = { balanceStr = it },
                        label = { Text("Balance (Can be negative)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { last4 = it.take(4) },
                        label = { Text("Last 4 digits (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (type == AccountType.CREDIT_CARD) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = creditLimitStr,
                            onValueChange = { creditLimitStr = it },
                            label = { Text("Credit Limit") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Payment Due Note (e.g. bill due 20 Nov)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val bal = balanceStr.toDoubleOrNull() ?: 0.0
                        val limit = creditLimitStr.toDoubleOrNull() ?: 0.0
                        viewModel.saveAccount(
                            id = editing?.id,
                            name = name,
                            type = type,
                            institution = inst,
                            balance = bal,
                            accountNumberLast4 = last4,
                            colorHex = colorHex,
                            iconName = when (type) {
                                AccountType.BANK -> "account_balance"
                                AccountType.MFS -> "phone_android"
                                AccountType.CASH -> "payments"
                                AccountType.CREDIT_CARD -> "credit_card"
                                else -> "account_balance_wallet"
                            },
                            creditLimit = limit,
                            paymentDueDate = dueDate,
                            notes = ""
                        )
                        showAddDialog = false
                        accountToEdit = null
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    accountToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
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
            onSortSelected = { option ->
                viewModel.setAccountSortOption(option)
            },
            onGroupByTypeChanged = { grouped ->
                viewModel.setAccountGroupByType(grouped)
            },
            onOpenCustomReorder = {
                showSortSheet = false
                showReorderSheet = true
            },
            onApplyCurrentAsCustom = {
                viewModel.applyOrderAsCustom(sortedAccounts)
            },
            onDismiss = { showSortSheet = false }
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
private fun AccountRowItem(
    acc: AccountEntity,
    currencySymbol: String,
    showTypeBadge: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                iconName = acc.iconName,
                colorHex = acc.colorHex,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = acc.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    )
                    if (showTypeBadge) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = acc.type.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                val subtext = listOfNotNull(
                    if (acc.accountNumberLast4.isNotBlank()) "••${acc.accountNumberLast4}" else null,
                    if (acc.institution.isNotBlank()) acc.institution else null,
                    if (acc.paymentDueDate.isNotBlank()) acc.paymentDueDate else null
                ).joinToString(" · ")
                if (subtext.isNotBlank()) {
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            val isCC = acc.type == AccountType.CREDIT_CARD
            val ccOutstanding = if (acc.balance < 0) kotlin.math.abs(acc.balance) else acc.balance
            Column(horizontalAlignment = Alignment.End) {
                if (isCC) {
                    Text(
                        text = "OUTSTANDING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
                Text(
                    text = CurrencyFormatter.format(if (isCC) ccOutstanding else acc.balance, currencySymbol),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCC && ccOutstanding > 0) ExpenseRed else if (acc.balance < 0) ExpenseRed else MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }

        // For Credit Cards: show utilization progress bar and "Healthy" status!
        if (acc.type == AccountType.CREDIT_CARD && acc.creditLimit > 0) {
            val usedAmt = kotlin.math.abs(acc.balance)
            val usedPct = ((usedAmt / acc.creditLimit) * 100).toInt()
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (usedAmt / acc.creditLimit).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (usedPct > 30) ExpenseRed else IncomeGreen,
                trackColor = Color(0xFFE2E8F0)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$usedPct% of ${CurrencyFormatter.format(acc.creditLimit, currencySymbol)} limit used",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = if (usedPct <= 30) "Healthy" else "High Usage",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (usedPct <= 30) IncomeGreen else ExpenseRed
                )
            }
        }
    }
}

