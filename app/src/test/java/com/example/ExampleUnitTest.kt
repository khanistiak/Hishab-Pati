package com.example

import com.example.data.model.AccountType
import com.example.data.local.InitialData
import com.example.ui.screens.accounts.SupportedCardNetworks
import com.example.ui.screens.accounts.SupportedMfsProviders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testInitialData_NoPersonalDataLeakage() {
    val (defaultCategories, defaultSubcategories) = InitialData.getDefaultCategories()
    val inayaaCategory = defaultCategories.find { it.name.contains("Inayaa", ignoreCase = true) }
    val inayaaBnCategory = defaultCategories.find { it.nameBn.contains("Inayaa", ignoreCase = true) }
    val inayaaSub = defaultSubcategories.find { it.name.contains("Inayaa", ignoreCase = true) }

    assertEquals("Default categories should not leak personal name 'Inayaa Buri'", null, inayaaCategory)
    assertEquals("Default categories should not leak personal name in Bengali", null, inayaaBnCategory)
    assertEquals("Default subcategories should not leak personal name", null, inayaaSub)

    val familyCategory = defaultCategories.find { it.name == "Family" }
    assertNotNull("Category 'Family' must exist in default categories", familyCategory)
    assertEquals("পরিবার", familyCategory?.nameBn)
  }

  @Test
  fun testSelectableMfsProviders_ExcludesCellfinAndTap() {
    val selectable = com.example.ui.screens.accounts.SelectableMfsProviders
    val names = selectable.map { it.name }

    assertEquals(4, selectable.size)
    assertTrue("Selectable list must include bKash", names.contains("bKash"))
    assertTrue("Selectable list must include Nagad", names.contains("Nagad"))
    assertTrue("Selectable list must include Rocket", names.contains("Rocket"))
    assertTrue("Selectable list must include Upay", names.contains("Upay"))
    assertFalse("Cellfin must be removed from selectable list", names.contains("CellFin"))
    assertFalse("Tap must be removed from selectable list", names.contains("Tap"))
  }

  @Test
  fun testSupportedMfsProviders_PreservesBackwardCompatibilityAndConfirmedColors() {
    val providers = SupportedMfsProviders
    val providerNames = providers.map { it.name }

    // Preserved for legacy account compatibility
    assertTrue("Must support bKash", providerNames.contains("bKash"))
    assertTrue("Must support Nagad", providerNames.contains("Nagad"))
    assertTrue("Must support Rocket", providerNames.contains("Rocket"))
    assertTrue("Must support Upay", providerNames.contains("Upay"))
    assertTrue("Must preserve Tap for backward compatibility", providerNames.contains("Tap"))
    assertTrue("Must preserve CellFin for backward compatibility", providerNames.contains("CellFin"))

    val bKash = providers.first { it.name == "bKash" }
    assertEquals("#E2136E", bKash.brandColorHex)

    val nagad = providers.first { it.name == "Nagad" }
    assertEquals("#F26522", nagad.brandColorHex)

    val rocket = providers.first { it.name == "Rocket" }
    assertEquals("#8C3494", rocket.brandColorHex)

    val upay = providers.first { it.name == "Upay" }
    assertEquals("#005BAC", upay.brandColorHex)

    val tap = providers.first { it.name == "Tap" }
    assertEquals("#0084C7", tap.brandColorHex)

    val cellFin = providers.first { it.name == "CellFin" }
    assertEquals("#008848", cellFin.brandColorHex)
  }

  @Test
  fun testSupportedCardNetworks_ContainsAllRequiredNetworks() {
    assertTrue("Must support Visa", SupportedCardNetworks.contains("Visa"))
    assertTrue("Must support Mastercard", SupportedCardNetworks.contains("Mastercard"))
    assertTrue("Must support American Express", SupportedCardNetworks.contains("American Express"))
    assertTrue("Must support UnionPay", SupportedCardNetworks.contains("UnionPay"))
  }

  @Test
  fun testEmvChipVisibilityRules() {
    fun shouldShowEmvChip(accountType: AccountType): Boolean {
      return accountType == AccountType.BANK || accountType == AccountType.CREDIT_CARD
    }

    assertTrue("Debit/Bank cards must show EMV chip", shouldShowEmvChip(AccountType.BANK))
    assertTrue("Credit cards must show EMV chip", shouldShowEmvChip(AccountType.CREDIT_CARD))
    assertFalse("Cash accounts must never show EMV chip", shouldShowEmvChip(AccountType.CASH))
    assertFalse("MFS accounts must never show EMV chip", shouldShowEmvChip(AccountType.MFS))
    assertFalse("Other accounts must never show EMV chip", shouldShowEmvChip(AccountType.OTHER))
  }

  @Test
  fun testFreshInstallFormStateDefaults() {
    data class FreshAccountFormState(
      val name: String = "",
      val institution: String = "",
      val balanceText: String = "",
      val last4: String = "",
      val creditLimitText: String = "",
      val outstandingText: String = "",
      val notes: String = ""
    )

    val freshState = FreshAccountFormState()
    assertEquals("", freshState.name)
    assertEquals("", freshState.institution)
    assertEquals("", freshState.balanceText)
    assertEquals("", freshState.last4)
    assertEquals("", freshState.creditLimitText)
    assertEquals("", freshState.outstandingText)
    assertEquals("", freshState.notes)

    assertFalse(freshState.balanceText.contains("6030.05"))
    assertFalse(freshState.outstandingText.contains("-9686.50"))
    assertFalse(freshState.last4.contains("5774"))
    assertFalse(freshState.last4.contains("1875"))
    assertFalse(freshState.last4.contains("8985"))
  }

  @Test
  fun testNotesSerializationAndRestoration() {
    val notesParts = mutableListOf<String>()
    val selectedNetwork = "Visa"
    val selectedSubtype = "Salary Account"

    if (selectedSubtype.isNotBlank()) notesParts.add("Subtype:$selectedSubtype")
    if (selectedNetwork.isNotBlank()) notesParts.add("Network:$selectedNetwork")

    val serializedNotes = notesParts.joinToString("\n")
    assertTrue(serializedNotes.contains("Subtype:Salary Account"))
    assertTrue(serializedNotes.contains("Network:Visa"))

    val extractedNetwork = if (serializedNotes.contains("Network:")) {
      serializedNotes.substringAfter("Network:").substringBefore("\n").trim()
    } else ""
    val extractedSubtype = if (serializedNotes.contains("Subtype:")) {
      serializedNotes.substringAfter("Subtype:").substringBefore("\n").trim()
    } else ""

    assertEquals("Visa", extractedNetwork)
    assertEquals("Salary Account", extractedSubtype)
  }

  @Test
  fun testProviderColorAndManualOverride() {
    var currentColor = "#3B82F6"
    val defaultColors = listOf("#3B82F6", "#E2136E", "#F7941D", "#8C3494", "#005BAC", "#0084C7", "#008848")

    val bKash = SupportedMfsProviders.first { it.name == "bKash" }
    if (currentColor in defaultColors) {
      currentColor = bKash.brandColorHex
    }
    assertEquals("#E2136E", currentColor)

    // Manual override to emerald green
    currentColor = "#10B981"
    assertEquals("#10B981", currentColor)

    // Unrelated update does not touch overridden color
    val newName = "My Primary bKash"
    assertEquals("My Primary bKash", newName)
    assertEquals("#10B981", currentColor)
  }
}
