package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val title: String,
    val category: String = "Miscellaneous",
    val subcategory: String = "",
    val accountId: String,
    val toAccountId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isRecurring: Boolean = false,
    val payeeOrPayer: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val debtId: String? = null,
    val excludeFromBudgets: Boolean = false
)
