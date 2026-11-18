package com.example

import com.example.data.backup.FinancialIntegrityValidator
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.example.ui.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for Home Screen Summary Cards:
 * 1. Total Balance calculation honoring `includeInTotal` and hidden status.
 * 2. Spent This Month calculation using FinancialIntegrityValidator.
 * 3. Adding/updating transactions reactively updates monthly spending.
 * 4. Internal transfers and CC payments excluded from monthly spending.
 * 5. Consistent currency formatting across both cards.
 */
class HomeSummaryCardsTest {

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
}
