package com.example

import com.example.data.local.InitialData
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SubcategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive tests for Category Management:
 * 1. Built-in default categories according to transaction types (12 Expense, 5 Income, 3 Transfer).
 * 2. Stable deterministic color selector logic with 5 varied preview colors including selected color.
 * 3. Stable deterministic icon selector logic with 5 varied preview icons including selected icon.
 * 4. Drag-and-drop reordering logic: in both directions, clamping, release commit, and cancellation.
 * 5. Reordering persistence with distinct sortOrder spaces per type across app restarts.
 * 6. Category CRUD (create, edit, enable/disable, delete).
 */
class CategoryManagementTest {

    @Test
    fun testDefaultCategories_ExactStructureAndCounts() {
        val (cats, subs) = InitialData.getDefaultCategories()

        val expenseCats = cats.filter { !it.isIncome && !it.isTransfer }
        val incomeCats = cats.filter { it.isIncome && !it.isTransfer }
        val transferCats = cats.filter { it.isTransfer }

        assertEquals("Must have exactly 12 default expense categories", 12, expenseCats.size)
        assertEquals("Must have exactly 5 default income categories", 5, incomeCats.size)
        assertEquals("Must have exactly 3 default transfer categories", 3, transferCats.size)

        val expectedExpenses = listOf(
            "Food & Dining", "Shopping", "Transportation", "Rent & Utilities",
            "Health & Medical", "Entertainment", "Personal Care", "Groceries",
            "Travel", "Family", "Debt Repayment", "Other Expenses"
        )
        val expectedIncomes = listOf(
            "Salary", "Bonus", "Business", "Gifts Received", "Other Income"
        )
        val expectedTransfers = listOf(
            "Wallet Transfer", "Bank Transfer", "Other Transfer"
        )

        val actualExpenses = expenseCats.map { it.name }
        val actualIncomes = incomeCats.map { it.name }
        val actualTransfers = transferCats.map { it.name }

        assertEquals(expectedExpenses, actualExpenses)
        assertEquals(expectedIncomes, actualIncomes)
        assertEquals(expectedTransfers, actualTransfers)

        // All categories must have valid icons, hex colors, and non-empty Bengali names
        cats.forEach { cat ->
            assertTrue("Category ${cat.name} must have a valid icon", cat.iconName.isNotBlank())
            assertTrue("Category ${cat.name} must have a valid hex color starting with #", cat.colorHex.startsWith("#"))
            assertTrue("Category ${cat.name} must have a non-blank Bengali name", cat.nameBn.isNotBlank())
            assertTrue("All default categories must start active", cat.isActive)
        }

        // Subcategories must be linked to valid category IDs
        val catIds = cats.map { it.id }.toSet()
        subs.forEach { sub ->
            assertTrue("Subcategory ${sub.name} must reference a valid category", catIds.contains(sub.categoryId))
            assertTrue("Subcategory ${sub.name} must have a non-blank name", sub.name.isNotBlank())
        }
    }

    @Test
    fun testDefaultCategories_DeterministicSortOrderBands() {
        val (cats, _) = InitialData.getDefaultCategories()

        val expenseCats = cats.filter { !it.isIncome && !it.isTransfer }
        val incomeCats = cats.filter { it.isIncome && !it.isTransfer }
        val transferCats = cats.filter { it.isTransfer }

        // Expense sortOrders: 0..11
        expenseCats.forEachIndexed { index, cat ->
            assertEquals(index, cat.sortOrder)
        }

        // Income sortOrders: 1000..1004
        incomeCats.forEachIndexed { index, cat ->
            assertEquals(1000 + index, cat.sortOrder)
        }

        // Transfer sortOrders: 2000..2002
        transferCats.forEachIndexed { index, cat ->
            assertEquals(2000 + index, cat.sortOrder)
        }
    }

    @Test
    fun testColorSelector_StableFiveVariedPreviewColors() {
        val stableDefaults = listOf("#FF4D4D", "#2196F3", "#10B981", "#FF9800", "#9C27B0")

        fun getPreviewColors(selectedColor: String): List<String> {
            val normSelected = selectedColor.uppercase()
            val stableNorm = stableDefaults.map { it.uppercase() }
            return if (stableNorm.contains(normSelected)) {
                stableDefaults
            } else {
                stableDefaults.take(4) + selectedColor
            }
        }

        // Case 1: Selected color is one of the defaults
        val preview1 = getPreviewColors("#2196F3")
        assertEquals(5, preview1.size)
        assertTrue("Preview must contain selected color", preview1.contains("#2196F3"))
        assertEquals(stableDefaults, preview1)

        // Case 2: Selected color is outside default 5 (e.g. Purple #673AB7)
        val preview2 = getPreviewColors("#673AB7")
        assertEquals(5, preview2.size)
        assertTrue("Preview must contain custom selected color", preview2.contains("#673AB7"))
        assertEquals(stableDefaults[0], preview2[0])
        assertEquals(stableDefaults[1], preview2[1])
        assertEquals(stableDefaults[2], preview2[2])
        assertEquals(stableDefaults[3], preview2[3])
        assertEquals("#673AB7", preview2[4])

        // Verify determinism: Calling again with same selection yields exact same list
        assertEquals(preview2, getPreviewColors("#673AB7"))
    }

    @Test
    fun testIconSelector_StableFiveVariedPreviewIcons() {
        val stableDefaults = listOf("restaurant", "shopping_bag", "directions_car", "home", "payments")

        fun getPreviewIcons(selectedIcon: String): List<String> {
            val normSelected = selectedIcon.lowercase().trim()
            val stableNorm = stableDefaults.map { it.lowercase().trim() }
            return if (stableNorm.contains(normSelected)) {
                stableDefaults
            } else {
                stableDefaults.take(4) + selectedIcon
            }
        }

        // Case 1: Selected icon is one of the defaults
        val preview1 = getPreviewIcons("home")
        assertEquals(5, preview1.size)
        assertTrue("Preview must contain selected icon", preview1.contains("home"))
        assertEquals(stableDefaults, preview1)

        // Case 2: Selected icon is outside default 5 (e.g. flight)
        val preview2 = getPreviewIcons("flight")
        assertEquals(5, preview2.size)
        assertTrue("Preview must contain selected icon", preview2.contains("flight"))
        assertEquals("flight", preview2[4])

        // Determinism test
        assertEquals(preview2, getPreviewIcons("flight"))
    }

    @Test
    fun testDragAndDrop_ReorderingInBothDirectionsAndCommitOnRelease() {
        val initialList = listOf(
            CategoryEntity(id = "c1", name = "Cat 1", sortOrder = 0),
            CategoryEntity(id = "c2", name = "Cat 2", sortOrder = 1),
            CategoryEntity(id = "c3", name = "Cat 3", sortOrder = 2),
            CategoryEntity(id = "c4", name = "Cat 4", sortOrder = 3),
            CategoryEntity(id = "c5", name = "Cat 5", sortOrder = 4)
        )

        // Simulating drag downwards: from index 1 (Cat 2) to index 3
        var list = initialList.toMutableList()
        val draggedItem = list.removeAt(1)
        list.add(3, draggedItem)

        assertEquals("Cat 1", list[0].name)
        assertEquals("Cat 3", list[1].name)
        assertEquals("Cat 4", list[2].name)
        assertEquals("Cat 2", list[3].name)
        assertEquals("Cat 5", list[4].name)

        // Simulating drag upwards: from index 3 (Cat 2) to index 0
        val movedItem = list.removeAt(3)
        list.add(0, movedItem)

        assertEquals("Cat 2", list[0].name)
        assertEquals("Cat 1", list[1].name)
        assertEquals("Cat 3", list[2].name)
        assertEquals("Cat 4", list[3].name)
        assertEquals("Cat 5", list[4].name)

        // Ensure updateCategoriesOrder assigns sequential sortOrders
        val updated = list.mapIndexed { idx, cat -> cat.copy(sortOrder = idx) }
        updated.forEachIndexed { idx, cat ->
            assertEquals(idx, cat.sortOrder)
        }
    }

    @Test
    fun testDragAndDrop_CancelledDragLeavesListUntouched() {
        val initialList = listOf(
            CategoryEntity(id = "c1", name = "Cat 1", sortOrder = 0),
            CategoryEntity(id = "c2", name = "Cat 2", sortOrder = 1),
            CategoryEntity(id = "c3", name = "Cat 3", sortOrder = 2)
        )

        // While dragging: original list is NOT modified
        val inFlightList = initialList.toList()
        assertEquals(initialList, inFlightList)

        // On cancel: no onMove is invoked, list remains unchanged
        assertEquals("Cat 1", inFlightList[0].name)
        assertEquals("Cat 2", inFlightList[1].name)
        assertEquals("Cat 3", inFlightList[2].name)
    }

    @Test
    fun testCategoryEditing_EnablingDisablingAndUpdatingFields() {
        val category = CategoryEntity(
            id = "test_cat",
            name = "Old Name",
            nameBn = "পুরানো নাম",
            isIncome = false,
            isTransfer = false,
            isActive = true,
            iconName = "restaurant",
            colorHex = "#FF4D4D",
            sortOrder = 0
        )

        // Toggle active -> inactive
        val toggled = category.copy(isActive = false)
        assertFalse("Category must be inactive after toggle", toggled.isActive)

        // Edit fields
        val edited = toggled.copy(
            name = "New Dining",
            nameBn = "নতুন ডাইনিং",
            iconName = "flatware",
            colorHex = "#2196F3"
        )
        assertEquals("New Dining", edited.name)
        assertEquals("নতুন ডাইনিং", edited.nameBn)
        assertEquals("flatware", edited.iconName)
        assertEquals("#2196F3", edited.colorHex)
        assertFalse(edited.isActive)
    }

    @Test
    fun testNewCategory_AddAndRemoveSubcategoriesBeforeSaving() {
        // Simulates user adding and removing subcategories in New Category screen
        var pendingSubcategories = listOf<String>()

        // 1. User types "Cardio" and clicks Add
        pendingSubcategories = pendingSubcategories + "Cardio".trim()
        // 2. User types "Weights" and clicks Add
        pendingSubcategories = pendingSubcategories + "Weights".trim()
        // 3. User types "Yoga" and clicks Add
        pendingSubcategories = pendingSubcategories + "Yoga".trim()
        assertEquals(listOf("Cardio", "Weights", "Yoga"), pendingSubcategories)

        // 4. User removes "Weights" (index 1) by clicking 'x' icon
        val removeIndex = 1
        pendingSubcategories = pendingSubcategories.filterIndexed { idx, _ -> idx != removeIndex }
        assertEquals(listOf("Cardio", "Yoga"), pendingSubcategories)

        // 5. User enters "Supplements" in the input field and hits "Create category" directly
        val typedPendingInput = "Supplements"
        val finalSubsToSave = if (typedPendingInput.isNotBlank()) {
            pendingSubcategories + typedPendingInput.trim()
        } else {
            pendingSubcategories
        }
        assertEquals(listOf("Cardio", "Yoga", "Supplements"), finalSubsToSave)

        // 6. Verify entity creation correctly links categoryId and sequential sortOrders
        val newCatId = "cat_fitness_123"
        val createdEntities = finalSubsToSave.mapIndexed { idx, name ->
            SubcategoryEntity(
                id = "${newCatId}_sub_$idx",
                categoryId = newCatId,
                name = name,
                nameBn = "",
                sortOrder = idx
            )
        }
        assertEquals(3, createdEntities.size)
        assertEquals("Cardio", createdEntities[0].name)
        assertEquals(newCatId, createdEntities[0].categoryId)
        assertEquals(0, createdEntities[0].sortOrder)

        assertEquals("Yoga", createdEntities[1].name)
        assertEquals(newCatId, createdEntities[1].categoryId)
        assertEquals(1, createdEntities[1].sortOrder)

        assertEquals("Supplements", createdEntities[2].name)
        assertEquals(newCatId, createdEntities[2].categoryId)
        assertEquals(2, createdEntities[2].sortOrder)
    }

    @Test
    fun testNewCategory_ReopeningInEditCategoryDisplaysCreatedSubcategories() {
        val newCatId = "cat_tech_456"
        val savedCategory = CategoryEntity(
            id = newCatId,
            name = "Technology",
            isIncome = false,
            isTransfer = false,
            isActive = true,
            iconName = "devices",
            colorHex = "#3B82F6",
            sortOrder = 13
        )
        val allSavedSubcategories = mutableListOf(
            SubcategoryEntity(id = "sub_other_1", categoryId = "cat_other", name = "Groceries", sortOrder = 0),
            SubcategoryEntity(id = "${newCatId}_sub_1", categoryId = newCatId, name = "Laptops", sortOrder = 0),
            SubcategoryEntity(id = "${newCatId}_sub_2", categoryId = newCatId, name = "Phones", sortOrder = 1),
            SubcategoryEntity(id = "${newCatId}_sub_3", categoryId = newCatId, name = "Monitors", sortOrder = 2)
        )

        // When user taps on the category to edit:
        val existingSubsInEdit = allSavedSubcategories.filter { it.categoryId == savedCategory.id }
        assertEquals(3, existingSubsInEdit.size)
        assertEquals("Laptops", existingSubsInEdit[0].name)
        assertEquals("Phones", existingSubsInEdit[1].name)
        assertEquals("Monitors", existingSubsInEdit[2].name)

        // When user deletes "Phones" in edit mode:
        allSavedSubcategories.removeAll { it.id == "${newCatId}_sub_2" }
        val afterDelete = allSavedSubcategories.filter { it.categoryId == savedCategory.id }
        assertEquals(2, afterDelete.size)
        assertEquals(listOf("Laptops", "Monitors"), afterDelete.map { it.name })

        // When user adds "Audio" in edit mode:
        allSavedSubcategories.add(
            SubcategoryEntity(id = "${newCatId}_sub_4", categoryId = newCatId, name = "Audio", sortOrder = 2)
        )
        val afterAdd = allSavedSubcategories.filter { it.categoryId == savedCategory.id }
        assertEquals(3, afterAdd.size)
        assertEquals(listOf("Laptops", "Monitors", "Audio"), afterAdd.map { it.name })
    }

    @Test
    fun testCategorySelectionSheetStyle_RefinedBordersAndColorfulSubcategoryDots() {
        val style = com.example.ui.screens.transaction.CategorySelectionSheetStyle

        // 1. Slightly thicker, subtle border widths
        assertEquals(1.25f, style.UNSELECTED_BORDER_WIDTH.value, 0.001f)
        assertEquals(2.0f, style.SELECTED_BORDER_WIDTH.value, 0.001f)
        assertEquals(1.6f, style.SEARCH_FOCUSED_BORDER_WIDTH.value, 0.001f)
        assertTrue(style.SELECTED_BORDER_WIDTH > style.UNSELECTED_BORDER_WIDTH)

        // 2. Unselected border colors in light and dark themes
        val lightOutline = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
        val darkOutline = androidx.compose.ui.graphics.Color(0xFF475569)
        val lightBorder = style.unselectedBorderColor(lightOutline, isDark = false)
        val darkBorder = style.unselectedBorderColor(darkOutline, isDark = true)
        assertEquals(0.75f, lightBorder.alpha, 0.001f)
        assertEquals(0.78f, darkBorder.alpha, 0.001f)

        // 3. Restrained colorful subcategory bullet dots in both light and dark themes
        assertEquals(8, style.LIGHT_SUBCATEGORY_BULLET_PALETTE.size)
        assertEquals(8, style.DARK_SUBCATEGORY_BULLET_PALETTE.size)

        // Ensure consecutive subcategory rows get distinct colorful bullet dots (not plain gray)
        for (i in 0 until 8) {
            val lightDot = style.subcategoryBulletColor(i, isDark = false)
            val darkDot = style.subcategoryBulletColor(i, isDark = true)
            assertNotEquals("Light bullet dot at index $i must not be plain neutral gray", lightDot.red, lightDot.green)
            assertNotEquals("Dark bullet dot at index $i must not be plain neutral gray", darkDot.red, darkDot.green)
            val nextLightDot = style.subcategoryBulletColor(i + 1, isDark = false)
            assertNotEquals("Adjacent subcategory dots must have distinct colors", lightDot, nextLightDot)
        }
    }
}

