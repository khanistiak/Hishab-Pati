package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeColorOption
import com.example.data.model.ThemeMode
import com.example.ui.components.HishabPatiCustomDateRangePickerDialog
import com.example.ui.components.HishabPatiDatePickerDialog
import com.example.ui.components.HishabPatiDatePickerStyle
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
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HishabPatiDatePickerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDatePickerStyle_TokensAndLightDarkColors() {
        assertEquals(28f, HishabPatiDatePickerStyle.DIALOG_CORNER_RADIUS.value, 0.001f)
        assertEquals(20f, HishabPatiDatePickerStyle.HEADER_CORNER_RADIUS.value, 0.001f)
        assertEquals(12f, HishabPatiDatePickerStyle.DAY_CELL_CORNER_RADIUS.value, 0.001f)
        assertEquals(14f, HishabPatiDatePickerStyle.BUTTON_CORNER_RADIUS.value, 0.001f)

        val primary = Color(ThemeColorOption.SKY_CYAN.primaryHex)
        val lightOutline = Color(0xFFCBD5E1)
        val darkOutline = Color(0xFF475569)

        val lightBorder = HishabPatiDatePickerStyle.dialogBorderColor(lightOutline, isDark = false)
        val darkBorder = HishabPatiDatePickerStyle.dialogBorderColor(darkOutline, isDark = true)
        assertEquals(0.75f, lightBorder.alpha, 0.001f)
        assertEquals(0.78f, darkBorder.alpha, 0.001f)

        val lightHeaderBg = HishabPatiDatePickerStyle.headerBackgroundColor(primary, isDark = false)
        val darkHeaderBg = HishabPatiDatePickerStyle.headerBackgroundColor(primary, isDark = true)
        assertEquals(0.09f, lightHeaderBg.alpha, 0.001f)
        assertEquals(0.16f, darkHeaderBg.alpha, 0.001f)

        assertEquals(primary, HishabPatiDatePickerStyle.selectedDayBackgroundColor(primary))
    }

    @Test
    fun testBuildMonthGrid_AndManualDateParsing() {
        val cells = HishabPatiDatePickerStyle.buildMonthGrid(year = 2026, month = Calendar.NOVEMBER)
        assertEquals(42, cells.size)
        val currentMonthDays = cells.filter { it.isCurrentMonth }
        assertEquals(30, currentMonthDays.size)
        assertEquals(1, currentMonthDays.first().dayOfMonth)
        assertEquals(30, currentMonthDays.last().dayOfMonth)

        // Manual date parsing
        val parsedIso = HishabPatiDatePickerStyle.parseManualDateInput("2026-11-15")
        assertNotNull(parsedIso)
        val calIso = Calendar.getInstance().apply { timeInMillis = parsedIso!! }
        assertEquals(2026, calIso.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, calIso.get(Calendar.MONTH))
        assertEquals(15, calIso.get(Calendar.DAY_OF_MONTH))

        val parsedSlash = HishabPatiDatePickerStyle.parseManualDateInput("20/11/2026")
        assertNotNull(parsedSlash)
        val calSlash = Calendar.getInstance().apply { timeInMillis = parsedSlash!! }
        assertEquals(2026, calSlash.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, calSlash.get(Calendar.MONTH))
        assertEquals(20, calSlash.get(Calendar.DAY_OF_MONTH))

        // Bangla digits manual input
        val parsedBangla = HishabPatiDatePickerStyle.parseManualDateInput("২০২৬-১১-২৫")
        assertNotNull(parsedBangla)
        val calBangla = Calendar.getInstance().apply { timeInMillis = parsedBangla!! }
        assertEquals(25, calBangla.get(Calendar.DAY_OF_MONTH))

        // Invalid input returns null
        assertNull(HishabPatiDatePickerStyle.parseManualDateInput("not-a-date"))
        assertNull(HishabPatiDatePickerStyle.parseManualDateInput(""))
    }

    @Test
    fun testSingleDatePicker_SelectDayNavigateMonthManualInputAndConfirm() {
        val initialCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 10, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var confirmedMillis: Long? = null
        var dismissed = false

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                HishabPatiDatePickerDialog(
                    initialSelectedDateMillis = initialCal.timeInMillis,
                    onDateSelected = { confirmedMillis = it },
                    onDismissRequest = { dismissed = true }
                )
            }
        }

        // Verify dialog and initial header are displayed
        composeTestRule.onNodeWithTag("hishab_pati_date_picker_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("November 2026").assertIsDisplayed()

        // Navigate to previous month (October 2026) and back to November 2026
        composeTestRule.onNodeWithTag("date_picker_prev_month").performClick()
        composeTestRule.onNodeWithText("October 2026").assertIsDisplayed()
        composeTestRule.onNodeWithTag("date_picker_next_month").performClick()
        composeTestRule.onNodeWithText("November 2026").assertIsDisplayed()

        // Open year/month selector grid and close it
        composeTestRule.onNodeWithTag("date_picker_year_selector_toggle").performClick()
        composeTestRule.onNodeWithTag("date_picker_year_month_grid").assertIsDisplayed()
        composeTestRule.onNodeWithTag("date_picker_year_selector_toggle").performClick()

        // Select day 18 in November 2026
        composeTestRule.onNodeWithTag("date_picker_day_18").performClick()

        // Switch to manual date entry mode, test invalid then valid date
        composeTestRule.onNodeWithTag("date_picker_mode_toggle").performClick()
        composeTestRule.onNodeWithTag("date_picker_manual_input").assertIsDisplayed()

        composeTestRule.onNodeWithTag("date_picker_manual_input").performTextReplacement("invalid-date")
        composeTestRule.onNodeWithTag("date_picker_confirm_button").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("date_picker_manual_input").performTextReplacement("2026-11-22")
        composeTestRule.onNodeWithTag("date_picker_confirm_button").assertIsEnabled()

        // Confirm selection
        composeTestRule.onNodeWithTag("date_picker_confirm_button").performClick()

        assertFalse(dismissed)
        assertNotNull(confirmedMillis)
        val resultCal = Calendar.getInstance().apply { timeInMillis = confirmedMillis!! }
        assertEquals(2026, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, resultCal.get(Calendar.MONTH))
        assertEquals(22, resultCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testCustomDateRangePicker_TwoStepStartAndEndDateFlowInDarkMode() {
        val initialStartCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 5, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var selectedRange: Pair<Long, Long>? = null
        var selectedLabel: String? = null

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.DARK) {
                HishabPatiCustomDateRangePickerDialog(
                    initialStartDateMillis = initialStartCal.timeInMillis,
                    language = AppLanguage.ENGLISH,
                    onDateRangeSelected = { range, label ->
                        selectedRange = range
                        selectedLabel = label
                    },
                    onDismissRequest = {}
                )
            }
        }

        // Step 1: Select Start Date (Nov 10, 2026) and press OK
        composeTestRule.onNodeWithText("Select Start Date").assertIsDisplayed()
        composeTestRule.onNodeWithTag("date_picker_day_10").performClick()
        composeTestRule.onNodeWithTag("date_picker_confirm_button").performClick()

        // Step 2: Select End Date (Nov 20, 2026) and press OK
        composeTestRule.onNodeWithText("Select End Date").assertIsDisplayed()
        composeTestRule.onNodeWithTag("date_picker_day_20").performClick()
        composeTestRule.onNodeWithTag("date_picker_confirm_button").performClick()

        assertNotNull(selectedRange)
        assertEquals("Nov 10 - Nov 20", selectedLabel)

        val startCal = Calendar.getInstance().apply { timeInMillis = selectedRange!!.first }
        val endCal = Calendar.getInstance().apply { timeInMillis = selectedRange!!.second }
        assertEquals(10, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))

        assertEquals(20, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertTrue(selectedRange!!.second > selectedRange!!.first)
    }
}
