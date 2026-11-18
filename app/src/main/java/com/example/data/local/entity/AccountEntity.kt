package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: AccountType,
    val institution: String = "",
    val balance: Double = 0.0,
    val openingBalance: Double = 0.0,
    val accountNumberLast4: String = "",
    val colorHex: String = "#4F46E5",
    val iconName: String = "account_balance",
    val isHidden: Boolean = false,
    val sortOrder: Int = 0,
    val creditLimit: Double = 0.0,
    val billingDate: String = "",
    val paymentDueDate: String = "",
    val minPayment: Double = 0.0,
    val notes: String = "",
    val includeInTotal: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
