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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.example.ui.navigation.walletCardSharedBounds
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.foundation.border
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
    val currentTab by viewModel.currentTab.collectAsState()
    val currentSubScreen by viewModel.currentSubScreen.collectAsState()
    val isHomeActive = currentTab == MainTab.HOME && currentSubScreen == SubScreen.NONE

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
    val earnedThisMonth = remember(transactions, refYear, refMonth) {
        transactions.filter { tx ->
            FinancialIntegrityValidator.isEligibleIncome(tx) &&
                FinancialIntegrityValidator.isInYearMonth(tx.timestamp, refYear, refMonth)
        }.sumOf { it.amount }
    }
    val visibleAccountCount = remember(accounts) { accounts.count { !it.isHidden } }
    val cycleHomeCardStyle: () -> Unit = {
        viewModel.updateHomeCustomization(
            homeCustomization.copy(cardStyle = homeCustomization.cardStyle.next())
        )
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
        extraTopGap = 6.dp,
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
            // 1 & 2. Unified Top Compact Frosted Glass KPI Strip + Fluid Card View Mode Switcher
            item(key = "home_accounts_section") {
                val layoutSpring = remember {
                    spring<androidx.compose.ui.unit.IntSize>(
                        dampingRatio = 0.88f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                }
                val cardViewMode = homeCustomization.cardStyle

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = layoutSpring)
                ) {
                    AnimatedVisibility(
                        visible = cardViewMode != HomeCardStyle.BALANCE_CARD,
                        enter = fadeIn(animationSpec = tween(durationMillis = 260, delayMillis = 40)) +
                            expandVertically(
                                animationSpec = spring(
                                    dampingRatio = 0.88f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 180)) +
                            shrinkVertically(
                                animationSpec = spring(
                                    dampingRatio = 0.88f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                    ) {
                        HomeSummaryCardsSection(
                            totalBalance = netWorth.totalAssets,
                            spentThisMonth = spentThisMonth,
                            isBalanceVisible = isBalanceVisible,
                            currencySymbol = currencySymbol,
                            isBangla = isBangla,
                            onToggleVisibility = { isBalanceVisible = !isBalanceVisible }
                        )
                    }

                    AnimatedContent(
                        targetState = cardViewMode,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(durationMillis = 260, delayMillis = 40)) +
                                scaleIn(
                                    initialScale = 0.96f,
                                    animationSpec = spring(
                                        dampingRatio = 0.86f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )) togetherWith
                                (fadeOut(animationSpec = tween(durationMillis = 180)) +
                                    scaleOut(
                                        targetScale = 0.97f,
                                        animationSpec = tween(durationMillis = 180)
                                    )) using SizeTransform(
                                clip = false,
                                sizeAnimationSpec = { _, _ -> layoutSpring }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = "home_card_view_mode_transition"
                    ) { activeMode ->
                        when (activeMode) {
                            HomeCardStyle.STACK -> {
                                StackedAccountCards(
                                    accounts = accounts,
                                    isBalanceVisible = isBalanceVisible,
                                    currencySymbol = currencySymbol,
                                    isBangla = isBangla,
                                    isHomeActive = isHomeActive,
                                    currentCardStyle = activeMode,
                                    showHeader = true,
                                    onSwitchViewMode = cycleHomeCardStyle,
                                    onAccountClick = { acc -> viewModel.openAccountDetail(acc.id) },
                                    onAllAccountsClick = { viewModel.openSubScreen(SubScreen.ACCOUNTS_MANAGEMENT) }
                                )
                            }
                            HomeCardStyle.CAROUSEL -> {
                                AccountCardsCarousel(
                                    accounts = accounts,
                                    isBalanceVisible = isBalanceVisible,
                                    currencySymbol = currencySymbol,
                                    language = lang,
                                    isBangla = isBangla,
                                    currentCardStyle = activeMode,
                                    showHeader = true,
                                    onSwitchViewMode = cycleHomeCardStyle,
                                    onAccountClick = { acc -> viewModel.openAccountDetail(acc.id) },
                                    onAllAccountsClick = { viewModel.openSubScreen(SubScreen.ACCOUNTS_MANAGEMENT) }
                                )
                            }
                            HomeCardStyle.BALANCE_CARD -> {
                                BalanceCardHero(
                                    totalBalance = netWorth.totalAssets,
                                    earnedThisMonth = earnedThisMonth,
                                    spentThisMonth = spentThisMonth,
                                    accountCount = visibleAccountCount,
                                    isBalanceVisible = isBalanceVisible,
                                    currencySymbol = currencySymbol,
                                    isBangla = isBangla,
                                    currentStyle = activeMode,
                                    onCardClick = { viewModel.openSubScreen(SubScreen.ACCOUNTS_MANAGEMENT) },
                                    onToggleVisibility = { isBalanceVisible = !isBalanceVisible },
                                    onSwitchViewMode = cycleHomeCardStyle
                                )
                            }
                        }
                    }
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
    onToggleVisibility: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val royalIndigo = Color(0xFF4F46E5)
    val coralRose = Color(0xFFF43F5E)
    val language = if (isBangla) {
        com.example.data.model.AppLanguage.BANGLA
    } else {
        com.example.data.model.AppLanguage.ENGLISH
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp)
            .testTag("home_unified_kpi_card"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FrostedKpiGlassPill(
            label = AppStrings.t("total_balance_caps", language),
            amount = if (isBalanceVisible) CurrencyFormatter.format(totalBalance, currencySymbol) else "••••••••",
            icon = Icons.Rounded.AccountBalanceWallet,
            iconTint = if (isDark) Color(0xFF818CF8) else royalIndigo,
            badgeBgColor = royalIndigo.copy(alpha = if (isDark) 0.24f else 0.12f),
            isDark = isDark,
            isBangla = isBangla,
            onClick = onToggleVisibility,
            modifier = Modifier
                .weight(1f)
                .testTag("home_total_balance_card")
        )

        FrostedKpiGlassPill(
            label = AppStrings.t("spent_this_month_caps", language),
            amount = if (isBalanceVisible) CurrencyFormatter.format(spentThisMonth, currencySymbol) else "••••••••",
            icon = Icons.Rounded.TrendingDown,
            iconTint = if (isDark) Color(0xFFFB7185) else coralRose,
            badgeBgColor = coralRose.copy(alpha = if (isDark) 0.24f else 0.12f),
            isDark = isDark,
            isBangla = isBangla,
            onClick = onToggleVisibility,
            modifier = Modifier
                .weight(1f)
                .testTag("home_spent_this_month_card")
        )
    }
}

@Composable
private fun FrostedKpiGlassPill(
    label: String,
    amount: String,
    icon: ImageVector,
    iconTint: Color,
    badgeBgColor: Color,
    isDark: Boolean,
    isBangla: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(18.dp)
    val badgeShape = RoundedCornerShape(8.dp)
    var amountFontSizeSp by remember(amount) { mutableFloatStateOf(18f) }

    val borderBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.03f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.75f),
                    Color.White.copy(alpha = 0.48f),
                    Color.White.copy(alpha = 0.26f)
                )
            )
        }
    }

    val surfaceColor = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.55f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }

    Row(
        modifier = modifier
            .clip(pillShape)
            .background(surfaceColor, pillShape)
            .border(width = 1.dp, brush = borderBrush, shape = pillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(badgeShape)
                .background(badgeBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    letterSpacing = if (isBangla) 0.sp else 0.45.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = amountFontSizeSp.sp,
                    lineHeight = (amountFontSizeSp + 3f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (result.didOverflowWidth && amountFontSizeSp > 13f) {
                        amountFontSizeSp = (amountFontSizeSp - 0.5f).coerceAtLeast(13f)
                    }
                }
            )
        }
    }
}

@Composable
private fun SharedWalletCardsHeaderRow(
    accountCount: Int,
    currentCardStyle: HomeCardStyle,
    isBangla: Boolean,
    onAllAccountsClick: () -> Unit,
    onSwitchViewMode: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pillLabel = if (currentCardStyle == HomeCardStyle.STACK) {
        val remainingCount = (accountCount - 1).coerceAtLeast(0)
        if (remainingCount > 0) {
            if (isBangla) "+${CurrencyFormatter.convertToBanglaDigits(remainingCount.toString())} আরও"
            else "+$remainingCount more"
        } else {
            if (isBangla) "${CurrencyFormatter.convertToBanglaDigits(accountCount.toString())}টি অ্যাকাউন্ট"
            else if (accountCount == 1) "1 account" else "$accountCount accounts"
        }
    } else {
        if (isBangla) {
            "${CurrencyFormatter.convertToBanglaDigits(accountCount.toString())}টি অ্যাকাউন্ট"
        } else {
            if (accountCount == 1) "1 account" else "$accountCount accounts"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.58f)
                    else Color.White.copy(alpha = 0.64f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.14f)
                    else Color.White.copy(alpha = 0.78f),
                    shape = CircleShape
                )
                .padding(horizontal = 11.dp, vertical = 5.dp)
        ) {
            Text(
                text = pillLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                    fontSize = 12.sp
                ),
                maxLines = 1,
                softWrap = false
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onAllAccountsClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag(
                        if (currentCardStyle == HomeCardStyle.STACK) {
                            "stack_all_accounts_button"
                        } else {
                            "carousel_all_accounts_button"
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (isBangla) "সকল অ্যাকাউন্ট" else "All accounts",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    )
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(11.dp)
                )
            }

            HomeCardViewSwitcherButton(
                currentStyle = currentCardStyle,
                isBangla = isBangla,
                onClick = onSwitchViewMode
            )
        }
    }
}

@Composable
private fun AccountCardsCarousel(
    accounts: List<AccountEntity>,
    isBalanceVisible: Boolean,
    currencySymbol: String,
    language: com.example.data.model.AppLanguage,
    isBangla: Boolean,
    currentCardStyle: HomeCardStyle,
    showHeader: Boolean = true,
    onSwitchViewMode: () -> Unit,
    onAccountClick: (AccountEntity) -> Unit,
    onAllAccountsClick: () -> Unit
) {
    val visibleAccounts = remember(accounts) { accounts.filter { !it.isHidden } }
    val carouselListState = rememberLazyListState()
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = carouselListState)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (showHeader) 6.dp else 0.dp, bottom = if (showHeader) 8.dp else 0.dp)
            .testTag("home_wallet_card_carousel")
    ) {
        if (showHeader) {
            SharedWalletCardsHeaderRow(
                accountCount = visibleAccounts.size,
                currentCardStyle = currentCardStyle,
                isBangla = isBangla,
                onAllAccountsClick = onAllAccountsClick,
                onSwitchViewMode = onSwitchViewMode
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (visibleAccounts.isNotEmpty()) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidth = (maxWidth - 32.dp).coerceAtLeast(260.dp)
                LazyRow(
                    state = carouselListState,
                    flingBehavior = snapFlingBehavior,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleAccounts, key = { it.id }) { acc ->
                        com.example.ui.screens.accounts.HishabPatiAccountCard(
                            account = acc,
                            currencySymbol = currencySymbol,
                            isBalanceVisible = isBalanceVisible,
                            language = language,
                            cornerRadius = 22.dp,
                            shadowElevation = 8.dp,
                            onClick = { onAccountClick(acc) },
                            modifier = Modifier
                                .width(cardWidth)
                                .height(180.dp)
                                .walletCardSharedBounds(
                                    accountId = acc.id,
                                    cornerRadius = 22.dp
                                )
                        )
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
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

                // TOP OF HOME Options (Card carousel vs Card stack)
                val sheetLanguage = if (isBangla) {
                    com.example.data.model.AppLanguage.BANGLA
                } else {
                    com.example.data.model.AppLanguage.ENGLISH
                }
                val isDarkSheet = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                val previewCanvasColor = if (isDarkSheet) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                } else {
                    Color(0xFFF8FAFC)
                }

                val primaryAccent = MaterialTheme.colorScheme.primary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: Balance card (total balance hero with monthly income/expense pills)
                    val isBalanceSelected = current.cardStyle == HomeCardStyle.BALANCE_CARD
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isBalanceSelected) 2.dp else 1.dp,
                            color = if (isBalanceSelected) primaryAccent else Color.LightGray.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onUpdate(current.copy(cardStyle = HomeCardStyle.BALANCE_CARD))
                            }
                            .testTag("customize_home_card_balance")
                    ) {
                        Column(modifier = Modifier.padding(9.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(previewCanvasColor),
                                contentAlignment = Alignment.Center
                            ) {
                                // Mini Balance Card Hero thumbnail
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 7.dp)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(primaryAccent, Color(0xFF312E81))
                                            )
                                        )
                                        .padding(7.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(2.5.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.72f))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(44.dp)
                                                .height(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White.copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 3.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF6EE7B7))
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White.copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 3.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFDA4AF))
                                            )
                                        }
                                    }
                                }

                                if (isBalanceSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(primaryAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = AppStrings.t("home_style_balance_card", sheetLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = AppStrings.t("home_style_balance_card_subtitle", sheetLanguage),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    fontSize = 10.5.sp,
                                    lineHeight = 13.5.sp
                                ),
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Option 2: Card stack (layered vertical wallet stack)
                    val isStackSelected = current.cardStyle == HomeCardStyle.STACK
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isStackSelected) 2.dp else 1.dp,
                            color = if (isStackSelected) primaryAccent else Color.LightGray.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onUpdate(current.copy(cardStyle = HomeCardStyle.STACK))
                            }
                            .testTag("customize_home_card_stack")
                    ) {
                        Column(modifier = Modifier.padding(9.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(previewCanvasColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(width = 72.dp, height = 46.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 64.dp, height = 32.dp)
                                            .offset(y = (-10).dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(Color(0xFF0D9488))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 68.dp, height = 34.dp)
                                            .offset(y = (-5).dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(Color(0xFF2563EB))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 72.dp, height = 36.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFFDB2777), Color(0xFFE11D48))
                                                )
                                            )
                                            .padding(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .size(width = 9.dp, height = 6.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFFF59E0B))
                                        )
                                    }
                                }

                                if (isStackSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(primaryAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = AppStrings.t("home_style_card_stack", sheetLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = AppStrings.t("home_style_card_stack_subtitle", sheetLanguage),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    fontSize = 10.5.sp,
                                    lineHeight = 13.5.sp
                                ),
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Option 3: Card carousel (horizontal scrolling account cards)
                    val isCarouselSelected = current.cardStyle == HomeCardStyle.CAROUSEL
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isCarouselSelected) 2.dp else 1.dp,
                            color = if (isCarouselSelected) primaryAccent else Color.LightGray.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onUpdate(current.copy(cardStyle = HomeCardStyle.CAROUSEL))
                            }
                            .testTag("customize_home_card_carousel")
                    ) {
                        Column(modifier = Modifier.padding(9.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(previewCanvasColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 8.dp)
                                        .offset(y = (-3).dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 56.dp, height = 38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF5A45FF), Color(0xFF4F46E5))
                                                )
                                            )
                                            .padding(6.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .width(30.dp)
                                                    .height(4.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(18.dp)
                                                    .height(3.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.72f))
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .size(width = 8.dp, height = 5.dp)
                                                .clip(RoundedCornerShape(1.5.dp))
                                                .background(Color(0xFFF59E0B))
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(width = 30.dp, height = 34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF0D9488), Color(0xFF2563EB))
                                                )
                                            )
                                            .padding(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(14.dp)
                                                .height(3.5.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.85f))
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(8.dp)
                                            .height(3.dp)
                                            .clip(CircleShape)
                                            .background(primaryAccent)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF94A3B8).copy(alpha = 0.55f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF94A3B8).copy(alpha = 0.35f))
                                    )
                                }

                                if (isCarouselSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(primaryAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = AppStrings.t("home_style_card_carousel", sheetLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = AppStrings.t("home_style_card_carousel_subtitle", sheetLanguage),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    fontSize = 10.5.sp,
                                    lineHeight = 13.5.sp
                                ),
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
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

    val explicitOverallBudget = monthBudgets.firstOrNull { it.isOverall }
    val definedBudget = explicitOverallBudget?.monthlyLimit?.takeIf { it > 0.0 }
        ?: FinancialIntegrityValidator.computeTotalMonthlyBudget(monthBudgets, monthKey).takeIf { it > 0.0 }
    val hasBudget = definedBudget != null && definedBudget > 0.0
    val overallBudget = definedBudget ?: 0.0
    val totalExpense = if (explicitOverallBudget != null) {
        monthExpenseTransactions.sumOf { it.amount }
    } else {
        val catBudgets = monthBudgets.filter { !it.isOverall && !it.categoryName.equals("Overall", ignoreCase = true) }
        if (catBudgets.isNotEmpty()) {
            monthExpenseTransactions.filter { tx ->
                catBudgets.any { b -> FinancialIntegrityValidator.isMatchingBudgetTransaction(tx, b) }
            }.sumOf { it.amount }
        } else {
            monthExpenseTransactions.sumOf { it.amount }
        }
    }

    val percentage = if (overallBudget > 0) ((totalExpense / overallBudget) * 100).toInt().coerceIn(0, 999) else 0
    val remaining = overallBudget - totalExpense

    val isBn = CurrencyFormatter.currentLanguage == com.example.data.model.AppLanguage.BANGLA
    val localizedMonth = if (isBn) DateFormatter.localizeText(monthName, com.example.data.model.AppLanguage.BANGLA) else monthName

    TranslucentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
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

                val categoryBudgetsWithSpent = monthBudgets
                    .filter { !it.isOverall && !it.categoryName.equals("Overall", ignoreCase = true) && it.monthlyLimit > 0.0 }
                    .map { catBgt ->
                        val catSpent = monthExpenseTransactions
                            .filter { FinancialIntegrityValidator.isMatchingBudgetTransaction(it, catBgt) }
                            .sumOf { it.amount }
                        val ratio = if (catBgt.monthlyLimit > 0.0) catSpent / catBgt.monthlyLimit else 0.0
                        Triple(catBgt, catSpent, ratio)
                    }
                    .sortedWith(compareByDescending<Triple<BudgetEntity, Double, Double>> { it.third }.thenByDescending { it.second })
                    .take(3)

                if (categoryBudgetsWithSpent.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    categoryBudgetsWithSpent.forEach { (catBgt, catSpent, _) ->
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
                .padding(horizontal = 16.dp, vertical = 6.dp)
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
