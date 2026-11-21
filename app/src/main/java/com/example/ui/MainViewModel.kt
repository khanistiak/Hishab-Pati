package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.backup.AutoBackupFrequency
import com.example.data.backup.AutoBackupScheduler
import com.example.data.backup.BackupDiagnosticError
import com.example.data.backup.BackupErrorCode
import com.example.data.backup.BackupPreview
import com.example.data.backup.BackupRestoreException
import com.example.data.backup.JsonBackupManager
import com.example.data.backup.RestoreMode
import com.example.data.backup.RestoreProgress
import com.example.data.backup.RestoreStage
import com.example.data.backup.RestoreUiState
import com.example.data.backup.SampleDataGenerator
import com.example.data.firebase.FirestoreWalletRepository
import com.example.data.local.AppDatabase
import com.example.data.local.ExtendedSettingsState
import com.example.data.local.UserSettingsPreferences
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountSortOption
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.BackgroundTextureOption
import com.example.data.model.DebtType
import com.example.data.model.ThemeColorOption
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionType
import com.example.data.notification.NotificationStore
import com.example.data.repository.WalletRepository
import com.example.data.update.GitHubReleaseInfo
import com.example.data.update.GitHubUpdateManager
import com.example.data.update.InstallApkResult
import com.example.data.update.UpdateState
import com.example.ui.navigation.NavigationScrollStateStore
import com.example.ui.navigation.SubScreenNavEntry
import com.example.ui.screens.insights.InsightsTabMode
import com.example.ui.screens.notifications.NotificationDestinationType
import com.example.ui.screens.notifications.generateRealNotifications
import com.example.ui.screens.plan.CurrencyRateRepository
import com.example.ui.screens.plan.calculateDebtRemaining
import com.example.ui.screens.plan.isDebtAdditionTransaction
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.voice.ParsedVoiceTransaction
import com.example.voice.VoiceTransactionParser
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

data class CurrencyConverterUiState(
    val fromCode: String = "BDT",
    val toCode: String = "USD",
    val amountInput: String = "1000",
    val ratesPerUsd: Map<String, Double> = emptyMap(),
    val customOverrides: Map<String, Double> = emptyMap(),
    val lastUpdatedMillis: Long = 0L,
    val isRefreshing: Boolean = false,
    val isOfflineFallback: Boolean = false
)

enum class MainTab {
    HOME,
    ACTIVITY,
    PLAN,
    INSIGHTS
}

enum class SubScreen {
    NONE,
    ACCOUNTS_MANAGEMENT,
    ADD_EDIT_TRANSACTION,
    SETTINGS,
    PROFILE_SETTINGS,
    THEME_SETTINGS,
    CATEGORIES_MANAGEMENT,
    ACCOUNT_DETAIL,
    EDIT_ACCOUNT,
    ADJUST_BALANCE,
    BUDGET_DASHBOARD,
    ADD_BUDGET,
    CURRENCY_SELECTION,
    MANAGE_DEBTS,
    DEBT_DETAILS,
    ADD_DEBT,
    NEW_SAVINGS_GOAL,
    CURRENCY_CONVERTER,
    MONTHLY_RECAP,
    SPENDING_COMPARISON,
    INCOME_TRACKER,
    CALENDAR,
    ADD_DEBT_TRANSACTION,
    NOTIFICATIONS,
    NOTIFICATION_MANAGER,
    USER_PROFILE,
    BACKUP_RESTORE,
    ABOUT,
    FILTERED_REPORTS
}

data class FilteredReportArgs(
    val category: String? = null,
    val subcategory: String? = null,
    val dateRange: Pair<Long, Long>? = null,
    val dateRangeLabel: String? = null,
    val filterType: ActivityFilterType = ActivityFilterType.EXPENSES,
    val accountId: String? = null,
    val searchQuery: String = ""
)

enum class InsightsPeriod(val label: String, val bnLabel: String) {
    DAILY("Daily", "দৈনিক"),
    THIS_WEEK("This Week", "এই সপ্তাহ"),
    THIS_MONTH("This Month", "এই মাস"),
    LAST_MONTH("Last Month", "গত মাস"),
    THIS_YEAR("This Year", "এই বছর"),
    LAST_YEAR("Last Year", "গত বছর"),
    CUSTOM("Custom", "কাস্টম")
}

enum class ActivityFilterType(val label: String) {
    ALL("All"),
    EXPENSES("Expenses"),
    INCOME("Income"),
    TRANSFERS("Transfers")
}

enum class ActivitySort(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_AMOUNT("Highest Amount"),
    LOWEST_AMOUNT("Lowest Amount")
}

data class NetWorthSummary(
    val netWorth: Double = 0.0,
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0
)

enum class HomeCardStyle {
    BALANCE_CARD,
    STACK,
    CAROUSEL;

    fun next(): HomeCardStyle = when (this) {
        BALANCE_CARD -> STACK
        STACK -> CAROUSEL
        CAROUSEL -> BALANCE_CARD
    }
}

enum class BalanceCardStyle {
    STANDARD,
    GRADIENT
}

val DEFAULT_HOME_SECTIONS: List<String> = listOf(
    "budget",
    "bills",
    "recent_activity",
    "savings",
    "financial_health",
    "smart_insights",
    "top_spending",
    "spending_by_day",
    "spending_streak",
    "shortcuts"
)

data class HomeCustomization(
    val cardStyle: HomeCardStyle = HomeCardStyle.STACK,
    val balanceCardStyle: BalanceCardStyle = BalanceCardStyle.STANDARD,
    val showBudgetProgress: Boolean = true,
    val showUpcomingBills: Boolean = true,
    val showRecentActivity: Boolean = true,
    val showSavingsGoals: Boolean = true,
    val showFinancialHealth: Boolean = true,
    val showSmartInsights: Boolean = true,
    val showTopSpending: Boolean = true,
    val showSpendingByDay: Boolean = true,
    val showSpendingStreak: Boolean = true,
    val showQuickShortcuts: Boolean = true,
    val sectionOrder: List<String> = DEFAULT_HOME_SECTIONS
)

enum class ActiveWalletSheet {
    NONE,
    SOURCE,
    DESTINATION
}

data class TransactionDraftState(
    val editingTransactionId: String? = null,
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val expression: String = "",
    val isCalculatorVisible: Boolean = true,
    val title: String = "",
    val selectedCategoryName: String = "Food & Dining",
    val selectedSubcategoryName: String? = null,
    val selectedAccountId: String = "",
    val selectedToAccountId: String = "",
    val userManuallyChangedWallet: Boolean = false,
    val selectedDateTimestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val activeWalletSheet: ActiveWalletSheet = ActiveWalletSheet.NONE,
    val optimisticCategories: List<CategoryEntity> = emptyList(),
    val optimisticSubcategories: List<SubcategoryEntity> = emptyList(),
    val newlyCreatedCategoryName: String? = null,
    val voiceOrReceiptNotice: String? = null,
    val isReceiptNotice: Boolean = false,
    val excludeFromBudgets: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = WalletRepository(database)

    val backupManager = JsonBackupManager(database)
    val firestoreRepository = FirestoreWalletRepository(application)
    val isSyncingCloud = MutableStateFlow(false)

    val accounts: StateFlow<List<AccountEntity>> = repository.accounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val subcategories: StateFlow<List<SubcategoryEntity>> = repository.subcategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.budgets
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val debts: StateFlow<List<DebtEntity>> = repository.debts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.savingsGoals
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settings: StateFlow<AppSettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _extendedSettings = MutableStateFlow(UserSettingsPreferences.load(application))
    val extendedSettings: StateFlow<ExtendedSettingsState> = _extendedSettings.asStateFlow()

    val notificationStore = NotificationStore(application)
    val unreadNotificationCount: StateFlow<Int> = combine(
        budgets,
        debts,
        accounts,
        transactions,
        NotificationStore.updates
    ) { bList, dList, aList, tList, _ ->
        val s = settings.value
        val ext = _extendedSettings.value
        generateRealNotifications(
            budgets = bList,
            debts = dList,
            accounts = aList,
            transactions = tList,
            currencySymbol = s?.currencySymbol ?: "৳",
            language = s?.language ?: AppLanguage.ENGLISH,
            notificationStore = notificationStore,
            extendedSettings = ext
        ).count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val selectedDebt = MutableStateFlow<DebtEntity?>(null)
    val debtFilterType = MutableStateFlow(DebtType.BORROWED)
    val debtToEdit = MutableStateFlow<DebtEntity?>(null)
    val selectedDebtForTransaction = MutableStateFlow<DebtEntity?>(null)
    val debtTransactionType = MutableStateFlow("REPAYMENT")
    val showSettledDebts = MutableStateFlow(false)

    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _currentSubScreen = MutableStateFlow(SubScreen.NONE)
    val currentSubScreen: StateFlow<SubScreen> = _currentSubScreen.asStateFlow()

    val activitySearchQuery = MutableStateFlow("")
    val activityFilter = MutableStateFlow(ActivityFilterType.ALL)
    val activitySort = MutableStateFlow(ActivitySort.NEWEST)
    val activitySelectedCategory = MutableStateFlow<String?>(null)
    val activitySelectedAccount = MutableStateFlow<String?>(null)
    val activityCustomDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val activityDateRangeLabel = MutableStateFlow<String?>(null)

    val insightsTabMode = MutableStateFlow(InsightsTabMode.EXPENSE)
    val insightsPeriod = MutableStateFlow(InsightsPeriod.THIS_MONTH)
    val insightsPeriodOffset = MutableStateFlow(0)
    val insightsCustomStartDate = MutableStateFlow<Long?>(null)
    val insightsCustomEndDate = MutableStateFlow<Long?>(null)
    val insightsSelectedCategory = MutableStateFlow<String?>(null)
    val filteredReportArgs = MutableStateFlow<FilteredReportArgs?>(null)

    fun openFilteredReports(
        category: String? = null,
        subcategory: String? = null,
        dateRange: Pair<Long, Long>? = null,
        dateRangeLabel: String? = null,
        filterType: ActivityFilterType = ActivityFilterType.EXPENSES,
        accountId: String? = null,
        searchQuery: String = ""
    ) {
        filteredReportArgs.value = FilteredReportArgs(
            category = category,
            subcategory = subcategory,
            dateRange = dateRange,
            dateRangeLabel = dateRangeLabel,
            filterType = filterType,
            accountId = accountId,
            searchQuery = searchQuery
        )
        openSubScreen(SubScreen.FILTERED_REPORTS)
    }

    fun navigateToActivityWithFilter(
        category: String? = null,
        subcategory: String? = null,
        query: String? = null,
        dateRange: Pair<Long, Long>? = null,
        dateRangeLabel: String? = null,
        filterType: ActivityFilterType? = null,
        clearAccountFilter: Boolean = false
    ) {
        activitySelectedCategory.value = category
        activitySearchQuery.value = query ?: subcategory ?: ""
        activityCustomDateRange.value = dateRange
        activityDateRangeLabel.value = dateRangeLabel
        if (filterType != null) {
            activityFilter.value = filterType
        }
        if (clearAccountFilter) {
            activitySelectedAccount.value = null
        }
        openSubScreen(SubScreen.NONE)
        _currentTab.value = MainTab.ACTIVITY
    }

    fun clearActivityFilters() {
        activitySearchQuery.value = ""
        activityFilter.value = ActivityFilterType.ALL
        activitySelectedCategory.value = null
        activitySelectedAccount.value = null
        activityCustomDateRange.value = null
        activityDateRangeLabel.value = null
    }

    val transactionToEdit = MutableStateFlow<TransactionEntity?>(null)
    val newTransactionInitialType = MutableStateFlow(TransactionType.EXPENSE)

    private fun loadHomeCustomization(): HomeCustomization =
        UserSettingsPreferences.loadHomeCustomization(getApplication())

    val homeCustomization = MutableStateFlow(loadHomeCustomization())

    fun updateHomeCustomization(customization: HomeCustomization) {
        val normalized = customization.copy(
            sectionOrder = UserSettingsPreferences.normalizeHomeSectionOrder(customization.sectionOrder)
        )
        homeCustomization.value = normalized
        UserSettingsPreferences.saveHomeCustomization(getApplication(), normalized)
    }

    fun updateExtendedSettings(transform: (ExtendedSettingsState) -> ExtendedSettingsState) {
        val updated = transform(_extendedSettings.value)
        _extendedSettings.value = updated
        UserSettingsPreferences.save(getApplication(), updated)
    }

    fun setDailyLoggingReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0) {
        updateExtendedSettings { it.withDailyLoggingReminder(enabled = enabled, hour = hour, minute = minute) }
    }

    fun setCreditPaymentDueReminder(enabled: Boolean, remindMeOption: String = "1 day before") {
        updateExtendedSettings { it.withCreditPaymentDueReminder(enabled = enabled, remindMeOption = remindMeOption) }
    }

    fun setCreditStatementClosingReminder(enabled: Boolean) {
        updateExtendedSettings { it.withCreditStatementClosingReminder(enabled = enabled) }
    }

    fun setPlannedPaymentsSummary(enabled: Boolean, hour: Int = 9, minute: Int = 0) {
        updateExtendedSettings { it.withPlannedPaymentsSummary(enabled = enabled, hour = hour, minute = minute) }
    }

    fun setEnableSubcategories(enabled: Boolean) {
        updateExtendedSettings { it.copy(enableSubcategories = enabled) }
    }

    fun setAutoOpenAmountKeypad(enabled: Boolean) {
        updateExtendedSettings { it.copy(autoOpenAmountKeypad = enabled) }
    }

    fun setDefaultWalletId(walletId: String?) {
        updateExtendedSettings { it.copy(defaultWalletId = walletId ?: "") }
    }

    fun setStartOfWeek(calendarDay: Int) {
        updateExtendedSettings { it.copy(startOfWeek = calendarDay) }
    }

    fun setUseScreenLock(enabled: Boolean) {
        updateExtendedSettings { it.copy(useScreenLock = enabled) }
        toggleAppLock(enabled)
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        updateExtendedSettings { it.copy(hapticFeedbackEnabled = enabled) }
    }

    val isVoiceDialogVisible = MutableStateFlow(false)
    val voiceParsedResult = MutableStateFlow<ParsedVoiceTransaction?>(null)
    val pendingStartReceiptScan = MutableStateFlow(false)

    val backupPreviewState = MutableStateFlow<BackupPreview?>(null)
    val backupJsonStringPending = MutableStateFlow<String?>(null)
    val backupFileNamePending = MutableStateFlow<String?>(null)
    val restoreUiState = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
    val dashboardRefreshTrigger = MutableStateFlow(0L)
    val userNotificationMessage = MutableStateFlow<String?>(null)

    val selectedAccountId = MutableStateFlow<String?>(null)
    val accountToEdit = MutableStateFlow<AccountEntity?>(null)
    val accountForAdjustBalance = MutableStateFlow<AccountEntity?>(null)
    val preselectedAccountIdForNewTx = MutableStateFlow<String?>(null)
    val accountSortOption = MutableStateFlow(AccountSortOption.CUSTOM)
    val accountGroupByType = MutableStateFlow(false)

    val isBackingUpCloud = MutableStateFlow(false)
    val isRestoringCloud = MutableStateFlow(false)

    private val backupPrefs: SharedPreferences by lazy {
        getApplication<Application>().getSharedPreferences("hishab_pati_backup_prefs", Context.MODE_PRIVATE)
    }

    val isCloudSyncEnabled = MutableStateFlow(true)
    val lastCloudBackupTimestamp = MutableStateFlow(0L)
    val lastCloudBackupSize = MutableStateFlow(0L)
    val lastCloudSyncTimestamp = MutableStateFlow(0L)
    val currentUser = MutableStateFlow<FirebaseUser?>(runCatching { Firebase.auth.currentUser }.getOrNull())
    val autoBackupFrequency = MutableStateFlow(AutoBackupFrequency.DAILY)

    val updateManager = GitHubUpdateManager()
    val updateState: StateFlow<UpdateState> = updateManager.updateState
    val isUpdateDialogVisible = MutableStateFlow(false)
    val lastUpdateCheckTimestamp = MutableStateFlow(
        UserSettingsPreferences.getLastUpdateCheckTimestamp(application)
    )
    val updatePermissionNotice = MutableStateFlow<String?>(null)
    private var updateDownloadJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        currentUser.value = auth.currentUser
    }

    init {
        runCatching {
            Firebase.auth.addAuthStateListener(authListener)
        }
        viewModelScope.launch {
            repository.initializeIfNeeded()
        }
        checkForUpdatesOnLaunch()
    }

    override fun onCleared() {
        super.onCleared()
        runCatching {
            Firebase.auth.removeAuthStateListener(authListener)
        }
    }

    fun setAutoBackupFrequency(frequency: AutoBackupFrequency) {
        autoBackupFrequency.value = frequency
        backupPrefs.edit().putString("auto_backup_frequency", frequency.name).apply()
        AutoBackupScheduler.schedule(getApplication(), frequency)
    }

    fun onUserSignedIn() {
        currentUser.value = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (isCloudSyncEnabled.value) {
            syncWithCloud(force = false)
        }
    }

    fun onUserSignedOut() {
        currentUser.value = null
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        isCloudSyncEnabled.value = enabled
        backupPrefs.edit().putBoolean("cloud_sync_enabled", enabled).apply()
    }

    fun backupToCloud() {
        viewModelScope.launch {
            isBackingUpCloud.value = true
            try {
                val json = backupManager.createBackupJson()
                val res = firestoreRepository.saveCloudBackup(
                    jsonString = json,
                    accountsCount = accounts.value.size,
                    transactionsCount = transactions.value.size,
                    categoriesCount = categories.value.size,
                    debtsCount = debts.value.size
                )
                if (res.isSuccess) {
                    val now = System.currentTimeMillis()
                    lastCloudBackupTimestamp.value = now
                    lastCloudBackupSize.value = json.toByteArray(Charsets.UTF_8).size.toLong()
                    lastCloudSyncTimestamp.value = now
                    userNotificationMessage.value = "Cloud backup completed successfully!"
                } else {
                    userNotificationMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Cloud backup failed."
                }
            } catch (e: Exception) {
                userNotificationMessage.value = e.localizedMessage ?: "Cloud backup failed."
            } finally {
                isBackingUpCloud.value = false
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            isRestoringCloud.value = true
            try {
                val fetched = firestoreRepository.fetchLatestCloudBackup()
                if (fetched.isSuccess) {
                    val (json, info) = fetched.getOrThrow()
                    prepareBackupPreview(
                        jsonString = json,
                        fileName = "cloud_backup.json",
                        fileSizeBytes = info.fileSizeBytes
                    )
                } else {
                    userNotificationMessage.value = fetched.exceptionOrNull()?.localizedMessage ?: "No cloud backup found."
                }
            } catch (e: Exception) {
                userNotificationMessage.value = e.localizedMessage ?: "Could not restore from cloud."
            } finally {
                isRestoringCloud.value = false
            }
        }
    }

    fun syncWithCloud(force: Boolean = false) {
        viewModelScope.launch {
            if (currentUser.value == null) return@launch
            isSyncingCloud.value = true
            try {
                firestoreRepository.syncLocalToCloud(accounts.value, transactions.value)
                lastCloudSyncTimestamp.value = System.currentTimeMillis()
            } catch (_: Exception) {
            } finally {
                isSyncingCloud.value = false
            }
        }
    }

    val scrollStateStore = NavigationScrollStateStore()
    private val nextNavEntryId = AtomicLong(1L)
    private val _subScreenStack = mutableListOf<SubScreen>()
    private val _subScreenEntryStack = mutableListOf<SubScreenNavEntry>()
    private val _currentSubScreenEntry = MutableStateFlow<SubScreenNavEntry?>(null)
    val currentSubScreenEntry: StateFlow<SubScreenNavEntry?> = _currentSubScreenEntry.asStateFlow()

    val settingsScrollIndex = MutableStateFlow(0)
    val settingsScrollOffset = MutableStateFlow(0)
    private val _settingsSessionId = MutableStateFlow(0)
    val settingsSessionId: StateFlow<Int> = _settingsSessionId.asStateFlow()

    private fun resolveInstanceKeyForSubScreen(screen: SubScreen): String {
        return when (screen) {
            SubScreen.ACCOUNT_DETAIL -> selectedAccountId.value ?: "default"
            SubScreen.EDIT_ACCOUNT -> accountToEdit.value?.id ?: "new"
            SubScreen.ADJUST_BALANCE -> accountForAdjustBalance.value?.id ?: "default"
            SubScreen.ADD_EDIT_TRANSACTION -> transactionToEdit.value?.id ?: "new_${newTransactionInitialType.value.name}"
            SubScreen.ADD_BUDGET -> budgetToEdit.value?.id ?: "new"
            SubScreen.DEBT_DETAILS -> selectedDebt.value?.id ?: "default"
            SubScreen.ADD_DEBT -> debtToEdit.value?.id ?: "new_${debtFilterType.value.name}"
            SubScreen.ADD_DEBT_TRANSACTION -> "${selectedDebtForTransaction.value?.id ?: "default"}_${debtTransactionType.value}"
            SubScreen.FILTERED_REPORTS -> "${filteredReportArgs.value?.category ?: "all"}_${filteredReportArgs.value?.filterType?.name ?: "EXPENSES"}"
            else -> "root"
        }
    }

    private fun createSubScreenEntry(screen: SubScreen): SubScreenNavEntry {
        return SubScreenNavEntry(
            screen = screen,
            instanceKey = resolveInstanceKeyForSubScreen(screen),
            entryId = nextNavEntryId.getAndIncrement()
        )
    }

    fun resolveNavigationScopeKey(
        screenOverride: SubScreen? = null,
        tabOverride: MainTab? = null,
        activeTab: MainTab = _currentTab.value,
        activeEntry: SubScreenNavEntry? = _currentSubScreenEntry.value
    ): String {
        if (screenOverride != null && screenOverride != SubScreen.NONE) {
            if (activeEntry != null && activeEntry.screen == screenOverride) {
                return activeEntry.stateKey
            }
            val stacked = _subScreenEntryStack.lastOrNull { it.screen == screenOverride }
            if (stacked != null) {
                return stacked.stateKey
            }
            return "SUB:0:${screenOverride.name}:${resolveInstanceKeyForSubScreen(screenOverride)}"
        }
        if (tabOverride != null) {
            return "TAB:${tabOverride.name}"
        }
        if (activeEntry != null && _currentSubScreen.value != SubScreen.NONE) {
            return activeEntry.stateKey
        }
        return "TAB:${activeTab.name}"
    }

    @Composable
    fun rememberNavigationScopeKey(
        screenOverride: SubScreen? = null,
        tabOverride: MainTab? = null
    ): String {
        val activeTab by currentTab.collectAsState()
        val activeEntry by currentSubScreenEntry.collectAsState()
        return resolveNavigationScopeKey(
            screenOverride = screenOverride,
            tabOverride = tabOverride,
            activeTab = activeTab,
            activeEntry = activeEntry
        )
    }

    @Composable
    fun rememberScreenLazyListState(
        subKey: String = "main",
        screenOverride: SubScreen? = null,
        tabOverride: MainTab? = null
    ): LazyListState {
        return com.example.ui.navigation.rememberScreenLazyListState(
            viewModel = this,
            subKey = subKey,
            screenOverride = screenOverride,
            tabOverride = tabOverride
        )
    }

    @Composable
    fun rememberScreenScrollState(
        subKey: String = "main",
        screenOverride: SubScreen? = null,
        tabOverride: MainTab? = null
    ): ScrollState {
        return com.example.ui.navigation.rememberScreenScrollState(
            viewModel = this,
            subKey = subKey,
            screenOverride = screenOverride,
            tabOverride = tabOverride
        )
    }

    fun updateSettingsScrollPosition(index: Int, offset: Int) {
        settingsScrollIndex.value = index.coerceAtLeast(0)
        settingsScrollOffset.value = offset.coerceAtLeast(0)
    }

    fun resetSettingsScrollPosition() {
        settingsScrollIndex.value = 0
        settingsScrollOffset.value = 0
        _settingsSessionId.value += 1
        _currentSubScreenEntry.value?.let { entry ->
            if (entry.screen == SubScreen.SETTINGS || entry.screen == SubScreen.PROFILE_SETTINGS) {
                scrollStateStore.clearScopePrefix(entry.stateKey)
            }
        }
        _subScreenEntryStack
            .filter { it.screen == SubScreen.SETTINGS || it.screen == SubScreen.PROFILE_SETTINGS }
            .forEach { scrollStateStore.clearScopePrefix(it.stateKey) }
    }

    fun isSettingsInNavigationHierarchy(): Boolean {
        return _currentSubScreen.value == SubScreen.SETTINGS ||
            _currentSubScreen.value == SubScreen.PROFILE_SETTINGS ||
            _subScreenStack.contains(SubScreen.SETTINGS) ||
            _subScreenStack.contains(SubScreen.PROFILE_SETTINGS)
    }

    private val _transactionDraftState = MutableStateFlow<TransactionDraftState?>(null)
    val transactionDraftState: StateFlow<TransactionDraftState?> = _transactionDraftState.asStateFlow()

    private val _optimisticAccounts = MutableStateFlow<List<AccountEntity>>(emptyList())
    val optimisticAccounts: StateFlow<List<AccountEntity>> = _optimisticAccounts.asStateFlow()

    fun updateTransactionDraft(draft: TransactionDraftState?) {
        _transactionDraftState.value = draft
    }

    fun clearTransactionDraft() {
        _transactionDraftState.value = null
    }

    fun openAddAccountFromTransaction(draft: TransactionDraftState) {
        _transactionDraftState.value = draft
        accountToEdit.value = null
        if (_currentSubScreen.value == SubScreen.NONE) {
            _currentSubScreen.value = SubScreen.ADD_EDIT_TRANSACTION
            _currentSubScreenEntry.value = createSubScreenEntry(SubScreen.ADD_EDIT_TRANSACTION)
        }
        openSubScreen(SubScreen.EDIT_ACCOUNT)
    }

    fun setTab(tab: MainTab) {
        _subScreenStack.clear()
        _subScreenEntryStack.clear()
        _currentSubScreenEntry.value = null
        _currentSubScreen.value = SubScreen.NONE
        scrollStateStore.clearAllSubScreens()
        resetSettingsScrollPosition()
        _currentTab.value = tab
    }

    fun openSubScreen(screen: SubScreen) {
        if (screen == SubScreen.NONE) {
            _subScreenStack.clear()
            _subScreenEntryStack.clear()
            _currentSubScreenEntry.value = null
            _currentSubScreen.value = SubScreen.NONE
            scrollStateStore.clearAllSubScreens()
            return
        }
        val current = _currentSubScreen.value
        val currentEntry = _currentSubScreenEntry.value
        val targetInstanceKey = resolveInstanceKeyForSubScreen(screen)
        val isSameDestination = current == screen && currentEntry?.instanceKey == targetInstanceKey
        val settingsAlreadyInHierarchy = isSettingsInNavigationHierarchy()
        if (screen == SubScreen.SETTINGS && !settingsAlreadyInHierarchy) {
            settingsScrollIndex.value = 0
            settingsScrollOffset.value = 0
        }
        if (current != SubScreen.NONE && !isSameDestination) {
            _subScreenStack.add(current)
            val entryToPush = currentEntry ?: createSubScreenEntry(current)
            _subScreenEntryStack.add(entryToPush)
        }
        if (!isSameDestination || currentEntry == null) {
            _currentSubScreenEntry.value = createSubScreenEntry(screen)
        }
        _currentSubScreen.value = screen
    }

    fun openAccountDetail(accountId: String) {
        selectedAccountId.value = accountId
        openSubScreen(SubScreen.ACCOUNT_DETAIL)
    }

    fun openEditAccount(account: AccountEntity?) {
        accountToEdit.value = account
        openSubScreen(SubScreen.EDIT_ACCOUNT)
    }

    fun openAdjustBalance(account: AccountEntity) {
        accountForAdjustBalance.value = account
        openSubScreen(SubScreen.ADJUST_BALANCE)
    }

    fun openAddAccount() {
        accountToEdit.value = null
        openSubScreen(SubScreen.EDIT_ACCOUNT)
    }

    fun openAddTransactionForAccount(accId: String) {
        clearTransactionDraft()
        transactionToEdit.value = null
        newTransactionInitialType.value = TransactionType.EXPENSE
        preselectedAccountIdForNewTx.value = accId
        openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
    }

    fun openTransferFromAccount(accId: String? = null) {
        clearTransactionDraft()
        transactionToEdit.value = null
        newTransactionInitialType.value = TransactionType.TRANSFER
        preselectedAccountIdForNewTx.value = accId
        openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
    }

    val budgetToEdit = MutableStateFlow<BudgetEntity?>(null)
    val addBudgetDefaultIsOverall = MutableStateFlow(false)
    val addBudgetDefaultCategory = MutableStateFlow<String?>(null)
    val addBudgetDefaultMonthKey = MutableStateFlow<String?>(null)

    fun openBudgetDashboard() {
        openSubScreen(SubScreen.BUDGET_DASHBOARD)
    }

    fun openAddBudget(
        budget: BudgetEntity? = null,
        defaultIsOverall: Boolean = false,
        defaultCategory: String? = null,
        defaultMonthKey: String? = null
    ) {
        budgetToEdit.value = budget
        addBudgetDefaultIsOverall.value = defaultIsOverall
        addBudgetDefaultCategory.value = defaultCategory
        addBudgetDefaultMonthKey.value = defaultMonthKey
        openSubScreen(SubScreen.ADD_BUDGET)
    }

    fun openManageDebts(initialType: DebtType = DebtType.BORROWED) {
        debtFilterType.value = initialType
        openSubScreen(SubScreen.MANAGE_DEBTS)
    }

    fun openDebtDetails(debt: DebtEntity) {
        selectedDebt.value = debt
        openSubScreen(SubScreen.DEBT_DETAILS)
    }

    fun openAddDebt(type: DebtType = DebtType.BORROWED, debtToEditItem: DebtEntity? = null) {
        debtFilterType.value = type
        debtToEdit.value = debtToEditItem
        openSubScreen(SubScreen.ADD_DEBT)
    }

    fun openNewSavingsGoal() {
        openSubScreen(SubScreen.NEW_SAVINGS_GOAL)
    }

    fun openCurrencyConverter() {
        openSubScreen(SubScreen.CURRENCY_CONVERTER)
        ensureCurrencyRatesFresh()
    }

    fun openMonthlyRecap() {
        openSubScreen(SubScreen.MONTHLY_RECAP)
    }

    fun openSpendingComparison() {
        openSubScreen(SubScreen.SPENDING_COMPARISON)
    }

    fun openIncomeTracker() {
        openSubScreen(SubScreen.INCOME_TRACKER)
    }

    fun openCalendar() {
        openSubScreen(SubScreen.CALENDAR)
    }

    fun openAddDebtTransaction(debt: DebtEntity, type: String = "REPAYMENT") {
        selectedDebtForTransaction.value = debt
        debtTransactionType.value = type
        openSubScreen(SubScreen.ADD_DEBT_TRANSACTION)
    }

    fun saveDebtTransaction(
        debt: DebtEntity,
        type: String,
        amount: Double,
        account: AccountEntity?,
        notes: String = "",
        tags: String = "",
        customTitle: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0.0) {
            return
        }
        val normalizedType = type.trim().uppercase(Locale.ENGLISH).replace(" ", "_")
        when (normalizedType) {
            "INTEREST", "ADD_INTEREST" -> {
                addDebtInterest(
                    debtId = debt.id,
                    interestAmount = amount,
                    customTitle = customTitle,
                    notes = notes,
                    timestamp = timestamp
                )
            }
            "INCREASE", "BORROW_MORE", "LEND_MORE", "BORROWED_MORE", "LENT_MORE" -> {
                borrowOrLendMore(
                    debtId = debt.id,
                    additionalAmount = amount,
                    account = account,
                    notes = notes,
                    customTitle = customTitle,
                    timestamp = timestamp
                )
            }
            else -> {
                recordDebtPayment(
                    debtId = debt.id,
                    paymentAmount = amount,
                    note = notes,
                    account = account,
                    customTitle = customTitle,
                    timestamp = timestamp
                )
            }
        }
        closeSubScreen(SubScreen.ADD_DEBT_TRANSACTION)
    }

    fun toggleThemeModeQuick() {
        val current = settings.value?.themeMode ?: ThemeMode.AUTO
        val next = when (current) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.AUTO -> ThemeMode.DARK
        }
        setThemeMode(next)
    }

    fun closeSubScreen(expectedScreen: SubScreen? = null) {
        val closing = _currentSubScreen.value
        if (expectedScreen != null && closing != expectedScreen) {
            return
        }
        val closingEntry = _currentSubScreenEntry.value
        if (closingEntry != null) {
            scrollStateStore.clearScopePrefix(closingEntry.stateKey)
        }

        if (_subScreenStack.isNotEmpty()) {
            val prevScreen = _subScreenStack.removeAt(_subScreenStack.lastIndex)
            val prevEntry = if (_subScreenEntryStack.isNotEmpty()) {
                _subScreenEntryStack.removeAt(_subScreenEntryStack.lastIndex)
            } else {
                createSubScreenEntry(prevScreen)
            }
            _currentSubScreenEntry.value = prevEntry
            _currentSubScreen.value = prevScreen
        } else {
            _currentSubScreenEntry.value = null
            _currentSubScreen.value = SubScreen.NONE
        }

        val remainingScreens = buildList {
            addAll(_subScreenStack)
            if (_currentSubScreen.value != SubScreen.NONE) {
                add(_currentSubScreen.value)
            }
        }

        when (closing) {
            SubScreen.ADJUST_BALANCE -> {
                if (!remainingScreens.contains(SubScreen.ADJUST_BALANCE)) {
                    accountForAdjustBalance.value = null
                }
            }
            SubScreen.EDIT_ACCOUNT -> {
                if (!remainingScreens.contains(SubScreen.EDIT_ACCOUNT)) {
                    accountToEdit.value = null
                }
            }
            SubScreen.ACCOUNT_DETAIL -> {
                if (!remainingScreens.contains(SubScreen.ACCOUNT_DETAIL)) {
                    selectedAccountId.value = null
                }
            }
            SubScreen.ADD_DEBT -> {
                if (!remainingScreens.contains(SubScreen.ADD_DEBT)) {
                    debtToEdit.value = null
                }
            }
            SubScreen.DEBT_DETAILS -> {
                if (!remainingScreens.contains(SubScreen.DEBT_DETAILS)) {
                    selectedDebt.value = null
                }
            }
            SubScreen.ADD_DEBT_TRANSACTION -> {
                if (!remainingScreens.contains(SubScreen.ADD_DEBT_TRANSACTION)) {
                    selectedDebtForTransaction.value = null
                }
            }
            SubScreen.ADD_BUDGET -> {
                if (!remainingScreens.contains(SubScreen.ADD_BUDGET)) {
                    budgetToEdit.value = null
                    addBudgetDefaultIsOverall.value = false
                    addBudgetDefaultCategory.value = null
                    addBudgetDefaultMonthKey.value = null
                }
            }
            SubScreen.ADD_EDIT_TRANSACTION -> {
                if (!remainingScreens.contains(SubScreen.ADD_EDIT_TRANSACTION)) {
                    transactionToEdit.value = null
                    preselectedAccountIdForNewTx.value = null
                    newTransactionInitialType.value = TransactionType.EXPENSE
                    clearTransactionDraft()
                }
            }
            SubScreen.FILTERED_REPORTS -> {
                if (!remainingScreens.contains(SubScreen.FILTERED_REPORTS)) {
                    filteredReportArgs.value = null
                }
            }
            else -> {}
        }

        if (!isSettingsInNavigationHierarchy()) {
            settingsScrollIndex.value = 0
            settingsScrollOffset.value = 0
        }
    }

    fun handleBackPress(): Boolean {
        if (_currentSubScreen.value != SubScreen.NONE) {
            closeSubScreen()
            return true
        }
        if (_currentTab.value == MainTab.INSIGHTS && insightsSelectedCategory.value != null) {
            insightsSelectedCategory.value = null
            return true
        }
        if (_currentTab.value != MainTab.HOME) {
            setTab(MainTab.HOME)
            return true
        }
        return false
    }

    fun setCurrency(code: String, symbol: String) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettingsEntity()
            repository.saveSettings(current.copy(currencyCode = code, currencySymbol = symbol))
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettingsEntity()
            repository.saveSettings(current.copy(language = language))
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettingsEntity()
            repository.saveSettings(current.copy(themeMode = mode))
        }
    }

    fun setThemeColor(color: ThemeColorOption) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettingsEntity()
            repository.saveSettings(current.copy(themeColor = color))
        }
    }

    fun setBackgroundTexture(texture: BackgroundTextureOption) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettingsEntity()
            repository.saveSettings(current.copy(backgroundTexture = texture))
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        updateExtendedSettings { it.copy(useScreenLock = enabled) }
        viewModelScope.launch {
            val current = settings.value ?: AppSettingsEntity()
            repository.saveSettings(current.copy(appLockEnabled = enabled))
        }
    }

    fun toggleScreenshotProtection(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettingsEntity()
            repository.saveSettings(current.copy(screenshotProtectionEnabled = enabled))
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        updateExtendedSettings { it.withMasterNotificationsToggled(enabled) }
        viewModelScope.launch {
            val current = settings.value ?: AppSettingsEntity()
            repository.saveSettings(current.copy(notificationsEnabled = enabled))
        }
    }

    fun navigateToNotificationDestination(
        destinationType: NotificationDestinationType,
        targetEntityId: String? = null,
        targetSearchQuery: String? = null,
        targetAmount: Double? = null
    ) {
        when (destinationType) {
            NotificationDestinationType.BUDGET -> openBudgetDashboard()
            NotificationDestinationType.DEBT -> {
                val matchedDebt = targetEntityId?.let { id -> debts.value.firstOrNull { it.id == id } }
                if (matchedDebt != null) {
                    openDebtDetails(matchedDebt)
                } else {
                    openManageDebts()
                }
            }
            NotificationDestinationType.WALLET, NotificationDestinationType.BILL -> {
                if (!targetEntityId.isNullOrBlank()) {
                    openAccountDetail(targetEntityId)
                } else {
                    openSubScreen(SubScreen.ACCOUNTS_MANAGEMENT)
                }
            }
            NotificationDestinationType.TRANSACTION -> {
                navigateToActivityWithFilter(query = targetSearchQuery)
            }
            NotificationDestinationType.INSIGHTS -> {
                setTab(MainTab.INSIGHTS)
            }
            NotificationDestinationType.GENERAL -> {
                clearTransactionDraft()
                transactionToEdit.value = null
                openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
            }
        }
    }

    private fun extractDebtIdFromNotes(notes: String?): String? {
        if (notes.isNullOrBlank()) return null
        val match = Regex("\\[Debt:([^\\]]+)\\]").find(notes)
        return match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    fun saveTransaction(
        id: String? = null,
        type: TransactionType,
        amount: Double,
        title: String,
        category: String,
        subcategory: String? = null,
        accountId: String,
        toAccountId: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        notes: String? = null,
        debtId: String? = null,
        excludeFromBudgets: Boolean = false
    ) {
        viewModelScope.launch {
            val existing = if (id != null) {
                transactions.value.firstOrNull { it.id == id } ?: repository.getTransactionById(id)
            } else {
                null
            }
            val resolvedDebtId = existing?.debtId?.takeIf { it.isNotBlank() }
                ?: debtId?.takeIf { it.isNotBlank() }
                ?: extractDebtIdFromNotes(existing?.notes)
                ?: extractDebtIdFromNotes(notes)

            val isSynthesizedInitialDebtTx = existing == null &&
                !resolvedDebtId.isNullOrBlank() &&
                id?.startsWith("init_") == true

            val resolvedId = when {
                isSynthesizedInitialDebtTx -> "tx_debt_init_${resolvedDebtId}"
                id != null -> id
                else -> "tx_${UUID.randomUUID().toString().take(8)}"
            }

            val resolvedSubcategory = when {
                isSynthesizedInitialDebtTx -> "Initial Debt"
                else -> (subcategory ?: "").trim()
            }

            val parentDebt = resolvedDebtId?.let { dId ->
                repository.getDebtById(dId) ?: debts.value.firstOrNull { it.id == dId }
            }

            val tx = TransactionEntity(
                id = resolvedId,
                type = type,
                amount = amount,
                title = title.trim(),
                category = category.trim(),
                subcategory = resolvedSubcategory,
                accountId = accountId,
                toAccountId = toAccountId,
                timestamp = timestamp,
                notes = notes ?: "",
                payeeOrPayer = existing?.payeeOrPayer?.ifBlank { parentDebt?.personName ?: "" }
                    ?: parentDebt?.personName
                    ?: "",
                debtId = resolvedDebtId,
                excludeFromBudgets = excludeFromBudgets
            )

            if (existing != null) {
                repository.updateTransaction(existing, tx)
                if (!resolvedDebtId.isNullOrBlank()) {
                    syncDebtAfterTransactionUpdated(resolvedDebtId, existing, tx)
                }
            } else if (isSynthesizedInitialDebtTx && resolvedDebtId != null) {
                repository.insertTransaction(tx)
                performRecalculateDebt(resolvedDebtId)
            } else {
                repository.insertTransaction(tx)
                if (!resolvedDebtId.isNullOrBlank()) {
                    performRecalculateDebt(resolvedDebtId)
                }
            }
            clearTransactionDraft()
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            val existing = transactions.value.firstOrNull { it.id == id }
                ?: repository.getTransactionById(id)
            repository.deleteTransaction(id)
            val linkedDebtId = existing?.debtId
            if (!linkedDebtId.isNullOrBlank()) {
                syncDebtAfterTransactionDeleted(linkedDebtId, existing)
            }
        }
    }

    fun duplicateTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            val copyTx = tx.copy(
                id = "tx_${UUID.randomUUID().toString().take(8)}",
                timestamp = System.currentTimeMillis()
            )
            repository.insertTransaction(copyTx)
        }
    }

    fun saveAccount(
        id: String? = null,
        name: String,
        type: AccountType,
        institution: String = "",
        balance: Double = 0.0,
        accountNumberLast4: String = "",
        colorHex: String = "#1E88E5",
        iconName: String = "AccountBalanceWallet",
        creditLimit: Double = 0.0,
        billingDate: String = "",
        paymentDueDate: String = "",
        minPayment: Double = 0.0,
        notes: String = "",
        includeInTotal: Boolean = true,
        isHidden: Boolean = false
    ): AccountEntity {
        val accountId = id ?: "acc_${UUID.randomUUID().toString().take(8)}"
        val existing = (accounts.value + _optimisticAccounts.value).firstOrNull { it.id == accountId }
        val nextSortOrder = existing?.sortOrder ?: ((accounts.value.maxOfOrNull { it.sortOrder } ?: -1) + 1)
        val entity = AccountEntity(
            id = accountId,
            name = name,
            type = type,
            institution = institution,
            balance = balance,
            openingBalance = existing?.openingBalance ?: balance,
            accountNumberLast4 = accountNumberLast4,
            colorHex = colorHex,
            iconName = iconName,
            isHidden = isHidden,
            sortOrder = nextSortOrder,
            creditLimit = creditLimit,
            billingDate = billingDate,
            paymentDueDate = paymentDueDate,
            minPayment = minPayment,
            notes = notes,
            includeInTotal = includeInTotal,
            updatedAt = System.currentTimeMillis()
        )
        _optimisticAccounts.value = _optimisticAccounts.value.filter { it.id != accountId } + entity
        viewModelScope.launch {
            if (id != null && existing != null) {
                repository.updateAccount(entity)
            } else {
                repository.insertAccount(entity)
            }
        }
        return entity
    }

    fun recordCreditCardPayment(
        fromAccountId: String,
        creditCardId: String,
        amount: Double,
        note: String = "Credit Card Bill Payment"
    ) {
        saveTransaction(
            type = TransactionType.CC_PAYMENT,
            amount = amount,
            title = note,
            category = "Transfer",
            accountId = fromAccountId,
            toAccountId = creditCardId,
            notes = note
        )
    }

    fun deleteAccount(id: String) {
        _optimisticAccounts.value = _optimisticAccounts.value.filter { it.id != id }
        viewModelScope.launch {
            repository.deleteAccount(id)
        }
    }

    fun adjustAccountBalance(id: String, newBalance: Double, recordAsTransaction: Boolean = true) {
        viewModelScope.launch {
            repository.adjustAccountBalance(id, newBalance, recordAsTransaction)
        }
    }

    fun archiveAccount(id: String) {
        viewModelScope.launch {
            repository.setAccountArchived(id, true)
        }
    }

    fun unarchiveAccount(id: String) {
        viewModelScope.launch {
            repository.setAccountArchived(id, false)
        }
    }

    fun reorderAccounts(fromIndex: Int, toIndex: Int) {
        val currentList = accounts.value.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices) return
        val moved = currentList.removeAt(fromIndex)
        currentList.add(toIndex, moved)
        reorderAccountsList(currentList)
    }

    fun setAccountSortOption(option: AccountSortOption) {
        accountSortOption.value = option
    }

    fun setAccountGroupByType(grouped: Boolean) {
        accountGroupByType.value = grouped
    }

    fun applyOrderAsCustom(orderedList: List<AccountEntity>) {
        accountSortOption.value = AccountSortOption.CUSTOM
        reorderAccountsList(orderedList)
    }

    fun reorderAccountsList(newOrder: List<AccountEntity>) {
        viewModelScope.launch {
            repository.reorderAccounts(newOrder)
        }
    }

    fun processVoiceInput(spokenText: String) {
        voiceParsedResult.value = VoiceTransactionParser.parse(
            spokenText = spokenText,
            accounts = accounts.value,
            categories = categories.value
        )
    }

    fun confirmVoiceTransaction(parsed: ParsedVoiceTransaction) {
        val defaultAccId = accounts.value.firstOrNull()?.id ?: "acc_cash"
        saveTransaction(
            type = parsed.type,
            amount = parsed.amount,
            title = parsed.description.ifBlank { parsed.categoryName.ifBlank { "Voice Entry" } },
            category = parsed.categoryName.ifBlank { "Miscellaneous" },
            subcategory = parsed.subcategoryName,
            accountId = parsed.matchedAccountId ?: defaultAccId,
            toAccountId = parsed.matchedToAccountId,
            notes = parsed.rawText
        )
        voiceParsedResult.value = null
        isVoiceDialogVisible.value = false
    }

    fun addCategory(
        name: String,
        nameBn: String? = null,
        isIncome: Boolean = false,
        isTransfer: Boolean = false,
        isActive: Boolean = true,
        iconName: String = "Category",
        colorHex: String = "#5C6BC0",
        initialSubcategories: List<String> = emptyList()
    ): Pair<CategoryEntity, List<SubcategoryEntity>> {
        val catId = "cat_${UUID.randomUUID().toString().take(8)}"
        val maxSort = categories.value.maxOfOrNull { it.sortOrder } ?: 0
        val categoryEntity = CategoryEntity(
            id = catId,
            name = name.trim(),
            nameBn = nameBn ?: "",
            iconName = iconName,
            colorHex = colorHex,
            isIncome = isIncome,
            isTransfer = isTransfer,
            isActive = isActive,
            sortOrder = maxSort + 1
        )
        val subEntities = initialSubcategories.mapNotNull { subName ->
            val trimmed = subName.trim()
            if (trimmed.isEmpty()) null
            else SubcategoryEntity(
                id = "sub_${UUID.randomUUID().toString().take(8)}",
                categoryId = catId,
                name = trimmed
            )
        }
        viewModelScope.launch {
            repository.insertCategory(categoryEntity)
            if (subEntities.isNotEmpty()) {
                repository.insertSubcategories(subEntities)
            }
        }
        return categoryEntity to subEntities
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.insertCategory(category)
        }
    }

    fun reorderCategory(category: CategoryEntity, moveUp: Boolean) {
        val sameType = categories.value.filter { it.isIncome == category.isIncome && it.isTransfer == category.isTransfer }
            .sortedBy { it.sortOrder }
            .toMutableList()
        val idx = sameType.indexOfFirst { it.id == category.id }
        if (idx == -1) return
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx !in sameType.indices) return
        val item = sameType.removeAt(idx)
        sameType.add(targetIdx, item)
        updateCategoriesOrder(sameType)
    }

    fun updateCategoriesOrder(reorderedList: List<CategoryEntity>) {
        viewModelScope.launch {
            val updated = reorderedList.mapIndexed { index, cat -> cat.copy(sortOrder = index) }
            repository.insertCategories(updated)
        }
    }

    fun restoreDefaultCategories() {
        viewModelScope.launch {
            repository.restoreDefaultCategories()
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    fun addSubcategory(categoryId: String, name: String, nameBn: String? = null) {
        viewModelScope.launch {
            val sub = SubcategoryEntity(
                id = "sub_${UUID.randomUUID().toString().take(8)}",
                categoryId = categoryId,
                name = name.trim(),
                nameBn = nameBn ?: ""
            )
            repository.insertSubcategory(sub)
        }
    }

    fun deleteSubcategory(id: String) {
        viewModelScope.launch {
            repository.deleteSubcategory(id)
        }
    }

    fun saveBudget(
        id: String? = null,
        categoryName: String,
        limit: Double,
        monthYear: String = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date()),
        isOverall: Boolean = false,
        rolloverEnabled: Boolean = false,
        name: String = categoryName,
        subcategoryName: String? = null,
        iconName: String = "Category",
        colorHex: String = "#0EA5E9",
        alertPercentage: Int = 80
    ) {
        viewModelScope.launch {
            val entity = BudgetEntity(
                id = id ?: "b_${UUID.randomUUID().toString().take(8)}",
                name = name,
                categoryName = categoryName,
                subcategoryName = subcategoryName ?: "",
                iconName = iconName,
                colorHex = colorHex,
                monthlyLimit = limit,
                monthYear = monthYear,
                isOverall = isOverall,
                rolloverEnabled = rolloverEnabled,
                alertPercentage = alertPercentage
            )
            repository.insertBudget(entity)
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    suspend fun performRecalculateDebt(debtId: String): DebtEntity? {
        val debt = repository.getDebtById(debtId) ?: return null
        val txs = repository.getTransactionsForDebt(debtId)
        if (txs.isEmpty()) return debt
        val isPayable = debt.type == DebtType.BORROWED
        val hasInit = txs.any {
            it.subcategory.equals("Initial Debt", ignoreCase = true) ||
                it.id == "tx_debt_init_${debt.id}"
        }
        if (!hasInit) return debt
        val additions = txs.filter { isDebtAdditionTransaction(it, isPayable) }.sumOf { it.amount }
        val payments = txs.filter { !isDebtAdditionTransaction(it, isPayable) }.sumOf { it.amount }
        val newPaid = payments.coerceAtMost(additions)
        val newRemaining = (additions - newPaid).coerceAtLeast(0.0)
        val newSettled = additions > 0.0 && newRemaining <= 0.001
        val reconciled = debt.copy(
            totalAmount = additions,
            paidAmount = newPaid,
            amount = newRemaining,
            isSettled = newSettled
        )
        if (reconciled != debt) {
            repository.updateDebt(reconciled)
            if (selectedDebt.value?.id == debtId) {
                selectedDebt.value = reconciled
            }
        }
        return reconciled
    }

    fun recalculateDebt(debtId: String) {
        viewModelScope.launch {
            performRecalculateDebt(debtId)
        }
    }

    fun recalculateAllDebts() {}

    fun insertDebtWithInitialTransaction(debt: DebtEntity) {
        insertDebt(debt)
    }

    fun insertDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.insertDebt(debt)
        }
    }

    fun updateDebtAndInitialTransaction(debt: DebtEntity) {
        updateDebt(debt)
    }

    fun updateDebt(debt: DebtEntity) {
        viewModelScope.launch {
            val normalizedTotal = if (debt.totalAmount > 0.0) debt.totalAmount else (debt.amount + debt.paidAmount)
            val newRemaining = (normalizedTotal - debt.paidAmount).coerceAtLeast(0.0)
            val normalizedDebt = debt.copy(
                totalAmount = normalizedTotal,
                amount = newRemaining,
                isSettled = normalizedTotal > 0.0 && newRemaining <= 0.001
            )
            repository.updateDebt(normalizedDebt)
            if (selectedDebt.value?.id == normalizedDebt.id) {
                selectedDebt.value = normalizedDebt
            }
        }
    }

    fun deleteDebt(id: String) {
        viewModelScope.launch {
            repository.deleteDebt(id)
        }
    }

    fun markDebtAsPaid(debtId: String) {
        viewModelScope.launch {
            val existing = repository.getDebtById(debtId)
                ?: debts.value.firstOrNull { it.id == debtId }
                ?: return@launch
            val baseTotal = if (existing.totalAmount > 0.0) existing.totalAmount else (existing.amount + existing.paidAmount)
            val updated = existing.copy(
                amount = 0.0,
                totalAmount = baseTotal,
                paidAmount = baseTotal,
                isSettled = true
            )
            repository.updateDebt(updated)
            if (selectedDebt.value?.id == debtId) {
                selectedDebt.value = updated
            }
        }
    }

    fun recordDebtPayment(
        debtId: String,
        paymentAmount: Double,
        note: String = "",
        account: AccountEntity? = null,
        customTitle: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (paymentAmount <= 0.0) return
        viewModelScope.launch {
            val existing = repository.getDebtById(debtId)
                ?: debts.value.firstOrNull { it.id == debtId }
                ?: return@launch
            val baseTotal = if (existing.totalAmount > 0.0) existing.totalAmount else (existing.amount + existing.paidAmount)
            val newPaid = (existing.paidAmount + paymentAmount).coerceAtMost(baseTotal)
            val newRemaining = (baseTotal - newPaid).coerceAtLeast(0.0)
            val isSettled = baseTotal > 0.0 && newRemaining <= 0.001
            val updated = existing.copy(
                amount = newRemaining,
                totalAmount = baseTotal,
                paidAmount = newPaid,
                isSettled = isSettled
            )
            repository.updateDebt(updated)
            if (selectedDebt.value?.id == debtId) {
                selectedDebt.value = updated
            }

            val isPayable = existing.type == DebtType.BORROWED
            val tx = TransactionEntity(
                id = "tx_repay_${existing.id}_${UUID.randomUUID().toString().take(8)}",
                type = if (isPayable) TransactionType.EXPENSE else TransactionType.INCOME,
                amount = paymentAmount,
                title = customTitle.trim().ifBlank { if (isPayable) "Debt Repayment" else "Payment Received" },
                category = if (isPayable) "Debt Repayment" else "Debt Collection",
                subcategory = if (isSettled) "Final Settlement" else "Installment",
                accountId = account?.id ?: "",
                timestamp = timestamp,
                notes = buildString {
                    if (note.isNotBlank()) append(note.trim()).append(" ")
                    append("[Debt:${existing.id}]")
                }.trim(),
                payeeOrPayer = existing.personName,
                debtId = existing.id
            )
            repository.insertTransaction(tx)
        }
    }

    fun addDebtInterest(
        debtId: String,
        interestAmount: Double,
        customTitle: String = "",
        notes: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (interestAmount <= 0.0) return
        viewModelScope.launch {
            val existing = repository.getDebtById(debtId)
                ?: debts.value.firstOrNull { it.id == debtId }
                ?: return@launch
            val baseTotal = if (existing.totalAmount > 0.0) existing.totalAmount else (existing.amount + existing.paidAmount)
            val newTotal = baseTotal + interestAmount
            val newRemaining = (newTotal - existing.paidAmount).coerceAtLeast(0.0)
            val updated = existing.copy(
                amount = newRemaining,
                totalAmount = newTotal,
                isSettled = newTotal > 0.0 && newRemaining <= 0.001
            )
            repository.updateDebt(updated)
            if (selectedDebt.value?.id == debtId) {
                selectedDebt.value = updated
            }

            val isPayable = existing.type == DebtType.BORROWED
            val tx = TransactionEntity(
                id = "tx_debt_${existing.id}_${UUID.randomUUID().toString().take(8)}",
                type = if (isPayable) TransactionType.BORROW else TransactionType.LEND,
                amount = interestAmount,
                title = customTitle.trim().ifBlank { "Interest" },
                category = "Interest",
                subcategory = "Interest",
                accountId = "",
                timestamp = timestamp,
                notes = buildString {
                    if (notes.isNotBlank()) append(notes.trim()).append(" ")
                    append("[Debt:${existing.id}]")
                }.trim(),
                payeeOrPayer = existing.personName,
                debtId = existing.id
            )
            repository.insertTransaction(tx)
        }
    }

    fun borrowOrLendMore(
        debtId: String,
        additionalAmount: Double,
        account: AccountEntity? = null,
        notes: String = "",
        customTitle: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (additionalAmount <= 0.0) return
        viewModelScope.launch {
            val existing = repository.getDebtById(debtId)
                ?: debts.value.firstOrNull { it.id == debtId }
                ?: return@launch
            val baseTotal = if (existing.totalAmount > 0.0) existing.totalAmount else (existing.amount + existing.paidAmount)
            val newTotal = baseTotal + additionalAmount
            val newRemaining = (newTotal - existing.paidAmount).coerceAtLeast(0.0)
            val updated = existing.copy(
                amount = newRemaining,
                totalAmount = newTotal,
                isSettled = newTotal > 0.0 && newRemaining <= 0.001
            )
            repository.updateDebt(updated)
            if (selectedDebt.value?.id == debtId) {
                selectedDebt.value = updated
            }

            val isPayable = existing.type == DebtType.BORROWED
            val defaultLabel = if (isPayable) "Borrowed More" else "Lent More"
            val tx = TransactionEntity(
                id = "tx_debt_${existing.id}_${UUID.randomUUID().toString().take(8)}",
                type = if (isPayable) TransactionType.BORROW else TransactionType.LEND,
                amount = additionalAmount,
                title = customTitle.trim().ifBlank { defaultLabel },
                category = if (isPayable) "Debt / Borrow" else "Debt / Lend",
                subcategory = if (isPayable) "Borrow More" else "Lend More",
                accountId = account?.id ?: "",
                timestamp = timestamp,
                notes = buildString {
                    if (notes.isNotBlank()) append(notes.trim()).append(" ")
                    append("[Debt:${existing.id}]")
                }.trim(),
                payeeOrPayer = existing.personName,
                debtId = existing.id
            )
            repository.insertTransaction(tx)
        }
    }

    private suspend fun syncDebtAfterTransactionDeleted(debtId: String, deletedTx: TransactionEntity) {
        val existing = repository.getDebtById(debtId)
            ?: debts.value.firstOrNull { it.id == debtId }
            ?: return
        val isPayable = existing.type == DebtType.BORROWED
        val isAddition = isDebtAdditionTransaction(deletedTx, isPayable)
        val baseTotal = if (existing.totalAmount > 0.0) existing.totalAmount else (existing.amount + existing.paidAmount)
        val updated = if (isAddition) {
            val newTotal = (baseTotal - deletedTx.amount).coerceAtLeast(0.0)
            val newPaid = existing.paidAmount.coerceAtMost(newTotal)
            val newRemaining = (newTotal - newPaid).coerceAtLeast(0.0)
            existing.copy(
                totalAmount = newTotal,
                paidAmount = newPaid,
                amount = newRemaining,
                isSettled = newTotal > 0.0 && newRemaining <= 0.001
            )
        } else {
            val newPaid = (existing.paidAmount - deletedTx.amount).coerceAtLeast(0.0)
            val newRemaining = (baseTotal - newPaid).coerceAtLeast(0.0)
            existing.copy(
                totalAmount = baseTotal,
                paidAmount = newPaid,
                amount = newRemaining,
                isSettled = baseTotal > 0.0 && newRemaining <= 0.001
            )
        }
        repository.updateDebt(updated)
        if (selectedDebt.value?.id == debtId) {
            selectedDebt.value = updated
        }
    }

    private suspend fun syncDebtAfterTransactionUpdated(
        debtId: String,
        oldTx: TransactionEntity,
        newTx: TransactionEntity
    ) {
        val existing = repository.getDebtById(debtId)
            ?: debts.value.firstOrNull { it.id == debtId }
            ?: return
        val isPayable = existing.type == DebtType.BORROWED
        val oldIsAddition = isDebtAdditionTransaction(oldTx, isPayable)
        val newIsAddition = isDebtAdditionTransaction(newTx, isPayable)
        val delta = newTx.amount - oldTx.amount
        if (delta == 0.0 && oldIsAddition == newIsAddition) return

        val baseTotal = if (existing.totalAmount > 0.0) existing.totalAmount else (existing.amount + existing.paidAmount)
        val updated = when {
            oldIsAddition && newIsAddition -> {
                val newTotal = (baseTotal + delta).coerceAtLeast(0.0)
                val newPaid = existing.paidAmount.coerceAtMost(newTotal)
                val newRemaining = (newTotal - newPaid).coerceAtLeast(0.0)
                existing.copy(
                    totalAmount = newTotal,
                    paidAmount = newPaid,
                    amount = newRemaining,
                    isSettled = newTotal > 0.0 && newRemaining <= 0.001
                )
            }
            !oldIsAddition && !newIsAddition -> {
                val newPaid = (existing.paidAmount + delta).coerceIn(0.0, baseTotal)
                val newRemaining = (baseTotal - newPaid).coerceAtLeast(0.0)
                existing.copy(
                    totalAmount = baseTotal,
                    paidAmount = newPaid,
                    amount = newRemaining,
                    isSettled = baseTotal > 0.0 && newRemaining <= 0.001
                )
            }
            oldIsAddition && !newIsAddition -> {
                val newTotal = (baseTotal - oldTx.amount).coerceAtLeast(0.0)
                val newPaid = (existing.paidAmount + newTx.amount).coerceIn(0.0, newTotal)
                val newRemaining = (newTotal - newPaid).coerceAtLeast(0.0)
                existing.copy(
                    totalAmount = newTotal,
                    paidAmount = newPaid,
                    amount = newRemaining,
                    isSettled = newTotal > 0.0 && newRemaining <= 0.001
                )
            }
            else -> {
                val newTotal = (baseTotal + newTx.amount).coerceAtLeast(0.0)
                val newPaid = (existing.paidAmount - oldTx.amount).coerceIn(0.0, newTotal)
                val newRemaining = (newTotal - newPaid).coerceAtLeast(0.0)
                existing.copy(
                    totalAmount = newTotal,
                    paidAmount = newPaid,
                    amount = newRemaining,
                    isSettled = newTotal > 0.0 && newRemaining <= 0.001
                )
            }
        }
        repository.updateDebt(updated)
        if (selectedDebt.value?.id == debtId) {
            selectedDebt.value = updated
        }
    }

    fun insertSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.insertGoal(goal)
        }
    }

    fun updateSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun deleteSavingsGoal(id: String) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }

    fun addSavingsContribution(goalId: String, amount: Double) {
        viewModelScope.launch {
            val existing = savingsGoals.value.firstOrNull { it.id == goalId } ?: return@launch
            val newSaved = existing.savedAmount + amount
            repository.updateGoal(
                existing.copy(
                    savedAmount = newSaved,
                    isCompleted = newSaved >= existing.targetAmount
                )
            )
        }
    }

    fun prepareBackupPreview(jsonString: String, fileName: String = "backup.json", fileSizeBytes: Long = 0L) {
        viewModelScope.launch {
            val preview = backupManager.previewBackup(jsonString, fileName, fileSizeBytes)
            backupPreviewState.value = preview
            backupJsonStringPending.value = jsonString
            backupFileNamePending.value = fileName
            restoreUiState.value = if (preview.isValid) {
                RestoreUiState.Step1Preview(preview = preview, selectedMode = RestoreMode.REPLACE_ALL)
            } else {
                val diag = preview.diagnosticError ?: BackupDiagnosticError(
                    errorCode = BackupErrorCode.ERR_INVALID_JSON,
                    failedStage = RestoreStage.VALIDATING_STRUCTURE,
                    technicalDetails = preview.errorMessage ?: "Invalid backup file."
                )
                RestoreUiState.Step4Error(
                    error = diag,
                    fileName = fileName
                )
            }
        }
    }

    fun selectRestoreMode(mode: RestoreMode) {
        val current = restoreUiState.value
        if (current is RestoreUiState.Step1Preview) {
            restoreUiState.value = current.copy(selectedMode = mode)
        }
    }

    fun proceedToRestoreConfirmation() {
        val current = restoreUiState.value
        if (current is RestoreUiState.Step1Preview) {
            restoreUiState.value = RestoreUiState.Step2Confirm(
                preview = current.preview,
                mode = current.selectedMode
            )
        }
    }

    fun backToRestorePreview() {
        val current = restoreUiState.value
        if (current is RestoreUiState.Step2Confirm) {
            restoreUiState.value = RestoreUiState.Step1Preview(
                preview = current.preview,
                selectedMode = current.mode
            )
        }
    }

    fun executeRestore(replaceAll: Boolean) {
        executeRestoreWithMode(if (replaceAll) RestoreMode.REPLACE_ALL else RestoreMode.MERGE)
    }

    fun executeRestoreWithMode(mode: RestoreMode) {
        val json = backupJsonStringPending.value ?: return
        val fileName = backupFileNamePending.value ?: "backup.json"
        viewModelScope.launch {
            restoreUiState.value = RestoreUiState.Step3Restoring(
                progress = RestoreProgress(
                    stage = RestoreStage.READING_FILE,
                    percentage = 5,
                    isIndeterminate = false,
                    statusDetail = "Starting restore..."
                ),
                mode = mode
            )
            val res = backupManager.restoreWithDetailedSummary(
                jsonString = json,
                replaceAll = mode == RestoreMode.REPLACE_ALL
            ) { progress ->
                restoreUiState.value = RestoreUiState.Step3Restoring(
                    progress = progress,
                    mode = mode
                )
            }
            if (res.isSuccess) {
                refreshAllApplicationStates()
                restoreUiState.value = RestoreUiState.Step4Success(res.getOrThrow())
            } else {
                val ex = res.exceptionOrNull()
                val diag = (ex as? BackupRestoreException)?.diagnostic ?: BackupDiagnosticError(
                    errorCode = BackupErrorCode.ERR_UNEXPECTED,
                    failedStage = RestoreStage.SAVING_TO_DATABASE,
                    technicalDetails = ex?.localizedMessage ?: "Restore failed."
                )
                restoreUiState.value = RestoreUiState.Step4Error(
                    error = diag,
                    fileName = fileName
                )
            }
        }
    }

    fun reportBackupReadError(fileName: String, details: String) {
        val diag = BackupDiagnosticError(
            errorCode = BackupErrorCode.ERR_STORAGE_READ,
            failedStage = RestoreStage.READING_FILE,
            technicalDetails = details
        )
        restoreUiState.value = RestoreUiState.Step4Error(
            error = diag,
            fileName = fileName
        )
    }

    fun completeRestoreAndGoToDashboard() {
        cancelRestore()
        setTab(MainTab.HOME)
    }

    fun cancelRestore() {
        restoreUiState.value = RestoreUiState.Idle
        backupPreviewState.value = null
        backupJsonStringPending.value = null
        backupFileNamePending.value = null
    }

    fun refreshAllApplicationStates() {
        _optimisticAccounts.value = emptyList()
        dashboardRefreshTrigger.value = System.currentTimeMillis()
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            repository.resetToSampleData()
            refreshAllApplicationStates()
        }
    }

    fun eraseAllData(preserveCloud: Boolean = true) {
        viewModelScope.launch {
            repository.eraseAllData()
            if (!preserveCloud) {
                firestoreRepository.deleteCloudAllData()
            }
            refreshAllApplicationStates()
        }
    }

    fun getSampleJsonTemplate(): String {
        return SampleDataGenerator.generateSampleJsonTemplate()
    }

    fun openBackupRestoreScreen() {
        openSubScreen(SubScreen.BACKUP_RESTORE)
    }

    fun clearNotificationMessage() {
        userNotificationMessage.value = null
    }

    fun calculateNetWorth(
        accountsList: List<AccountEntity> = accounts.value,
        debtsList: List<DebtEntity> = debts.value
    ): NetWorthSummary {
        val activeAccounts = accountsList.filter { !it.isHidden && it.includeInTotal }
        val assets = activeAccounts.filter { it.type != AccountType.CREDIT_CARD && it.balance > 0.0 }
            .sumOf { it.balance } +
            debtsList.filter { it.type == DebtType.LENT && !it.isSettled && it.includeInBalance }
                .sumOf { calculateDebtRemaining(it) }

        val liabilities = activeAccounts.filter { it.type == AccountType.CREDIT_CARD || it.balance < 0.0 }
            .sumOf { kotlin.math.abs(it.balance) } +
            debtsList.filter { it.type == DebtType.BORROWED && !it.isSettled && it.includeInBalance }
                .sumOf { calculateDebtRemaining(it) }

        return NetWorthSummary(
            netWorth = assets - liabilities,
            totalAssets = assets,
            totalLiabilities = liabilities
        )
    }

    fun getCategorySpending(
        monthYearPrefix: String = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())
    ): Map<String, Double> {
        val fmt = SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
        return transactions.value
            .filter { it.type == TransactionType.EXPENSE && fmt.format(Date(it.timestamp)) == monthYearPrefix }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    private fun checkForUpdatesOnLaunch() {
        val appContext = getApplication<Application>()
        if (!UserSettingsPreferences.shouldRunAutoUpdateCheck(appContext)) return

        viewModelScope.launch {
            val result = updateManager.checkForUpdate(appContext)
            if (result is UpdateState.UpToDate || result is UpdateState.Available) {
                val now = System.currentTimeMillis()
                UserSettingsPreferences.setLastUpdateCheckTimestamp(appContext, now)
                lastUpdateCheckTimestamp.value = now
            }
            if (result is UpdateState.Available) {
                val skipped = UserSettingsPreferences.getSkippedUpdateVersion(appContext)
                val normalizedSkipped = skipped?.let { GitHubUpdateManager.normalizeVersionTag(it) }
                if (normalizedSkipped != result.releaseInfo.versionName &&
                    skipped != result.releaseInfo.tagName
                ) {
                    updatePermissionNotice.value = null
                    isUpdateDialogVisible.value = true
                }
            }
        }
    }

    fun checkForUpdatesManually() {
        val appContext = getApplication<Application>()
        if (updateState.value is UpdateState.Checking || updateState.value is UpdateState.Downloading) {
            isUpdateDialogVisible.value = true
            return
        }
        val isBangla = settings.value?.language == AppLanguage.BANGLA
        updatePermissionNotice.value = null

        viewModelScope.launch {
            val result = updateManager.checkForUpdate(appContext)
            if (result is UpdateState.UpToDate || result is UpdateState.Available) {
                val now = System.currentTimeMillis()
                UserSettingsPreferences.setLastUpdateCheckTimestamp(appContext, now)
                lastUpdateCheckTimestamp.value = now
            }
            when (result) {
                is UpdateState.Available -> {
                    isUpdateDialogVisible.value = true
                }
                is UpdateState.UpToDate -> {
                    isUpdateDialogVisible.value = false
                    userNotificationMessage.value = if (isBangla) {
                        "আপটুডেট • v${BuildConfig.VERSION_NAME}"
                    } else {
                        "Up to date • v${BuildConfig.VERSION_NAME}"
                    }
                }
                is UpdateState.Error -> {
                    isUpdateDialogVisible.value = false
                    updateManager.resetToIdle()
                    userNotificationMessage.value = if (isBangla) {
                        "আপডেট যাচাই করা যায়নি"
                    } else {
                        "Couldn't check for updates"
                    }
                }
                else -> Unit
            }
        }
    }

    fun startUpdateDownload(releaseInfo: GitHubReleaseInfo) {
        val appContext = getApplication<Application>()
        updatePermissionNotice.value = null
        updateDownloadJob?.cancel()
        isUpdateDialogVisible.value = true
        updateDownloadJob = viewModelScope.launch {
            updateManager.downloadApk(appContext, releaseInfo)
        }
    }

    fun cancelUpdateDownload(fallbackRelease: GitHubReleaseInfo? = null) {
        updateManager.cancelDownload(fallbackRelease)
        updateDownloadJob?.cancel()
        updateDownloadJob = null
    }

    fun installUpdate(apkFile: File, activityContext: Context? = null) {
        val targetContext = activityContext ?: getApplication<Application>()
        val isBangla = settings.value?.language == AppLanguage.BANGLA
        when (updateManager.installApk(targetContext, apkFile)) {
            InstallApkResult.NEEDS_UNKNOWN_SOURCES_PERMISSION -> {
                updatePermissionNotice.value = if (isBangla) {
                    "ইনস্টল করতে সেটিংস থেকে \"অজানা অ্যাপ ইনস্টল\" অনুমতি দিন এবং ফিরে এসে আবার \"এখনই ইনস্টল\" ট্যাপ করুন।"
                } else {
                    "Please allow \"Install unknown apps\" for Hishab Pati in Settings, then return and tap \"Install Now\"."
                }
            }
            InstallApkResult.LAUNCHED -> {
                updatePermissionNotice.value = null
            }
            else -> Unit
        }
    }

    fun clearUpdatePermissionNoticeIfGranted(context: Context) {
        if (updatePermissionNotice.value != null &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            context.packageManager.canRequestPackageInstalls()
        ) {
            updatePermissionNotice.value = null
        }
    }

    fun skipUpdateVersion(version: String) {
        val appContext = getApplication<Application>()
        UserSettingsPreferences.setSkippedUpdateVersion(appContext, version)
        updatePermissionNotice.value = null
        isUpdateDialogVisible.value = false
        updateManager.resetToIdle()
    }

    fun dismissUpdateDialog() {
        updatePermissionNotice.value = null
        isUpdateDialogVisible.value = false
        if (updateState.value is UpdateState.Error) {
            updateManager.resetToIdle()
        }
    }

    // -------------------------------------------------------------------------
    // Currency Converter (Live open.er-api.com + Offline Cache + Custom Rates)
    // -------------------------------------------------------------------------
    private val currencyRateRepository by lazy {
        CurrencyRateRepository(getApplication())
    }

    private val _currencyConverterState by lazy {
        MutableStateFlow(
            CurrencyConverterUiState(
                fromCode = "BDT",
                toCode = "USD",
                amountInput = "1000",
                ratesPerUsd = currencyRateRepository.loadCachedRatesPerUsd(),
                customOverrides = currencyRateRepository.loadCustomOverrides(),
                lastUpdatedMillis = currencyRateRepository.getLastUpdatedMillis(),
                isRefreshing = false,
                isOfflineFallback = false
            )
        )
    }

    val currencyConverterState: StateFlow<CurrencyConverterUiState>
        get() = _currencyConverterState.asStateFlow()

    private var currencyRefreshJob: Job? = null

    fun ensureCurrencyRatesFresh() {
        if (currencyRateRepository.shouldAutoRefresh() && !_currencyConverterState.value.isRefreshing) {
            refreshCurrencyRates(showToast = false)
        }
    }

    fun refreshCurrencyRates(showToast: Boolean = true) {
        if (currencyRefreshJob?.isActive == true) return
        currencyRefreshJob = viewModelScope.launch {
            _currencyConverterState.update { it.copy(isRefreshing = true) }
            val result = currencyRateRepository.fetchLiveRates()
            _currencyConverterState.update { current ->
                current.copy(
                    ratesPerUsd = result.ratesPerUsd,
                    lastUpdatedMillis = result.lastUpdatedMillis,
                    isRefreshing = false,
                    isOfflineFallback = !result.isLiveSuccess
                )
            }
            if (showToast) {
                val lang = settings.value?.language ?: AppLanguage.ENGLISH
                userNotificationMessage.value = if (result.isLiveSuccess) {
                    AppStrings.t("exchange_rates_updated", lang)
                } else {
                    AppStrings.t("offline_cached_rates", lang)
                }
            }
        }
    }

    fun setCurrencyAmount(rawInput: String) {
        val english = CurrencyFormatter.convertToEnglishDigits(rawInput)
        val filtered = buildString {
            var seenDot = false
            for (ch in english) {
                when {
                    ch in '0'..'9' -> append(ch)
                    ch == '.' && !seenDot -> {
                        seenDot = true
                        append(ch)
                    }
                }
            }
        }.take(14)
        _currencyConverterState.update { it.copy(amountInput = filtered) }
    }

    fun setFromCurrency(code: String) {
        _currencyConverterState.update { current ->
            if (code == current.toCode) {
                current.copy(fromCode = code, toCode = current.fromCode)
            } else {
                current.copy(fromCode = code)
            }
        }
    }

    fun setToCurrency(code: String) {
        _currencyConverterState.update { current ->
            if (code == current.fromCode) {
                current.copy(toCode = code, fromCode = current.toCode)
            } else {
                current.copy(toCode = code)
            }
        }
    }

    fun swapCurrencies() {
        _currencyConverterState.update { current ->
            current.copy(fromCode = current.toCode, toCode = current.fromCode)
        }
    }

    fun saveCustomExchangeRate(fromCode: String, toCode: String, rate: Double) {
        if (fromCode == toCode || rate <= 0.0) return
        val updated = currencyRateRepository.saveCustomOverride(fromCode, toCode, rate)
        _currencyConverterState.update { it.copy(customOverrides = updated) }
        val lang = settings.value?.language ?: AppLanguage.ENGLISH
        userNotificationMessage.value = AppStrings.t("custom_rate_saved", lang)
    }

    fun resetCustomExchangeRate(fromCode: String, toCode: String) {
        val updated = currencyRateRepository.clearCustomOverride(fromCode, toCode)
        _currencyConverterState.update { it.copy(customOverrides = updated) }
        val lang = settings.value?.language ?: AppLanguage.ENGLISH
        userNotificationMessage.value = AppStrings.t("reverted_to_live_rate", lang)
    }
}
