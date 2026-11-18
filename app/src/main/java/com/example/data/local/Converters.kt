package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.BackgroundTextureOption
import com.example.data.model.DebtType
import com.example.data.model.ThemeColorOption
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = try {
        AccountType.valueOf(value)
    } catch (_: Exception) {
        AccountType.OTHER
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (_: Exception) {
        TransactionType.EXPENSE
    }

    @TypeConverter
    fun fromDebtType(value: DebtType): String = value.name

    @TypeConverter
    fun toDebtType(value: String): DebtType = try {
        DebtType.valueOf(value)
    } catch (_: Exception) {
        DebtType.LENT
    }

    @TypeConverter
    fun fromAppLanguage(value: AppLanguage): String = value.name

    @TypeConverter
    fun toAppLanguage(value: String): AppLanguage = try {
        AppLanguage.valueOf(value)
    } catch (_: Exception) {
        AppLanguage.ENGLISH
    }

    @TypeConverter
    fun fromThemeMode(value: ThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String): ThemeMode = try {
        ThemeMode.valueOf(value)
    } catch (_: Exception) {
        ThemeMode.AUTO
    }

    @TypeConverter
    fun fromThemeColor(value: ThemeColorOption): String = value.name

    @TypeConverter
    fun toThemeColor(value: String): ThemeColorOption = try {
        ThemeColorOption.valueOf(value)
    } catch (_: Exception) {
        ThemeColorOption.SKY_CYAN
    }

    @TypeConverter
    fun fromBackgroundTexture(value: BackgroundTextureOption): String = value.name

    @TypeConverter
    fun toBackgroundTexture(value: String): BackgroundTextureOption = try {
        BackgroundTextureOption.valueOf(value)
    } catch (_: Exception) {
        BackgroundTextureOption.AURORA
    }
}
