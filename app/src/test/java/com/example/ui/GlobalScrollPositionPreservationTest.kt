package com.example.ui

import android.Manifest
import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import com.example.MyWalletApp
import com.example.data.local.ExtendedSettingsState
import com.example.data.local.UserSettingsPreferences
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import com.example.ui.navigation.LazyScrollPosition
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GlobalScrollPositionPreservationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var application: Application
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        UserSettingsPreferences.save(application, ExtendedSettingsState())
        viewModel = MainViewModel(application)
        viewModel.setTab(MainTab.HOME)
    }

    private fun currentSavedScroll(subKey: String = "main", screenOverride: SubScreen? = null, tabOverride: MainTab? = null): LazyScrollPosition {
        val scopeKey = viewModel.resolveNavigationScopeKey(
            screenOverride = screenOverride,
            tabOverride = tabOverride
        )
        return viewModel.scrollStateStore.getLazyPosition("$scopeKey|LAZY:$subKey")
    }

    @Test
    fun accountsNestedFlow_preservesScrollAcrossAccounts_AccountDetail_EditAccount_AdjustBalance_andSeparatesInstances() {
        val accountA = AccountEntity(
            id = "acc_scroll_a",
            name = "Alpha Checking",
            type = AccountType.BANK,
            balance = 50000.0
        )
        val accountB = AccountEntity(
            id = "acc_scroll_b",
            name = "Beta Savings",
            type = AccountType.BANK,
            balance = 80000.0
        )
        runBlocking {
            viewModel.repository.insertAccount(accountA)
            viewModel.repository.insertAccount(accountB)
            for (i in 1..15) {
                viewModel.repository.insertAccount(
                    AccountEntity(
                        id = "acc_extra_$i",
                        name = "Extra Wallet $i",
                        type = AccountType.CASH,
                        balance = 1000.0 * i
                    )
                )
                viewModel.repository.insertTransaction(
                    TransactionEntity(
                        id = "tx_acc_a_$i",
                        accountId = accountA.id,
                        title = "Alpha Expense $i",
                        amount = 100.0 * i,
                        type = TransactionType.EXPENSE,
                        category = "Food & Dining",
                        timestamp = System.currentTimeMillis() - i * 3600_000L
                    )
                )
                viewModel.repository.insertTransaction(
                    TransactionEntity(
                        id = "tx_acc_b_$i",
                        accountId = accountB.id,
                        title = "Beta Expense $i",
                        amount = 200.0 * i,
                        type = TransactionType.EXPENSE,
                        category = "Shopping",
                        timestamp = System.currentTimeMillis() - i * 3600_000L
                    )
                )
            }
        }

        composeRule.setContent {
            MaterialTheme {
                MyWalletApp(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // 1. Open Accounts Management and scroll down
        composeRule.runOnIdle {
            viewModel.openSubScreen(SubScreen.ACCOUNTS_MANAGEMENT)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("accounts_lazy_column").performTouchInput {
            swipeUp(startY = bottom * 0.85f, endY = top + 40f, durationMillis = 200)
        }
        composeRule.waitForIdle()

        val accountsScrollBefore = currentSavedScroll(screenOverride = SubScreen.ACCOUNTS_MANAGEMENT)
        assertTrue(
            "Expected AccountsScreen to have non-zero scroll position",
            accountsScrollBefore.index > 0 || accountsScrollBefore.offset > 0
        )

        // 2. Open Account A Detail and scroll down
        composeRule.runOnIdle {
            viewModel.openAccountDetail(accountA.id)
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.ACCOUNT_DETAIL, viewModel.currentSubScreen.value)

        composeRule.onNodeWithTag("account_detail_lazy_column").performTouchInput {
            swipeUp(startY = bottom * 0.85f, endY = top + 40f, durationMillis = 200)
        }
        composeRule.waitForIdle()

        val accountADetailScrollBefore = currentSavedScroll(
            subKey = accountA.id,
            screenOverride = SubScreen.ACCOUNT_DETAIL
        )
        assertTrue(
            "Expected AccountDetailScreen for Account A to have non-zero scroll position",
            accountADetailScrollBefore.index > 0 || accountADetailScrollBefore.offset > 0
        )

        // 3. Open Edit Account for Account A and scroll down
        composeRule.runOnIdle {
            viewModel.openEditAccount(accountA)
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.EDIT_ACCOUNT, viewModel.currentSubScreen.value)

        composeRule.onNodeWithTag("edit_account_lazy_column").performTouchInput {
            swipeUp(startY = bottom * 0.85f, endY = top + 40f, durationMillis = 200)
        }
        composeRule.waitForIdle()

        val editAccountScrollBefore = currentSavedScroll(
            subKey = accountA.id,
            screenOverride = SubScreen.EDIT_ACCOUNT
        )
        assertTrue(
            "Expected EditAccountScreen to have non-zero scroll position",
            editAccountScrollBefore.index > 0 || editAccountScrollBefore.offset > 0
        )

        // 4. Open Adjust Balance from Edit Account
        composeRule.runOnIdle {
            viewModel.openAdjustBalance(accountA)
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.ADJUST_BALANCE, viewModel.currentSubScreen.value)

        // 5. Press Back -> returns to Edit Account with exact scroll restored
        composeRule.runOnIdle {
            viewModel.handleBackPress()
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.EDIT_ACCOUNT, viewModel.currentSubScreen.value)
        val editAccountScrollAfter = currentSavedScroll(
            subKey = accountA.id,
            screenOverride = SubScreen.EDIT_ACCOUNT
        )
        assertEquals(editAccountScrollBefore, editAccountScrollAfter)

        // 6. Press Back -> returns to Account A Detail with exact scroll restored
        composeRule.runOnIdle {
            viewModel.handleBackPress()
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.ACCOUNT_DETAIL, viewModel.currentSubScreen.value)
        assertEquals(accountA.id, viewModel.selectedAccountId.value)
        val accountADetailScrollAfter = currentSavedScroll(
            subKey = accountA.id,
            screenOverride = SubScreen.ACCOUNT_DETAIL
        )
        assertEquals(accountADetailScrollBefore, accountADetailScrollAfter)

        // 7. Press Back -> returns to Accounts Management with exact scroll restored
        composeRule.runOnIdle {
            viewModel.handleBackPress()
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.ACCOUNTS_MANAGEMENT, viewModel.currentSubScreen.value)
        val accountsScrollAfter = currentSavedScroll(screenOverride = SubScreen.ACCOUNTS_MANAGEMENT)
        assertEquals(accountsScrollBefore, accountsScrollAfter)

        // 8. Open Account B Detail -> verify Account B starts at the top (0, 0), independent of Account A
        composeRule.runOnIdle {
            viewModel.openAccountDetail(accountB.id)
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.ACCOUNT_DETAIL, viewModel.currentSubScreen.value)
        val accountBDetailInitialScroll = currentSavedScroll(
            subKey = accountB.id,
            screenOverride = SubScreen.ACCOUNT_DETAIL
        )
        assertEquals(LazyScrollPosition(0, 0), accountBDetailInitialScroll)
    }

    @Test
    fun planToDebts_toDebtDetails_toAddDebtTransaction_andBack_preservesScrollAtEveryLevel() {
        val debt1 = DebtEntity(
            id = "debt_scroll_1",
            personName = "Rahim Uddin",
            amount = 25000.0,
            totalAmount = 25000.0,
            paidAmount = 0.0,
            type = DebtType.BORROWED,
            reason = "Business Loan"
        )
        runBlocking {
            viewModel.repository.insertDebt(debt1)
            for (i in 2..16) {
                viewModel.repository.insertDebt(
                    DebtEntity(
                        id = "debt_scroll_$i",
                        personName = "Contact $i",
                        amount = 1000.0 * i,
                        totalAmount = 1000.0 * i,
                        paidAmount = 0.0,
                        type = DebtType.BORROWED,
                        reason = "Loan $i"
                    )
                )
            }
            for (i in 1..15) {
                viewModel.repository.insertTransaction(
                    TransactionEntity(
                        id = "tx_debt_hist_$i",
                        accountId = "",
                        title = "Installment $i",
                        amount = 500.0,
                        type = TransactionType.EXPENSE,
                        category = "Debt Repayment",
                        subcategory = "Installment",
                        debtId = debt1.id,
                        timestamp = System.currentTimeMillis() - i * 3600_000L
                    )
                )
            }
        }

        composeRule.setContent {
            MaterialTheme {
                MyWalletApp(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // 1. Switch to PLAN tab and scroll down
        composeRule.runOnIdle {
            viewModel.setTab(MainTab.PLAN)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("plan_lazy_column").performTouchInput {
            swipeUp(startY = bottom * 0.85f, endY = top + 40f, durationMillis = 200)
        }
        composeRule.waitForIdle()
        val planScrollBefore = currentSavedScroll(tabOverride = MainTab.PLAN)
        assertTrue(
            "Expected PlanScreen to have non-zero scroll position",
            planScrollBefore.index > 0 || planScrollBefore.offset > 0
        )

        // 2. Open Manage Debts and scroll down
        composeRule.runOnIdle {
            viewModel.openManageDebts(DebtType.BORROWED)
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.MANAGE_DEBTS, viewModel.currentSubScreen.value)

        composeRule.onNodeWithTag("manage_debts_lazy_column").performTouchInput {
            swipeUp(startY = bottom * 0.85f, endY = top + 40f, durationMillis = 200)
        }
        composeRule.waitForIdle()
        val manageDebtsScrollBefore = currentSavedScroll(
            subKey = DebtType.BORROWED.name,
            screenOverride = SubScreen.MANAGE_DEBTS
        )
        assertTrue(
            "Expected ManageDebtsScreen to have non-zero scroll position",
            manageDebtsScrollBefore.index > 0 || manageDebtsScrollBefore.offset > 0
        )

        // 3. Open Debt Details for debt1 and scroll down
        composeRule.runOnIdle {
            viewModel.openDebtDetails(debt1)
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.DEBT_DETAILS, viewModel.currentSubScreen.value)

        composeRule.onNodeWithTag("debt_details_lazy_column").performTouchInput {
            swipeUp(startY = bottom * 0.85f, endY = top + 40f, durationMillis = 200)
        }
        composeRule.waitForIdle()
        val debtDetailsScrollBefore = currentSavedScroll(
            subKey = debt1.id,
            screenOverride = SubScreen.DEBT_DETAILS
        )
        assertTrue(
            "Expected DebtDetailsScreen to have non-zero scroll position",
            debtDetailsScrollBefore.index > 0 || debtDetailsScrollBefore.offset > 0
        )

        // 4. Open Add Debt Transaction
        composeRule.runOnIdle {
            viewModel.openAddDebtTransaction(debt1, "Record Payment")
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.ADD_DEBT_TRANSACTION, viewModel.currentSubScreen.value)

        // 5. Press Back -> returns to DebtDetailsScreen with exact scroll restored
        composeRule.runOnIdle {
            viewModel.handleBackPress()
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.DEBT_DETAILS, viewModel.currentSubScreen.value)
        assertEquals(debt1.id, viewModel.selectedDebt.value?.id)
        val debtDetailsScrollAfter = currentSavedScroll(
            subKey = debt1.id,
            screenOverride = SubScreen.DEBT_DETAILS
        )
        assertEquals(debtDetailsScrollBefore, debtDetailsScrollAfter)

        // 6. Press Back -> returns to ManageDebtsScreen with exact scroll restored
        composeRule.runOnIdle {
            viewModel.handleBackPress()
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.MANAGE_DEBTS, viewModel.currentSubScreen.value)
        val manageDebtsScrollAfter = currentSavedScroll(
            subKey = DebtType.BORROWED.name,
            screenOverride = SubScreen.MANAGE_DEBTS
        )
        assertEquals(manageDebtsScrollBefore, manageDebtsScrollAfter)

        // 7. Press Back -> returns to Plan tab with exact scroll restored
        composeRule.runOnIdle {
            viewModel.handleBackPress()
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.NONE, viewModel.currentSubScreen.value)
        assertEquals(MainTab.PLAN, viewModel.currentTab.value)
        val planScrollAfter = currentSavedScroll(tabOverride = MainTab.PLAN)
        assertEquals(planScrollBefore, planScrollAfter)
    }

    @Test
    fun settingsToCategories_toInlineEditCategory_andBack_preservesBothCategoriesAndSettingsScroll() {
        runBlocking {
            for (i in 1..15) {
                viewModel.repository.insertCategory(
                    com.example.data.local.entity.CategoryEntity(
                        id = "cat_scroll_test_$i",
                        name = "Custom Expense Category $i",
                        isIncome = false,
                        isTransfer = false,
                        isActive = true,
                        sortOrder = i
                    )
                )
            }
        }

        composeRule.setContent {
            MaterialTheme {
                MyWalletApp(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // 1. Open Settings and scroll down
        composeRule.runOnIdle {
            viewModel.openSubScreen(SubScreen.SETTINGS)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_lazy_column").performTouchInput {
            swipeUp(startY = bottom * 0.85f, endY = top + 40f, durationMillis = 200)
        }
        composeRule.waitForIdle()

        val settingsScrollBefore = currentSavedScroll(screenOverride = SubScreen.SETTINGS)
        assertTrue(
            "Expected SettingsScreen to have non-zero scroll position",
            settingsScrollBefore.index > 0 || settingsScrollBefore.offset > 0
        )

        // 2. Open Categories Management and scroll down
        composeRule.runOnIdle {
            viewModel.openSubScreen(SubScreen.CATEGORIES_MANAGEMENT)
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.CATEGORIES_MANAGEMENT, viewModel.currentSubScreen.value)

        composeRule.onNodeWithTag("categories_lazy_column").performTouchInput {
            swipeUp(startY = bottom * 0.85f, endY = top + 40f, durationMillis = 200)
        }
        composeRule.waitForIdle()

        val categoriesScrollBefore = currentSavedScroll(
            subKey = "tab_0",
            screenOverride = SubScreen.CATEGORIES_MANAGEMENT
        )
        assertTrue(
            "Expected CategoriesScreen to have non-zero scroll position",
            categoriesScrollBefore.index > 0 || categoriesScrollBefore.offset > 0
        )

        // 3. Tap Add Category button to open inline AddEditCategoryPanel
        composeRule.onNodeWithTag("add_category_header_button").performClick()
        composeRule.waitForIdle()

        // 4. Press Back from inline AddEditCategoryPanel -> returns to Categories list with scroll preserved
        composeRule.onNodeWithTag("category_back_button").performClick()
        composeRule.waitForIdle()

        val categoriesScrollAfterInlineBack = currentSavedScroll(
            subKey = "tab_0",
            screenOverride = SubScreen.CATEGORIES_MANAGEMENT
        )
        assertEquals(categoriesScrollBefore, categoriesScrollAfterInlineBack)

        // 5. Press Back from Categories -> returns to Settings with Settings scroll preserved
        composeRule.runOnIdle {
            viewModel.handleBackPress()
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.SETTINGS, viewModel.currentSubScreen.value)
        val settingsScrollAfter = currentSavedScroll(screenOverride = SubScreen.SETTINGS)
        assertEquals(settingsScrollBefore, settingsScrollAfter)
    }
}
