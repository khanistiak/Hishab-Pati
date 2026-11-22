package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DebtType

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val categoryName: String,
    val monthlyLimit: Double,
    val monthYear: String, // e.g. "2026-10" or "ALL"
    val isOverall: Boolean = false,
    val rolloverEnabled: Boolean = false,
    val name: String = "",
    val subcategoryName: String = "",
    val iconName: String = "",
    val colorHex: String = "",
    val alertPercentage: Int = 80,
    val isManual: Boolean = false,
    val recurrence: String = "MONTHLY", // "MONTHLY", "WEEKLY", "YEARLY", "ONE_TIME"
    val startDateMillis: Long = 0L,
    val description: String = "",
    val selectedCategories: String = "", // pipe-separated category names for multi-category automatic budgets
    val selectedSubcategories: String = "" // pipe-separated "Category::Subcategory" entries
) {
    fun parsedCategories(): List<String> {
        val fromMulti = selectedCategories
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (fromMulti.isNotEmpty()) return fromMulti.distinct()
        return if (categoryName.isNotBlank() && !isOverall && !categoryName.equals("Overall", ignoreCase = true) && !categoryName.equals("Manual", ignoreCase = true)) {
            listOf(categoryName.trim())
        } else {
            emptyList()
        }
    }

    fun parsedSubcategoryPairs(): List<Pair<String, String>> {
        val fromMulti = selectedSubcategories
            .split("|")
            .mapNotNull { entry ->
                val trimmed = entry.trim()
                val sepIdx = trimmed.indexOf("::")
                if (sepIdx > 0 && sepIdx < trimmed.length - 2) {
                    val cat = trimmed.substring(0, sepIdx).trim()
                    val sub = trimmed.substring(sepIdx + 2).trim()
                    if (cat.isNotEmpty() && sub.isNotEmpty()) cat to sub else null
                } else null
            }
        if (fromMulti.isNotEmpty()) return fromMulti.distinct()
        return if (subcategoryName.isNotBlank() && categoryName.isNotBlank()) {
            listOf(categoryName.trim() to subcategoryName.trim())
        } else {
            emptyList()
        }
    }
}

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey
    val id: String,
    val personName: String,
    val amount: Double, // remaining amount
    val type: DebtType, // BORROWED (Payable) or LENT (Receivable)
    val dueDate: String = "",
    val notes: String = "",
    val isSettled: Boolean = false,
    val accountId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdDateStr: String = "",
    val totalAmount: Double = amount,
    val paidAmount: Double = 0.0,
    val reason: String = "",
    val colorHex: String = "#3B82F6",
    val tentativeCloseDate: Long? = null,
    val tentativeCloseDateStr: String = "",
    val tags: String = "",
    val includeInBalance: Boolean = true,
    val bindToWallet: Boolean = false
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey
    val id: String = "goal_${System.currentTimeMillis()}",
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val themeColorHex: String = "#F59E0B",
    val iconName: String = "savings",
    val targetDeadline: Long? = null,
    val targetDeadlineStr: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

data class SavingsGoalContribution(
    val id: String,
    val goalId: String,
    val amount: Double,
    val isWithdrawal: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val walletId: String = "",
    val walletName: String = ""
)

