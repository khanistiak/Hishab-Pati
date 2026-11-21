package com.example.ui

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.MyWalletApp
import com.example.data.local.ExtendedSettingsState
import com.example.data.local.UserSettingsPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class LoansAndDebtFixesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var application: Application
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        UserSettingsPreferences.save(application, ExtendedSettingsState())
        val db = AppDatabase.getDatabase(application)
        runBlocking {
            db.debtDao().deleteAllDebts()
            db.transactionDao().deleteAllTransactions()
            db.accountDao().deleteAllAccounts()
        }
        viewModel = MainViewModel(application)
        viewModel.setTab(MainTab.PLAN)
    }

    @Test
    fun fix1_manageDebts_tabSwitching_filtersLazyColumnImmediately() {
        val borrowedDebt = DebtEntity(
            id = "debt_fix1_borrowed",
            personName = "Arian Borrowed",
            amount = 12000.0,
            totalAmount = 18000.0,
            paidAmount = 6000.0,
            type = DebtType.BORROWED,
            isSettled = false
        )
        val lentDebt1 = DebtEntity(
            id = "debt_fix1_lent_1",
            personName = "Nabila Lent",
            amount = 5000.0,
            totalAmount = 9000.0,
            paidAmount = 4000.0,
            type = DebtType.LENT,
            isSettled = false
        )
        val lentDebt2 = DebtEntity(
            id = "debt_fix1_lent_2",
            personName = "Rafi Lent",
            amount = 3000.0,
            totalAmount = 3000.0,
            paidAmount = 0.0,
            type = DebtType.LENT,
            isSettled = false
        )

        runBlocking {
            viewModel.repository.insertDebt(borrowedDebt)
            viewModel.repository.insertDebt(lentDebt1)
            viewModel.repository.insertDebt(lentDebt2)
        }

        composeRule.setContent {
            MaterialTheme {
                MyWalletApp(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            viewModel.openManageDebts(DebtType.BORROWED)
        }
        composeRule.waitForIdle()

        // Initially on Borrowed tab -> shows borrowedDebt, hides lent debts
        composeRule.onNodeWithTag("manage_debts_lazy_column")
            .performScrollToNode(hasTestTag("debt_item_debt_fix1_borrowed"))
        composeRule.onNodeWithTag("debt_item_debt_fix1_borrowed").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("debt_item_debt_fix1_lent_1").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("debt_summary_count").assertTextContains("1 record")

        // Tap Lent tab -> immediately shows both Lent records and hides Borrowed record
        composeRule.onNodeWithTag("debt_type_lent").performClick()
        composeRule.waitForIdle()

        assertEquals(DebtType.LENT, viewModel.debtFilterType.value)
        composeRule.onNodeWithTag("manage_debts_lazy_column")
            .performScrollToNode(hasTestTag("debt_item_debt_fix1_lent_1"))
        composeRule.onNodeWithTag("debt_item_debt_fix1_lent_1").assertIsDisplayed()
        composeRule.onNodeWithTag("manage_debts_lazy_column")
            .performScrollToNode(hasTestTag("debt_item_debt_fix1_lent_2"))
        composeRule.onNodeWithTag("debt_item_debt_fix1_lent_2").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("debt_item_debt_fix1_borrowed").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("manage_debts_lazy_column")
            .performScrollToNode(hasTestTag("debt_summary_count"))
        composeRule.onNodeWithTag("debt_summary_count").assertTextContains("2 records", substring = true)

        // Tap Borrowed tab again -> switches back cleanly
        composeRule.onNodeWithTag("debt_type_borrowed").performClick()
        composeRule.waitForIdle()

        assertEquals(DebtType.BORROWED, viewModel.debtFilterType.value)
        composeRule.onNodeWithTag("manage_debts_lazy_column")
            .performScrollToNode(hasTestTag("debt_item_debt_fix1_borrowed"))
        composeRule.onNodeWithTag("debt_item_debt_fix1_borrowed").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("debt_item_debt_fix1_lent_1").fetchSemanticsNodes().size)
    }

    @Test
    fun fix2_backNavigation_fromDebtDetails_returnsToManageDebtsOrPlanCorrectly() {
        val debt = DebtEntity(
            id = "debt_fix2_nav",
            personName = "Back Nav Person",
            amount = 7000.0,
            totalAmount = 10000.0,
            paidAmount = 3000.0,
            type = DebtType.BORROWED,
            isSettled = false
        )
        runBlocking {
            viewModel.repository.insertDebt(debt)
        }

        composeRule.setContent {
            MaterialTheme {
                MyWalletApp(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Path A: Plan -> Manage Debts -> Debt Details -> Back button -> MUST land on Manage Debts
        composeRule.runOnIdle {
            viewModel.openManageDebts(DebtType.BORROWED)
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.MANAGE_DEBTS, viewModel.currentSubScreen.value)

        composeRule.onNodeWithTag("manage_debts_lazy_column")
            .performScrollToNode(hasTestTag("debt_item_debt_fix2_nav"))
        composeRule.onNodeWithTag("debt_item_debt_fix2_nav").performClick()
        composeRule.waitForIdle()
        assertEquals(SubScreen.DEBT_DETAILS, viewModel.currentSubScreen.value)

        composeRule.onNodeWithTag("debt_details_back_button").performClick()
        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()

        assertEquals(
            "Pressing Back from Debt Details opened via Manage Debts must return to Manage Debts",
            SubScreen.MANAGE_DEBTS,
            viewModel.currentSubScreen.value
        )

        // Back from Manage Debts -> returns to Plan (SubScreen.NONE)
        composeRule.onNodeWithTag("manage_debts_back_button").performClick()
        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()
        assertEquals(SubScreen.NONE, viewModel.currentSubScreen.value)

        // Path B: Plan -> Debt Details directly -> Back button -> returns to Plan (SubScreen.NONE)
        composeRule.runOnIdle {
            viewModel.openDebtDetails(debt)
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.DEBT_DETAILS, viewModel.currentSubScreen.value)

        composeRule.onNodeWithTag("debt_details_back_button").performClick()
        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()
        assertEquals(SubScreen.NONE, viewModel.currentSubScreen.value)
    }

    @Test
    fun fix3_and_fix4_bentoDetailSheet_and_autoCloseOnEditUpdate() {
        val wallet = AccountEntity(
            id = "acc_fix34",
            name = "bKash Personal",
            type = AccountType.MFS,
            balance = 20000.0,
            colorHex = "#E11D48"
        )
        val debt = DebtEntity(
            id = "debt_fix34",
            personName = "Farhan Kabir",
            amount = 8000.0,
            totalAmount = 10000.0,
            paidAmount = 2000.0,
            type = DebtType.BORROWED,
            accountId = wallet.id,
            bindToWallet = true,
            isSettled = false
        )
        val initTx = TransactionEntity(
            id = "tx_debt_init_${debt.id}",
            accountId = wallet.id,
            title = "Initial Debt (Borrowed)",
            amount = 10000.0,
            type = TransactionType.BORROW,
            category = "Debt / Borrow",
            subcategory = "Initial Debt",
            notes = "Initial principal [Debt:${debt.id}]",
            debtId = debt.id,
            timestamp = System.currentTimeMillis() - 86_400_000L
        )
        val repayTx = TransactionEntity(
            id = "tx_repay_${debt.id}_1",
            accountId = wallet.id,
            title = "Repayment to Farhan",
            amount = 2000.0,
            type = TransactionType.EXPENSE,
            category = "Debt Repayment",
            subcategory = "Installment",
            notes = "Sample partial repayment with a long descriptive note to ensure clean wrapping without text collision [Debt:${debt.id}]",
            debtId = debt.id,
            timestamp = System.currentTimeMillis()
        )

        runBlocking {
            viewModel.repository.insertAccount(wallet)
            viewModel.repository.insertDebt(debt)
            viewModel.repository.insertTransaction(initTx)
            viewModel.repository.insertTransaction(repayTx)
        }

        composeRule.setContent {
            MaterialTheme {
                MyWalletApp(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            viewModel.openManageDebts(DebtType.BORROWED)
            viewModel.openDebtDetails(debt)
        }
        composeRule.waitForIdle()

        // Scroll to history row and tap -> opens Bento Bottom Sheet
        composeRule.onNodeWithTag("debt_details_lazy_column")
            .performScrollToNode(hasTestTag("debt_history_item_${repayTx.id}"))
        composeRule.onNodeWithTag("debt_history_item_${repayTx.id}").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("debt_history_detail_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("debt_history_sheet_bento_card").assertIsDisplayed()
        composeRule.onNodeWithTag("debt_history_sheet_wallet").assertTextContains("bKash Personal", substring = true)
        composeRule.onNodeWithTag("debt_history_sheet_details").assertTextContains("Sample partial repayment", substring = true)
        composeRule.onNodeWithTag("delete_debt_transaction_button").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_debt_transaction_button").assertIsDisplayed()

        // Tap Edit -> navigates to AddTransactionScreen (Edit mode)
        composeRule.onNodeWithTag("edit_debt_transaction_button").performClick()
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.waitForIdle()
        assertEquals(SubScreen.ADD_EDIT_TRANSACTION, viewModel.currentSubScreen.value)

        // Tap Update transaction -> auto-closes back to DebtDetailsScreen
        composeRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()

        assertEquals(
            "Updating a debt transaction must auto-close Edit screen back to Debt Details",
            SubScreen.DEBT_DETAILS,
            viewModel.currentSubScreen.value
        )

        // Also verify editing the repayment amount from 2000 -> 3500 recalculates parent debt totals
        composeRule.runOnIdle {
            viewModel.saveTransaction(
                id = repayTx.id,
                type = repayTx.type,
                amount = 3500.0,
                title = repayTx.title,
                category = repayTx.category,
                subcategory = repayTx.subcategory,
                accountId = wallet.id,
                notes = repayTx.notes,
                timestamp = repayTx.timestamp,
                debtId = debt.id
            )
        }
        composeRule.waitUntil(timeoutMillis = 3000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val updated = viewModel.debts.value.find { it.id == debt.id }
            updated != null && updated.paidAmount == 3500.0 && updated.amount == 6500.0
        }
        val refreshedDebt = viewModel.debts.value.find { it.id == debt.id }
        assertNotNull(refreshedDebt)
        assertEquals(3500.0, refreshedDebt!!.paidAmount, 0.01)
        assertEquals(6500.0, refreshedDebt.amount, 0.01)
        assertEquals(10000.0, refreshedDebt.totalAmount, 0.01)
    }

    @Test
    fun fix5_addDebtTransaction_amountCardFocus_inputAndSaveUpdatesDebt() {
        val wallet = AccountEntity(
            id = "acc_fix5",
            name = "Main Cash",
            type = AccountType.CASH,
            balance = 15000.0
        )
        val debt = DebtEntity(
            id = "debt_fix5",
            personName = "Nabila Rahman",
            amount = 5000.0,
            totalAmount = 9000.0,
            paidAmount = 4000.0,
            type = DebtType.LENT,
            accountId = wallet.id,
            bindToWallet = true,
            isSettled = false
        )
        runBlocking {
            viewModel.repository.insertAccount(wallet)
            viewModel.repository.insertDebt(debt)
        }

        composeRule.setContent {
            MaterialTheme {
                MyWalletApp(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            viewModel.openManageDebts(DebtType.LENT)
            viewModel.openDebtDetails(debt)
            viewModel.openAddDebtTransaction(debt, "Lend More")
        }
        composeRule.waitForIdle()
        assertEquals(SubScreen.ADD_DEBT_TRANSACTION, viewModel.currentSubScreen.value)

        // Tapping Done with empty amount displays validation error and keeps screen open
        composeRule.onNodeWithTag("debt_tx_done_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_debt_transaction_lazy_column")
            .performScrollToNode(hasTestTag("debt_tx_amount_error"))
        composeRule.onNodeWithTag("debt_tx_amount_error").assertIsDisplayed()
        assertEquals(SubScreen.ADD_DEBT_TRANSACTION, viewModel.currentSubScreen.value)

        // Tapping the Amount Bento card focuses the BasicTextField
        composeRule.onNodeWithTag("debt_tx_amount_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("debt_tx_amount_input").assertIsFocused()

        // Typing numeric amount + tapping Done saves, updates debt, and returns to Debt Details
        composeRule.onNodeWithTag("debt_tx_amount_input").performTextInput("2500.50")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("debt_tx_amount_formatted_preview", useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule.onNodeWithTag("debt_tx_done_button").performClick()
        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()

        assertEquals(
            "Saving a valid debt transaction must close back to Debt Details",
            SubScreen.DEBT_DETAILS,
            viewModel.currentSubScreen.value
        )

        composeRule.waitUntil(timeoutMillis = 3000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val updated = viewModel.debts.value.find { it.id == debt.id }
            updated != null && updated.amount >= 7500.0
        }
        val updatedDebt = viewModel.debts.value.find { it.id == debt.id }
        assertNotNull(updatedDebt)
        assertTrue(
            "Expected debt remaining to increase by 2500.50 (was 5000.0, now ${updatedDebt?.amount})",
            (updatedDebt?.amount ?: 0.0) >= 7500.0
        )
    }
}
