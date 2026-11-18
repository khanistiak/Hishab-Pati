package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AppLanguage
import com.example.data.model.BackgroundTextureOption
import com.example.data.model.ThemeColorOption
import com.example.data.model.ThemeMode

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val userName: String = "Guest User",
    val country: String = "Bangladesh",
    val currencyCode: String = "BDT",
    val currencySymbol: String = "৳",
    val secondaryCurrencyCode: String = "USD",
    val secondaryCurrencySymbol: String = "$",
    val profileAvatarType: String = "INITIALS",
    val profileAvatarColorHex: String = "#4F46E5",
    val profileAvatarSymbol: String = "person",
    val profilePhotoUri: String = "",
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val themeColor: ThemeColorOption = ThemeColorOption.SKY_CYAN,
    val backgroundTexture: BackgroundTextureOption = BackgroundTextureOption.AURORA,
    val appLockEnabled: Boolean = false,
    val screenshotProtectionEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val cloudSyncEmail: String = "",
    val lastSyncTimestamp: Long = 0L,
    val hasSampleDataLoaded: Boolean = false
)
