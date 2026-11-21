package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.InitialData
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class WalletRepository(private val db: AppDatabase) {

    val accounts: Flow<List<AccountEntity>> = db.accountDao().getAllAccounts()
    val transactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()
    val categories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategories()
    val subcategories: Flow<List<SubcategoryEntity>> = db.categoryDao().getAllSubcategories()
    val budgets: Flow<List<BudgetEntity>> = db.budgetDao().getAllBudgets()
    val debts: Flow<List<DebtEntity>> = db.debtDao().getAllDebts()
    val savingsGoals: Flow<List<SavingsGoalEntity>> = db.savingsGoalDao().getAllGoals()
    val settings: Flow<AppSettingsEntity?> = db.settingsDao().getSettingsFlow()

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        val existingSettings = db.settingsDao().getSettings()
        if (existingSettings == null) {
            val (cats, subs) = InitialData.getDefaultCategories()
            db.categoryDao().insertCategories(cats)
            db.categoryDao().insertSubcategories(subs)

            db.settingsDao().saveSettings(
                AppSettingsEntity(
                    id = 1,
                    hasSampleDataLoaded = false
                )
            )
        }
    }

    suspend fun saveSettings(settings: AppSettingsEntity) = withContext(Dispatchers.IO) {
        db.settingsDao().saveSettings(settings)
    }

    suspend fun getSettings(): AppSettingsEntity {
        return withContext(Dispatchers.IO) {
            db.settingsDao().getSettings() ?: AppSettingsEntity()
        }
    }

    // Account Operations
    suspend fun insertAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        db.accountDao().insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        db.accountDao().updateAccount(account)
    }

    suspend fun deleteAccount(id: String) = withContext(Dispatchers.IO) {
        db.accountDao().deleteAccountById(id)
    }

    suspend fun reorderAccounts(orderedAccounts: List<AccountEntity>) = withContext(Dispatchers.IO) {
        orderedAccounts.forEachIndexed { index, account ->
            db.accountDao().updateAccount(account.copy(sortOrder = index))
        }
    }

    suspend fun adjustAccountBalance(
        accountId: String,
        newBalance: Double,
        recordAsTransaction: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val account = db.accountDao().getAccountById(accountId) ?: return@withContext
        val diff = newBalance - account.balance
        if (diff != 0.0) {
            if (recordAsTransaction) {
                val isIncome = diff > 0
                val tx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                    amount = kotlin.math.abs(diff),
                    title = "Balance Adjustment",
                    category = "Financial",
                    subcategory = "Adjustment",
                    accountId = accountId,
                    timestamp = System.currentTimeMillis(),
                    notes = "Adjusted from ${account.balance} to $newBalance"
                )
                db.transactionDao().insertTransaction(tx)
            }
            db.accountDao().updateAccount(account.copy(balance = newBalance, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun setAccountArchived(accountId: String, archived: Boolean) = withContext(Dispatchers.IO) {
        val account = db.accountDao().getAccountById(accountId) ?: return@withContext
        db.accountDao().updateAccount(account.copy(isHidden = archived, updatedAt = System.currentTimeMillis()))
    }

    // Transaction Operations
    suspend fun getTransactionById(id: String): TransactionEntity? = withContext(Dispatchers.IO) {
        db.transactionDao().getTransactionById(id)
    }

    suspend fun insertTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        db.transactionDao().insertTransaction(transaction)
        applyBalanceEffect(transaction, isUndo = false)
    }

    suspend fun deleteTransaction(id: String) = withContext(Dispatchers.IO) {
        val tx = db.transactionDao().getTransactionById(id) ?: return@withContext
        applyBalanceEffect(tx, isUndo = true)
        db.transactionDao().deleteTransactionById(id)
    }

    suspend fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) = withContext(Dispatchers.IO) {
        applyBalanceEffect(oldTx, isUndo = true)
        db.transactionDao().updateTransaction(newTx)
        applyBalanceEffect(newTx, isUndo = false)
    }

    private suspend fun applyBalanceEffect(tx: TransactionEntity, isUndo: Boolean) {
        val multiplier = if (isUndo) -1.0 else 1.0
        val fromAccount = db.accountDao().getAccountById(tx.accountId)

        when (tx.type) {
            TransactionType.EXPENSE, TransactionType.LEND -> {
                fromAccount?.let {
                    val updated = it.copy(balance = it.balance - (tx.amount * multiplier))
                    db.accountDao().updateAccount(updated)
                }
            }
            TransactionType.INCOME, TransactionType.BORROW -> {
                fromAccount?.let {
                    val updated = it.copy(balance = it.balance + (tx.amount * multiplier))
                    db.accountDao().updateAccount(updated)
                }
            }
            TransactionType.TRANSFER -> {
                fromAccount?.let {
                    val updatedFrom = it.copy(balance = it.balance - (tx.amount * multiplier))
                    db.accountDao().updateAccount(updatedFrom)
                }
                if (tx.toAccountId != null) {
                    val toAccount = db.accountDao().getAccountById(tx.toAccountId)
                    toAccount?.let {
                        val updatedTo = it.copy(balance = it.balance + (tx.amount * multiplier))
                        db.accountDao().updateAccount(updatedTo)
                    }
                }
            }
            TransactionType.CC_PAYMENT -> {
                fromAccount?.let {
                    val updatedFrom = it.copy(balance = it.balance - (tx.amount * multiplier))
                    db.accountDao().updateAccount(updatedFrom)
                }
                if (tx.toAccountId != null) {
                    val ccAccount = db.accountDao().getAccountById(tx.toAccountId)
                    ccAccount?.let {
                        val updatedCC = it.copy(balance = it.balance + (tx.amount * multiplier))
                        db.accountDao().updateAccount(updatedCC)
                    }
                }
            }
        }
    }

    // Budget Operations
    suspend fun insertBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        db.budgetDao().insertBudget(budget)
    }

    suspend fun deleteBudget(id: String) = withContext(Dispatchers.IO) {
        db.budgetDao().deleteBudgetById(id)
    }

    // Category Operations
    suspend fun insertCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        db.categoryDao().insertCategory(category)
    }

    suspend fun insertCategories(categories: List<CategoryEntity>) = withContext(Dispatchers.IO) {
        db.categoryDao().insertCategories(categories)
    }

    suspend fun deleteCategory(id: String) = withContext(Dispatchers.IO) {
        db.categoryDao().deleteCategoryById(id)
    }

    suspend fun insertSubcategory(sub: SubcategoryEntity) = withContext(Dispatchers.IO) {
        db.categoryDao().insertSubcategories(listOf(sub))
    }

    suspend fun insertSubcategories(subs: List<SubcategoryEntity>) = withContext(Dispatchers.IO) {
        db.categoryDao().insertSubcategories(subs)
    }

    suspend fun deleteSubcategory(id: String) = withContext(Dispatchers.IO) {
        db.categoryDao().deleteSubcategoryById(id)
    }

    suspend fun restoreDefaultCategories() = withContext(Dispatchers.IO) {
        val (defaultCats, defaultSubs) = com.example.data.local.InitialData.getDefaultCategories()
        db.categoryDao().deleteAllCategories()
        db.categoryDao().deleteAllSubcategories()
        db.categoryDao().insertCategories(defaultCats)
        db.categoryDao().insertSubcategories(defaultSubs)
    }

    // Debt Operations
    suspend fun insertDebt(debt: DebtEntity) = withContext(Dispatchers.IO) {
        db.debtDao().insertDebt(debt)
    }

    suspend fun updateDebt(debt: DebtEntity) = withContext(Dispatchers.IO) {
        db.debtDao().updateDebt(debt)
    }

    suspend fun deleteDebt(id: String) = withContext(Dispatchers.IO) {
        db.debtDao().deleteDebtById(id)
    }

    suspend fun getDebtById(id: String): DebtEntity? = withContext(Dispatchers.IO) {
        db.debtDao().getDebtById(id)
    }

    suspend fun getAllDebtsList(): List<DebtEntity> = withContext(Dispatchers.IO) {
        db.debtDao().getAllDebtsList()
    }

    suspend fun getTransactionsForDebt(debtId: String): List<TransactionEntity> = withContext(Dispatchers.IO) {
        db.transactionDao().getTransactionsForDebt(debtId)
    }

    // Savings Goal Operations
    suspend fun insertGoal(goal: SavingsGoalEntity) = withContext(Dispatchers.IO) {
        db.savingsGoalDao().insertGoal(goal)
    }

    suspend fun updateGoal(goal: SavingsGoalEntity) = withContext(Dispatchers.IO) {
        db.savingsGoalDao().updateGoal(goal)
    }

    suspend fun deleteGoal(id: String) = withContext(Dispatchers.IO) {
        db.savingsGoalDao().deleteGoalById(id)
    }

    // Erase all data & Reset to sample
    suspend fun eraseAllData() = withContext(Dispatchers.IO) {
        db.transactionDao().deleteAllTransactions()
        db.accountDao().deleteAllAccounts()
        db.budgetDao().deleteAllBudgets()
        db.debtDao().deleteAllDebts()
        db.savingsGoalDao().deleteAllGoals()
        val currentSettings = db.settingsDao().getSettings() ?: AppSettingsEntity()
        db.settingsDao().saveSettings(currentSettings.copy(hasSampleDataLoaded = false))
    }

    suspend fun resetToSampleData() = withContext(Dispatchers.IO) {
        eraseAllData()
        val (cats, subs) = InitialData.getDefaultCategories()
        db.categoryDao().insertCategories(cats)
        db.categoryDao().insertSubcategories(subs)
        val settings = db.settingsDao().getSettings() ?: AppSettingsEntity()
        db.settingsDao().saveSettings(settings.copy(hasSampleDataLoaded = false))
    }
}
