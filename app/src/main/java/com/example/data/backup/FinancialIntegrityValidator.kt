package com.example.data.backup

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class FinancialVerificationReport(
    val isValid: Boolean,
    val totalIncome: Double,
    val totalExpense: Double,
    val currentMonthIncome: Double,
    val currentMonthExpense: Double,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val categorySpendingCurrentMonth: Map<String, Double>,
    val errorDetails: String? = null
)

object FinancialIntegrityValidator {

    fun isInternalTransfer(tx: TransactionEntity): Boolean {
        return tx.type == TransactionType.TRANSFER ||
            tx.type == TransactionType.CC_PAYMENT ||
            tx.category.equals("Internal Transfer", ignoreCase = true) ||
            tx.category.equals("Transfer", ignoreCase = true)
    }

    fun isCreditCardPayment(tx: TransactionEntity): Boolean {
        if (tx.type == TransactionType.CC_PAYMENT) return true
        if (tx.category.equals("Credit Card Payment", ignoreCase = true)) return true
        if (tx.subcategory.equals("Credit Card Payment", ignoreCase = true)) return true
        if (tx.type == TransactionType.EXPENSE && tx.title.contains("Credit Card Bill", ignoreCase = true)) {
            return true
        }
        return false
    }

    fun isExcludedFromBudgetAndExpense(tx: TransactionEntity): Boolean {
        if (tx.type != TransactionType.EXPENSE) return true
        if (isInternalTransfer(tx)) return true
        if (isCreditCardPayment(tx)) return true
        if (tx.category.equals("Debt Repayment", ignoreCase = true)) return true
        if (tx.category.equals("Lend Money", ignoreCase = true)) return true
        if (tx.category.equals("Personal", ignoreCase = true) && tx.subcategory.equals("Lend Money", ignoreCase = true)) return true
        if (tx.category.equals("Adjustment", ignoreCase = true)) return true
        if (tx.subcategory.equals("Adjustment", ignoreCase = true)) return true
        if (tx.subcategory.equals("Reconciliation", ignoreCase = true)) return true
        if (tx.excludeFromBudgets) return true
        if (tx.notes.contains("[EXCLUDE_BUDGET]", ignoreCase = true)) return true
        return false
    }

    fun isEligibleExpense(tx: TransactionEntity): Boolean {
        return !isExcludedFromBudgetAndExpense(tx)
    }

    fun isMatchingBudgetTransaction(tx: TransactionEntity, budget: BudgetEntity): Boolean {
        if (!isEligibleExpense(tx)) return false
        val assignedIds = tx.parsedAssignedBudgetIds()
        if (budget.isManual) {
            return assignedIds.contains(budget.id)
        }
        if (assignedIds.contains(budget.id)) return true
        if (budget.isOverall || budget.categoryName.equals("Overall", ignoreCase = true)) return true

        val txCat = tx.category.trim()
        val txSub = tx.subcategory.trim()
        val multiCategories = budget.parsedCategories()
        val multiSubPairs = budget.parsedSubcategoryPairs()

        if (multiSubPairs.isNotEmpty()) {
            // Match if exact (category, subcategory) pair is selected
            val matchesPair = multiSubPairs.any { (cat, sub) ->
                txCat.equals(cat, ignoreCase = true) && txSub.equals(sub, ignoreCase = true)
            }
            if (matchesPair) return true
            // Also match if tx's category is selected as a whole category (i.e., in multiCategories with no sub-filters for that category)
            val categoriesWithSubFilters = multiSubPairs.map { it.first.lowercase(Locale.US) }.toSet()
            val matchesWholeCategory = multiCategories.any { cat ->
                txCat.equals(cat, ignoreCase = true) && !categoriesWithSubFilters.contains(cat.lowercase(Locale.US))
            }
            return matchesWholeCategory
        }

        if (multiCategories.isNotEmpty()) {
            return multiCategories.any { cat -> txCat.equals(cat, ignoreCase = true) }
        }

        val catMatches = txCat.equals(budget.categoryName.trim(), ignoreCase = true)
        if (!catMatches) return false
        return budget.subcategoryName.isBlank() ||
            txSub.equals(budget.subcategoryName.trim(), ignoreCase = true)
    }

    fun isEligibleIncome(tx: TransactionEntity): Boolean {
        if (tx.type != TransactionType.INCOME) return false
        if (isInternalTransfer(tx)) return false
        if (tx.category.equals("Loan Received", ignoreCase = true)) return false
        if (tx.category.equals("Adjustment", ignoreCase = true)) return false
        if (tx.subcategory.equals("Adjustment", ignoreCase = true)) return false
        if (tx.subcategory.equals("Reconciliation", ignoreCase = true)) return false
        if (tx.title.equals("Account Adjustment", ignoreCase = true)) return false
        if (tx.title.equals("Cash adjustment", ignoreCase = true)) return false
        return true
    }

    fun isInYearMonth(timestamp: Long, year: Int, monthZeroBased: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == monthZeroBased
    }

    fun getYearMonthKey(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun resolveReferenceYearMonth(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>
    ): Pair<Int, Int> {
        val nowCal = Calendar.getInstance()
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowMonth = nowCal.get(Calendar.MONTH)
        val hasCurrentMonthTx = transactions.any { isInYearMonth(it.timestamp, nowYear, nowMonth) }
        if (hasCurrentMonthTx) {
            return nowYear to nowMonth
        }
        val budgetMonth = budgets.firstOrNull { it.monthYear.matches(Regex("\\d{4}-\\d{2}")) }?.monthYear
        if (budgetMonth != null) {
            val parts = budgetMonth.split("-")
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            if (y != null && m != null && m in 1..12) {
                return y to (m - 1)
            }
        }
        val latestTx = transactions.maxByOrNull { it.timestamp }
        if (latestTx != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = latestTx.timestamp }
            return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
        }
        return nowYear to nowMonth
    }

    /**
     * Calculates total monthly budget limit with parent/child double-counting prevention:
     * - If a category has subcategory budgets, sum their limits as the category limit.
     * - If no subcategory budgets exist for that category, use the parent category budget limit.
     * - Overall budget acts as fallback when no category/subcategory budgets exist.
     */
    fun computeTotalMonthlyBudget(budgets: List<BudgetEntity>, monthKey: String): Double {
        val monthBudgets = budgets.filter { it.monthYear == monthKey || it.monthYear == "ALL" }
        val overallBudgetEntity = monthBudgets.firstOrNull { it.isOverall }
        val nonOverallBudgets = monthBudgets.filter { !it.isOverall && !it.categoryName.equals("Overall", ignoreCase = true) }

        // Manual & multi-category custom budgets each contribute their own limit cleanly
        val manualAndCustomBudgets = nonOverallBudgets.filter {
            it.isManual || it.parsedCategories().size > 1 || it.parsedSubcategoryPairs().size > 1
        }
        val standardSingleCatBudgets = nonOverallBudgets.filter {
            !it.isManual && it.parsedCategories().size <= 1 && it.parsedSubcategoryPairs().size <= 1
        }

        val budgetsByCategory = standardSingleCatBudgets.groupBy { it.categoryName }
        val applicableStandardSum = budgetsByCategory.values.sumOf { catBudgets ->
            val subBudgets = catBudgets.filter { it.subcategoryName.isNotBlank() || it.parsedSubcategoryPairs().isNotEmpty() }
            if (subBudgets.isNotEmpty()) {
                subBudgets.sumOf { it.monthlyLimit }
            } else {
                catBudgets.sumOf { it.monthlyLimit }
            }
        }
        val applicableCategoryLimitsSum = applicableStandardSum + manualAndCustomBudgets.sumOf { it.monthlyLimit }
        return if (overallBudgetEntity != null && overallBudgetEntity.monthlyLimit > 0.0 && applicableCategoryLimitsSum == 0.0) {
            overallBudgetEntity.monthlyLimit
        } else if (applicableCategoryLimitsSum > 0.0) {
            applicableCategoryLimitsSum
        } else {
            overallBudgetEntity?.monthlyLimit ?: 0.0
        }
    }

    fun computeSingleTxBalanceDelta(tx: TransactionEntity, accountId: String): Double {
        var delta = 0.0
        if (tx.accountId == accountId) {
            when (tx.type) {
                TransactionType.EXPENSE, TransactionType.LEND,
                TransactionType.TRANSFER, TransactionType.CC_PAYMENT -> {
                    delta -= tx.amount
                }
                TransactionType.INCOME, TransactionType.BORROW -> {
                    delta += tx.amount
                }
            }
        }
        if (tx.toAccountId == accountId &&
            (tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT)
        ) {
            delta += tx.amount
        }
        return delta
    }

    fun verifyRestoredData(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        debts: List<DebtEntity>
    ): FinancialVerificationReport {
        val accountMap = accounts.associateBy { it.id }

        // Check referential integrity and financial sanity
        for (tx in transactions) {
            if (tx.amount.isNaN() || tx.amount.isInfinite() || tx.amount < 0.0) {
                return FinancialVerificationReport(
                    isValid = false,
                    totalIncome = 0.0,
                    totalExpense = 0.0,
                    currentMonthIncome = 0.0,
                    currentMonthExpense = 0.0,
                    totalAssets = 0.0,
                    totalLiabilities = 0.0,
                    netWorth = 0.0,
                    categorySpendingCurrentMonth = emptyMap(),
                    errorDetails = "Invalid transaction amount for transaction ID '${tx.id}'"
                )
            }
            if (tx.accountId.isBlank() || !accountMap.containsKey(tx.accountId)) {
                return FinancialVerificationReport(
                    isValid = false,
                    totalIncome = 0.0,
                    totalExpense = 0.0,
                    currentMonthIncome = 0.0,
                    currentMonthExpense = 0.0,
                    totalAssets = 0.0,
                    totalLiabilities = 0.0,
                    netWorth = 0.0,
                    categorySpendingCurrentMonth = emptyMap(),
                    errorDetails = "Missing account reference '${tx.accountId}' on transaction '${tx.id}'"
                )
            }
            if ((tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT) &&
                !tx.toAccountId.isNullOrBlank() &&
                !accountMap.containsKey(tx.toAccountId)
            ) {
                return FinancialVerificationReport(
                    isValid = false,
                    totalIncome = 0.0,
                    totalExpense = 0.0,
                    currentMonthIncome = 0.0,
                    currentMonthExpense = 0.0,
                    totalAssets = 0.0,
                    totalLiabilities = 0.0,
                    netWorth = 0.0,
                    categorySpendingCurrentMonth = emptyMap(),
                    errorDetails = "Missing transfer destination account '${tx.toAccountId}' on transaction '${tx.id}'"
                )
            }
        }

        for (acc in accounts) {
            if (acc.balance.isNaN() || acc.balance.isInfinite()) {
                return FinancialVerificationReport(
                    isValid = false,
                    totalIncome = 0.0,
                    totalExpense = 0.0,
                    currentMonthIncome = 0.0,
                    currentMonthExpense = 0.0,
                    totalAssets = 0.0,
                    totalLiabilities = 0.0,
                    netWorth = 0.0,
                    categorySpendingCurrentMonth = emptyMap(),
                    errorDetails = "Invalid balance on account '${acc.id}'"
                )
            }
        }

        for (debt in debts) {
            if (debt.amount.isNaN() || debt.amount.isInfinite() || debt.amount < 0.0) {
                return FinancialVerificationReport(
                    isValid = false,
                    totalIncome = 0.0,
                    totalExpense = 0.0,
                    currentMonthIncome = 0.0,
                    currentMonthExpense = 0.0,
                    totalAssets = 0.0,
                    totalLiabilities = 0.0,
                    netWorth = 0.0,
                    categorySpendingCurrentMonth = emptyMap(),
                    errorDetails = "Invalid amount on debt '${debt.id}'"
                )
            }
        }

        val totalIncome = transactions.filter { isEligibleIncome(it) }.sumOf { it.amount }
        val totalExpense = transactions.filter { isEligibleExpense(it) }.sumOf { it.amount }

        val (refYear, refMonth) = resolveReferenceYearMonth(transactions, budgets)
        val monthTxs = transactions.filter { isInYearMonth(it.timestamp, refYear, refMonth) }
        val currentMonthIncome = monthTxs.filter { isEligibleIncome(it) }.sumOf { it.amount }
        val currentMonthExpense = monthTxs.filter { isEligibleExpense(it) }.sumOf { it.amount }

        val categorySpending = monthTxs
            .filter { isEligibleExpense(it) }
            .groupBy { it.category.ifBlank { "Miscellaneous" } }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        var totalAssets = 0.0
        var totalLiabilities = 0.0
        for (acc in accounts) {
            if (acc.type == AccountType.CREDIT_CARD) {
                if (acc.balance < 0) {
                    totalLiabilities += abs(acc.balance)
                } else {
                    totalAssets += acc.balance
                }
            } else {
                if (acc.balance >= 0) {
                    totalAssets += acc.balance
                } else {
                    totalLiabilities += abs(acc.balance)
                }
            }
        }

        return FinancialVerificationReport(
            isValid = true,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            currentMonthIncome = currentMonthIncome,
            currentMonthExpense = currentMonthExpense,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = totalAssets - totalLiabilities,
            categorySpendingCurrentMonth = categorySpending
        )
    }
}
