package com.example.ui.screens.home

import androidx.compose.ui.zIndex
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.screens.accounts.CardNetworkLogo
import com.example.ui.screens.accounts.EmvChip
import com.example.ui.screens.accounts.MfsProviderLogo

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.DragHandle
import com.example.ui.util.DragDropListState
import com.example.ui.util.rememberDragDropListState
import com.example.ui.util.reorderableDragHandle
import com.example.ui.util.SixDotDragHandle
import com.example.ui.util.dragDropItem
import com.example.ui.util.dragHandle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.data.backup.FinancialIntegrityValidator
import com.example.data.local.UserSettingsPreferences
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.ui.BalanceCardStyle
import com.example.ui.HomeCardStyle
import com.example.ui.HomeCustomization
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.lazy.LazyListState
import com.example.ui.components.AvatarBadge
import com.example.ui.components.CategoryIcon
import com.example.ui.components.FrostedGlassSurface
import com.example.ui.components.LocalFrostedBackdropState
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.TranslucentCard
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import com.example.ui.navigation.rememberScreenLazyListState
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState = viewModel.rememberScreenLazyListState(tabOverride = MainTab.HOME)
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val homeCustomization by viewModel.homeCustomization.collectAsState()
    val extendedSettings by viewModel.extendedSettings.collectAsState()

    var isBalanceVisible by remember { mutableStateOf(true) }

    val netWorth = remember(accounts, debts) { viewModel.calculateNetWorth(accounts, debts) }
    val lang = settings?.language ?: com.example.data.model.AppLanguage.ENGLISH
    val isBangla = lang == com.example.data.model.AppLanguage.BANGLA
    val currencySymbol = settings?.currencySymbol ?: "৳"

    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    val hasUnreadNotifications = unreadNotificationCount > 0

    val (refYear, refMonth) = remember(transactions, budgets) {
        FinancialIntegrityValidator.resolveReferenceYearMonth(transactions, budgets)
    }
    val spentThisMonth = remember(transactions, refYear, refMonth) {
        transactions.filter { tx ->
            FinancialIntegrityValidator.isEligibleExpense(tx) &&
                FinancialIntegrityValidator.isInYearMonth(tx.timestamp, refYear, refMonth)
        }.sumOf { it.amount }
    }

    // Real camera capture launcher for receipts
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.userNotificationMessage.value = if (isBangla) "রসিদের ছবি তোলা হয়েছে" else "Receipt photo captured"
            viewModel.newTransactionInitialType.value = TransactionType.EXPENSE
            viewModel.transactionToEdit.value = null
            viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        } else {
            viewModel.userNotificationMessage.value = if (isBangla) "ক্যামেরা পারমিশন আবশ্যক" else "Camera permission is required"
        }
    }

    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 4.dp,
        bottomNavClearance = 112.dp
    )

    val currentUser by viewModel.currentUser.collectAsState()
    val user = currentUser
    val currentDisplayName = if (user != null) {
        val dName = user.displayName
        if (!dName.isNullOrBlank()) dName else (settings?.userName ?: "Google User")
    } else {
        val sName = settings?.userName
        if (!sName.isNullOrBlank()) sName else "Guest User"
    }
    val photoUrl = if (user != null && user.photoUrl != null) {
        user.photoUrl.toString()
    } else {
        settings?.profilePhotoUri?.takeIf { it.isNotBlank() }
    }
    val symbol = if (settings?.profileAvatarType == "SYMBOL") settings?.profileAvatarSymbol else null
    val avatarColor = try {
        Color(android.graphics.Color.parseColor(settings?.profileAvatarColorHex ?: "#4F46E5"))
    } catch (_: Exception) {
        Color(0xFF4F46E5)
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
                .testTag("home_lazy_column"),
            contentPadding = scrollLayout.contentPadding
        ) {
            // 1. Summary Cards: Total Balance & Spent This Month
            item(key = "home_balance_card") {
                HomeSummaryCardsSection(
                totalBalance = netWorth.totalAssets,
                spentThisMonth = spentThisMonth,
                isBalanceVisible = isBalanceVisible,
                currencySymbol = currencySymbol,
                isBangla = isBangla,
                isGradient = homeCustomization.balanceCardStyle == BalanceCardStyle.GRADIENT,
                onToggleVisibility = { isBalanceVisible = !isBalanceVisible }
            )
        }

        // 3. Accounts Section (Card Stack vs Carousel based on Customize Home)
        item(key = "home_accounts_section") {
            if (homeCustomization.cardStyle == HomeCardStyle.STACK) {
                StackedAccountCards(
                    accounts = accounts,
                    isBalanceVisible = isBalanceVisible,
                    currencySymbol = currencySymbol,
                    isBangla = isBangla,
                    onAccountClick = { acc -> viewModel.openAccountDetail(acc.id) },
                    onAllAccountsClick = { viewModel.openSubScreen(SubScreen.ACCOUNTS_MANAGEMENT) }
                )
            } else {
                AccountCardsCarousel(
                    accounts = accounts,
                    isBalanceVisible = isBalanceVisible,
                    currencySymbol = currencySymbol,
                    onAccountClick = { acc -> viewModel.openAccountDetail(acc.id) }
                )
            }
        }

        // 4. Dynamic Sections in User-Customized Order
        val orderedSections = UserSettingsPreferences.normalizeHomeSectionOrder(homeCustomization.sectionOrder)
        orderedSections.forEach { sectionKey ->
            when (sectionKey) {
                "budget" -> {
                    if (homeCustomization.showBudgetProgress) {
                        item(key = "home_monthly_budget") {
                            MonthlyBudgetSection(
                                budgets = budgets,
                                transactions = transactions,
                                currencySymbol = currencySymbol,
                                onDetailsClick = { viewModel.openBudgetDashboard() }
                            )
                        }
                    }
                }
                "bills" -> {
                    if (homeCustomization.showUpcomingBills) {
                        item(key = "home_upcoming_bills") {
                            UpcomingBillsSection(
                                debts = debts,
                                accounts = accounts,
                                currencySymbol = currencySymbol,
                                isBangla = isBangla,
                                onManageDebtsClick = { viewModel.openSubScreen(SubScreen.MANAGE_DEBTS) },
                                onAccountClick = { accId -> viewModel.openAccountDetail(accId) },
                                onAddDebtClick = { viewModel.openSubScreen(SubScreen.ADD_DEBT) }
                            )
                        }
                    }
                }
                "recent_activity" -> {
                    if (homeCustomization.showRecentActivity) {
                        item(key = "home_recent_activity") {
                            RecentActivitySection(
                                transactions = transactions.sortedByDescending { it.timestamp }.take(5),
                                categories = categories,
                                currencySymbol = currencySymbol,
                                isBangla = isBangla,
                                onSeeAllClick = { viewModel.setTab(MainTab.ACTIVITY) },
                                onTransactionClick = { tx ->
                                    viewModel.transactionToEdit.value = tx
                                    viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
                                }
                            )
                        }
                    }
                }
                "savings" -> {
                    if (homeCustomization.showSavingsGoals) {
                        item(key = "home_savings_goals") {
                            HomeSavingsGoalsSection(
                                goals = savingsGoals.sortedBy { it.isCompleted },
                                currencySymbol = currencySymbol,
                                isBangla = isBangla,
                                onSeeAllClick = { viewModel.setTab(MainTab.PLAN) },
                                onAddGoalClick = { viewModel.openSubScreen(SubScreen.NEW_SAVINGS_GOAL) }
                            )
                        }
                    }
                }
                "financial_health" -> {
                    if (homeCustomization.showFinancialHealth) {
                        item(key = "home_financial_health") {
                            FinancialHealthCard(
                                transactions = transactions,
                                budgets = budgets,
                                isBangla = isBangla,
                                onDetailsClick = { viewModel.setTab(MainTab.INSIGHTS) }
                            )
                        }
                    }
                }
                "smart_insights" -> {
                    if (homeCustomization.showSmartInsights) {
                        item(key = "home_smart_insight") {
                            SmartInsightBanner(
                                transactions = transactions,
                                budgets = budgets,
                                currencySymbol = currencySymbol,
                                isBangla = isBangla,
                                onClick = { viewModel.setTab(MainTab.INSIGHTS) }
                            )
                        }
                    }
                }
                "top_spending" -> {
                    if (homeCustomization.showTopSpending) {
                        item(key = "home_top_spending") {
                            TopSpendingSection(
                                transactions = transactions,
                                categories = categories,
                                currencySymbol = currencySymbol,
                                isBangla = isBangla,
                                onInsightsClick = { viewModel.setTab(MainTab.INSIGHTS) }
                            )
                        }
                    }
                }
                "spending_by_day" -> {
                    if (homeCustomization.showSpendingByDay) {
                        item(key = "home_spending_by_day") {
                            SpendingByDaySection(
                                transactions = transactions,
                                currencySymbol = currencySymbol,
                                startOfWeek = extendedSettings.startOfWeek,
                                isBangla = isBangla,
                                onInsightsClick = { viewModel.setTab(MainTab.INSIGHTS) }
                            )
                        }
                    }
                }
                "spending_streak" -> {
                    if (homeCustomization.showSpendingStreak) {
                        item(key = "home_spending_streak") {
                            SpendingStreakSection(
                                transactions = transactions,
                                budgets = budgets,
                                currencySymbol = currencySymbol,
                                isBangla = isBangla,
                                onDetailsClick = { viewModel.setTab(MainTab.INSIGHTS) }
                            )
                        }
                    }
                }
                "shortcuts" -> {
                    if (homeCustomization.showQuickShortcuts) {
                        item(key = "home_quick_shortcuts") {
                            QuickShortcutsRow(
                                isBangla = isBangla,
                                onTransferClick = {
                                    viewModel.newTransactionInitialType.value = TransactionType.TRANSFER
                                    viewModel.transactionToEdit.value = null
                                    viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
                                },
                                onScanReceiptClick = {
                                    viewModel.newTransactionInitialType.value = TransactionType.EXPENSE
                                    viewModel.transactionToEdit.value = null
                                    viewModel.pendingStartReceiptScan.value = true
                                    viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
                                },
                                onExpenseClick = {
                                    viewModel.newTransactionInitialType.value = TransactionType.EXPENSE
                                    viewModel.transactionToEdit.value = null
                                    viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
                                },
                                onVoiceEntryClick = {
                                    viewModel.isVoiceDialogVisible.value = true
                                }
                            )
                        }
                    }
                }
            }
        }
        }

        // Shared Fixed Liquid-Crystal Top Header (Profile, Greeting, Theme Toggle, Search, Notification)
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "home_fixed_header"
        ) {
            HomeHeader(
                userName = currentDisplayName,
                themeMode = settings?.themeMode ?: com.example.data.model.ThemeMode.AUTO,
                photoUrl = photoUrl,
                symbolName = symbol,
                backgroundColor = avatarColor,
                isBangla = isBangla,
                hasUnreadNotifications = hasUnreadNotifications,
                onAvatarClick = { viewModel.openSubScreen(SubScreen.SETTINGS) },
                onThemeToggle = { viewModel.toggleThemeModeQuick() },
                onSearchClick = { viewModel.setTab(MainTab.ACTIVITY) },
                onNotificationClick = { viewModel.openSubScreen(SubScreen.NOTIFICATIONS) }
            )
        }
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    themeMode: com.example.data.model.ThemeMode = com.example.data.model.ThemeMode.AUTO,
    photoUrl: String? = null,
    symbolName: String? = null,
    backgroundColor: Color = Color(0xFF6366F1),
    isBangla: Boolean = false,
    hasUnreadNotifications: Boolean = false,
    onAvatarClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val greeting = if (isBangla) {
        when {
            hour < 12 -> "শুভ সকাল,"
            hour < 17 -> "শুভ অপরাহ্ন,"
            else -> "শুভ সন্ধ্যা,"
        }
    } else {
        when {
            hour < 12 -> "Good Morning,"
            hour < 17 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
    }

    val initials = userName.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "U" }
        .uppercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarBadge(
            initials = initials,
            photoUrl = photoUrl,
            symbolName = symbolName,
            backgroundColor = backgroundColor,
            onClick = onAvatarClick
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        // Quick Theme Mode Toggle Button (Light / Dark / Auto)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shadowElevation = 1.dp,
            onClick = onThemeToggle,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (themeMode) {
                        com.example.data.model.ThemeMode.LIGHT -> Icons.Default.LightMode
                        com.example.data.model.ThemeMode.DARK -> Icons.Default.DarkMode
                        com.example.data.model.ThemeMode.AUTO -> Icons.Default.AutoAwesome
                    },
                    contentDescription = "Theme: ${themeMode.displayName}",
                    tint = when (themeMode) {
                        com.example.data.model.ThemeMode.LIGHT -> Color(0xFFF59E0B) // warm amber sun
                        com.example.data.model.ThemeMode.DARK -> Color(0xFF818CF8) // indigo moon
                        com.example.data.model.ThemeMode.AUTO -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shadowElevation = 1.dp,
            onClick = onSearchClick,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shadowElevation = 1.dp,
            onClick = onNotificationClick,
            modifier = Modifier
                .size(42.dp)
                .testTag("home_notification_btn")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                // Red badge indicator for active alerts
                if (hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 9.dp, end = 9.dp)
                            .size(8.dp)
                            .background(ExpenseRed, CircleShape)
                            .testTag("home_notification_badge")
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSummaryCardsSection(
    totalBalance: Double,
    spentThisMonth: Double,
    isBalanceVisible: Boolean,
    currencySymbol: String,
    isBangla: Boolean = false,
    isGradient: Boolean = false,
    onToggleVisibility: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeSummaryCard(
            label = if (isBangla) "মোট ব্যালেন্স" else "Total Balance",
            amount = if (isBalanceVisible) CurrencyFormatter.format(totalBalance, currencySymbol) else "••••••••",
            isGradient = isGradient,
            onClick = onToggleVisibility,
            testTag = "home_total_balance_card",
            modifier = Modifier.weight(1f)
        )

        HomeSummaryCard(
            label = if (isBangla) "এই মাসের খরচ" else "Spent This Month",
            amount = if (isBalanceVisible) CurrencyFormatter.format(spentThisMonth, currencySymbol) else "••••••••",
            isGradient = isGradient,
            onClick = onToggleVisibility,
            testTag = "home_spent_this_month_card",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HomeSummaryCard(
    label: String,
    amount: String,
    isGradient: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    if (isGradient) {
        Surface(
            modifier = modifier
                .testTag(testTag)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF4338CA),
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF4F46E5), Color(0xFF6366F1), Color(0xFF4338CA))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    } else {
        TranslucentCard(
            modifier = modifier.testTag(testTag),
            cornerRadius = 22.dp,
            onClick = onClick
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StackedAccountCards(
    accounts: List<AccountEntity>,
    isBalanceVisible: Boolean,
    currencySymbol: String,
    isBangla: Boolean = false,
    onAccountClick: (AccountEntity) -> Unit,
    onAllAccountsClick: () -> Unit
) {
    val visibleAccounts = accounts.filter { !it.isHidden }
    if (visibleAccounts.isEmpty()) {
        TranslucentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            cornerRadius = 26.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isBangla) "কোনো অ্যাকাউন্ট নেই" else "No accounts yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBangla) "লেনদেন শুরু করতে একটি অ্যাকাউন্ট যোগ করুন" else "Add your Bank, MFS, or Cash account to get started",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                androidx.compose.material3.Button(
                    onClick = onAllAccountsClick,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(text = if (isBangla) "অ্যাকাউন্ট যোগ করুন" else "Add Account")
                }
            }
        }
        return
    }

    var topIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val n = visibleAccounts.size
    val activeIndex = ((topIndex % n) + n) % n

    TranslucentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        cornerRadius = 26.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Container Header: "+X more" and "All accounts >"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val remainingCount = (n - 1).coerceAtLeast(0)
                val remainingText = if (remainingCount > 0) {
                    if (isBangla) "+${CurrencyFormatter.convertToBanglaDigits(remainingCount.toString())} আরও" else "+$remainingCount more"
                } else {
                    if (isBangla) "১টি অ্যাকাউন্ট" else "1 account"
                }
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontSize = 13.5.sp
                    )
                )
                Row(
                    modifier = Modifier.clickable { onAllAccountsClick() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isBangla) "সকল অ্যাকাউন্ট" else "All accounts",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stack Container with Genuine Layering & Butter-Smooth Bidirectional Animation
            val peekStep = 40.dp
            val cardHeight = 188.dp
            val totalStackHeight = when {
                n >= 3 -> cardHeight + peekStep * 2
                n == 2 -> cardHeight + peekStep
                else -> cardHeight
            }

            val dragOffset = dragOffsetX.value
            val dragProgress = (kotlin.math.abs(dragOffset) / 220f).coerceIn(0f, 1f)

            val frontBaseY = when {
                n >= 3 -> peekStep * 2
                n == 2 -> peekStep
                else -> 0.dp
            }
            val midBaseY = if (n >= 3) peekStep else 0.dp
            val backBaseY = 0.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalStackHeight)
            ) {
                // If 3 or more accounts: Render backmost card (index activeIndex + 2)
                if (n >= 3) {
                    val backIdx = (activeIndex + 2) % n
                    val backAcc = visibleAccounts[backIdx]

                    val dynamicBackY = if (dragOffset < 0) {
                        backBaseY + (midBaseY - backBaseY) * dragProgress
                    } else {
                        backBaseY
                    }
                    val dynamicBackScale = if (dragOffset < 0) {
                        0.94f + (0.97f - 0.94f) * dragProgress
                    } else {
                        0.94f
                    }

                    StackedWalletCard(
                        account = backAcc,
                        currencySymbol = currencySymbol,
                        isBalanceVisible = isBalanceVisible,
                        isBangla = isBangla,
                        isFrontCard = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeight)
                            .offset(y = dynamicBackY)
                            .graphicsLayer {
                                scaleX = dynamicBackScale
                                scaleY = dynamicBackScale
                            }
                            .zIndex(1f)
                            .clickable {
                                coroutineScope.launch {
                                    topIndex = backIdx
                                }
                            }
                    )
                }

                // If 2 or more accounts: Render middle card (index activeIndex + 1)
                if (n >= 2) {
                    val midIdx = (activeIndex + 1) % n
                    val midAcc = visibleAccounts[midIdx]

                    val dynamicMidY = if (dragOffset < 0) {
                        midBaseY + (frontBaseY - midBaseY) * dragProgress
                    } else {
                        midBaseY - ((midBaseY - backBaseY) * dragProgress * 0.5f)
                    }
                    val dynamicMidScale = if (dragOffset < 0) {
                        0.97f + (1.0f - 0.97f) * dragProgress
                    } else {
                        0.97f - (0.03f * dragProgress)
                    }

                    StackedWalletCard(
                        account = midAcc,
                        currencySymbol = currencySymbol,
                        isBalanceVisible = isBalanceVisible,
                        isBangla = isBangla,
                        isFrontCard = (dragOffset < 0 && dragProgress > 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeight)
                            .offset(y = dynamicMidY)
                            .graphicsLayer {
                                scaleX = dynamicMidScale
                                scaleY = dynamicMidScale
                            }
                            .zIndex(2f)
                            .clickable {
                                coroutineScope.launch {
                                    topIndex = midIdx
                                }
                            }
                    )
                }

                // Active Front Card (interactive & swipeable)
                val activeAcc = visibleAccounts[activeIndex]
                val frontDynamicY = if (dragOffset > 0) {
                    frontBaseY - ((frontBaseY - midBaseY) * dragProgress * 0.4f)
                } else {
                    frontBaseY
                }
                val frontDynamicScale = if (dragOffset > 0) {
                    1.0f - (0.03f * dragProgress)
                } else {
                    1.0f
                }

                StackedWalletCard(
                    account = activeAcc,
                    currencySymbol = currencySymbol,
                    isBalanceVisible = isBalanceVisible,
                    isBangla = isBangla,
                    isFrontCard = (dragOffset <= 0 || dragProgress < 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .offset(y = frontDynamicY)
                        .offset {
                            IntOffset(if (dragOffset < 0) dragOffsetX.value.roundToInt() else 0, 0)
                        }
                        .graphicsLayer {
                            scaleX = frontDynamicScale
                            scaleY = frontDynamicScale
                            if (dragOffset < 0) {
                                rotationZ = (dragOffsetX.value / 35f).coerceIn(-12f, 0f)
                                alpha = (1f - (dragProgress * 0.15f)).coerceIn(0.7f, 1f)
                            }
                        }
                        .zIndex(3f)
                        .pointerInput(n, activeIndex) {
                            if (n > 1) {
                                detectHorizontalDragGestures(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = {
                                        isDragging = false
                                        coroutineScope.launch {
                                            if (dragOffsetX.value < -70f) {
                                                // Swipe Left: animate current card off-screen left, advance to next card
                                                dragOffsetX.animateTo(-500f, tween(180, easing = FastOutSlowInEasing))
                                                topIndex = (topIndex + 1) % n
                                                dragOffsetX.snapTo(0f)
                                            } else if (dragOffsetX.value > 70f) {
                                                // Swipe Right: animate previous card smoothly into position, move to previous card
                                                dragOffsetX.animateTo(220f, tween(180, easing = FastOutSlowInEasing))
                                                topIndex = if (topIndex - 1 < 0) n - 1 else topIndex - 1
                                                dragOffsetX.snapTo(0f)
                                            } else {
                                                // Spring back smoothly into resting position
                                                dragOffsetX.animateTo(
                                                    0f,
                                                    spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                                )
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        coroutineScope.launch {
                                            dragOffsetX.animateTo(0f, spring(dampingRatio = 0.82f))
                                        }
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch {
                                            dragOffsetX.snapTo(dragOffsetX.value + dragAmount)
                                        }
                                    }
                                )
                            }
                        }
                        .clickable {
                            if (kotlin.math.abs(dragOffsetX.value) < 10f) {
                                onAccountClick(activeAcc)
                            }
                        }
                )

                // When dragging right: previous card slides in smoothly from the left over the deck
                if (dragOffset > 0 && n > 1) {
                    val prevIdx = if (activeIndex - 1 < 0) n - 1 else activeIndex - 1
                    val prevAcc = visibleAccounts[prevIdx]
                    val prevX = (-360f * (1f - dragProgress)).dp
                    val prevRotation = (-8f * (1f - dragProgress))

                    StackedWalletCard(
                        account = prevAcc,
                        currencySymbol = currencySymbol,
                        isBalanceVisible = isBalanceVisible,
                        isBangla = isBangla,
                        isFrontCard = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeight)
                            .offset(y = frontBaseY)
                            .offset(x = prevX)
                            .graphicsLayer {
                                rotationZ = prevRotation
                            }
                            .zIndex(4f)
                    )
                }
            }
        }
    }
}

/**
 * Resolves account card color matching account creation and detail screens.
 * Uses saved colorHex, or falls back to official brand colors (bKash #E2136E, Nagad #F26522, etc.).
 */
private fun resolveAccountCardColor(account: AccountEntity): Color {
    val hex = account.colorHex.trim()
    if (hex.isNotBlank() && hex != "#000000" && hex != "0") {
        try {
            return Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {}
    }
    return if (account.type == AccountType.MFS) {
        val prov = (account.institution.ifBlank { account.name }).lowercase()
        when {
            prov.contains("bkash") -> Color(0xFFE2136E)
            prov.contains("nagad") -> Color(0xFFF26522)
            prov.contains("rocket") -> Color(0xFF8C3494)
            prov.contains("upay") -> Color(0xFF005BAC)
            prov.contains("tap") -> Color(0xFF0084C7)
            prov.contains("cellfin") -> Color(0xFF008848)
            else -> Color(0xFF00897B)
        }
    } else when (account.type) {
        AccountType.CREDIT_CARD -> Color(0xFF1E293B)
        AccountType.BANK -> Color(0xFF2563EB)
        AccountType.CASH -> Color(0xFF059669)
        else -> Color(0xFF4F46E5)
    }
}

@Composable
private fun StackedWalletCard(
    account: AccountEntity,
    currencySymbol: String,
    isBalanceVisible: Boolean,
    isBangla: Boolean,
    isFrontCard: Boolean,
    modifier: Modifier = Modifier
) {
    val cardColor = resolveAccountCardColor(account)

    val network = remember(account.notes) {
        account.notes.split("|").firstOrNull { it.startsWith("Network:") }?.removePrefix("Network:")?.trim()
    }
    val subtype = remember(account.notes) {
        account.notes.split("|").firstOrNull { it.startsWith("Subtype:") }?.removePrefix("Subtype:")?.trim()
    }
    val isCredit = account.type == AccountType.CREDIT_CARD
    val displayAmount = if (isCredit) {
        if (account.balance < 0) kotlin.math.abs(account.balance) else account.balance
    } else {
        account.balance
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        shadowElevation = if (isFrontCard) 4.dp else 2.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            cardColor,
                            cardColor.copy(alpha = 0.88f),
                            Color(0xFF0F172A).copy(alpha = 0.42f)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row:
                // Background cards: Icon + Name (left) and Balance (right)
                // Front card: Icon + Name + Subtitle (left), EMV Chip + Contactless / Cash (right).
                // Duplicate balance on top is HIDDEN on the front card.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(if (isFrontCard) 36.dp else 28.dp)
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
                                    modifier = Modifier.size(if (isFrontCard) 18.dp else 15.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isFrontCard) 14.5.sp else 13.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isFrontCard) {
                                val subtitleText = when (account.type) {
                                    AccountType.CREDIT_CARD -> if (!network.isNullOrBlank()) "$network Credit Card" else account.institution.ifBlank { "Credit Card" }
                                    AccountType.BANK -> subtype?.ifBlank { null } ?: (if (!network.isNullOrBlank()) "$network Debit Card" else account.institution.ifBlank { "Debit Card" })
                                    AccountType.CASH -> "Cash"
                                    AccountType.MFS -> account.institution.ifBlank { "MFS Wallet" }
                                    else -> account.type.displayName
                                }
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (isFrontCard) {
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
                                    EmvChip(width = 30.dp, height = 21.dp)
                                }
                            }
                            AccountType.CASH -> {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.20f),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = "Cash",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    } else {
                        // Background Card: Legible balance on the right of top tab
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isBalanceVisible) CurrencyFormatter.format(displayAmount, currencySymbol) else "••••••",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            )
                            if (account.type == AccountType.BANK || account.type == AccountType.CREDIT_CARD) {
                                Icon(
                                    imageVector = Icons.Default.Contactless,
                                    contentDescription = "Contactless",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Middle: Prominent balance display on the left (Front Card)
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = if (isCredit) {
                            if (isBangla) "বকেয়া (আউটস্ট্যান্ডিং)" else "OUTSTANDING"
                        } else {
                            if (isBangla) "ব্যালেন্স" else "BALANCE"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBalanceVisible) CurrencyFormatter.format(displayAmount, currencySymbol) else "••••••••",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                    )
                }

                // Bottom row: Institution / Note (Left) and Network / Provider Branding + Masked Number (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bottomSubtitle = when (account.type) {
                        AccountType.CREDIT_CARD -> account.institution.ifBlank { "Credit Card" }
                        AccountType.BANK -> account.institution.ifBlank { subtype ?: "Debit Card" }
                        AccountType.CASH -> "Cash"
                        AccountType.MFS -> account.type.displayName
                        else -> account.type.displayName
                    }
                    Text(
                        text = bottomSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (account.type == AccountType.MFS) {
                            val providerName = account.institution.ifBlank { account.name }
                            MfsProviderLogo(provider = providerName)
                        } else if (account.type == AccountType.BANK || account.type == AccountType.CREDIT_CARD) {
                            if (!network.isNullOrBlank()) {
                                CardNetworkLogo(network = network, modifier = Modifier.height(18.dp))
                            }
                        }

                        if (account.accountNumberLast4.isNotBlank()) {
                            val masked = if (isBangla) CurrencyFormatter.convertToBanglaDigits(account.accountNumberLast4) else account.accountNumberLast4
                            Text(
                                text = "•••• $masked",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            )
                        } else if (account.type == AccountType.CASH) {
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun AccountCardsCarousel(
    accounts: List<AccountEntity>,
    isBalanceVisible: Boolean,
    currencySymbol: String,
    onAccountClick: (AccountEntity) -> Unit
) {
    if (accounts.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(accounts.filter { !it.isHidden }) { acc ->
            val cardColor = resolveAccountCardColor(acc)

            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .height(160.dp),
                shape = RoundedCornerShape(24.dp),
                color = cardColor,
                shadowElevation = 4.dp,
                onClick = { onAccountClick(acc) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(cardColor, cardColor.copy(alpha = 0.82f))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.22f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (acc.type) {
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
                                        text = acc.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1
                                    )
                                    if (acc.institution.isNotBlank()) {
                                        Text(
                                            text = acc.institution,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White.copy(alpha = 0.78f),
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            val isCard = acc.type == AccountType.BANK || acc.type == AccountType.CREDIT_CARD
                            if (isCard) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Contactless,
                                        contentDescription = "Contactless",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    com.example.ui.screens.accounts.EmvChip(
                                        modifier = Modifier.size(24.dp, 17.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        val isCredit = acc.type == AccountType.CREDIT_CARD
                        val displayAmount = if (isCredit) {
                            if (acc.balance < 0) kotlin.math.abs(acc.balance) else acc.balance
                        } else {
                            acc.balance
                        }

                        Text(
                            text = if (isCredit) "OUTSTANDING" else "BALANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = if (isBalanceVisible) CurrencyFormatter.format(displayAmount, currencySymbol) else "••••••",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val isCardType = acc.type == AccountType.BANK || acc.type == AccountType.CREDIT_CARD
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = acc.type.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (acc.type == AccountType.MFS) {
                                    val providerName = acc.institution.ifBlank { acc.name }
                                    com.example.ui.screens.accounts.MfsProviderLogo(
                                        provider = providerName
                                    )
                                } else if (acc.accountNumberLast4.isNotBlank()) {
                                    Text(
                                        text = "•••• ${acc.accountNumberLast4}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                if (isCardType) {
                                    val cardNetwork = if (acc.notes.contains("Network:")) {
                                        acc.notes.substringAfter("Network:").substringBefore("\n").trim()
                                    } else ""
                                    if (cardNetwork.isNotBlank()) {
                                        com.example.ui.screens.accounts.CardNetworkLogo(
                                            network = cardNetwork,
                                            modifier = Modifier.height(20.dp)
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

@Composable
private fun QuickShortcutsRow(
    isBangla: Boolean = false,
    onTransferClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onVoiceEntryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_section_shortcuts")
    ) {
        SectionHeader(
            title = if (isBangla) "শর্টকাটস" else "Shortcuts"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionButton(
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                label = if (isBangla) "স্থানান্তর" else "Transfer",
                onClick = onTransferClick
            )
            QuickActionButton(
                icon = Icons.Default.CameraAlt,
                label = if (isBangla) "রসিদ স্ক্যান" else "Scan receipt",
                onClick = onScanReceiptClick
            )
            QuickActionButton(
                icon = Icons.Default.ReceiptLong,
                label = if (isBangla) "ব্যয়" else "Expense",
                onClick = onExpenseClick
            )
            QuickActionButton(
                icon = Icons.Default.Mic,
                label = if (isBangla) "ভয়েস এন্ট্রি" else "Voice Entry",
                onClick = onVoiceEntryClick
            )
        }
    }
}

private data class HomeSectionMeta(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val isChecked: (HomeCustomization) -> Boolean,
    val updateChecked: (HomeCustomization, Boolean) -> HomeCustomization
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomizeHomeBottomSheet(
    current: HomeCustomization,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (HomeCustomization) -> Unit
) {
    val activeCount = listOf(
        current.showBudgetProgress,
        current.showUpcomingBills,
        current.showRecentActivity,
        current.showSavingsGoals,
        current.showFinancialHealth,
        current.showSmartInsights,
        current.showTopSpending,
        current.showSpendingByDay,
        current.showSpendingStreak,
        current.showQuickShortcuts
    ).count { it }

    val sectionMetaList = remember(isBangla) {
        listOf(
            HomeSectionMeta("budget", Icons.Default.PieChart, if (isBangla) "মাসিক বাজেট" else "Monthly budget", if (isBangla) "ক্যাটাগরি অনুযায়ী অবশিষ্ট বাজেট" else "What's left this month, by category", { it.showBudgetProgress }, { c, v -> c.copy(showBudgetProgress = v) }),
            HomeSectionMeta("bills", Icons.Default.ReceiptLong, if (isBangla) "আসন্ন বিলসমূহ" else "Upcoming bills", if (isBangla) "পরবর্তী পরিশোধযোগ্য বিল" else "The next three payments due", { it.showUpcomingBills }, { c, v -> c.copy(showUpcomingBills = v) }),
            HomeSectionMeta("recent_activity", Icons.AutoMirrored.Filled.CompareArrows, if (isBangla) "সাম্প্রতিক কার্যকলাপ" else "Recent activity", if (isBangla) "আপনার সাম্প্রতিক লেনদেন" else "Your latest transactions", { it.showRecentActivity }, { c, v -> c.copy(showRecentActivity = v) }),
            HomeSectionMeta("savings", Icons.Default.Flag, if (isBangla) "সঞ্চয় লক্ষ্য" else "Savings goals", if (isBangla) "প্রতিটি লক্ষ্যের অগ্রগতি" else "Progress toward each goal", { it.showSavingsGoals }, { c, v -> c.copy(showSavingsGoals = v) }),
            HomeSectionMeta("financial_health", Icons.Default.Favorite, if (isBangla) "আর্থিক স্বাস্থ্য" else "Financial health", if (isBangla) "আপনার স্কোর এবং সঞ্চয়ের হার" else "Your score and savings rate", { it.showFinancialHealth }, { c, v -> c.copy(showFinancialHealth = v) }),
            HomeSectionMeta("smart_insights", Icons.Default.Psychology, if (isBangla) "স্মার্ট অন্তর্দৃষ্টি" else "Smart insight", if (isBangla) "এই মাসের গুরুত্বপূর্ণ তথ্য" else "One thing worth knowing this month", { it.showSmartInsights }, { c, v -> c.copy(showSmartInsights = v) }),
            HomeSectionMeta("top_spending", Icons.Default.Category, if (isBangla) "শীর্ষ ব্যয়" else "Top spending", if (isBangla) "আপনার অর্থ কোথায় ব্যয় হয়েছে" else "Where your money went", { it.showTopSpending }, { c, v -> c.copy(showTopSpending = v) }),
            HomeSectionMeta("spending_by_day", Icons.Default.BarChart, if (isBangla) "দিনভিত্তিক ব্যয়" else "Spending by day", if (isBangla) "কোন দিনগুলোতে বেশি ব্যয় হয়" else "Which weekdays cost the most", { it.showSpendingByDay }, { c, v -> c.copy(showSpendingByDay = v) }),
            HomeSectionMeta("spending_streak", Icons.Default.LocalFireDepartment, if (isBangla) "ব্যয় নিয়ন্ত্রণ স্ট্রিক" else "Spending streak", if (isBangla) "দৈনিক গড়ের নিচে থাকা দিনসমূহ" else "Days under your daily average", { it.showSpendingStreak }, { c, v -> c.copy(showSpendingStreak = v) }),
            HomeSectionMeta("shortcuts", Icons.Default.Bolt, if (isBangla) "শর্টকাটস" else "Shortcuts", if (isBangla) "পিন করা প্রয়োজনীয় টুলস" else "Your pinned tools", { it.showQuickShortcuts }, { c, v -> c.copy(showQuickShortcuts = v) })
        )
    }
    val metaMap = remember(sectionMetaList) { sectionMetaList.associateBy { it.id } }

    var localSectionOrder by remember {
        mutableStateOf(
            UserSettingsPreferences.normalizeHomeSectionOrder(current.sectionOrder)
                .filter { metaMap.containsKey(it) }
                .toMutableList()
        )
    }

    val bottomSheetListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    val dragDropState = rememberDragDropListState(
        lazyListState = bottomSheetListState,
        indexOffset = 1,
        onMove = { fromIndex, toIndex ->
            if (fromIndex in localSectionOrder.indices && toIndex in localSectionOrder.indices && fromIndex != toIndex) {
                val list = localSectionOrder.toMutableList()
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                localSectionOrder = list
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        },
        onDragEnd = {
            onUpdate(current.copy(sectionOrder = localSectionOrder))
        },
        onDragCancel = {
            localSectionOrder = UserSettingsPreferences.normalizeHomeSectionOrder(current.sectionOrder)
                .filter { metaMap.containsKey(it) }
                .toMutableList()
        },
        keyProvider = { idx ->
            if (idx in localSectionOrder.indices) "sec_reorder_${localSectionOrder[idx]}" else idx
        },
        liveReorder = true
    )

    LaunchedEffect(current.sectionOrder) {
        if (!dragDropState.isDragging) {
            val normalized = UserSettingsPreferences.normalizeHomeSectionOrder(current.sectionOrder)
                .filter { metaMap.containsKey(it) }
            if (normalized != localSectionOrder) {
                localSectionOrder = normalized.toMutableList()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 8.dp)
                        .size(width = 44.dp, height = 4.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                ) {}
            }
        }
    ) {
        LazyColumn(
            state = bottomSheetListState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 36.dp)
        ) {
            // Title + Reset Button Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBangla) "হোম কাস্টমাইজ করুন" else "Customise home",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = if (isBangla) "রিসেট" else "Reset",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4F46E5),
                            fontSize = 15.sp
                        ),
                        modifier = Modifier
                            .clickable {
                                localSectionOrder = com.example.ui.DEFAULT_HOME_SECTIONS.toMutableList()
                                onUpdate(
                                    HomeCustomization(
                                        cardStyle = HomeCardStyle.STACK,
                                        balanceCardStyle = BalanceCardStyle.STANDARD,
                                        showBudgetProgress = true,
                                        showUpcomingBills = true,
                                        showRecentActivity = true,
                                        showSavingsGoals = false,
                                        showFinancialHealth = false,
                                        showSmartInsights = false,
                                        showTopSpending = false,
                                        showSpendingByDay = false,
                                        showSpendingStreak = false,
                                        showQuickShortcuts = false,
                                        sectionOrder = com.example.ui.DEFAULT_HOME_SECTIONS
                                    )
                                )
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                val subtitleText = if (isBangla) {
                    "${CurrencyFormatter.convertToBanglaDigits(activeCount.toString())}টি সেকশন সক্রিয় (মোট ১০টির মধ্যে)"
                } else {
                    "$activeCount of 10 sections on"
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(22.dp))

                // TOP OF HOME Category Header
                Text(
                    text = if (isBangla) "হোমের শীর্ষে" else "TOP OF HOME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        letterSpacing = 1.2.sp,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                // TOP OF HOME Options (Balance card vs Card stack)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Option 1: Balance card
                    val isBalanceSelected = current.cardStyle == HomeCardStyle.CAROUSEL
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isBalanceSelected) 2.dp else 1.dp,
                            color = if (isBalanceSelected) Color(0xFF4F46E5) else Color.LightGray.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onUpdate(current.copy(cardStyle = HomeCardStyle.CAROUSEL))
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Mini illustration
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(86.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF8FAFC)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Miniature Balance card
                                Box(
                                    modifier = Modifier
                                        .size(width = 96.dp, height = 50.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF5A45FF))
                                        .padding(horizontal = 10.dp, vertical = 12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .width(54.dp)
                                                .height(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.75f))
                                        )
                                    }
                                }

                                if (isBalanceSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4F46E5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isBangla) "ব্যালেন্স কার্ড" else "Balance card",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (isBangla) "মোট ও চলতি মাস" else "Your total and this month",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    // Option 2: Card stack
                    val isStackSelected = current.cardStyle == HomeCardStyle.STACK
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isStackSelected) 2.dp else 1.dp,
                            color = if (isStackSelected) Color(0xFF4F46E5) else Color.LightGray.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onUpdate(current.copy(cardStyle = HomeCardStyle.STACK))
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Mini illustration with 3 overlapping cards
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(86.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF8FAFC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(width = 100.dp, height = 54.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    // Card 1 (teal backmost)
                                    Box(
                                        modifier = Modifier
                                            .size(width = 92.dp, height = 40.dp)
                                            .offset(y = (-10).dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0D9488))
                                    )
                                    // Card 2 (blue middle)
                                    Box(
                                        modifier = Modifier
                                            .size(width = 95.dp, height = 42.dp)
                                            .offset(y = (-5).dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF2563EB))
                                    )
                                    // Card 3 (magenta/pink front)
                                    Box(
                                        modifier = Modifier
                                            .size(width = 98.dp, height = 44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFFDB2777), Color(0xFFE11D48))
                                                )
                                            )
                                            .padding(6.dp)
                                    ) {
                                        // Miniature gold chip
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .size(width = 11.dp, height = 7.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFFF59E0B))
                                        )
                                    }
                                }

                                if (isStackSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4F46E5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isBangla) "কার্ড স্ট্যাক" else "Card stack",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (isBangla) "প্রতিটি অ্যাকাউন্ট একটি কার্ড" else "Each account as a card",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SECTIONS Category Header with instruction
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isBangla) "সেকশনসমূহ" else "SECTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isBangla) "সেকশন সরাতে হ্যান্ডেলটি (⠿) চেপে ধরে রাখুন" else "Press and hold the handle to move sections",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                            fontSize = 12.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Dynamic Reorderable Sections with live visual feedback, placeholder gap, and smooth animation
            items(
                items = localSectionOrder,
                key = { secId -> "sec_reorder_$secId" }
            ) { secId ->
                val meta = metaMap[secId]
                if (meta != null) {
                    val isBeingDragged = dragDropState.isDragging && dragDropState.draggedItemId == "sec_reorder_$secId"
                    val index = localSectionOrder.indexOf(secId)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (!isBeingDragged) {
                                    Modifier.animateItem()
                                } else {
                                    Modifier
                                }
                            )
                            .zIndex(if (isBeingDragged) 10f else 1f)
                    ) {
                        // Display insertion drop target placeholder in the layout behind the floating dragged item
                        if (isBeingDragged) {
                            val isDark = isSystemInDarkTheme()
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF312E81).copy(alpha = 0.35f) else Color(0xFFEEF2FF).copy(alpha = 0.75f),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if (isDark) Color(0xFF818CF8).copy(alpha = 0.60f) else Color(0xFF6366F1).copy(alpha = 0.50f)
                                ),
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = if (isBangla) "এখানে ড্রপ করুন" else "Drop section here",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Visibly lift selected row using elevation, subtle shadow, and highlighted border
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shadowElevation = if (isBeingDragged) 8.dp else 0.dp,
                            border = if (isBeingDragged) BorderStroke(1.5.dp, Color(0xFF4F46E5)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    if (isBeingDragged) {
                                        translationY = dragDropState.translationY
                                        scaleX = 1.01f
                                        scaleY = 1.01f
                                    }
                                }
                                .padding(vertical = if (isBeingDragged) 2.dp else 0.dp)
                        ) {
                            CustomizeSectionItem(
                                secId = secId,
                                icon = meta.icon,
                                title = meta.title,
                                subtitle = meta.subtitle,
                                checked = meta.isChecked(current),
                                isBeingDragged = isBeingDragged,
                                index = index,
                                listSize = localSectionOrder.size,
                                dragDropState = dragDropState,
                                haptic = haptic,
                                onCheckedChange = { isChecked ->
                                    val updated = meta.updateChecked(current, isChecked)
                                    onUpdate(updated)
                                }
                            )
                        }
                    }

                    if (!isBeingDragged && index < localSectionOrder.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizeSectionItem(
    secId: String,
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    isBeingDragged: Boolean,
    index: Int,
    listSize: Int,
    dragDropState: DragDropListState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isBeingDragged) { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEEF2FF),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(0.85f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4F46E5),
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFFE2E8F0),
                    uncheckedBorderColor = Color(0xFFCBD5E1)
                )
            )

            // Standard Six-Dot Drag Handle (⠿) - Long press starts dragging with haptic feedback
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isBeingDragged) Color(0xFF4F46E5).copy(alpha = 0.15f) else Color.Transparent,
                modifier = Modifier
                    .size(width = 44.dp, height = 48.dp)
                    .reorderableDragHandle(
                        key = "sec_reorder_$secId",
                        index = index,
                        listSize = listSize,
                        state = dragDropState,
                        onLongPressFeedback = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SixDotDragHandle(
                        tint = if (isBeingDragged) Color(0xFF4F46E5) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@Composable
private fun MonthlyBudgetSection(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    onDetailsClick: () -> Unit
) {
    val (refYear, refMonth) = remember(transactions, budgets) {
        FinancialIntegrityValidator.resolveReferenceYearMonth(transactions, budgets)
    }
    val monthKey = remember(refYear, refMonth) {
        String.format(java.util.Locale.US, "%04d-%02d", refYear, refMonth + 1)
    }
    val monthName = remember(refYear, refMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, refYear)
            set(Calendar.MONTH, refMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        java.text.SimpleDateFormat("MMMM", java.util.Locale.ENGLISH).format(cal.time)
    }

    val monthBudgets = remember(budgets, monthKey) {
        val exact = budgets.filter { it.monthYear == monthKey || it.monthYear == "ALL" }
        if (exact.isNotEmpty()) exact else budgets
    }

    val monthExpenseTransactions = remember(transactions, refYear, refMonth) {
        transactions.filter { tx ->
            if (!FinancialIntegrityValidator.isEligibleExpense(tx)) return@filter false
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            cal.get(Calendar.YEAR) == refYear && cal.get(Calendar.MONTH) == refMonth
        }
    }

    val definedBudget = monthBudgets.firstOrNull { it.isOverall }?.monthlyLimit
        ?: monthBudgets.filter { !it.isOverall }.sumOf { it.monthlyLimit }.takeIf { it > 0.0 }
    val hasBudget = definedBudget != null && definedBudget > 0.0
    val overallBudget = definedBudget ?: 0.0
    val totalExpense = monthExpenseTransactions.sumOf { it.amount }

    val percentage = if (overallBudget > 0) ((totalExpense / overallBudget) * 100).toInt().coerceIn(0, 999) else 0
    val remaining = overallBudget - totalExpense

    val isBn = CurrencyFormatter.currentLanguage == com.example.data.model.AppLanguage.BANGLA
    val localizedMonth = if (isBn) DateFormatter.localizeText(monthName, com.example.data.model.AppLanguage.BANGLA) else monthName

    TranslucentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("home_section_budget"),
        onClick = onDetailsClick
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "$localizedMonth মাসের বাজেট" else "$monthName budget",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
                Text(
                    text = if (hasBudget) (if (isBn) "বিস্তারিত" else "Details") else (if (isBn) "বাজেট সেট করুন" else "Set Limit"),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!hasBudget) {
                Text(
                    text = if (isBn) "কোন মাসিক বাজেট নির্ধারিত নেই" else "No monthly budget set",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBn) "একটি সামগ্রিক বা ক্যাটাগরিভিত্তিক মাসিক খরচের সীমা নির্ধারণ করতে ট্যাপ করুন।" else "Tap to set an overall or category monthly spending limit.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val spentStr = CurrencyFormatter.format(totalExpense, currencySymbol)
                    val budgetStr = CurrencyFormatter.format(overallBudget, currencySymbol)
                    Text(
                        text = if (isBn) "$spentStr / $budgetStr" else "$spentStr of $budgetStr",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    val pctStr = if (isBn) CurrencyFormatter.convertToBanglaDigits("$percentage%") else "$percentage%"
                    Text(
                        text = pctStr,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (percentage > 90) ExpenseRed else MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { if (overallBudget > 0) (totalExpense / overallBudget).toFloat().coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (percentage > 90) ExpenseRed else Color(0xFFF97316),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                val perDay = (remaining / 7).toInt().coerceAtLeast(0)
                val statusText = if (remaining >= 0) {
                    if (isBn) {
                        "${CurrencyFormatter.format(remaining, currencySymbol)} অবশিষ্ট · ${CurrencyFormatter.format(perDay.toDouble(), currencySymbol)}/দিন বাকি"
                    } else {
                        "${CurrencyFormatter.format(remaining, currencySymbol)} left · $currencySymbol$perDay/day remaining"
                    }
                } else {
                    if (isBn) {
                        "বাজেটের চেয়ে ${CurrencyFormatter.format(kotlin.math.abs(remaining), currencySymbol)} বেশি খরচ হয়েছে"
                    } else {
                        "Over budget by ${CurrencyFormatter.format(kotlin.math.abs(remaining), currencySymbol)}"
                    }
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                )

                val categoryBudgets = monthBudgets.filter { !it.isOverall }.take(3)
                if (categoryBudgets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    categoryBudgets.forEach { catBgt ->
                        val catSpent = monthExpenseTransactions
                            .filter { it.category.equals(catBgt.categoryName, ignoreCase = true) }
                            .sumOf { it.amount }
                        val catProgress = if (catBgt.monthlyLimit > 0) (catSpent / catBgt.monthlyLimit).toFloat().coerceIn(0f, 1f) else 0f
                        val catIsOver = catSpent > catBgt.monthlyLimit
                        val catPct = if (catBgt.monthlyLimit > 0) (catSpent / catBgt.monthlyLimit * 100).toInt() else 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = catBgt.categoryName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1.3f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .weight(2f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = catProgress)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (catIsOver) ExpenseRed else MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            val catPctStr = if (isBn) CurrencyFormatter.convertToBanglaDigits("$catPct%") else "$catPct%"
                            Text(
                                text = catPctStr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (catIsOver) ExpenseRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBn) "ক্যাটাগরি ড্যাশবোর্ড দেখুন →" else "Open Category Dashboard →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun resolveHomeCategoryVisual(
    categoryName: String,
    categories: List<CategoryEntity>,
    fallbackColorHex: String
): Pair<String, String> {
    val matched = categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
    if (matched != null) {
        return matched.iconName to matched.colorHex
    }
    val lower = categoryName.lowercase()
    val icon = when {
        lower.contains("rent") || lower.contains("housing") || lower.contains("util") -> "home"
        lower.contains("grocer") || lower.contains("essential") -> "shopping_cart"
        lower.contains("food") || lower.contains("dining") || lower.contains("eat") -> "restaurant"
        lower.contains("transport") || lower.contains("fuel") -> "directions_car"
        lower.contains("health") || lower.contains("medical") -> "medical_services"
        lower.contains("shop") -> "shopping_bag"
        lower.contains("educat") -> "school"
        lower.contains("entertain") -> "movie"
        lower.contains("salary") || lower.contains("income") -> "payments"
        else -> "category"
    }
    return icon to fallbackColorHex
}

@Composable
private fun TopSpendingSection(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity> = emptyList(),
    currencySymbol: String,
    isBangla: Boolean = false,
    onInsightsClick: () -> Unit
) {
    val (refYear, refMonth) = remember(transactions) {
        FinancialIntegrityValidator.resolveReferenceYearMonth(transactions, emptyList())
    }
    val categoryTotals = remember(transactions, refYear, refMonth) {
        transactions
            .filter { tx ->
                if (!FinancialIntegrityValidator.isEligibleExpense(tx)) return@filter false
                val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                cal.get(Calendar.YEAR) == refYear && cal.get(Calendar.MONTH) == refMonth
            }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    val maxAmount = categoryTotals.firstOrNull()?.second?.coerceAtLeast(1.0) ?: 1.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_section_top_spending")
    ) {
        SectionHeader(
            title = if (isBangla) "শীর্ষ ব্যয়" else "Top spending",
            actionText = if (isBangla) "ইনসাইটস" else "Insights",
            onActionClick = onInsightsClick
        )

        TranslucentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            onClick = onInsightsClick
        ) {
            if (categoryTotals.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (isBangla) "এই মাসে কোনো ব্যয় রেকর্ড হয়নি" else "No spending recorded this month",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isBangla) "শীর্ষ ব্যয়ের ক্যাটাগরি দেখতে একটি ব্যয় যোগ করুন।" else "Log an expense to see where your money went.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    categoryTotals.forEachIndexed { index, (cat, amt) ->
                        val fallbackColor = when (index) {
                            0 -> "#3B82F6"
                            1 -> "#10B981"
                            2 -> "#6366F1"
                            3 -> "#F59E0B"
                            else -> "#EC4899"
                        }
                        val (iconName, colorHex) = resolveHomeCategoryVisual(cat, categories, fallbackColor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIcon(
                                iconName = iconName,
                                colorHex = colorHex,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = CurrencyFormatter.format(amt, currencySymbol),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (amt / maxAmount).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color(0xFFF1F5F9)
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
private fun SpendingByDaySection(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    startOfWeek: Int = Calendar.SUNDAY,
    isBangla: Boolean = false,
    onInsightsClick: () -> Unit
) {
    val orderedWeekDays = remember(startOfWeek) {
        UserSettingsPreferences.orderedWeekDays(startOfWeek)
    }
    val dayLabels = remember(orderedWeekDays, isBangla) {
        orderedWeekDays.map { if (isBangla) it.shortBn else it.singleEn }
    }

    val expenseTxs = transactions.filter { FinancialIntegrityValidator.isEligibleExpense(it) }
    val dayAmounts = FloatArray(7)
    val anchorTime = expenseTxs.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
    val sevenDaysAgo = anchorTime - 7L * 24 * 3600 * 1000

    for (tx in expenseTxs) {
        if (tx.timestamp in sevenDaysAgo..anchorTime) {
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val calDay = txCal.get(Calendar.DAY_OF_WEEK)
            val slotIndex = orderedWeekDays.indexOfFirst { it.calendarDay == calDay }.coerceIn(0, 6)
            dayAmounts[slotIndex] += tx.amount.toFloat()
        }
    }

    val hasData = dayAmounts.any { it > 0f }
    val effectiveValues = dayAmounts.toList()
    val maxVal = effectiveValues.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_section_spending_by_day")
    ) {
        SectionHeader(
            title = if (isBangla) "দিনভিত্তিক ব্যয়" else "Spending by day",
            actionText = if (isBangla) "ইনসাইটস" else "Insights",
            onActionClick = onInsightsClick
        )

        TranslucentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            onClick = onInsightsClick
        ) {
            if (!hasData) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (isBangla) "কোনো দৈনিক ব্যয়ের তথ্য নেই" else "No daily spending data yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isBangla) "কোন দিনগুলোতে বেশি ব্যয় হয় তা দেখতে ব্যয় যোগ করুন।" else "Record expenses to see which weekdays cost the most.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        effectiveValues.forEachIndexed { idx, v ->
                            val barHeight = if (v > 0f) (v / maxVal * 85).dp.coerceAtLeast(8.dp) else 4.dp
                            val isPeak = v > 0f && v == maxVal
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                isPeak -> MaterialTheme.colorScheme.primary
                                                v > 0f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = dayLabels[idx],
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium
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
private fun RecentActivitySection(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity> = emptyList(),
    currencySymbol: String,
    isBangla: Boolean = false,
    onSeeAllClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_section_recent_activity")
    ) {
        SectionHeader(
            title = if (isBangla) "সাম্প্রতিক কার্যকলাপ" else "Recent activity",
            actionText = if (isBangla) "সব দেখুন" else "See all",
            onActionClick = onSeeAllClick
        )

        TranslucentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "এখনো কোনো সাম্প্রতিক লেনদেন নেই" else "No recent transactions yet",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        )
                    }
                } else {
                    transactions.forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTransactionClick(tx) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isTransfer = FinancialIntegrityValidator.isInternalTransfer(tx)
                            val isPositive = tx.type == TransactionType.INCOME || tx.type == TransactionType.BORROW
                            val fallbackColor = when {
                                isTransfer -> "#3B82F6"
                                isPositive -> "#10B981"
                                else -> "#EF4444"
                            }
                            val (iconName, resolvedColor) = if (isTransfer) {
                                "swap_horiz" to fallbackColor
                            } else {
                                resolveHomeCategoryVisual(tx.category, categories, fallbackColor)
                            }
                            CategoryIcon(
                                iconName = iconName,
                                colorHex = resolvedColor,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${tx.category} · ${tx.subcategory.ifBlank { if (isBangla) "অ্যাকাউন্ট" else "Account" }}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            val prefix = when {
                                isTransfer -> "⇄ "
                                isPositive -> "+"
                                else -> "-"
                            }
                            val amountColor = when {
                                isTransfer -> MaterialTheme.colorScheme.primary
                                isPositive -> IncomeGreen
                                else -> MaterialTheme.colorScheme.onBackground
                            }
                            Text(
                                text = prefix + CurrencyFormatter.format(tx.amount, currencySymbol),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = amountColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialHealthCard(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    isBangla: Boolean = false,
    onDetailsClick: () -> Unit
) {
    val (refYear, refMonth) = remember(transactions, budgets) {
        FinancialIntegrityValidator.resolveReferenceYearMonth(transactions, budgets)
    }
    val (hasActivity, score, gradeText, summaryText) = remember(transactions, refYear, refMonth, isBangla) {
        val monthIncome = transactions.filter { tx ->
            if (!FinancialIntegrityValidator.isEligibleIncome(tx)) return@filter false
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            cal.get(Calendar.YEAR) == refYear && cal.get(Calendar.MONTH) == refMonth
        }.sumOf { it.amount }

        val monthExpense = transactions.filter { tx ->
            if (!FinancialIntegrityValidator.isEligibleExpense(tx)) return@filter false
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            cal.get(Calendar.YEAR) == refYear && cal.get(Calendar.MONTH) == refMonth
        }.sumOf { it.amount }

        val active = monthIncome > 0 || monthExpense > 0
        if (!active) {
            HealthSummaryTuple(
                hasActivity = false,
                score = 0,
                gradeText = if (isBangla) "এখনো তথ্য নেই" else "No monthly activity yet",
                summaryText = if (isBangla) "সঞ্চয়ের হার দেখতে আয় এবং ব্যয় যোগ করুন।" else "Track income and expenses to see your savings rate."
            )
        } else {
            val savingsRate = if (monthIncome > 0) {
                (((monthIncome - monthExpense) / monthIncome) * 100).toInt().coerceIn(-100, 100)
            } else -100

            val computedScore = when {
                savingsRate >= 30 -> (85 + (savingsRate - 30) / 3).coerceIn(85, 98)
                savingsRate >= 15 -> (72 + (savingsRate - 15)).coerceIn(72, 84)
                savingsRate >= 0 -> (55 + savingsRate).coerceIn(55, 71)
                else -> (50 + savingsRate / 2).coerceIn(25, 54)
            }
            val grade = when {
                computedScore >= 80 -> if (isBangla) "চমৎকার · A" else "Excellent · A"
                computedScore >= 65 -> if (isBangla) "ভালো · B" else "Good · B"
                computedScore >= 50 -> if (isBangla) "মোটামুটি · C" else "Fair · C"
                else -> if (isBangla) "মনোযোগ প্রয়োজন · D" else "Needs Attention · D"
            }
            val text = if (monthIncome > 0 && savingsRate >= 0) {
                if (isBangla) "এই মাসে আপনি আয়ের $savingsRate% সঞ্চয় করেছেন।" else "You kept $savingsRate% of your income this month."
            } else if (monthIncome > 0) {
                val overPct = kotlin.math.abs(savingsRate)
                if (isBangla) "এই মাসে ব্যয় আয়ের চেয়ে $overPct% বেশি।" else "Expenses exceeded monthly income by $overPct%."
            } else {
                if (isBangla) "সঞ্চয়ের হার হিসাব করতে এই মাসের আয় যোগ করুন।" else "Log income alongside expenses to calculate your savings rate."
            }
            HealthSummaryTuple(true, computedScore, grade, text)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_section_financial_health")
    ) {
        SectionHeader(
            title = if (isBangla) "আর্থিক স্বাস্থ্য" else "Financial health",
            actionText = if (isBangla) "বিস্তারিত" else "Details",
            onActionClick = onDetailsClick
        )

        TranslucentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            onClick = onDetailsClick
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { if (hasActivity) (score / 100f).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxSize(),
                        color = if (!hasActivity) MaterialTheme.colorScheme.outlineVariant else if (score >= 65) IncomeGreen else ExpenseRed,
                        trackColor = Color(0xFFE2E8F0),
                        strokeWidth = 6.dp
                    )
                    Text(
                        text = if (hasActivity) {
                            if (isBangla) CurrencyFormatter.convertToBanglaDigits("$score") else "$score"
                        } else "--",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = gradeText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                !hasActivity -> MaterialTheme.colorScheme.onSurface
                                score >= 65 -> IncomeGreen
                                else -> ExpenseRed
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

private data class HealthSummaryTuple(
    val hasActivity: Boolean,
    val score: Int,
    val gradeText: String,
    val summaryText: String
)

@Composable
private fun SmartInsightBanner(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    currencySymbol: String,
    isBangla: Boolean = false,
    onClick: () -> Unit
) {
    val insightText = remember(transactions, budgets, currencySymbol, isBangla) {
        if (transactions.isEmpty()) {
            if (isBangla) "মাসিক ব্যয়ের ব্যক্তিগত পরামর্শ পেতে লেনদেন যোগ করুন।"
            else "Add transactions to unlock personalized monthly spending insights."
        } else {
            val (refYear, refMonth) = FinancialIntegrityValidator.resolveReferenceYearMonth(transactions, budgets)
            val prevCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, refYear)
                set(Calendar.MONTH, refMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -1)
            }
            val prevYear = prevCal.get(Calendar.YEAR)
            val prevMonth = prevCal.get(Calendar.MONTH)

            val thisMonthExpenses = transactions.filter {
                FinancialIntegrityValidator.isEligibleExpense(it) &&
                    FinancialIntegrityValidator.isInYearMonth(it.timestamp, refYear, refMonth)
            }
            val thisExpenseTotal = thisMonthExpenses.sumOf { it.amount }
            val lastExpenseTotal = transactions.filter {
                FinancialIntegrityValidator.isEligibleExpense(it) &&
                    FinancialIntegrityValidator.isInYearMonth(it.timestamp, prevYear, prevMonth)
            }.sumOf { it.amount }

            val topCategory = thisMonthExpenses
                .groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .maxByOrNull { it.value }

            when {
                lastExpenseTotal > 0.0 && thisExpenseTotal > 0.0 -> {
                    val deltaPct = (((thisExpenseTotal - lastExpenseTotal) / lastExpenseTotal) * 100).toInt()
                    if (deltaPct <= 0) {
                        val absPct = kotlin.math.abs(deltaPct)
                        if (isBangla) "দারুণ অগ্রগতি! গত মাসের তুলনায় ব্যয় $absPct% কম হয়েছে।"
                        else "Great job! You're spending $absPct% less than last month."
                    } else {
                        if (isBangla) "গত মাসের তুলনায় এই মাসে ব্যয় $deltaPct% বেশি হয়েছে।"
                        else "Spending is up $deltaPct% compared to last month so far."
                    }
                }
                topCategory != null && thisExpenseTotal > 0.0 -> {
                    val sharePct = ((topCategory.value / thisExpenseTotal) * 100).toInt().coerceIn(1, 100)
                    val formattedAmt = CurrencyFormatter.format(topCategory.value, currencySymbol)
                    if (isBangla) {
                        "এই মাসে সর্বোচ্চ ব্যয় ${topCategory.key} ($formattedAmt, মোট ব্যয়ের $sharePct%)।"
                    } else {
                        "${topCategory.key} leads this month's spending at $formattedAmt ($sharePct% of expenses)."
                    }
                }
                else -> {
                    if (isBangla) "এই মাসে ব্যয় বিশ্লেষণের জন্য আরও লেনদেন যোগ করুন।"
                    else "Log an expense this month to highlight your top spending pattern."
                }
            }
        }
    }

    TranslucentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("home_section_smart_insights"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isBangla) "স্মার্ট অন্তর্দৃষ্টি" else "SMART INSIGHT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = insightText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

private data class HomeUpcomingBillItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val isCreditCard: Boolean,
    val accountId: String? = null
)

@Composable
private fun UpcomingBillsSection(
    debts: List<DebtEntity>,
    accounts: List<AccountEntity> = emptyList(),
    currencySymbol: String,
    isBangla: Boolean,
    onManageDebtsClick: () -> Unit,
    onAccountClick: (String) -> Unit = {},
    onAddDebtClick: () -> Unit
) {
    val upcomingBills = remember(debts, accounts, isBangla) {
        val creditCardBills = accounts
            .filter {
                !it.isHidden &&
                    it.type == AccountType.CREDIT_CARD &&
                    (it.paymentDueDate.isNotBlank() || it.balance < 0.0 || it.minPayment > 0.0)
            }
            .map { cc ->
                val outstanding = if (cc.balance < 0.0) kotlin.math.abs(cc.balance) else cc.minPayment
                val dueLabel = if (cc.paymentDueDate.isNotBlank()) {
                    if (isBangla) "বিল তারিখ: ${cc.paymentDueDate}" else "Card bill · Due ${cc.paymentDueDate}"
                } else {
                    if (isBangla) "ক্রেডিট কার্ড বিল" else "Credit card payment due"
                }
                HomeUpcomingBillItem(
                    id = "cc_${cc.id}",
                    title = cc.name,
                    subtitle = dueLabel,
                    amount = outstanding,
                    isCreditCard = true,
                    accountId = cc.id
                )
            }

        val debtBills = debts
            .filter { it.type == DebtType.BORROWED && !it.isSettled && it.amount > 0.0 }
            .map { debt ->
                val dueStr = debt.tentativeCloseDateStr.ifBlank { debt.dueDate }
                val subtitle = if (dueStr.isNotBlank()) {
                    if (isBangla) "পরিশোধের তারিখ: $dueStr" else "Due: $dueStr"
                } else {
                    if (isBangla) "পরিশোধযোগ্য ঋণ" else "Payable loan due"
                }
                HomeUpcomingBillItem(
                    id = "debt_${debt.id}",
                    title = debt.personName,
                    subtitle = subtitle,
                    amount = debt.amount,
                    isCreditCard = false
                )
            }

        (creditCardBills + debtBills).take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("home_section_bills")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBangla) "আসন্ন বিলসমূহ" else "Upcoming bills",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
            Text(
                text = if (isBangla) "সকল দেখুন" else "See all",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .clickable { onManageDebtsClick() }
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (upcomingBills.isEmpty()) {
            TranslucentCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isBangla) "কোনো আসন্ন বিল বকেয়া নেই" else "No upcoming bills due",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isBangla) "সব বিল পরিশোধিত আছে" else "You're all caught up for now",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                    Text(
                        text = if (isBangla) "+ যোগ করুন" else "+ Add",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable { onAddDebtClick() }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                upcomingBills.forEach { bill ->
                    TranslucentCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        onClick = {
                            if (bill.isCreditCard && bill.accountId != null) {
                                onAccountClick(bill.accountId)
                            } else {
                                onManageDebtsClick()
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (bill.isCreditCard) Color(0xFFF59E0B).copy(alpha = 0.14f) else ExpenseRed.copy(alpha = 0.12f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (bill.isCreditCard) Icons.Default.CreditCard else Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = if (bill.isCreditCard) Color(0xFFD97706) else ExpenseRed,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = bill.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bill.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = CurrencyFormatter.format(bill.amount, currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (bill.isCreditCard) Color(0xFFD97706) else ExpenseRed
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSavingsGoalsSection(
    goals: List<SavingsGoalEntity>,
    currencySymbol: String,
    isBangla: Boolean,
    onSeeAllClick: () -> Unit,
    onAddGoalClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("home_section_savings")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBangla) "সঞ্চয় লক্ষ্য" else "Savings goals",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
            Text(
                text = if (isBangla) "সকল দেখুন" else "See all",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .clickable { onSeeAllClick() }
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (goals.isEmpty()) {
            TranslucentCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = IncomeGreen.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isBangla) "কোনো সক্রিয় সঞ্চয় লক্ষ্য নেই" else "No active savings goals",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isBangla) "নতুন সঞ্চয় লক্ষ্য নির্ধারণ করুন" else "Create a goal to stay on track",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                    Text(
                        text = if (isBangla) "+ লক্ষ্য" else "+ New goal",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable { onAddGoalClick() }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.take(2).forEach { goal ->
                    TranslucentCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        onClick = onSeeAllClick
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = goal.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val progressPercent = if (goal.targetAmount > 0) ((goal.savedAmount / goal.targetAmount) * 100).toInt() else 0
                                Text(
                                    text = if (isBangla) CurrencyFormatter.convertToBanglaDigits("$progressPercent%") else "$progressPercent%",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val savedFormatted = CurrencyFormatter.format(goal.savedAmount, currencySymbol)
                                val targetFormatted = CurrencyFormatter.format(goal.targetAmount, currencySymbol)
                                Text(
                                    text = if (isBangla) "$savedFormatted সঞ্চিত" else "$savedFormatted saved",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                                Text(
                                    text = if (isBangla) "লক্ষ্য: $targetFormatted" else "Target: $targetFormatted",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
private fun SpendingStreakSection(
    transactions: List<TransactionEntity> = emptyList(),
    budgets: List<BudgetEntity> = emptyList(),
    currencySymbol: String,
    isBangla: Boolean,
    onDetailsClick: () -> Unit
) {
    val (hasExpenses, streakDays) = remember(transactions, budgets) {
        val (refYear, refMonth) = FinancialIntegrityValidator.resolveReferenceYearMonth(transactions, budgets)
        val monthExpenses = transactions.filter {
            FinancialIntegrityValidator.isEligibleExpense(it) &&
                FinancialIntegrityValidator.isInYearMonth(it.timestamp, refYear, refMonth)
        }
        if (monthExpenses.isEmpty()) {
            false to 0
        } else {
            val cal = Calendar.getInstance()
            val byDay = mutableMapOf<Int, Double>()
            var maxDay = 1
            for (tx in monthExpenses) {
                cal.timeInMillis = tx.timestamp
                val dom = cal.get(Calendar.DAY_OF_MONTH)
                if (dom > maxDay) maxDay = dom
                byDay[dom] = (byDay[dom] ?: 0.0) + tx.amount
            }
            val nowCal = Calendar.getInstance()
            val anchorDay = if (nowCal.get(Calendar.YEAR) == refYear && nowCal.get(Calendar.MONTH) == refMonth) {
                maxOf(maxDay, nowCal.get(Calendar.DAY_OF_MONTH))
            } else {
                maxDay
            }
            val dailyAverage = (monthExpenses.sumOf { it.amount } / anchorDay.coerceAtLeast(1)).coerceAtLeast(1.0)
            var streak = 0
            for (d in anchorDay downTo 1) {
                val spend = byDay[d] ?: 0.0
                if (spend <= dailyAverage) {
                    streak++
                } else {
                    break
                }
            }
            true to streak
        }
    }

    val badgeLabel = if (isBangla) {
        "${CurrencyFormatter.convertToBanglaDigits(streakDays.toString())} দিন"
    } else {
        "$streakDays ${if (streakDays == 1) "day" else "days"}"
    }
    val subtitleText = when {
        !hasExpenses -> if (isBangla) "দৈনিক গড় ব্যয়ের নিচে থাকার দিন দেখতে ব্যয় যোগ করুন" else "Log expenses to track days under your daily average"
        streakDays > 0 -> if (isBangla) "টানা $badgeLabel দৈনিক ব্যয়ের গড়ের নিচে" else "$badgeLabel under your daily average"
        else -> if (isBangla) "আজকের ব্যয় দৈনিক গড়ের উপরে ছিল" else "Today's spending went above your daily average"
    }

    TranslucentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("home_section_spending_streak"),
        cornerRadius = 20.dp,
        onClick = onDetailsClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFEDD5),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🔥", fontSize = 24.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBangla) "ব্যয় নিয়ন্ত্রণ স্ট্রিক" else "Spending streak",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = badgeLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
