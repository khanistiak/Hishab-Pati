package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.data.model.TransactionType
import com.example.ui.util.CurrencyFormatter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FrostedBackdropState
import com.example.ui.components.FrostedGlassSurface
import com.example.ui.components.LocalFrostedBackdropState
import com.example.ui.components.crystalOuterShadow
import com.example.data.model.BackgroundTextureOption
import com.example.data.model.ThemeColorOption
import com.example.data.model.ThemeMode
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.navigation.rememberScreenLazyListState
import com.example.ui.components.VoiceTransactionDialog
import com.example.ui.screens.accounts.AccountDetailScreen
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.accounts.EditAccountScreen
import com.example.ui.screens.activity.ActivityScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.insights.InsightsScreen
import com.example.ui.screens.plan.PlanScreen
import androidx.activity.compose.BackHandler
import com.example.data.model.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.screens.plan.AddDebtScreen
import com.example.ui.screens.plan.DebtDetailsScreen
import com.example.ui.screens.plan.ManageDebtsScreen
import com.example.ui.screens.plan.NewSavingsGoalScreen
import com.example.ui.screens.plan.CalendarScreen
import com.example.ui.screens.plan.CurrencyConverterScreen
import com.example.ui.screens.plan.IncomeTrackerScreen
import com.example.ui.screens.plan.MonthlyRecapScreen
import com.example.ui.screens.plan.SpendingComparisonScreen
import com.example.ui.screens.plan.AddDebtTransactionScreen
import com.example.ui.screens.settings.AboutScreen
import com.example.ui.screens.settings.CategoriesScreen
import com.example.ui.screens.settings.CurrencySelectionScreen
import com.example.ui.screens.settings.ProfileSettingsScreen
import com.example.ui.screens.settings.ProfileScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.ThemeScreen
import com.example.ui.screens.settings.NotificationManagerScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.transaction.AddTransactionScreen
import com.example.ui.screens.backup.BackupRestoreScreen
import com.example.ui.components.BackupRestoreModal
import com.example.ui.theme.MyWalletBackground
import com.example.ui.theme.MyWalletTheme
import com.example.ui.util.AppHaptics
import com.example.ui.util.LocalHapticsEnabled

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.notification.NotificationHelper
import com.example.data.notification.NotificationStore
import com.example.ui.screens.notifications.NotificationDestinationType
import com.example.ui.screens.notifications.generateRealNotifications
import androidx.credentials.CredentialManager
import com.example.ui.auth.AuthManager
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    private var pendingNotificationIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.isForceDarkAllowed = false
        }
        configureAppCheck(this, intent)
        NotificationHelper.createNotificationChannel(this)
        pendingNotificationIntent = intent
        enableEdgeToEdge()
        setContent {
            var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
            var hasSkippedAuth by remember { mutableStateOf(false) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose {
                    Firebase.auth.removeAuthStateListener(listener)
                }
            }

            val mainViewModel: MainViewModel = viewModel()

            LaunchedEffect(Unit) {
                AuthManager.attemptAutoSignIn(
                    context = this@MainActivity,
                    credentialManager = CredentialManager.create(this@MainActivity),
                    onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                        mainViewModel.onUserSignedIn()
                    },
                    onUnauthenticated = { },
                    scope = this
                )
            }

            LaunchedEffect(pendingNotificationIntent) {
                pendingNotificationIntent?.let { incomingIntent ->
                    handleNotificationIntent(incomingIntent, mainViewModel)
                    pendingNotificationIntent = null
                }
            }

            val user = currentUser
            if (user == null && !hasSkippedAuth) {
                MyWalletTheme {
                    AuthScreen(
                        onAuthSuccess = {
                            currentUser = Firebase.auth.currentUser
                            mainViewModel.onUserSignedIn()
                        },
                        onSkip = {
                            hasSkippedAuth = true
                        },
                        viewModel = mainViewModel
                    )
                }
            } else {
                val settings by mainViewModel.settings.collectAsState()

                val themeMode = settings?.themeMode ?: ThemeMode.AUTO
                val themeColor = settings?.themeColor ?: ThemeColorOption.SKY_CYAN
                val backgroundTexture = settings?.backgroundTexture ?: BackgroundTextureOption.AURORA

                val isDark = when (themeMode) {
                    ThemeMode.AUTO -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }

                MyWalletTheme(
                    themeMode = themeMode,
                    themeColor = themeColor
                ) {
                    MyWalletBackground(
                        texture = backgroundTexture,
                        themeColor = themeColor,
                        isDark = isDark
                    ) {
                        MyWalletApp(viewModel = mainViewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationIntent = intent
    }

    private fun handleNotificationIntent(intent: Intent, viewModel: MainViewModel) {
        val notifId = intent.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_ID) ?: return
        val destTypeStr = intent.getStringExtra(NotificationHelper.EXTRA_DESTINATION_TYPE) ?: return
        val entityId = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_ENTITY_ID)
        val searchQuery = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_SEARCH_QUERY)
        val amount = intent.getDoubleExtra(NotificationHelper.EXTRA_TARGET_AMOUNT, 0.0)

        // Mark as read & dismiss from persistent store so it vanishes from in-app list immediately
        viewModel.notificationStore.markAsRead(notifId)
        viewModel.notificationStore.dismissNotification(notifId)
        NotificationHelper.cancelNotification(this, notifId)

        val destType = try {
            NotificationDestinationType.valueOf(destTypeStr)
        } catch (_: Exception) {
            NotificationDestinationType.GENERAL
        }

        viewModel.navigateToNotificationDestination(destType, entityId, searchQuery, amount)
        intent.removeExtra(NotificationHelper.EXTRA_NOTIFICATION_ID)
    }
}

@Composable
fun MyWalletApp(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentSubScreen by viewModel.currentSubScreen.collectAsState()
    val isVoiceDialogVisible by viewModel.isVoiceDialogVisible.collectAsState()
    val notificationMessage by viewModel.userNotificationMessage.collectAsState()
    val insightsSelectedCategory by viewModel.insightsSelectedCategory.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val budgets by viewModel.budgets.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val storeUpdates by NotificationStore.updates.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val extendedSettings by viewModel.extendedSettings.collectAsState()

    // Android 13+ (API 33+) Notification runtime permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val notifs = generateRealNotifications(
                budgets = budgets,
                debts = debts,
                accounts = accounts,
                transactions = transactions,
                currencySymbol = settings?.currencySymbol ?: "৳",
                language = settings?.language ?: AppLanguage.ENGLISH,
                notificationStore = viewModel.notificationStore,
                extendedSettings = extendedSettings
            )
            NotificationHelper.syncSystemNotifications(
                context = context,
                notifications = notifs,
                isBn = settings?.language == AppLanguage.BANGLA,
                enabled = extendedSettings.notificationsEnabled && (settings?.notificationsEnabled ?: true)
            )
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (_: Exception) {
                }
            }
        }
    }

    // Sync Android system notifications whenever relevant financial data or settings change
    LaunchedEffect(budgets, debts, accounts, transactions, settings?.notificationsEnabled, settings?.language, storeUpdates, extendedSettings) {
        val notifs = generateRealNotifications(
            budgets = budgets,
            debts = debts,
            accounts = accounts,
            transactions = transactions,
            currencySymbol = settings?.currencySymbol ?: "৳",
            language = settings?.language ?: AppLanguage.ENGLISH,
            notificationStore = viewModel.notificationStore,
            extendedSettings = extendedSettings
        )
        NotificationHelper.syncSystemNotifications(
            context = context,
            notifications = notifs,
            isBn = settings?.language == AppLanguage.BANGLA,
            enabled = extendedSettings.notificationsEnabled && (settings?.notificationsEnabled ?: true)
        )
    }

    // Screen Lock enforcement via Android KeyguardManager
    val keyguardManager = remember(context) {
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }
    val isDeviceSecure = remember(keyguardManager, extendedSettings.useScreenLock) {
        keyguardManager?.isDeviceSecure == true
    }
    var isUnlockedForSession by rememberSaveable { mutableStateOf(!extendedSettings.useScreenLock) }
    var lastStoppedAtMillis by rememberSaveable { mutableStateOf(0L) }
    var hasAutoPromptedLock by rememberSaveable { mutableStateOf(false) }

    val screenLockLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isUnlockedForSession = true
        }
    }

    val promptDeviceUnlock = {
        @Suppress("DEPRECATION")
        val confirmIntent = keyguardManager?.createConfirmDeviceCredentialIntent(
            "Hishab Pati Locked",
            "Unlock with your device screen lock to continue"
        )
        if (confirmIntent != null) {
            try {
                screenLockLauncher.launch(confirmIntent)
            } catch (_: Exception) {
                isUnlockedForSession = true
            }
        } else {
            isUnlockedForSession = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, extendedSettings.useScreenLock, isDeviceSecure) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    lastStoppedAtMillis = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    val now = System.currentTimeMillis()
                    if (extendedSettings.useScreenLock &&
                        isDeviceSecure &&
                        lastStoppedAtMillis > 0L &&
                        (now - lastStoppedAtMillis) > 3000L
                    ) {
                        isUnlockedForSession = false
                        hasAutoPromptedLock = false
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // When the user toggles Use Screen Lock on during an active session, keep the current session unlocked
    LaunchedEffect(extendedSettings.useScreenLock) {
        if (!extendedSettings.useScreenLock) {
            isUnlockedForSession = true
            hasAutoPromptedLock = false
        }
    }

    val canHandleBack = currentSubScreen != SubScreen.NONE ||
            currentTab != MainTab.HOME ||
            (currentTab == MainTab.INSIGHTS && insightsSelectedCategory != null)

    BackHandler(enabled = canHandleBack) {
        viewModel.handleBackPress()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notificationMessage) {
        notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotificationMessage()
        }
    }

    val showBottomBar = currentSubScreen == SubScreen.NONE
    CurrencyFormatter.currentLanguage = settings?.language ?: AppLanguage.ENGLISH

    val existingBackdropState = LocalFrostedBackdropState.current
    val fallbackContentLayer = rememberGraphicsLayer()
    val fallbackBgLayer = rememberGraphicsLayer()
    val frostedBackdropState = existingBackdropState ?: remember(fallbackContentLayer, fallbackBgLayer) {
        FrostedBackdropState(
            graphicsLayer = fallbackContentLayer,
            backgroundGraphicsLayer = fallbackBgLayer
        )
    }

    // SaveableStateHolder + currentSubScreenEntry so switching between Home, Activity, Plan, Insights,
    // and nested SubScreens preserves each screen instance's exact scroll position and state.
    val currentSubScreenEntry by viewModel.currentSubScreenEntry.collectAsState()
    val navSaveableStateHolder = rememberSaveableStateHolder()

    CompositionLocalProvider(
        LocalFrostedBackdropState provides frostedBackdropState,
        LocalHapticsEnabled provides extendedSettings.hapticFeedbackEnabled
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    MyWalletBottomNavigationBar(
                        currentTab = currentTab,
                        language = settings?.language ?: AppLanguage.ENGLISH,
                        onTabSelected = { viewModel.setTab(it) },
                        onAddClick = {
                            viewModel.clearTransactionDraft()
                            viewModel.transactionToEdit.value = null
                            viewModel.newTransactionInitialType.value = TransactionType.EXPENSE
                            viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
                        }
                    )
                }
            }
        ) { _ ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (currentSubScreen == SubScreen.NONE) {
                    val tabScopeKey = "TAB:${currentTab.name}"
                    key(tabScopeKey) {
                        navSaveableStateHolder.SaveableStateProvider(tabScopeKey) {
                            when (currentTab) {
                                MainTab.HOME -> HomeScreen(
                                    viewModel = viewModel
                                )
                                MainTab.ACTIVITY -> ActivityScreen(
                                    viewModel = viewModel
                                )
                                MainTab.PLAN -> PlanScreen(
                                    viewModel = viewModel
                                )
                                MainTab.INSIGHTS -> InsightsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                } else {
                    val subEntryKey = currentSubScreenEntry?.stateKey ?: "SUB:0:${currentSubScreen.name}:root"
                    key(subEntryKey) {
                        navSaveableStateHolder.SaveableStateProvider(subEntryKey) {
                            when (currentSubScreen) {
                                SubScreen.NONE -> Unit
                SubScreen.ACCOUNTS_MANAGEMENT -> {
                    AccountsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.ADD_EDIT_TRANSACTION -> {
                    AddTransactionScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.PROFILE_SETTINGS -> {
                    ProfileScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.CURRENCY_SELECTION -> {
                    CurrencySelectionScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.THEME_SETTINGS -> {
                    ThemeScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.CATEGORIES_MANAGEMENT -> {
                    CategoriesScreen(
                        viewModel = viewModel
                    )
                }
                SubScreen.ACCOUNT_DETAIL -> {
                    AccountDetailScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() },
                        onEditAccount = { acc -> viewModel.openEditAccount(acc) }
                    )
                }
                SubScreen.EDIT_ACCOUNT -> {
                    val accountToEdit by viewModel.accountToEdit.collectAsState()
                    EditAccountScreen(
                        viewModel = viewModel,
                        account = accountToEdit,
                        onBack = { viewModel.closeSubScreen() },
                        onNavigateToAdjustBalance = { acc -> viewModel.openAdjustBalance(acc) }
                    )
                }
                SubScreen.ADJUST_BALANCE -> {
                    val accountForAdjust by viewModel.accountForAdjustBalance.collectAsState()
                    com.example.ui.screens.accounts.AdjustBalanceScreen(
                        viewModel = viewModel,
                        account = accountForAdjust,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.BUDGET_DASHBOARD -> {
                    com.example.ui.screens.plan.BudgetDashboardScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.ADD_BUDGET -> {
                    val budgetToEdit by viewModel.budgetToEdit.collectAsState()
                    com.example.ui.screens.plan.AddBudgetScreen(
                        viewModel = viewModel,
                        initialBudget = budgetToEdit,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.MANAGE_DEBTS -> {
                    ManageDebtsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.DEBT_DETAILS -> {
                    DebtDetailsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.ADD_DEBT -> {
                    AddDebtScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.NEW_SAVINGS_GOAL -> {
                    NewSavingsGoalScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.CURRENCY_CONVERTER -> {
                    CurrencyConverterScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.MONTHLY_RECAP -> {
                    MonthlyRecapScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.SPENDING_COMPARISON -> {
                    SpendingComparisonScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.INCOME_TRACKER -> {
                    IncomeTrackerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.CALENDAR -> {
                    CalendarScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.ADD_DEBT_TRANSACTION -> {
                    AddDebtTransactionScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.NOTIFICATIONS -> {
                    NotificationsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.USER_PROFILE -> {
                    ProfileScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.BACKUP_RESTORE -> {
                    BackupRestoreScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.ABOUT -> {
                    AboutScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.NOTIFICATION_MANAGER -> {
                    NotificationManagerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                            }
                        }
                    }
                }

            if (extendedSettings.useScreenLock && isDeviceSecure && !isUnlockedForSession) {
                LaunchedEffect(hasAutoPromptedLock) {
                    if (!hasAutoPromptedLock) {
                        hasAutoPromptedLock = true
                        promptDeviceUnlock()
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("screen_lock_overlay"),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Hishab Pati is Locked",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Unlock with your device screen lock to access your wallets and transactions.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Button(
                            onClick = { promptDeviceUnlock() },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("screen_lock_unlock_button")
                        ) {
                            Text(
                                text = "Unlock App",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
        }
    }

    // Voice Transaction Entry Dialog
    if (isVoiceDialogVisible) {
        VoiceTransactionDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.isVoiceDialogVisible.value = false }
        )
    }

    // Multi-step Backup & Restore Modal (Preview -> Confirmation -> Restoring -> Result)
    val restoreUiState by viewModel.restoreUiState.collectAsState()
    BackupRestoreModal(
        state = restoreUiState,
        isBangla = settings?.language == AppLanguage.BANGLA,
        currencySymbol = settings?.currencySymbol ?: "৳",
        onSelectMode = { mode -> viewModel.selectRestoreMode(mode) },
        onProceedToConfirm = { viewModel.proceedToRestoreConfirmation() },
        onBackToPreview = { viewModel.backToRestorePreview() },
        onExecuteRestore = { mode -> viewModel.executeRestoreWithMode(mode) },
        onExportFreshBackup = {},
        onPickAnotherFile = { viewModel.cancelRestore() },
        onGoToDashboard = { viewModel.completeRestoreAndGoToDashboard() },
        onDismiss = { viewModel.cancelRestore() }
    )
}

private data class NavItem(
    val tab: MainTab,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
internal fun MyWalletBottomNavigationBar(
    currentTab: MainTab,
    language: AppLanguage = AppLanguage.ENGLISH,
    onTabSelected: (MainTab) -> Unit,
    onAddClick: () -> Unit
) {
    val items = listOf(
        NavItem(MainTab.HOME, AppStrings.t("nav_home", language), Icons.Default.Home, "tab_home"),
        NavItem(MainTab.ACTIVITY, AppStrings.t("nav_activity", language), Icons.AutoMirrored.Filled.ReceiptLong, "tab_activity"),
        NavItem(MainTab.PLAN, AppStrings.t("nav_plan", language), Icons.Default.AccountBalanceWallet, "tab_plan"),
        NavItem(MainTab.INSIGHTS, AppStrings.t("nav_insights", language), Icons.Default.PieChart, "tab_insights")
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = surfaceColor.luminance() < 0.5f
    val capsuleShape = RoundedCornerShape(32.dp)
    val view = LocalView.current
    val hapticsEnabled = LocalHapticsEnabled.current

    // Always-frosted, clear low-opacity liquid-glass appearance:
    // - Keeps the bottom navigation capsule and '+' button frosted at all times (even when idle at top)
    // - Background and scrolling content remain visible through the subtle blur (9.dp)
    // - Light surface tint (14%–22% in light mode, 18%–26% in dark mode) with a soft specular edge highlight
    val capsuleBaseBg = if (isDark) {
        MaterialTheme.colorScheme.background.copy(alpha = 0.18f)
    } else {
        surfaceColor.copy(alpha = 0.14f)
    }
    val capsuleOverlayBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1E293B).copy(alpha = 0.26f),
                Color(0xFF0F172A).copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.12f)
            )
        )
    }
    val capsuleBorder = BorderStroke(
        width = 1.dp,
        color = Color.White.copy(alpha = if (isDark) 0.20f else 0.68f)
    )
    val capsuleOuterShadowColor = Color.Black.copy(
        alpha = if (isDark) 0.22f else 0.09f
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding()
            .testTag("bottom_nav_container"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded liquid-crystal capsule grouping Home, Activity, Plan, and Insights
        FrostedGlassSurface(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .crystalOuterShadow(
                    shape = capsuleShape,
                    color = capsuleOuterShadowColor,
                    blurSpread = 8.dp,
                    offsetY = 3.dp
                )
                .testTag("bottom_nav_capsule"),
            shape = capsuleShape,
            baseBackgroundColor = capsuleBaseBg,
            overlayBrush = capsuleOverlayBrush,
            restingTintBrush = capsuleOverlayBrush,
            blurRadius = 9.dp,
            glassProgressOverride = 1f,
            border = capsuleBorder
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                val tabSlotWidth = maxWidth / items.size.toFloat()
                val indicatorOffsetX = tabSlotWidth * currentTab.ordinal.toFloat()
                val pillShape = RoundedCornerShape(26.dp)

                // Clear liquid-crystal selected-tab indicator:
                // - High-clarity translucent body (clear center so the capsule's frosted backdrop shows through,
                //   never milky or overly bright)
                // - Subtle diagonal/top specular sheen and restrained outer ambient depth
                // - Soft refractive edge highlight around the pill perimeter
                val crystalBodyBrush = if (isDark) {
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.10f),
                        0.42f to primaryColor.copy(alpha = 0.08f),
                        1.0f to primaryColor.copy(alpha = 0.14f)
                    )
                } else {
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.22f),
                        0.45f to primaryColor.copy(alpha = 0.05f),
                        1.0f to primaryColor.copy(alpha = 0.10f)
                    )
                }
                val crystalBorderBrush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = if (isDark) 0.28f else 0.62f),
                    0.5f to primaryColor.copy(alpha = if (isDark) 0.20f else 0.16f),
                    1.0f to Color.White.copy(alpha = if (isDark) 0.10f else 0.30f)
                )
                val crystalTopSheenAlpha = if (isDark) 0.07f else 0.16f
                val crystalOuterDepthColor = primaryColor.copy(
                    alpha = if (isDark) 0.14f else 0.07f
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffsetX)
                        .width(tabSlotWidth)
                        .fillMaxHeight()
                        .crystalOuterShadow(
                            shape = pillShape,
                            color = crystalOuterDepthColor,
                            blurSpread = 4.dp,
                            offsetY = 1.5.dp
                        )
                        .clip(pillShape)
                        .background(crystalBodyBrush)
                        .drawBehind {
                            // Subtle diagonal liquid-crystal sheen across the upper surface
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = crystalTopSheenAlpha),
                                        Color.Transparent,
                                        primaryColor.copy(alpha = if (isDark) 0.05f else 0.03f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width * 0.85f, size.height)
                                )
                            )
                        }
                        .border(
                            BorderStroke(width = 1.dp, brush = crystalBorderBrush),
                            shape = pillShape
                        )
                        .testTag("bottom_nav_selected_indicator")
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val selected = currentTab == item.tab
                        val contentTint = if (selected) {
                            primaryColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isDark) 0.84f else 0.78f
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(26.dp))
                                .clickable {
                                    AppHaptics.performTap(view, hapticsEnabled)
                                    onTabSelected(item.tab)
                                }
                                .padding(vertical = 4.dp)
                                .testTag(item.testTag),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = contentTint,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = contentTint
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Circular "+" action positioned beside the capsule, aligned to the same 64.dp height
        // and kept consistently frosted with a translucent liquid-glass tint
        val addButtonGlassBrush = Brush.linearGradient(
            listOf(
                Color(0xFF6366F1).copy(alpha = if (isDark) 0.68f else 0.72f),
                Color(0xFF7C3AED).copy(alpha = if (isDark) 0.70f else 0.74f),
                Color(0xFF4F46E5).copy(alpha = if (isDark) 0.74f else 0.78f)
            )
        )
        val addButtonBorder = BorderStroke(
            width = 1.2.dp,
            color = Color.White.copy(alpha = if (isDark) 0.34f else 0.56f)
        )

        FrostedGlassSurface(
            modifier = Modifier
                .size(64.dp)
                .crystalOuterShadow(
                    shape = CircleShape,
                    color = Color(0xFF4F46E5).copy(alpha = 0.24f),
                    blurSpread = 8.dp,
                    offsetY = 3.dp
                )
                .clip(CircleShape)
                .clickable(
                    onClick = {
                        AppHaptics.performTap(view, hapticsEnabled)
                        onAddClick()
                    }
                )
                .testTag("floating_add_transaction_button"),
            shape = CircleShape,
            baseBackgroundColor = Color.Transparent,
            overlayBrush = addButtonGlassBrush,
            restingTintBrush = addButtonGlassBrush,
            blurRadius = 9.dp,
            glassProgressOverride = 1f,
            border = addButtonBorder
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
