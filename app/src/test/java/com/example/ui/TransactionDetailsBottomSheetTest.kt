package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionType
import com.example.ui.screens.activity.TransactionDetailsSheetContent
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.MyWalletTheme
import com.example.ui.util.DateFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionDetailsBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testAccounts = listOf(
        AccountEntity(
            id = "acc_test_cash",
            name = "test",
            type = AccountType.CASH,
            balance = 2450.0,
            colorHex = "#10B981",
            iconName = "payments"
        ),
        AccountEntity(
            id = "acc_ebl_bank",
            name = "EBL Salary Account",
            type = AccountType.BANK,
            balance = 85000.0,
            colorHex = "#2563EB",
            iconName = "account_balance"
        ),
        AccountEntity(
            id = "acc_bkash",
            name = "bKash Personal",
            type = AccountType.MFS,
            balance = 12300.0,
            colorHex = "#E11D48",
            iconName = "phone_android"
        )
    )

    private val testCategories = listOf(
        CategoryEntity(
            id = "cat_food",
            name = "Food & Dining",
            iconName = "restaurant",
            colorHex = "#EF4444",
            isIncome = false
        ),
        CategoryEntity(
            id = "cat_salary",
            name = "Salary",
            iconName = "payments",
            colorHex = "#10B981",
            isIncome = true
        )
    )

    @Test
    fun expenseTransaction_withSubcategoryAndNote_displaysAllFieldsAndWalletClearly() {
        val timestamp = 1793945340000L // Nov 6, 2026
        val expenseTx = TransactionEntity(
            id = "tx_exp_1",
            type = TransactionType.EXPENSE,
            amount = 92.0,
            title = "yest",
            category = "Food & Dining",
            subcategory = "Coffee & Snacks",
            accountId = "acc_test_cash",
            timestamp = timestamp,
            notes = "Quick evening coffee with colleagues"
        )

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                TransactionDetailsSheetContent(
                    transaction = expenseTx,
                    accounts = testAccounts,
                    categories = testCategories,
                    currencySymbol = "৳",
                    language = AppLanguage.ENGLISH,
                    onDismiss = {},
                    onDuplicate = {},
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        // Verify title, formatted expense amount, and Expense type badge
        composeTestRule.onNodeWithTag("tx_details_title")
            .assertIsDisplayed()
            .assertTextContains("yest")
        composeTestRule.onNodeWithTag("tx_details_amount")
            .assertIsDisplayed()
            .assertTextContains("- ৳92")
        composeTestRule.onNodeWithTag("tx_details_type_badge")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Expense").assertIsDisplayed()

        // Verify prominent wallet card with wallet name and type badge
        composeTestRule.onNodeWithTag("tx_details_single_wallet_row").assertIsDisplayed()
        composeTestRule.onNodeWithText("test").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cash").assertIsDisplayed()

        // Verify category, subcategory, and date & time rows
        composeTestRule.onNodeWithTag("tx_details_category_row").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Dining").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tx_details_subcategory_row").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Coffee & Snacks").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tx_details_datetime_row").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(DateFormatter.formatFull(timestamp, AppLanguage.ENGLISH))
            .assertIsDisplayed()

        // Verify note section is displayed when present
        composeTestRule.onNodeWithTag("tx_details_notes_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_details_notes_text")
            .assertTextContains("Quick evening coffee with colleagues")
    }

    @Test
    fun incomeTransaction_withoutSubcategoryOrNote_omitsEmptyFieldsAndShowsIncomeStyling() {
        val timestamp = 1793945340000L
        val incomeTx = TransactionEntity(
            id = "tx_inc_1",
            type = TransactionType.INCOME,
            amount = 45000.0,
            title = "Monthly Salary",
            category = "Salary",
            subcategory = "",
            accountId = "acc_ebl_bank",
            timestamp = timestamp,
            notes = ""
        )

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.DARK) {
                TransactionDetailsSheetContent(
                    transaction = incomeTx,
                    accounts = testAccounts,
                    categories = testCategories,
                    currencySymbol = "৳",
                    language = AppLanguage.ENGLISH,
                    onDismiss = {},
                    onDuplicate = {},
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_details_title")
            .assertIsDisplayed()
            .assertTextContains("Monthly Salary")
        composeTestRule.onNodeWithTag("tx_details_amount")
            .assertIsDisplayed()
            .assertTextContains("+ ৳45,000")
        composeTestRule.onNodeWithText("Income").assertIsDisplayed()

        // Wallet info
        composeTestRule.onNodeWithTag("tx_details_single_wallet_row").assertIsDisplayed()
        composeTestRule.onNodeWithText("EBL Salary Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Debit Card").assertIsDisplayed()

        // Category & Date/Time present
        composeTestRule.onNodeWithTag("tx_details_category_row").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Salary").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_details_datetime_row").performScrollTo().assertIsDisplayed()

        // Subcategory and Notes must NOT be rendered when blank
        composeTestRule.onNodeWithTag("tx_details_subcategory_row").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tx_details_notes_section").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tx_details_payee_row").assertDoesNotExist()
    }

    @Test
    fun transferTransaction_displaysBothSourceAndDestinationWalletsClearly() {
        val timestamp = 1793945340000L
        val transferTx = TransactionEntity(
            id = "tx_trf_1",
            type = TransactionType.TRANSFER,
            amount = 5000.0,
            title = "Top up bKash from Bank",
            category = "Transfer",
            subcategory = "",
            accountId = "acc_ebl_bank",
            toAccountId = "acc_bkash",
            timestamp = timestamp,
            notes = ""
        )

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                TransactionDetailsSheetContent(
                    transaction = transferTx,
                    accounts = testAccounts,
                    categories = testCategories,
                    currencySymbol = "৳",
                    language = AppLanguage.ENGLISH,
                    onDismiss = {},
                    onDuplicate = {},
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_details_title")
            .assertIsDisplayed()
            .assertTextContains("Top up bKash from Bank")
        composeTestRule.onNodeWithTag("tx_details_amount")
            .assertIsDisplayed()
            .assertTextContains("৳5,000")
        composeTestRule.onNodeWithText("Transfer").assertIsDisplayed()

        // Transfer wallets card must show BOTH source and destination wallets
        composeTestRule.onNodeWithTag("tx_details_transfer_wallets_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_details_source_wallet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_details_destination_wallet").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("EBL Salary Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("bKash Personal").assertIsDisplayed()

        // Redundant "Transfer" category row and empty notes/subcategory should not be shown
        composeTestRule.onNodeWithTag("tx_details_category_row").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tx_details_subcategory_row").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tx_details_notes_section").assertDoesNotExist()
    }

    @Test
    fun actions_duplicateEditAndDeleteWithConfirmationFlow_preserveBehaviorAndScrollOnCompactScreen() {
        val tx = TransactionEntity(
            id = "tx_action_test",
            type = TransactionType.EXPENSE,
            amount = 1250.0,
            title = "Grocery Run",
            category = "Food & Dining",
            subcategory = "Supermarket",
            accountId = "acc_test_cash",
            timestamp = 1793945340000L,
            notes = "Weekly vegetables, dairy, rice, cooking oil, and household supplies from the local market."
        )

        var duplicatedTx: TransactionEntity? = null
        var editedTx: TransactionEntity? = null
        var deletedTxId: String? = null
        var showSheet by mutableStateOf(true)
        var txPendingDelete by mutableStateOf<TransactionEntity?>(null)

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                // Constrain to a compact phone screen viewport (360dp x 520dp) to verify scrollability
                Box(modifier = Modifier.width(360.dp).height(520.dp)) {
                    if (showSheet) {
                        TransactionDetailsSheetContent(
                            transaction = tx,
                            accounts = testAccounts,
                            categories = testCategories,
                            currencySymbol = "৳",
                            language = AppLanguage.ENGLISH,
                            onDismiss = { showSheet = false },
                            onDuplicate = { target ->
                                showSheet = false
                                duplicatedTx = target
                            },
                            onEdit = { target ->
                                showSheet = false
                                editedTx = target
                            },
                            onDelete = { target ->
                                txPendingDelete = target
                                showSheet = false
                            }
                        )
                    }

                    txPendingDelete?.let { target ->
                        AlertDialog(
                            onDismissRequest = { txPendingDelete = null },
                            modifier = Modifier.testTag("tx_delete_confirm_dialog"),
                            title = { Text("Delete Transaction") },
                            text = { Text("Are you sure you want to permanently delete \"${target.title}\"?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        deletedTxId = target.id
                                        txPendingDelete = null
                                    },
                                    modifier = Modifier.testTag("tx_delete_confirm_button")
                                ) {
                                    Text("Delete", color = ExpenseRed)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { txPendingDelete = null },
                                    modifier = Modifier.testTag("tx_delete_cancel_button")
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }

        // Scroll to the action row at the bottom of the compact phone viewport
        composeTestRule.onNodeWithTag("tx_details_duplicate_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_details_edit_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_details_delete_button").assertIsDisplayed()

        // 1. Test Duplicate action
        composeTestRule.onNodeWithTag("tx_details_duplicate_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals("tx_action_test", duplicatedTx?.id)
        assertTrue(!showSheet)

        // Reopen sheet and test Edit action
        showSheet = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tx_details_edit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("tx_action_test", editedTx?.id)
        assertTrue(!showSheet)

        // Reopen sheet and test Delete -> Cancel confirmation flow
        showSheet = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tx_details_delete_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Delete confirmation dialog must appear BEFORE deleting
        assertNull("Transaction must not be deleted before user confirms", deletedTxId)
        composeTestRule.onNodeWithTag("tx_delete_confirm_dialog").assertIsDisplayed()

        // Cancel keeps transaction intact
        composeTestRule.onNodeWithTag("tx_delete_cancel_button").performClick()
        composeTestRule.waitForIdle()
        assertNull(deletedTxId)

        // Reopen sheet and test Delete -> Confirm flow
        showSheet = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tx_details_delete_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tx_delete_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_delete_confirm_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals("tx_action_test", deletedTxId)
    }
}
