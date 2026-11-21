package com.example

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.FinancialIntegrityValidator
import com.example.data.backup.SampleDataGenerator
import com.example.data.local.ExtendedSettingsState
import com.example.data.local.UserSettingsPreferences
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionType
import com.example.ui.DEFAULT_HOME_SECTIONS
import com.example.ui.HomeCardStyle
import com.example.ui.HomeCustomization
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.theme.MyWalletTheme
import com.example.ui.util.CurrencyFormatter
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Unit & Compose UI tests for Home Screen Summary Cards and all 10 dashboard sections:
 * 1. Total Balance calculation honoring `includeInTotal` and hidden status.
 * 2. Spent This Month calculation using FinancialIntegrityValidator.
 * 3. Adding/updating transactions reactively updates monthly spending.
 * 4. Internal transfers and CC payments excluded from monthly spending.
 * 5. Consistent currency formatting across both cards.
 * 6. All 10 Customise Home sections render and scroll into view with sample data & empty states.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class HomeSummaryCardsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun calculateNetWorth(
        accountsList: List<AccountEntity>,
        debtsList: List<DebtEntity> = emptyList()
    ): Pair<Double, Double> {
        var assets = 0.0
        var liabilities = 0.0

        for (acc in accountsList) {
            if (acc.isHidden || !acc.includeInTotal) continue

            if (acc.type == AccountType.CREDIT_CARD) {
                if (acc.balance < 0) {
                    liabilities += kotlin.math.abs(acc.balance)
                } else {
                    assets += acc.balance
                }
            } else {
                if (acc.balance >= 0) {
                    assets += acc.balance
                } else {
                    liabilities += kotlin.math.abs(acc.balance)
                }
            }
        }

        return assets to liabilities
    }

    @Test
    fun testTotalBalanceExcludesDisabledAccounts() {
        val acc1 = AccountEntity(
            id = "acc_1",
            name = "Main Bank",
            type = AccountType.BANK,
            balance = 50000.0,
            includeInTotal = true,
            isHidden = false
        )
        val acc2 = AccountEntity(
            id = "acc_2",
            name = "Secret Wallet",
            type = AccountType.CASH,
            balance = 20000.0,
            includeInTotal = false, // excluded
            isHidden = false
        )
        val acc3 = AccountEntity(
            id = "acc_3",
            name = "Hidden Bank",
            type = AccountType.BANK,
            balance = 10000.0,
            includeInTotal = true,
            isHidden = true // hidden
        )

        val (totalAssets, _) = calculateNetWorth(listOf(acc1, acc2, acc3))
        assertEquals(50000.0, totalAssets, 0.01)
    }

    @Test
    fun testSpentThisMonthReactiveCalculation() {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        val currentTimestamp = cal.timeInMillis

        // Add 2 expense transactions and 1 internal transfer in current month
        val tx1 = TransactionEntity(
            id = "tx_1",
            accountId = "acc_1",
            title = "Groceries",
            amount = 3500.0,
            type = TransactionType.EXPENSE,
            category = "Daily Essentials",
            timestamp = currentTimestamp
        )
        val tx2 = TransactionEntity(
            id = "tx_2",
            accountId = "acc_1",
            title = "Dinner",
            amount = 1500.0,
            type = TransactionType.EXPENSE,
            category = "Dining",
            timestamp = currentTimestamp
        )
        val tx3 = TransactionEntity(
            id = "tx_3",
            accountId = "acc_1",
            title = "Internal Transfer",
            amount = 10000.0,
            type = TransactionType.TRANSFER,
            category = "Internal Transfer",
            timestamp = currentTimestamp
        )

        val list1 = listOf(tx1, tx2, tx3)
        val (refY, refM) = FinancialIntegrityValidator.resolveReferenceYearMonth(list1, emptyList())
        assertEquals(currentYear, refY)
        assertEquals(currentMonth, refM)

        val spent1 = list1.filter {
            FinancialIntegrityValidator.isEligibleExpense(it) &&
                FinancialIntegrityValidator.isInYearMonth(it.timestamp, refY, refM)
        }.sumOf { it.amount }

        assertEquals(5000.0, spent1, 0.01)

        // Adding another expense transaction reactively updates spent amount
        val tx4 = TransactionEntity(
            id = "tx_4",
            accountId = "acc_1",
            title = "Books",
            amount = 1200.0,
            type = TransactionType.EXPENSE,
            category = "Education",
            timestamp = currentTimestamp
        )
        val list2 = list1 + tx4
        val spent2 = list2.filter {
            FinancialIntegrityValidator.isEligibleExpense(it) &&
                FinancialIntegrityValidator.isInYearMonth(it.timestamp, refY, refM)
        }.sumOf { it.amount }

        assertEquals(6200.0, spent2, 0.01)
    }

    @Test
    fun testExcludedCategoriesDoNotInflateMonthlyExpense() {
        val cal = Calendar.getInstance()
        val currentTimestamp = cal.timeInMillis

        val txCc = TransactionEntity(
            id = "tx_cc",
            accountId = "acc_1",
            title = "Credit Card Bill Payment",
            amount = 15000.0,
            type = TransactionType.CC_PAYMENT,
            category = "Credit Card Payment",
            timestamp = currentTimestamp
        )
        val txDebt = TransactionEntity(
            id = "tx_debt",
            accountId = "acc_1",
            title = "Loan Repayment",
            amount = 5000.0,
            type = TransactionType.EXPENSE,
            category = "Debt Repayment",
            timestamp = currentTimestamp
        )

        val (refY, refM) = FinancialIntegrityValidator.resolveReferenceYearMonth(listOf(txCc, txDebt), emptyList())
        val spent = listOf(txCc, txDebt).filter {
            FinancialIntegrityValidator.isEligibleExpense(it) &&
                FinancialIntegrityValidator.isInYearMonth(it.timestamp, refY, refM)
        }.sumOf { it.amount }

        assertEquals(0.0, spent, 0.01)
    }

    @Test
    fun testFormattingConsistencyForBothCards() {
        val formattedBalance = CurrencyFormatter.format(170936.05, "৳")
        val formattedSpent = CurrencyFormatter.format(44020.0, "৳")

        assertTrue(formattedBalance.contains("1,70,936"))
        assertTrue(formattedSpent.contains("44,020"))
    }

    private fun initViewModelWithApp(): MainViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(
                app,
                FirebaseOptions.Builder()
                    .setApplicationId("com.example.app")
                    .setProjectId("test-project")
                    .setApiKey("fake-api-key")
                    .build()
            )
        }
        UserSettingsPreferences.save(app, ExtendedSettingsState())
        return MainViewModel(app)
    }

    @Test
    fun allTenEnabledSections_renderAndScrollIntoView_withSampleDataLoaded() {
        val viewModel = initViewModelWithApp()
        runBlocking {
            val sampleJson = SampleDataGenerator.generateSampleJsonTemplate()
            val result = viewModel.backupManager.restoreWithDetailedSummary(sampleJson, replaceAll = true)
            assertTrue("Sample backup restore must succeed", result.isSuccess)
        }

        viewModel.setTab(MainTab.HOME)
        viewModel.updateHomeCustomization(
            HomeCustomization(
                showBudgetProgress = true,
                showUpcomingBills = true,
                showRecentActivity = true,
                showSavingsGoals = true,
                showFinancialHealth = true,
                showSmartInsights = true,
                showTopSpending = true,
                showSpendingByDay = true,
                showSpendingStreak = true,
                showQuickShortcuts = true,
                sectionOrder = DEFAULT_HOME_SECTIONS
            )
        )

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                MyWalletApp(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_lazy_column").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_unified_kpi_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_total_balance_card", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_spent_this_month_card", useUnmergedTree = true).assertIsDisplayed()

        val allSectionTags = listOf(
            "home_section_budget",
            "home_section_bills",
            "home_section_recent_activity",
            "home_section_savings",
            "home_section_financial_health",
            "home_section_smart_insights",
            "home_section_top_spending",
            "home_section_spending_by_day",
            "home_section_spending_streak",
            "home_section_shortcuts"
        )

        allSectionTags.forEach { tag ->
            composeTestRule.onNodeWithTag("home_lazy_column")
                .performScrollToNode(hasTestTag(tag))
            composeTestRule.onNodeWithTag(tag)
                .assertIsDisplayed()
        }
    }

    @Test
    fun allTenEnabledSections_renderEmptyStatesVisibly_whenListsAreEmpty() {
        val viewModel = initViewModelWithApp()
        runBlocking {
            viewModel.eraseAllData(preserveCloud = true)
        }

        viewModel.setTab(MainTab.HOME)
        viewModel.updateHomeCustomization(
            HomeCustomization(
                showBudgetProgress = true,
                showUpcomingBills = true,
                showRecentActivity = true,
                showSavingsGoals = true,
                showFinancialHealth = true,
                showSmartInsights = true,
                showTopSpending = true,
                showSpendingByDay = true,
                showSpendingStreak = true,
                showQuickShortcuts = true,
                sectionOrder = DEFAULT_HOME_SECTIONS
            )
        )

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                MyWalletApp(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        val allSectionTags = listOf(
            "home_section_budget",
            "home_section_bills",
            "home_section_recent_activity",
            "home_section_savings",
            "home_section_financial_health",
            "home_section_smart_insights",
            "home_section_top_spending",
            "home_section_spending_by_day",
            "home_section_spending_streak",
            "home_section_shortcuts"
        )

        allSectionTags.forEach { tag ->
            composeTestRule.onNodeWithTag("home_lazy_column")
                .performScrollToNode(hasTestTag(tag))
            composeTestRule.onNodeWithTag(tag)
                .assertIsDisplayed()
        }
    }

    @Test
    fun inPlaceViewSwitcher_cyclesThreeModes_andAutoHidesTopKpiCardsInBalanceMode() {
        val viewModel = initViewModelWithApp()
        runBlocking {
            val sampleJson = SampleDataGenerator.generateSampleJsonTemplate()
            val result = viewModel.backupManager.restoreWithDetailedSummary(sampleJson, replaceAll = true)
            assertTrue("Sample backup restore must succeed", result.isSuccess)
        }

        viewModel.setTab(MainTab.HOME)
        viewModel.updateHomeCustomization(
            HomeCustomization(cardStyle = HomeCardStyle.STACK)
        )

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                MyWalletApp(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // 1. STACK mode: Unified KPI Bento card sits ABOVE the Wallet Card Stack
        composeTestRule.onNodeWithTag("home_unified_kpi_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_total_balance_card", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_spent_this_month_card", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_wallet_card_stack").assertIsDisplayed()
        val kpiBottom = composeTestRule.onNodeWithTag("home_unified_kpi_card").fetchSemanticsNode().boundsInRoot.bottom
        val stackTop = composeTestRule.onNodeWithTag("home_wallet_card_stack").fetchSemanticsNode().boundsInRoot.top
        assertTrue("Expected unified KPI Bento card above Wallet Card Stack", kpiBottom <= stackTop + 2f)

        // 2. Tap switcher -> CAROUSEL mode: Unified KPI card above Card Carousel
        composeTestRule.onNodeWithTag("home_card_view_switcher_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(HomeCardStyle.CAROUSEL, viewModel.homeCustomization.value.cardStyle)
        composeTestRule.onNodeWithTag("home_unified_kpi_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_total_balance_card", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_spent_this_month_card", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_wallet_card_carousel").assertIsDisplayed()
        val carouselKpiBottom = composeTestRule.onNodeWithTag("home_unified_kpi_card").fetchSemanticsNode().boundsInRoot.bottom
        val carouselTop = composeTestRule.onNodeWithTag("home_wallet_card_carousel").fetchSemanticsNode().boundsInRoot.top
        assertTrue("Expected unified KPI Bento card above Wallet Card Carousel", carouselKpiBottom <= carouselTop + 2f)

        // 3. Tap switcher -> BALANCE_CARD mode: BalanceCardHero visible & unified KPI card hidden
        composeTestRule.onNodeWithTag("home_card_view_switcher_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(HomeCardStyle.BALANCE_CARD, viewModel.homeCustomization.value.cardStyle)
        composeTestRule.onNodeWithTag("home_balance_card_hero").assertIsDisplayed()
        composeTestRule.onNodeWithTag("balance_card_income_pill", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("balance_card_expense_pill", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_unified_kpi_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_total_balance_card", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_spent_this_month_card", useUnmergedTree = true).assertDoesNotExist()

        // Persistence check across SharedPreferences reload
        val app = ApplicationProvider.getApplicationContext<Application>()
        val reloaded = UserSettingsPreferences.loadHomeCustomization(app)
        assertEquals(HomeCardStyle.BALANCE_CARD, reloaded.cardStyle)

        // 4. Tap switcher -> loops cleanly back to STACK mode with unified KPI card restored
        composeTestRule.onNodeWithTag("home_card_view_switcher_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(HomeCardStyle.STACK, viewModel.homeCustomization.value.cardStyle)
        composeTestRule.onNodeWithTag("home_unified_kpi_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_wallet_card_stack").assertIsDisplayed()
    }
}
