package com.example.ui

import androidx.compose.ui.graphics.Color
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeColorOption
import com.example.ui.screens.settings.SettingsAndAboutSpec
import com.example.ui.screens.settings.SettingsRowItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAndLanguageTest {

    @Test
    fun settingsRows_areInExactRequiredOrder_withAccountsAndAboutNavigation() {
        val expectedOrder = listOf(
            SettingsRowItem.NOTIFICATIONS,
            SettingsRowItem.THEME,
            SettingsRowItem.LANGUAGE,
            SettingsRowItem.DEFAULT_CURRENCY,
            SettingsRowItem.CATEGORIES,
            SettingsRowItem.ACCOUNTS,
            SettingsRowItem.BACKUP_RESTORE,
            SettingsRowItem.ABOUT
        )
        assertEquals(expectedOrder, SettingsAndAboutSpec.orderedRows)

        val englishTitles = SettingsAndAboutSpec.orderedRows.map {
            SettingsAndAboutSpec.rowTitle(it, AppLanguage.ENGLISH)
        }
        assertEquals(
            listOf(
                "Notifications",
                "Theme",
                "Language",
                "Default currency",
                "Categories",
                "Accounts",
                "Backup & Restore",
                "About"
            ),
            englishTitles
        )

        val banglaTitles = SettingsAndAboutSpec.orderedRows.map {
            SettingsAndAboutSpec.rowTitle(it, AppLanguage.BANGLA)
        }
        assertEquals(
            listOf(
                "বিজ্ঞপ্তি",
                "থিম",
                "ভাষা",
                "ডিফল্ট মুদ্রা",
                "ক্যাটাগরি",
                "অ্যাকাউন্টসমূহ",
                "ব্যাকআপ ও রিস্টোর",
                "সম্পর্কে"
            ),
            banglaTitles
        )

        // Verify navigation targets for each Settings row
        assertNull(SettingsRowItem.NOTIFICATIONS.targetSubScreen)
        assertEquals(SubScreen.THEME_SETTINGS, SettingsRowItem.THEME.targetSubScreen)
        assertNull(SettingsRowItem.LANGUAGE.targetSubScreen)
        assertEquals(SubScreen.CURRENCY_SELECTION, SettingsRowItem.DEFAULT_CURRENCY.targetSubScreen)
        assertEquals(SubScreen.CATEGORIES_MANAGEMENT, SettingsRowItem.CATEGORIES.targetSubScreen)
        assertEquals(SubScreen.ACCOUNTS_MANAGEMENT, SettingsRowItem.ACCOUNTS.targetSubScreen)
        assertEquals(SubScreen.BACKUP_RESTORE, SettingsRowItem.BACKUP_RESTORE.targetSubScreen)
        assertEquals(SubScreen.ABOUT, SettingsRowItem.ABOUT.targetSubScreen)
    }

    @Test
    fun aboutAppName_usesEnglishSourceName_andLocalizesWhenBangla_withoutSecondarySubtitle() {
        assertEquals("Hishab Pati", SettingsAndAboutSpec.aboutAppName(AppLanguage.ENGLISH))
        assertEquals("হিসাব পাতি", SettingsAndAboutSpec.aboutAppName(AppLanguage.BANGLA))
        assertNull(SettingsAndAboutSpec.aboutSecondarySubtitle())
    }

    @Test
    fun languagePopupOptions_highlightSelectedLanguage_andStyleConsistentlyInLightAndDarkThemes() {
        val englishSelected = SettingsAndAboutSpec.languagePopupOptions(AppLanguage.ENGLISH)
        assertEquals(2, englishSelected.size)
        assertEquals("English", englishSelected[0].title)
        assertTrue(englishSelected[0].isSelected)
        assertEquals("বাংলা (Bangla)", englishSelected[1].title)
        assertFalse(englishSelected[1].isSelected)

        val banglaSelected = SettingsAndAboutSpec.languagePopupOptions(AppLanguage.BANGLA)
        assertEquals(2, banglaSelected.size)
        assertFalse(banglaSelected[0].isSelected)
        assertTrue(banglaSelected[1].isSelected)

        assertEquals(28f, SettingsAndAboutSpec.DIALOG_CORNER_RADIUS.value, 0.001f)
        assertEquals(16f, SettingsAndAboutSpec.OPTION_CORNER_RADIUS.value, 0.001f)
        assertEquals(1.75f, SettingsAndAboutSpec.SELECTED_BORDER_WIDTH.value, 0.001f)
        assertEquals(1.0f, SettingsAndAboutSpec.UNSELECTED_BORDER_WIDTH.value, 0.001f)

        val primary = Color(ThemeColorOption.SKY_CYAN.primaryHex)
        val outline = Color(0xFFCBD5E1)
        val surfaceVariant = Color(0xFFF1F5F9)

        assertEquals(
            primary,
            SettingsAndAboutSpec.optionBorderColor(primary, outline, isSelected = true, isDark = false)
        )
        assertEquals(
            primary,
            SettingsAndAboutSpec.optionBorderColor(primary, outline, isSelected = true, isDark = true)
        )

        val lightSelectedBg = SettingsAndAboutSpec.optionContainerColor(primary, surfaceVariant, isSelected = true, isDark = false)
        val darkSelectedBg = SettingsAndAboutSpec.optionContainerColor(primary, surfaceVariant, isSelected = true, isDark = true)
        assertEquals(0.10f, lightSelectedBg.alpha, 0.005f)
        assertEquals(0.22f, darkSelectedBg.alpha, 0.005f)
    }
}
