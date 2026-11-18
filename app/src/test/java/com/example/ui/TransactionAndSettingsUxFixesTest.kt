package com.example.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ExtendedSettingsState
import com.example.data.local.UserSettingsPreferences
import com.example.data.model.TransactionType
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.backup.BackupRestoreScreen
import com.example.ui.screens.settings.AboutScreen
import com.example.ui.screens.settings.CategoriesScreen
import com.example.ui.screens.settings.CurrencySelectionScreen
import com.example.ui.screens.settings.NotificationManagerScreen
import com.example.ui.screens.settings.ProfileScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.StartOfWeekBottomSheet
import com.example.ui.screens.settings.ThemeScreen
import com.example.ui.screens.transaction.AddTransactionScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.util.Calendar
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionAndSettingsUxFixesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var application: Application
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        UserSettingsPreferences.save(application, ExtendedSettingsState())
        viewModel = MainViewModel(application)
        viewModel.clearTransactionDraft()
        viewModel.transactionToEdit.value = null
        viewModel.newTransactionInitialType.value = TransactionType.EXPENSE
        viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
    }

    @Test
    fun calculatorDismissesWhenFocusMovesToOtherFields_andReopensOnAmountTap_preservingAmount() {
        composeRule.setContent {
            MaterialTheme {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Calculator starts visible on New Expense screen
        assertTrue(
            "Calculator keypad should be visible initially",
            composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isNotEmpty()
        )

        // Enter amount 750 using keypad buttons
        composeRule.onNodeWithText("7").performClick()
        composeRule.onNodeWithText("5").performClick()
        composeRule.onNodeWithText("0").performClick()
        composeRule.waitForIdle()

        // Calculator remains visible while entering amount and amount displays 750
        assertTrue(
            "Calculator should stay visible while entering amount",
            composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithTag("amount_display_field")
            .assertTextContains("750", substring = true)

        // 2. Tap Transaction Name field -> calculator dismisses, amount preserved
        composeRule.onNodeWithTag("merchant_payee_input").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(
            "Calculator should dismiss when tapping Transaction Name field",
            composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isEmpty()
        )
        composeRule.onNodeWithTag("amount_display_field")
            .assertTextContains("750", substring = true)

        // 3. Tap Amount field -> calculator reopens, amount still 750
        composeRule.onNodeWithTag("amount_display_field").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(
            "Calculator should reopen when tapping Amount field",
            composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithTag("amount_display_field")
            .assertTextContains("750", substring = true)

        // 4. Tap Category field -> calculator dismisses and category sheet opens
        composeRule.onNodeWithTag("category_field_selector").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(
            "Calculator should dismiss when tapping Category field",
            composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isEmpty()
        )
        // Close category sheet
        composeRule.onNodeWithTag("btn_close_category_sheet").performClick()
        composeRule.waitForIdle()

        // Reopen calculator and verify Date field tap dismisses it
        composeRule.onNodeWithTag("amount_display_field").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(
            composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isNotEmpty()
        )

        composeRule.onNodeWithTag("transaction_date_picker").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(
            "Calculator should dismiss when tapping Date field",
            composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isEmpty()
        )
        // Dismiss date picker dialog
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()

        // Reopen calculator and verify Notes field tap dismisses it
        composeRule.onNodeWithTag("amount_display_field").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(
            composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isNotEmpty()
        )

        composeRule.onNodeWithTag("transaction_notes_input").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(
            "Calculator should dismiss when tapping Notes field",
            composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isEmpty()
        )

        // Reopen calculator and verify Wallet selector tap dismisses it
        composeRule.onNodeWithTag("amount_display_field").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(
            composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isNotEmpty()
        )

        composeRule.onNodeWithTag("tx_wallet_view_wallet_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(
            "Calculator should dismiss when opening Wallet selector",
            composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isEmpty()
        )
        // Close wallet sheet and confirm 750 is still intact
        composeRule.onNodeWithTag("btn_close_wallet_sheet").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("amount_display_field").performScrollTo()
            .assertTextContains("750", substring = true)
    }

    @Test
    fun walletSelectorAddAccountShortcut_cancellingAndSavingAccountPreservesExpenseAndRefreshesWallets() {
        composeRule.setContent {
            MaterialTheme {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Enter expense details: amount 420, merchant "Coffee Shop", notes "Latte"
        composeRule.onNodeWithText("4").performClick()
        composeRule.onNodeWithText("2").performClick()
        composeRule.onNodeWithText("0").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("merchant_payee_input").performScrollTo().performClick()
        composeRule.onNodeWithTag("merchant_payee_input").performTextInput("Coffee Shop")
        composeRule.onNodeWithTag("transaction_notes_input").performScrollTo().performClick()
        composeRule.onNodeWithTag("transaction_notes_input").performTextInput("Latte")
        composeRule.waitForIdle()

        // Open Select Wallet bottom sheet
        composeRule.onNodeWithTag("tx_wallet_view_wallet_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Verify both + (Add Account) and x (Close) icons are visible in the header
        composeRule.onNodeWithTag("btn_add_wallet_account").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_close_wallet_sheet").assertIsDisplayed()

        // Tap + to open Add Account screen
        composeRule.onNodeWithTag("btn_add_wallet_account").performClick()
        composeRule.waitForIdle()

        assertEquals(SubScreen.EDIT_ACCOUNT, viewModel.currentSubScreen.value)
        composeRule.onNodeWithText("Add account").assertIsDisplayed()

        // 1. Test cancelling account creation returns to the wallet selector with all expense details intact
        composeRule.onNodeWithTag("add_account_close_button").performClick()
        composeRule.waitForIdle()

        assertEquals(SubScreen.ADD_EDIT_TRANSACTION, viewModel.currentSubScreen.value)
        // Wallet selector sheet should still be open
        composeRule.onNodeWithTag("btn_add_wallet_account").assertIsDisplayed()

        // 2. Tap + again and create a new account "City Bank Card"
        composeRule.onNodeWithTag("btn_add_wallet_account").performClick()
        composeRule.waitForIdle()

        assertEquals(SubScreen.EDIT_ACCOUNT, viewModel.currentSubScreen.value)
        composeRule.onNodeWithTag("edit_account_lazy_column")
            .performScrollToNode(hasTestTag("account_name_input"))
        composeRule.onNodeWithTag("account_name_input").performTextInput("City Bank Card")
        composeRule.onNodeWithTag("update_account_button").performClick()
        composeRule.waitForIdle()

        // Verify we returned to New Expense and the new account appears immediately in the Wallet selector
        assertEquals(SubScreen.ADD_EDIT_TRANSACTION, viewModel.currentSubScreen.value)
        composeRule.onNodeWithTag("wallet_selection_lazy_column")
            .performScrollToNode(androidx.compose.ui.test.hasText("City Bank Card"))
        assertTrue(
            "New account 'City Bank Card' should appear immediately in wallet selector",
            composeRule.onAllNodesWithText("City Bank Card").fetchSemanticsNodes().isNotEmpty()
        )

        // Select the newly created account inside the wallet selection sheet
        composeRule.onNode(
            androidx.compose.ui.test.hasAnyAncestor(hasTestTag("wallet_selection_sheet")) and
                androidx.compose.ui.test.hasText("City Bank Card")
        ).performClick()
        composeRule.waitForIdle()

        // Verify all previously entered expense details are still preserved
        composeRule.onNodeWithTag("amount_display_field").performScrollTo()
            .assertTextContains("420", substring = true)
        composeRule.onNodeWithTag("merchant_payee_input").performScrollTo()
            .assertTextContains("Coffee Shop", substring = true)
        composeRule.onNodeWithTag("transaction_notes_input").performScrollTo()
            .assertTextContains("Latte", substring = true)
    }

    @Test
    fun newExpenseHeaderRemainsFixedWhileFormContentScrolls() {
        composeRule.setContent {
            MaterialTheme {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Dismiss calculator first so we have the full scrollable area at scroll offset 0
        composeRule.onNodeWithTag("calculator_dismiss_button").performClick()
        composeRule.waitForIdle()

        val initialHeaderBounds = composeRule.onNodeWithTag("add_transaction_fixed_header")
            .fetchSemanticsNode().boundsInRoot
        val initialAmountBounds = composeRule.onNodeWithTag("amount_card_container")
            .fetchSemanticsNode().boundsInRoot

        // Scroll the form content upward
        composeRule.onNodeWithTag("add_transaction_scroll_content")
            .performTouchInput {
                swipeUp(startY = bottom * 0.8f, endY = top + 50f, durationMillis = 300)
            }
        composeRule.waitForIdle()

        val scrolledHeaderBounds = composeRule.onNodeWithTag("add_transaction_fixed_header")
            .fetchSemanticsNode().boundsInRoot
        val scrolledAmountBounds = composeRule.onNodeWithTag("amount_card_container")
            .fetchSemanticsNode().boundsInRoot

        // Header must remain pinned at the exact same top position
        assertEquals(
            "Fixed header top should not move when scrolling form content",
            initialHeaderBounds.top,
            scrolledHeaderBounds.top,
            0.5f
        )
        assertEquals(
            "Fixed header bottom should not move when scrolling form content",
            initialHeaderBounds.bottom,
            scrolledHeaderBounds.bottom,
            0.5f
        )

        // Form content below the header must have scrolled upward
        assertTrue(
            "Form content should scroll underneath the fixed header (before=${initialAmountBounds.top}, after=${scrolledAmountBounds.top})",
            scrolledAmountBounds.top < initialAmountBounds.top
        )
    }

    @Test
    fun settingsScrollPositionIsPreservedWhenReturningFromSubpages_andResetWhenLeavingSettings() {
        viewModel.setTab(MainTab.HOME)
        viewModel.openSubScreen(SubScreen.SETTINGS)

        composeRule.setContent {
            MaterialTheme {
                val currentSubScreen by viewModel.currentSubScreen.collectAsState()
                val settingsSessionId by viewModel.settingsSessionId.collectAsState()
                val settingsScrollIndex by viewModel.settingsScrollIndex.collectAsState()
                val settingsScrollOffset by viewModel.settingsScrollOffset.collectAsState()
                val settingsListState = key(settingsSessionId) {
                    rememberLazyListState(
                        initialFirstVisibleItemIndex = settingsScrollIndex,
                        initialFirstVisibleItemScrollOffset = settingsScrollOffset
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentSubScreen) {
                        SubScreen.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.closeSubScreen() },
                                listState = settingsListState
                            )
                        }
                        SubScreen.ACCOUNTS_MANAGEMENT -> {
                            AccountsScreen(
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
                        SubScreen.NOTIFICATION_MANAGER -> {
                            NotificationManagerScreen(
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
                        SubScreen.CATEGORIES_MANAGEMENT -> {
                            CategoriesScreen(
                                viewModel = viewModel
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
                        SubScreen.USER_PROFILE -> {
                            ProfileScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.closeSubScreen() }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
        composeRule.waitForIdle()

        // Scroll Settings down to the WALLET, CURRENCY & DATA / ABOUT section
        composeRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_row_accounts"))
        composeRule.waitForIdle()

        val savedIndexBeforeSubpage = viewModel.settingsScrollIndex.value
        val savedOffsetBeforeSubpage = viewModel.settingsScrollOffset.value
        assertTrue(
            "Settings should have scrolled down (index=$savedIndexBeforeSubpage, offset=$savedOffsetBeforeSubpage)",
            savedIndexBeforeSubpage > 0 || savedOffsetBeforeSubpage > 0
        )

        // 1. Navigate to Accounts subpage from Settings
        composeRule.onNodeWithTag("settings_row_accounts").performClick()
        composeRule.waitForIdle()
        assertEquals(SubScreen.ACCOUNTS_MANAGEMENT, viewModel.currentSubScreen.value)

        // Return to Settings via Back
        viewModel.handleBackPress()
        composeRule.waitForIdle()

        assertEquals(SubScreen.SETTINGS, viewModel.currentSubScreen.value)
        assertEquals(savedIndexBeforeSubpage, viewModel.settingsScrollIndex.value)
        assertEquals(savedOffsetBeforeSubpage, viewModel.settingsScrollOffset.value)
        // Verify Accounts row is still immediately visible without scrolling again
        composeRule.onNodeWithTag("settings_row_accounts").assertIsDisplayed()

        // 2. Navigate to Backup & Restore subpage and return
        composeRule.onNodeWithTag("settings_row_backup_restore").performClick()
        composeRule.waitForIdle()
        assertEquals(SubScreen.BACKUP_RESTORE, viewModel.currentSubScreen.value)

        viewModel.handleBackPress()
        composeRule.waitForIdle()
        assertEquals(SubScreen.SETTINGS, viewModel.currentSubScreen.value)
        assertEquals(savedIndexBeforeSubpage, viewModel.settingsScrollIndex.value)
        assertEquals(savedOffsetBeforeSubpage, viewModel.settingsScrollOffset.value)

        // 3. Scroll further down to About, open About, and return
        composeRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_row_about"))
        composeRule.waitForIdle()

        val aboutIndex = viewModel.settingsScrollIndex.value
        val aboutOffset = viewModel.settingsScrollOffset.value

        composeRule.onNodeWithTag("settings_row_about").performClick()
        composeRule.waitForIdle()
        assertEquals(SubScreen.ABOUT, viewModel.currentSubScreen.value)

        viewModel.handleBackPress()
        composeRule.waitForIdle()
        assertEquals(SubScreen.SETTINGS, viewModel.currentSubScreen.value)
        assertEquals(aboutIndex, viewModel.settingsScrollIndex.value)
        assertEquals(aboutOffset, viewModel.settingsScrollOffset.value)
        composeRule.onNodeWithTag("settings_row_about").assertIsDisplayed()

        // 4. Exit Settings completely -> scroll state resets to top (0, 0)
        viewModel.handleBackPress()
        composeRule.waitForIdle()
        assertEquals(SubScreen.NONE, viewModel.currentSubScreen.value)
        assertEquals(0, viewModel.settingsScrollIndex.value)
        assertEquals(0, viewModel.settingsScrollOffset.value)
    }

    @Test
    fun calculatorDismissalHasNoLeftoverBackgroundDelayAcrossExpenseIncomeAndTransfer() {
        composeRule.setContent {
            MaterialTheme {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Helper to verify that when calculator is open, scroll content sits above it,
        // and within 160ms of dismissal, calculator_keypad_sheet is completely removed and scroll content fills full height
        fun assertCleanImmediateDismissal(dismissAction: () -> Unit) {
            assertTrue(
                "Expected calculator_keypad_sheet to be present before dismissal",
                composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isNotEmpty()
            )
            val scrollHeightWithCalc = composeRule
                .onNodeWithTag("add_transaction_scroll_content")
                .fetchSemanticsNode()
                .boundsInRoot
                .height

            composeRule.mainClock.autoAdvance = false
            try {
                dismissAction()
                composeRule.mainClock.advanceTimeBy(160L)
            } finally {
                composeRule.mainClock.autoAdvance = true
            }
            composeRule.waitForIdle()

            assertTrue(
                "Expected calculator_keypad_sheet and its background to be completely removed immediately after 140ms exit transition",
                composeRule.onAllNodesWithTag("calculator_keypad_sheet").fetchSemanticsNodes().isEmpty()
            )
            val scrollHeightWithoutCalc = composeRule
                .onNodeWithTag("add_transaction_scroll_content")
                .fetchSemanticsNode()
                .boundsInRoot
                .height
            assertTrue(
                "Expected scrollable content to immediately reclaim full height without leftover calculator background box (before=$scrollHeightWithCalc, after=$scrollHeightWithoutCalc)",
                scrollHeightWithoutCalc > scrollHeightWithCalc
            )
        }

        // 1. EXPENSE tab: enter expression 120 + 30, dismiss via Done button
        composeRule.onNodeWithTag("calc_btn_1").performClick()
        composeRule.onNodeWithTag("calc_btn_2").performClick()
        composeRule.onNodeWithTag("calc_btn_0").performClick()
        composeRule.onNodeWithTag("calc_btn_add").performClick()
        composeRule.onNodeWithTag("calc_btn_3").performClick()
        composeRule.onNodeWithTag("calc_btn_0").performClick()
        composeRule.waitForIdle()

        assertCleanImmediateDismissal {
            composeRule.onNodeWithTag("calc_btn_done").performClick()
        }
        composeRule.onNodeWithTag("amount_display_field").assertTextContains("150", substring = true)

        // Reopen and dismiss via collapse chevron button
        composeRule.onNodeWithTag("amount_display_field").performClick()
        composeRule.waitForIdle()
        assertCleanImmediateDismissal {
            composeRule.onNodeWithTag("calculator_dismiss_button").performClick()
        }
        composeRule.onNodeWithTag("amount_display_field").assertTextContains("150", substring = true)

        // Reopen and dismiss via tapping outside (fixed header)
        composeRule.onNodeWithTag("amount_card_container").performClick()
        composeRule.waitForIdle()
        assertCleanImmediateDismissal {
            composeRule.onNodeWithTag("add_transaction_fixed_header").performClick()
        }

        // 2. Switch to INCOME tab, reopen calculator, and dismiss via toggle button
        composeRule.onNodeWithText("Income").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("amount_display_field").performClick()
        composeRule.waitForIdle()
        assertCleanImmediateDismissal {
            composeRule.onNodeWithTag("calculator_toggle_button").performClick()
        }
        composeRule.onNodeWithTag("amount_display_field").assertTextContains("150", substring = true)

        // 3. Switch to TRANSFER tab, reopen calculator, and dismiss via Done button
        composeRule.onNodeWithText("Transfer").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("amount_display_field").performClick()
        composeRule.waitForIdle()
        assertCleanImmediateDismissal {
            composeRule.onNodeWithTag("calc_btn_done").performClick()
        }
        composeRule.onNodeWithTag("amount_display_field").assertTextContains("150", substring = true)
    }

    @Test
    fun startOfWeekBottomSheet_isCompact_andHasSaturdayFirstFollowedBySundayThroughFriday() {
        var selectedDay = Calendar.SUNDAY
        var sheetDismissed = false

        composeRule.setContent {
            MaterialTheme {
                StartOfWeekBottomSheet(
                    selectedDay = selectedDay,
                    language = com.example.data.model.AppLanguage.ENGLISH,
                    isDark = false,
                    hapticsEnabled = false,
                    onSelectDay = { day ->
                        selectedDay = day
                        sheetDismissed = true
                    },
                    onDismiss = {
                        sheetDismissed = true
                    }
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Verify all 7 day options exist and are displayed
        val expectedDays = listOf(
            "saturday",
            "sunday",
            "monday",
            "tuesday",
            "wednesday",
            "thursday",
            "friday"
        )
        expectedDays.forEach { dayName ->
            composeRule.onNodeWithTag("start_of_week_option_$dayName").assertIsDisplayed()
        }

        // 2. Verify Saturday is above Sunday, Sunday above Monday, etc. (vertical order in layout)
        val bounds = expectedDays.map { dayName ->
            composeRule.onNodeWithTag("start_of_week_option_$dayName")
                .fetchSemanticsNode()
                .boundsInRoot
        }
        for (i in 0 until bounds.size - 1) {
            assertTrue(
                "Expected ${expectedDays[i]} (top=${bounds[i].top}) to appear above ${expectedDays[i + 1]} (top=${bounds[i + 1].top})",
                bounds[i].bottom <= bounds[i + 1].top + 5f // within spacing tolerance
            )
        }

        // 3. Verify each day option row is compact (height <= 50dp / reasonable bounds)
        bounds.forEachIndexed { index, rect ->
            assertTrue(
                "Expected row ${expectedDays[index]} to have compact height (actual=${rect.height})",
                rect.height > 0 && rect.height <= 60f * composeRule.density.density
            )
        }

        // 4. Select Saturday (which is now first) and verify callback receives Calendar.SATURDAY
        composeRule.onNodeWithTag("start_of_week_option_saturday").performClick()
        composeRule.waitForIdle()

        assertEquals(Calendar.SATURDAY, selectedDay)
        assertTrue(sheetDismissed)
    }
}
