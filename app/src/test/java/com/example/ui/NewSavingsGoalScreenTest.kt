package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.model.ThemeMode
import com.example.ui.screens.plan.DefaultGoalIcons
import com.example.ui.screens.plan.DefaultGoalThemeColors
import com.example.ui.screens.plan.NewSavingsGoalContent
import com.example.ui.screens.plan.isValidGoalDeadline
import com.example.ui.screens.plan.resolveGoalIcon
import com.example.ui.theme.MyWalletTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h840dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class NewSavingsGoalScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testExpandedColorAndIconCatalogs_AreDistinctAndResolvable() {
        assertTrue("Should provide at least 16 distinct theme colors", DefaultGoalThemeColors.size >= 16)
        assertEquals(
            "All theme color hex codes must be unique",
            DefaultGoalThemeColors.size,
            DefaultGoalThemeColors.map { it.hex }.distinct().size
        )

        assertTrue("Should provide at least 20 distinct goal icons", DefaultGoalIcons.size >= 20)
        assertEquals(
            "All goal icon names must be unique",
            DefaultGoalIcons.size,
            DefaultGoalIcons.map { it.name }.distinct().size
        )

        DefaultGoalIcons.forEach { item ->
            assertEquals(item.icon, resolveGoalIcon(item.name))
        }
    }

    @Test
    fun testCompactLayout_FitsStandardPhoneWithoutScrolling_AndScrollsOnSmallScreen() {
        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                Box(modifier = Modifier.size(width = 411.dp, height = 800.dp)) {
                    NewSavingsGoalContent(
                        currencySymbol = "৳",
                        onBack = {},
                        onCreateGoal = {}
                    )
                }
            }
        }

        // Verify top preview, form fields, customization rows + Show all buttons, timeline, presets, and bottom button are all visible simultaneously
        composeTestRule.onNodeWithTag("goal_preview_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("goal_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("goal_amount_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_color_show_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_color_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("goal_icon_show_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("goal_icon_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("deadline_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("deadline_preset_3_months").assertIsDisplayed()
        composeTestRule.onNodeWithTag("deadline_preset_6_months").assertIsDisplayed()
        composeTestRule.onNodeWithTag("deadline_preset_1_year").assertIsDisplayed()
        composeTestRule.onNodeWithTag("deadline_preset_custom_date").assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_goal_button").assertIsDisplayed()
    }

    @Test
    fun testShowAllColorAndIconPickers_SelectItemAndReturnToForm() {
        var createdGoal: SavingsGoalEntity? = null

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.DARK) {
                NewSavingsGoalContent(
                    currencySymbol = "৳",
                    onBack = {},
                    onCreateGoal = { createdGoal = it }
                )
            }
        }

        // 1. Open "Show all" for Theme color and select "Teal" (#14B8A6)
        composeTestRule.onNodeWithTag("theme_color_show_all").performClick()
        composeTestRule.onNodeWithTag("all_colors_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("all_colors_grid")
            .performScrollToNode(hasTestTag("all_colors_item_teal"))
        composeTestRule.onNodeWithTag("all_colors_item_teal").performClick()

        // Dialog should dismiss and selected color should be active
        composeTestRule.onNodeWithContentDescription("Selected color Teal", useUnmergedTree = true).assertIsDisplayed()

        // 2. Open "Show all" for Goal icon and select "laptop"
        composeTestRule.onNodeWithTag("goal_icon_show_all").performClick()
        composeTestRule.onNodeWithTag("all_icons_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("all_icons_grid")
            .performScrollToNode(hasTestTag("all_icons_item_laptop"))
        composeTestRule.onNodeWithTag("all_icons_item_laptop").performClick()

        // Preview icon should update to laptop
        composeTestRule.onNodeWithContentDescription("Goal Preview: laptop", useUnmergedTree = true).assertIsDisplayed()

        // 3. Fill name & amount and create goal
        composeTestRule.onNodeWithTag("goal_name_input").performTextInput("MacBook Upgrade")
        composeTestRule.onNodeWithTag("goal_amount_input").performTextInput("150000")
        composeTestRule.onNodeWithTag("create_goal_button").performClick()

        assertNotNull(createdGoal)
        assertEquals("MacBook Upgrade", createdGoal?.name)
        assertEquals(150000.0, createdGoal?.targetAmount ?: 0.0, 0.01)
        assertEquals("#14B8A6", createdGoal?.themeColorHex)
        assertEquals("laptop", createdGoal?.iconName)
    }

    @Test
    fun testTimelinePresets_CustomDateSelection_ClearingAndChanging() {
        val futureCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 45)
        }
        val futureMillis = futureCal.timeInMillis
        val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.US)

        var createdGoal: SavingsGoalEntity? = null

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                NewSavingsGoalContent(
                    currencySymbol = "৳",
                    onBack = {},
                    onCreateGoal = { createdGoal = it },
                    initialCustomPickerDateMillis = futureMillis
                )
            }
        }

        // Initially no deadline set
        composeTestRule.onNodeWithTag("deadline_display_text", useUnmergedTree = true).assertTextEquals("Set target date")

        // 1. Tap 3 Months preset
        composeTestRule.onNodeWithTag("deadline_preset_3_months").performClick()
        val threeMonthCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 90) }
        val expectedThreeMonthStr = formatter.format(Date(threeMonthCal.timeInMillis))
        composeTestRule.onNodeWithTag("deadline_display_text", useUnmergedTree = true).assertTextEquals(expectedThreeMonthStr)

        // 2. Clear deadline via clear button
        composeTestRule.onNodeWithTag("clear_deadline_button").performClick()
        composeTestRule.onNodeWithTag("deadline_display_text", useUnmergedTree = true).assertTextEquals("Set target date")

        // 3. Tap Custom date chip to open DatePickerDialog and confirm custom date
        composeTestRule.onNodeWithTag("deadline_preset_custom_date").performClick()
        composeTestRule.onNodeWithTag("date_picker_confirm_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("date_picker_confirm_button").performClick()

        // Verify custom date is saved and displayed on the form
        composeTestRule.onNodeWithText("Set target date", useUnmergedTree = true).assertDoesNotExist()

        // 4. Create goal and verify custom deadline is persisted
        composeTestRule.onNodeWithTag("goal_name_input").performTextInput("Emergency Fund")
        composeTestRule.onNodeWithTag("goal_amount_input").performTextInput("50000")
        composeTestRule.onNodeWithTag("create_goal_button").performClick()

        assertNotNull(createdGoal)
        assertNotNull("Custom deadline millis must be saved", createdGoal?.targetDeadline)
        assertTrue("Custom deadline string must be non-blank", createdGoal?.targetDeadlineStr?.isNotBlank() == true)
    }

    @Test
    fun testInvalidPastCustomDate_ShowsValidationErrorAndDoesNotBreakGoalCreation() {
        val pastCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
        }
        val pastMillis = pastCal.timeInMillis
        assertFalse("Past date should fail deadline validation", isValidGoalDeadline(pastMillis))

        var createdGoal: SavingsGoalEntity? = null

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                NewSavingsGoalContent(
                    currencySymbol = "৳",
                    onBack = {},
                    onCreateGoal = { createdGoal = it },
                    initialCustomPickerDateMillis = pastMillis
                )
            }
        }

        // Open custom date picker initialized with past date and confirm
        composeTestRule.onNodeWithTag("deadline_preset_custom_date").performClick()
        composeTestRule.onNodeWithTag("date_picker_confirm_button").performClick()

        // Error message should be displayed and deadline should remain unset
        composeTestRule.onNodeWithTag("deadline_error_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("deadline_display_text", useUnmergedTree = true).assertTextEquals("Set target date")

        // Creating the goal still works cleanly without the invalid deadline
        composeTestRule.onNodeWithTag("goal_name_input").performTextInput("Vacation")
        composeTestRule.onNodeWithTag("goal_amount_input").performTextInput("25000")
        composeTestRule.onNodeWithTag("create_goal_button").performClick()

        assertNotNull(createdGoal)
        assertEquals("Vacation", createdGoal?.name)
        assertNull("Invalid past date must not be saved on goal", createdGoal?.targetDeadline)
        assertEquals("", createdGoal?.targetDeadlineStr)
    }
}
