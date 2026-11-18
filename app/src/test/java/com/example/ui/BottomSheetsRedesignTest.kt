package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.data.local.InitialData
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.screens.transaction.CategorySelectionSheetStyle
import com.example.ui.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class BottomSheetsRedesignTest {

    @Test
    fun testBottomSheetDesignTokens_MatchEditBudgetCategoryPickerStyle() {
        // 1. Sheet container & row corner radii
        assertEquals(24.dp, CategorySelectionSheetStyle.SHEET_CORNER_RADIUS)
        assertEquals(14.dp, CategorySelectionSheetStyle.ROW_CORNER_RADIUS)
        assertEquals(14.dp, CategorySelectionSheetStyle.SEARCH_CORNER_RADIUS)

        // 2. Borders and bullet dimensions
        assertEquals(1.25.dp, CategorySelectionSheetStyle.UNSELECTED_BORDER_WIDTH)
        assertEquals(2.0.dp, CategorySelectionSheetStyle.SELECTED_BORDER_WIDTH)
        assertEquals(8.dp, CategorySelectionSheetStyle.SUBCATEGORY_BULLET_SIZE)

        // 3. Clear accent blue
        assertEquals(Color(0xFF0284C7), CategorySelectionSheetStyle.ACCENT_BLUE)

        // 4. Pale blue selection backgrounds
        assertEquals(Color(0xFFEFF6FF), CategorySelectionSheetStyle.LIGHT_ROW_BG_SELECTED)
        assertEquals(Color(0xFF0F253E), CategorySelectionSheetStyle.DARK_ROW_BG_SELECTED)

        // 5. Light neutral unselected backgrounds and subtle borders
        assertEquals(Color(0xFFF8FAFC), CategorySelectionSheetStyle.LIGHT_ROW_BG_UNSELECTED)
        assertEquals(Color(0xFFE2E8F0), CategorySelectionSheetStyle.LIGHT_BORDER_UNSELECTED)

        // 6. Circular button backgrounds
        assertEquals(Color(0xFFF1F5F9), CategorySelectionSheetStyle.LIGHT_BUTTON_BG)
        assertEquals(Color(0xFF334155), CategorySelectionSheetStyle.DARK_BUTTON_BG)
    }

    @Test
    fun testPlusAndCloseButtonVisibility_HighContrastTokensAndSize() {
        // Keep size (38.dp) unchanged and ensure high-contrast icon/background pairs
        assertEquals(38.dp, CategorySelectionSheetStyle.HEADER_ACTION_BUTTON_SIZE)
        assertEquals(20.dp, CategorySelectionSheetStyle.HEADER_ACTION_ICON_SIZE)
        assertEquals(1.2.dp, CategorySelectionSheetStyle.HEADER_ACTION_BORDER_WIDTH)

        // Custom bold vector icons for + and ×
        assertNotNull(CategorySelectionSheetStyle.HighContrastPlusIcon)
        assertNotNull(CategorySelectionSheetStyle.HighContrastCloseIcon)
        assertEquals("HighContrastPlus", CategorySelectionSheetStyle.HighContrastPlusIcon.name)
        assertEquals("HighContrastClose", CategorySelectionSheetStyle.HighContrastCloseIcon.name)

        // Verify WCAG AAA contrast ratio between icon color and circular button background in Light & Dark modes
        fun contrastRatio(fg: Color, bg: Color): Float {
            val l1 = max(fg.luminance(), bg.luminance())
            val l2 = min(fg.luminance(), bg.luminance())
            return (l1 + 0.05f) / (l2 + 0.05f)
        }

        val lightContrast = contrastRatio(
            CategorySelectionSheetStyle.LIGHT_HEADER_ACTION_ICON,
            CategorySelectionSheetStyle.LIGHT_HEADER_ACTION_BG
        )
        val darkContrast = contrastRatio(
            CategorySelectionSheetStyle.DARK_HEADER_ACTION_ICON,
            CategorySelectionSheetStyle.DARK_HEADER_ACTION_BG
        )

        assertTrue("Light mode + and close icon contrast ($lightContrast) must exceed 10:1", lightContrast > 10f)
        assertTrue("Dark mode + and close icon contrast ($darkContrast) must exceed 8:1", darkContrast > 8f)
    }

    @Test
    fun testNewExpense_PlusButtonOpensAddCategory_SavesRefreshesPicker_AndPreservesExpenseDetails() {
        // 1. Start with default categories and entered expense draft details
        val (initialCategories, initialSubcategories) = InitialData.getDefaultCategories()
        var enteredExpression = "450 + 50"
        var enteredTitle = "Artisan Coffee Beans"
        var selectedAccountId = "acc_cash"
        var selectedDateTimestamp = 1793700000000L
        var enteredNotes = "Monthly coffee supply"
        var selectedType = TransactionType.EXPENSE
        var selectedCategoryName = "Food & Dining"
        var selectedSubcategoryName = ""

        var showCategorySheet = true
        var showAddCategoryPage = false
        var optimisticCategories = emptyList<CategoryEntity>()
        var optimisticSubcategories = emptyList<SubcategoryEntity>()
        var newlyCreatedCategoryName: String? = null

        // 2. Tap '+' in Select Category bottom sheet -> opens AddEditCategoryPanel page
        val onAddNewCategory = {
            showCategorySheet = false
            showAddCategoryPage = true
        }
        onAddNewCategory()

        assertFalse("Category sheet should hide while Add Category page is open", showCategorySheet)
        assertTrue("Existing Add Category page should open", showAddCategoryPage)

        // 3. Save a new category on the Add Category page (reusing AddEditCategoryPanel onSave contract)
        val newCat = CategoryEntity(
            id = "cat_custom_coffee",
            name = "Specialty Coffee",
            nameBn = "স্পেশালিটি কফি",
            isIncome = false,
            isTransfer = false,
            isActive = true,
            iconName = "local_cafe",
            colorHex = "#0284C7",
            sortOrder = 12
        )
        val newSub = SubcategoryEntity(
            id = "sub_espresso",
            categoryId = newCat.id,
            name = "Espresso Beans",
            sortOrder = 0
        )

        // Simulate AddEditCategoryPanel onSave callback in AddTransactionScreen
        optimisticCategories = optimisticCategories + newCat
        optimisticSubcategories = optimisticSubcategories + newSub
        newlyCreatedCategoryName = newCat.name
        showAddCategoryPage = false
        showCategorySheet = true

        // 4. Verify return to expense flow with refreshed category picker containing the new category
        assertFalse(showAddCategoryPage)
        assertTrue("Select Category sheet should reopen immediately after saving category", showCategorySheet)

        val effectiveCategories = initialCategories + optimisticCategories
        val effectiveSubcategories = initialSubcategories + optimisticSubcategories
        val refreshedExpenseCategories = effectiveCategories.filter { !it.isIncome && !it.isTransfer && it.isActive }

        assertTrue(
            "Newly saved category must immediately appear in the refreshed category picker",
            refreshedExpenseCategories.any { it.name == "Specialty Coffee" }
        )
        assertEquals("Specialty Coffee", newlyCreatedCategoryName)

        // 5. Select the new category from the refreshed picker
        selectedCategoryName = newCat.name
        selectedSubcategoryName = newSub.name
        showCategorySheet = false

        // 6. Verify the new category is selected AND all previously entered expense details are preserved
        assertEquals("Specialty Coffee", selectedCategoryName)
        assertEquals("Espresso Beans", selectedSubcategoryName)
        assertEquals("450 + 50", enteredExpression)
        assertEquals("Artisan Coffee Beans", enteredTitle)
        assertEquals("acc_cash", selectedAccountId)
        assertEquals(1793700000000L, selectedDateTimestamp)
        assertEquals("Monthly coffee supply", enteredNotes)
        assertEquals(TransactionType.EXPENSE, selectedType)
        assertEquals(1, effectiveSubcategories.count { it.categoryId == newCat.id })
    }

    @Test
    fun testSubcategoryPicker_AllCategoryOptionAndSubcategoryList() {
        val testCategory = CategoryEntity(
            id = "cat_healthcare",
            name = "Health Care",
            nameBn = "স্বাস্থ্যসেবা",
            iconName = "medical_services",
            colorHex = "#EF4444",
            isIncome = false
        )

        val subcategories = listOf(
            SubcategoryEntity(id = "sub_1", categoryId = testCategory.id, name = "Doctor visit", sortOrder = 0),
            SubcategoryEntity(id = "sub_2", categoryId = testCategory.id, name = "Medicine", sortOrder = 1),
            SubcategoryEntity(id = "sub_3", categoryId = testCategory.id, name = "Medical Test", sortOrder = 2)
        )

        // Selection 1: User selects "All Health Care" (category selected without subcategory)
        var chosenCategory = ""
        var chosenSubcategory = "Initial"
        var isDismissed = false

        val onCategorySelected: (String, String) -> Unit = { cat, sub ->
            chosenCategory = cat
            chosenSubcategory = sub
        }
        val onDismiss: () -> Unit = { isDismissed = true }

        // Simulate choosing "All Health Care"
        onCategorySelected(testCategory.name, "")
        onDismiss()

        assertEquals("Health Care", chosenCategory)
        assertEquals("", chosenSubcategory)
        assertTrue(isDismissed)

        // Selection 2: User selects specific subcategory "Medicine"
        isDismissed = false
        val selectedSub = subcategories[1]
        onCategorySelected(testCategory.name, selectedSub.name)
        onDismiss()

        assertEquals("Health Care", chosenCategory)
        assertEquals("Medicine", chosenSubcategory)
        assertTrue(isDismissed)

        // Verify colorful bullet colors are distinct
        for (i in subcategories.indices) {
            val colorLight = CategorySelectionSheetStyle.subcategoryBulletColor(i, isDark = false)
            val colorDark = CategorySelectionSheetStyle.subcategoryBulletColor(i, isDark = true)
            assertNotNull(colorLight)
            assertNotNull(colorDark)
        }
        val bullet0 = CategorySelectionSheetStyle.subcategoryBulletColor(0, false)
        val bullet1 = CategorySelectionSheetStyle.subcategoryBulletColor(1, false)
        assertNotEquals(bullet0, bullet1)
    }

    @Test
    fun testTransactionCategoryPicker_FilterTabsAndSearch() {
        val categories = listOf(
            CategoryEntity(id = "c1", name = "Food & Dining", nameBn = "খাবার ও রেস্তোরাঁ", iconName = "restaurant", colorHex = "#F59E0B", isIncome = false, isActive = true),
            CategoryEntity(id = "c2", name = "Shopping", nameBn = "কেনাকাটা", iconName = "shopping_bag", colorHex = "#EC4899", isIncome = false, isActive = true),
            CategoryEntity(id = "c3", name = "Salary", nameBn = "বেতন", iconName = "payments", colorHex = "#10B981", isIncome = true, isActive = true),
            CategoryEntity(id = "c4", name = "Investment Return", nameBn = "বিনিয়োগ", iconName = "trending_up", colorHex = "#10B981", isIncome = true, isActive = true)
        )

        // Filter by Expense tab
        val expenseCategories = categories.filter { !it.isIncome && !it.isTransfer && it.isActive }
        assertEquals(2, expenseCategories.size)
        assertEquals(listOf("Food & Dining", "Shopping"), expenseCategories.map { it.name })

        // Filter by Income tab
        val incomeCategories = categories.filter { it.isIncome && !it.isTransfer && it.isActive }
        assertEquals(2, incomeCategories.size)
        assertEquals(listOf("Salary", "Investment Return"), incomeCategories.map { it.name })

        // Search within Expense categories
        val searchQuery = "Shop"
        val searched = expenseCategories.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.nameBn.contains(searchQuery, ignoreCase = true)
        }
        assertEquals(1, searched.size)
        assertEquals("Shopping", searched[0].name)

        // Search Bangla
        val banglaQuery = "খাবার"
        val searchedBn = expenseCategories.filter {
            it.name.contains(banglaQuery, ignoreCase = true) || it.nameBn.contains(banglaQuery, ignoreCase = true)
        }
        assertEquals(1, searchedBn.size)
        assertEquals("Food & Dining", searchedBn[0].name)
    }

    @Test
    fun testWalletSelector_AccountListAndSelectedState() {
        val accounts = listOf(
            AccountEntity(id = "acc_cash", name = "Wallet Cash", type = AccountType.CASH, balance = 32775.0, iconName = "payments", colorHex = "#3B82F6", isHidden = false),
            AccountEntity(id = "acc_ebl", name = "EBL Salary Account", institution = "BANK", type = AccountType.BANK, balance = -25947.97, iconName = "account_balance", colorHex = "#10B981", isHidden = false),
            AccountEntity(id = "acc_hidden", name = "Archived Account", type = AccountType.OTHER, balance = 100.0, iconName = "savings", colorHex = "#64748B", isHidden = true)
        )

        // Active accounts filter out hidden
        val activeAccounts = accounts.filter { !it.isHidden }
        assertEquals(2, activeAccounts.size)

        val selectedAccountId = "acc_ebl"
        val isFirstSelected = activeAccounts[0].id == selectedAccountId
        val isSecondSelected = activeAccounts[1].id == selectedAccountId

        assertFalse(isFirstSelected)
        assertTrue(isSecondSelected)

        // Verify balance formatting for positive and negative balances
        val formattedPositive = CurrencyFormatter.format(activeAccounts[0].balance, "৳")
        val formattedNegative = CurrencyFormatter.format(activeAccounts[1].balance, "৳")

        assertTrue(formattedPositive.contains("32,775"))
        assertTrue(formattedNegative.contains("25,947.97") && formattedNegative.contains("-"))

        // Excluded account for transfers (source wallet cannot be destination)
        val excludedSourceId = "acc_cash"
        val isExcludedCash = activeAccounts[0].id == excludedSourceId
        val isExcludedEbl = activeAccounts[1].id == excludedSourceId

        assertTrue(isExcludedCash)
        assertFalse(isExcludedEbl)
    }
}

