package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AppLanguage

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val nameBn: String = "",
    val isIncome: Boolean = false,
    val isTransfer: Boolean = false,
    val isActive: Boolean = true,
    val iconName: String = "category",
    val colorHex: String = "#4F46E5",
    val sortOrder: Int = 0
) {
    fun getDisplayName(language: AppLanguage): String {
        return if (language == AppLanguage.BANGLA && nameBn.isNotBlank()) nameBn else name
    }
}

@Entity(tableName = "subcategories")
data class SubcategoryEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val name: String,
    val nameBn: String = "",
    val sortOrder: Int = 0
) {
    fun getDisplayName(language: AppLanguage): String {
        return if (language == AppLanguage.BANGLA && nameBn.isNotBlank()) nameBn else name
    }
}
