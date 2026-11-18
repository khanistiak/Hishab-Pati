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
    val alertPercentage: Int = 80
)

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
