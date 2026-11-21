package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.example.ui.navigation.LocalNavAnimatedVisibilityScope
import com.example.ui.navigation.LocalSharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.components.rememberReducedMotionEnabled
import com.example.ui.theme.CrystalAquaDeep
import com.example.ui.theme.CrystalSkyCyan
import com.example.ui.theme.CrystalSkyCyanBright
import com.example.ui.theme.CrystalSkyCyanDeep
import com.example.ui.theme.CrystalSkyCyanLightText
import com.example.data.model.BackgroundTextureOption
import com.example.data.model.ThemeColorOption
import com.example.data.model.ThemeMode
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.navigation.rememberScreenLazyListState
import com.example.ui.components.CrystalLiquidSnackbarHost
import com.example.ui.components.VoiceTransactionDialog
import com.example.ui.screens.accounts.AccountDetailScreen
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.accounts.EditAccountScreen
import com.example.ui.screens.activity.ActivityScreen
import com.example.ui.screens.activity.FilteredReportsScreen
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
import com.example.ui.components.UpdateDialog
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
import kotlin.math.abs

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
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.clearUpdatePermissionNoticeIfGranted(context)
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
            viewModel.clearNotificationMessage()
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
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
            snackbarHost = { CrystalLiquidSnackbarHost(snackbarHostState) },
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
                val reducedMotion = rememberReducedMotionEnabled()
                val rootHostKey = remember(currentSubScreen, currentSubScreenEntry) {
                    if (currentSubScreen == SubScreen.NONE) {
                        "MAIN_TABS"
                    } else {
                        currentSubScreenEntry?.stateKey ?: "SUB:0:${currentSubScreen.name}:root"
                    }
                }
                @OptIn(ExperimentalSharedTransitionApi::class)
                SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = rootHostKey,
                        transitionSpec = {
                            val isAccountDetailTransition =
                                targetState.contains(SubScreen.ACCOUNT_DETAIL.name) ||
                                    initialState.contains(SubScreen.ACCOUNT_DETAIL.name)
                            if (reducedMotion) {
                                fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                            } else if (isAccountDetailTransition) {
                                (fadeIn(tween(durationMillis = 300)) togetherWith
                                    fadeOut(tween(durationMillis = 220))) using SizeTransform(clip = false)
                            } else {
                                (fadeIn(tween(durationMillis = 200)) togetherWith
                                    fadeOut(tween(durationMillis = 160))) using SizeTransform(clip = false)
                            }
                        },
                        label = "root_shared_transition_host",
                        modifier = Modifier.fillMaxSize()
                    ) { hostKey ->
                        CompositionLocalProvider(
                            LocalSharedTransitionScope provides this@SharedTransitionLayout,
                            LocalNavAnimatedVisibilityScope provides this@AnimatedContent
                        ) {
                            if (hostKey == "MAIN_TABS") {
                                AnimatedContent(
                                    targetState = currentTab,
                                    transitionSpec = {
                                        if (reducedMotion) {
                                            fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                                        } else if (targetState.ordinal > initialState.ordinal) {
                                            (slideInHorizontally(
                                                animationSpec = tween(durationMillis = 240),
                                                initialOffsetX = { it / 12 }
                                            ) + fadeIn(animationSpec = tween(durationMillis = 240))) togetherWith
                                                (slideOutHorizontally(
                                                    animationSpec = tween(durationMillis = 180),
                                                    targetOffsetX = { -it / 12 }
                                                ) + fadeOut(animationSpec = tween(durationMillis = 180)))
                                        } else {
                                            (slideInHorizontally(
                                                animationSpec = tween(durationMillis = 240),
                                                initialOffsetX = { -it / 12 }
                                            ) + fadeIn(animationSpec = tween(durationMillis = 240))) togetherWith
                                                (slideOutHorizontally(
                                                    animationSpec = tween(durationMillis = 180),
                                                    targetOffsetX = { it / 12 }
                                                ) + fadeOut(animationSpec = tween(durationMillis = 180)))
                                        } using SizeTransform(clip = false)
                                    },
                                    label = "main_tab_directional_transition",
                                    modifier = Modifier.fillMaxSize()
                                ) { tab ->
                                    val tabScopeKey = "TAB:${tab.name}"
                                    navSaveableStateHolder.SaveableStateProvider(tabScopeKey) {
                                        when (tab) {
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
                                val resolvedSubScreen = remember(hostKey) {
                                    SubScreen.entries.firstOrNull { entry ->
                                        entry != SubScreen.NONE && hostKey.contains(":${entry.name}:")
                                    } ?: currentSubScreen
                                }
                                navSaveableStateHolder.SaveableStateProvider(hostKey) {
                                    when (resolvedSubScreen) {
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
                        onBack = { viewModel.closeSubScreen(SubScreen.ADD_EDIT_TRANSACTION) }
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
                        onBack = { viewModel.closeSubScreen(SubScreen.BUDGET_DASHBOARD) }
                    )
                }
                SubScreen.BUDGET_DETAILS -> {
                    com.example.ui.screens.plan.BudgetDetailsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen(SubScreen.BUDGET_DETAILS) }
                    )
                }
                SubScreen.ADD_BUDGET -> {
                    val budgetToEdit by viewModel.budgetToEdit.collectAsState()
                    val defaultIsOverall by viewModel.addBudgetDefaultIsOverall.collectAsState()
                    val defaultCategory by viewModel.addBudgetDefaultCategory.collectAsState()
                    val defaultMonthKey by viewModel.addBudgetDefaultMonthKey.collectAsState()
                    com.example.ui.screens.plan.AddEditBudgetScreen(
                        viewModel = viewModel,
                        initialBudget = budgetToEdit,
                        defaultIsOverall = defaultIsOverall,
                        defaultCategory = defaultCategory,
                        defaultMonthKey = defaultMonthKey,
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                SubScreen.MANAGE_DEBTS -> {
                    ManageDebtsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen(SubScreen.MANAGE_DEBTS) }
                    )
                }
                SubScreen.DEBT_DETAILS -> {
                    DebtDetailsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen(SubScreen.DEBT_DETAILS) }
                    )
                }
                SubScreen.ADD_DEBT -> {
                    AddDebtScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen(SubScreen.ADD_DEBT) }
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
                        onBack = { viewModel.closeSubScreen(SubScreen.ADD_DEBT_TRANSACTION) }
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
                SubScreen.FILTERED_REPORTS -> {
                    FilteredReportsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeSubScreen(SubScreen.FILTERED_REPORTS) }
                    )
                }
                            }
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

    // In-App GitHub Release Update Modal
    val updateState by viewModel.updateState.collectAsState()
    val isUpdateDialogVisible by viewModel.isUpdateDialogVisible.collectAsState()
    val updatePermissionNotice by viewModel.updatePermissionNotice.collectAsState()
    UpdateDialog(
        visible = isUpdateDialogVisible,
        state = updateState,
        isBangla = settings?.language == AppLanguage.BANGLA,
        permissionNotice = updatePermissionNotice,
        onUpdateNow = { releaseInfo -> viewModel.startUpdateDownload(releaseInfo) },
        onInstallNow = { apkFile -> viewModel.installUpdate(apkFile, context) },
        onCancelDownload = { fallbackRelease -> viewModel.cancelUpdateDownload(fallbackRelease) },
        onSkipVersion = { version -> viewModel.skipUpdateVersion(version) },
        onRetryCheck = { viewModel.checkForUpdatesManually() },
        onDismiss = { viewModel.dismissUpdateDialog() }
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
    val items = remember(language) {
        listOf(
            NavItem(MainTab.HOME, AppStrings.t("nav_home", language), Icons.Default.Home, "tab_home"),
            NavItem(MainTab.ACTIVITY, AppStrings.t("nav_activity", language), Icons.AutoMirrored.Filled.ReceiptLong, "tab_activity"),
            NavItem(MainTab.PLAN, AppStrings.t("nav_plan", language), Icons.Default.AccountBalanceWallet, "tab_plan"),
            NavItem(MainTab.INSIGHTS, AppStrings.t("nav_insights", language), Icons.Default.PieChart, "tab_insights")
        )
    }

    val primary = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = surfaceColor.luminance() < 0.5f
    val view = LocalView.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val reducedMotion = rememberReducedMotionEnabled()

    // Frosted Crystal Liquid floating capsule (CircleShape, 0.88f translucent surface, 1.dp specular rim)
    val capsuleTintColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
    } else {
        Color.White.copy(alpha = 0.88f)
    }
    val capsuleOverlayBrush = Brush.verticalGradient(
        listOf(
            if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.30f),
            if (isDark) Color.Transparent else primary.copy(alpha = 0.04f)
        )
    )
    val capsuleBorder = BorderStroke(
        width = 1.dp,
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (isDark) 0.24f else 0.85f),
                primary.copy(alpha = if (isDark) 0.16f else 0.18f)
            )
        )
    )
    val capsuleOuterShadowColor = Color.Black.copy(
        alpha = if (isDark) 0.28f else 0.10f
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding()
            .testTag("bottom_nav_container"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Floating Crystal Liquid capsule with continuous Aura Player-style sliding pill underlay
        FrostedGlassSurface(
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .crystalOuterShadow(
                    shape = CircleShape,
                    color = capsuleOuterShadowColor,
                    blurSpread = 10.dp,
                    offsetY = 4.dp
                )
                .testTag("bottom_nav_capsule"),
            shape = CircleShape,
            baseBackgroundColor = capsuleTintColor,
            overlayBrush = capsuleOverlayBrush,
            restingTintBrush = capsuleOverlayBrush,
            blurRadius = 10.dp,
            glassProgressOverride = 1f,
            border = capsuleBorder
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 5.dp)
            ) {
                val tabWidth = maxWidth / items.size.toFloat()
                val selectedIndex = currentTab.ordinal
                val pillSpringSpec = spring<androidx.compose.ui.unit.Dp>(
                    dampingRatio = 0.78f,
                    stiffness = 420f
                )

                val indicatorOffsetX by animateDpAsState(
                    targetValue = tabWidth * selectedIndex.toFloat(),
                    animationSpec = if (reducedMotion) snap() else pillSpringSpec,
                    label = "aura_sliding_pill_offset"
                )
                val animatedPillWidth by animateDpAsState(
                    targetValue = tabWidth,
                    animationSpec = if (reducedMotion) snap() else pillSpringSpec,
                    label = "aura_sliding_pill_width"
                )

                // Single continuous sliding pill underlay behind all 4 tabs
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffsetX)
                        .width(animatedPillWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.14f))
                        .border(
                            width = 1.dp,
                            color = primary.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                        .testTag("bottom_nav_selected_indicator")
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val selected = currentTab == item.tab
                        val targetTint = if (selected) {
                            primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val contentTint by animateColorAsState(
                            targetValue = targetTint,
                            animationSpec = if (reducedMotion) snap() else tween(durationMillis = 220),
                            label = "aura_tab_tint_${item.tab.name}"
                        )

                        // Subtle tactile icon bounce (1.0f -> 1.08f -> 1.0f) on active selection
                        val iconScale = remember { Animatable(1f) }
                        var initialized by remember { mutableStateOf(false) }
                        LaunchedEffect(selected) {
                            if (!initialized) {
                                initialized = true
                                iconScale.snapTo(1f)
                            } else if (selected && !reducedMotion) {
                                iconScale.snapTo(1f)
                                iconScale.animateTo(
                                    targetValue = 1.08f,
                                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 580f)
                                )
                                iconScale.animateTo(
                                    targetValue = 1.0f,
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f)
                                )
                            } else {
                                iconScale.snapTo(1f)
                            }
                        }

                        val tabInteractionSource = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = tabInteractionSource,
                                    indication = ripple(bounded = true, color = primary),
                                    onClick = {
                                        if (item.tab != currentTab) {
                                            AppHaptics.performTap(view, hapticsEnabled)
                                            onTabSelected(item.tab)
                                        }
                                    }
                                )
                                .padding(vertical = 3.dp)
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
                                    modifier = Modifier
                                        .size(21.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale.value
                                            scaleY = iconScale.value
                                        }
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = item.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    color = contentTint,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Circular 56.dp "+" quick-add orb with translucent Royal Indigo-to-Violet Liquid Crystal Glass & live backdrop blur
        val fabInteractionSource = remember { MutableInteractionSource() }
        val isFabPressed by fabInteractionSource.collectIsPressedAsState()
        val fabScale by animateFloatAsState(
            targetValue = if (isFabPressed && !reducedMotion) 0.92f else 1f,
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = 500f
            ),
            label = "quick_add_fab_press_scale"
        )
        val fabGlassAlpha = if (isDark) 0.68f else 0.72f
        val fabGlassTintBrush = remember(fabGlassAlpha) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF4F46E5).copy(alpha = fabGlassAlpha),
                    Color(0xFF7C3AED).copy(alpha = fabGlassAlpha)
                )
            )
        }
        val fabCrystalRim = remember {
            BorderStroke(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color(0xFFC4B5FD).copy(alpha = 0.30f)
                    )
                )
            )
        }
        val fabBackdropFallback = if (isDark) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            Color.White
        }

        FrostedGlassSurface(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = fabScale
                    scaleY = fabScale
                }
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFF5B21B6).copy(alpha = 0.24f),
                    spotColor = Color(0xFF5B21B6).copy(alpha = 0.32f)
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = fabInteractionSource,
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = {
                        AppHaptics.performTap(view, hapticsEnabled)
                        onAddClick()
                    }
                )
                .testTag("floating_add_transaction_button"),
            shape = CircleShape,
            baseBackgroundColor = fabBackdropFallback,
            overlayBrush = fabGlassTintBrush,
            restingTintBrush = fabGlassTintBrush,
            blurRadius = 14.dp,
            glassProgressOverride = 1f,
            border = fabCrystalRim
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Transaction",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
