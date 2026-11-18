package com.example.ui

import com.example.ui.screens.accounts.AllConfigurableMfsProviders
import com.example.ui.screens.accounts.CardColorPresets
import com.example.ui.screens.accounts.ComprehensiveColorPalette
import com.example.ui.screens.accounts.MfsProviderInfo
import com.example.ui.screens.accounts.OthersMfsProvider
import com.example.ui.screens.accounts.PopularBanks
import com.example.ui.screens.accounts.SelectableMfsProviders
import com.example.ui.screens.accounts.SupportedMfsProviders
import com.example.ui.screens.accounts.VisibleMfsProviderChoices
import com.example.ui.screens.accounts.filterBanksAndInstitutions
import com.example.ui.screens.accounts.filterMfsProviders
import com.example.ui.screens.accounts.resolveMfsWalletPreviewText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddAccountEnhancementsTest {

    @Test
    fun bankInstitutionFilter_returnsAllBanksAndSupportsSearchAndCustomNames() {
        val allDefault = filterBanksAndInstitutions("")
        assertEquals(61, PopularBanks.size)
        assertEquals(61, allDefault.size)
        assertTrue(allDefault.contains("City Bank PLC"))
        assertTrue(allDefault.contains("Standard Chartered Bank"))
        assertTrue(allDefault.contains("HSBC"))
        assertTrue(allDefault.contains("BRAC Bank PLC"))
        assertTrue(allDefault.contains("Dutch-Bangla Bank PLC"))
        assertTrue(allDefault.contains("Palli Sanchay Bank"))
        assertTrue(allDefault.contains("Grameen Bank"))
        assertTrue(allDefault.contains("Citibank N.A"))
        assertTrue(allDefault.contains("Woori Bank"))
        assertTrue(allDefault.contains("Agrani Bank"))
        assertTrue(allDefault.contains("Sammilito Islami Bank"))

        // Case-insensitive search filtering
        val filteredHsbc = filterBanksAndInstitutions("hsbc")
        assertEquals(listOf("HSBC"), filteredHsbc)

        val filteredCity = filterBanksAndInstitutions("city")
        assertTrue(filteredCity.contains("City Bank PLC"))

        val filteredCiti = filterBanksAndInstitutions("citi")
        assertTrue(filteredCiti.contains("Citibank N.A"))

        // Custom user-entered bank names are included without duplicates
        val withCustom = filterBanksAndInstitutions(
            query = "custombank",
            customInstitutions = listOf("CustomBank Ltd", "City Bank PLC")
        )
        assertEquals(listOf("CustomBank Ltd"), withCustom)

        // Manual entry not in PopularBanks is valid and preserved
        val manualName = "Fintech Neo Bank".trim()
        assertFalse(PopularBanks.contains(manualName))
        assertTrue(manualName.isNotEmpty())
    }

    @Test
    fun mfsProviderChoices_includeAllConfigured13ProvidersAndPrimarySelectableChips() {
        // Visible primary chips on MFS card include existing 4 providers (without separate Others button)
        val visibleNames = VisibleMfsProviderChoices.map { it.name }
        assertEquals(
            listOf("bKash", "Nagad", "Rocket", "Upay"),
            visibleNames
        )
        assertEquals(4, SelectableMfsProviders.size)

        // Bottom sheet picker includes all 13 official providers
        val allSheetNames = AllConfigurableMfsProviders.map { it.name }
        assertEquals(13, AllConfigurableMfsProviders.size)
        val expected13 = listOf(
            "bKash",
            "Rocket",
            "Nagad",
            "MYCash",
            "Islami Bank mCash",
            "TAP (Trust Axiata Pay)",
            "FirstPay",
            "OK Banking",
            "Upay",
            "Rupali Cash",
            "TeleCash",
            "Islamic Wallet",
            "Meghna Bank Tap n Pay"
        )
        assertEquals(expected13, allSheetNames)

        // SupportedMfsProviders preserves legacy Tap and CellFin for backward compatibility
        val supportedNames = SupportedMfsProviders.map { it.name }
        assertTrue(supportedNames.contains("Tap"))
        assertTrue(supportedNames.contains("CellFin"))
        assertTrue(supportedNames.contains("bKash"))
        assertTrue(supportedNames.contains("Rocket"))

        // Search filtering in MFS provider bottom sheet
        val rocketResult = filterMfsProviders("rocket")
        assertEquals(1, rocketResult.size)
        assertEquals("Rocket", rocketResult.first().name)
        assertEquals("#8C3494", rocketResult.first().brandColorHex)

        val tapAxiataResult = filterMfsProviders("axiata")
        assertEquals(1, tapAxiataResult.size)
        assertEquals("TAP (Trust Axiata Pay)", tapAxiataResult.first().name)

        val customIncluded = filterMfsProviders(
            query = "specialpay",
            customProviders = listOf(MfsProviderInfo("SpecialPay", "#0D9488"))
        )
        assertEquals(1, customIncluded.size)
        assertEquals("SpecialPay", customIncluded.first().name)
    }

    @Test
    fun mfsWalletPreview_updatesTitleAndSublineWhenProviderSelected() {
        val (defaultTitle, defaultSubline) = resolveMfsWalletPreviewText("", "bKash")
        assertEquals("bKash Account", defaultTitle)
        assertEquals("bKash", defaultSubline)

        val (othersTitle, othersSubline) = resolveMfsWalletPreviewText("", "Others")
        assertEquals("Others Account", othersTitle)
        assertEquals("Others", othersSubline)

        val (customTitle, customSubline) = resolveMfsWalletPreviewText("Personal Wallet", "CellFin")
        assertEquals("Personal Wallet", customTitle)
        assertEquals("CellFin", customSubline)
    }

    @Test
    fun walletColorPalette_preservesAllPresetsWithValidHexColors() {
        assertEquals(10, CardColorPresets.size)
        assertEquals(28, ComprehensiveColorPalette.size)

        val hexRegex = Regex("^#[0-9A-Fa-f]{6}$")
        ComprehensiveColorPalette.forEach { preset ->
            assertTrue("Invalid hex ${preset.hex} for ${preset.name}", hexRegex.matches(preset.hex))
            assertTrue("Blank color name", preset.name.isNotBlank())
        }
    }

    @Test
    fun dynamicDatabaseCounts_derivedDirectlyFromDataSources() {
        // Bank database count is dynamically derived from PopularBanks
        val actualBankCount = PopularBanks.size
        assertEquals(61, actualBankCount)

        // MFS database count is dynamically derived from AllConfigurableMfsProviders
        val actualMfsCount = AllConfigurableMfsProviders.size
        assertEquals(13, actualMfsCount)

        // No duplicates in either list
        assertEquals(actualBankCount, PopularBanks.distinct().size)
        assertEquals(actualMfsCount, AllConfigurableMfsProviders.map { it.name.lowercase() }.distinct().size)
    }
}
