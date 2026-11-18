package com.example.ui

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.example.ui.screens.accounts.AccountTxFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDetailScreenTest {

    private val account = AccountEntity(
        id = "ebl_salary",
        name = "EBL Salary Account",
        type = AccountType.BANK,
        institution = "Eastern Bank PLC",
        balance = 10030.05,
        accountNumberLast4 = "5432",
        notes = "Subtype:Debit Card|Network:Mastercard",
        colorHex = "#2563EB"
    )

    private val transactions = listOf(
        TransactionEntity(
            id = "tx1",
            accountId = "ebl_salary",
            toAccountId = null,
            amount = 6000.0,
            type = TransactionType.EXPENSE,
            category = "Medicine",
            subcategory = "",
            title = "Upanib",
            timestamp = 1730000000000L
        ),
        TransactionEntity(
            id = "tx2",
            accountId = "other_acc",
            toAccountId = "ebl_salary",
            amount = 10000.0,
            type = TransactionType.TRANSFER,
            category = "Transfer",
            subcategory = "",
            title = "Received",
            timestamp = 1729950000000L
        ),
        TransactionEntity(
            id = "tx3",
            accountId = "ebl_salary",
            toAccountId = "other_acc",
            amount = 10000.0,
            type = TransactionType.TRANSFER,
            category = "Transfer",
            subcategory = "",
            title = "Payment",
            timestamp = 1729850000000L
        ),
        TransactionEntity(
            id = "tx4",
            accountId = "ebl_salary",
            toAccountId = null,
            amount = 10000.0,
            type = TransactionType.INCOME,
            category = "Salary",
            subcategory = "",
            title = "Paid",
            timestamp = 1729750000000L
        )
    )

    @Test
    fun accountSummary_calculatesIncomeAndExpenseCorrectlyWithoutTxnsBox() {
        val accountTxs = transactions.filter { it.accountId == account.id || it.toAccountId == account.id }
        assertEquals(4, accountTxs.size)

        val totalIncome = accountTxs.filter {
            it.type == TransactionType.INCOME ||
            (it.type == TransactionType.TRANSFER && it.toAccountId == account.id) ||
            (it.type == TransactionType.CC_PAYMENT && it.toAccountId == account.id)
        }.sumOf { it.amount }

        val totalExpense = accountTxs.filter {
            it.type == TransactionType.EXPENSE ||
            (it.type == TransactionType.TRANSFER && it.accountId == account.id)
        }.sumOf { it.amount }

        // Income: tx2 (10000 transfer in) + tx4 (10000 income) = 20000
        assertEquals(20000.0, totalIncome, 0.001)

        // Expense: tx1 (6000 expense) + tx3 (10000 transfer out) = 16000
        assertEquals(16000.0, totalExpense, 0.001)
    }

    @Test
    fun transactionFilters_filterTransactionsAccurately() {
        val accountTxs = transactions.filter { it.accountId == account.id || it.toAccountId == account.id }

        val all = accountTxs
        assertEquals(4, all.size)

        val incomeOnly = accountTxs.filter {
            it.type == TransactionType.INCOME || (it.type == TransactionType.TRANSFER && it.toAccountId == account.id)
        }
        assertEquals(2, incomeOnly.size)

        val expensesOnly = accountTxs.filter {
            it.type == TransactionType.EXPENSE || (it.type == TransactionType.TRANSFER && it.accountId == account.id)
        }
        assertEquals(2, expensesOnly.size)

        val transfersOnly = accountTxs.filter { it.type == TransactionType.TRANSFER }
        assertEquals(2, transfersOnly.size)
    }

    @Test
    fun accountFilters_enumLabelsAreConsistent() {
        assertEquals("All", AccountTxFilter.ALL.label)
        assertEquals("Income", AccountTxFilter.INCOME.label)
        assertEquals("Expenses", AccountTxFilter.EXPENSES.label)
        assertEquals("Transfers", AccountTxFilter.TRANSFERS.label)
    }

    @Test
    fun accountSummary_showsFullAmountWithoutAbbreviations() {
        val totalIncome = 1300000.0
        val totalExpense = 1030000.0
        val currencySymbol = "৳"

        val formattedIncome = com.example.ui.util.CurrencyFormatter.format(totalIncome, currencySymbol)
        val formattedExpense = com.example.ui.util.CurrencyFormatter.format(totalExpense, currencySymbol)

        // Verifies no "M" or "million" or "K" abbreviations
        assertTrue(formattedIncome.contains("13,00,000") || formattedIncome.contains("1,300,000"))
        assertTrue(formattedExpense.contains("10,30,000") || formattedExpense.contains("1,030,000"))
        org.junit.Assert.assertFalse(formattedIncome.contains("M"))
        org.junit.Assert.assertFalse(formattedExpense.contains("M"))
    }
}
