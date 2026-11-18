package com.example.ui

import com.example.data.local.CreditPaymentRemindMeOption
import com.example.data.local.ExtendedSettingsState
import com.example.data.local.UserSettingsPreferences
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountType
import com.example.ui.components.HishabPatiDatePickerStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExtendedSettingsTest {

    @Test
    fun `ExtendedSettingsState defaults match expected Notification Manager and Settings specification`() {
        val defaults = ExtendedSettingsState()
        assertTrue(defaults.dailyLoggingReminderEnabled)
        assertEquals(20, defaults.dailyLoggingReminderHour)
        assertEquals(0, defaults.dailyLoggingReminderMinute)
        assertTrue(defaults.creditPaymentDueReminderEnabled)
        assertEquals("1 day before", defaults.creditPaymentRemindMeOption)
        assertTrue(defaults.creditStatementClosingReminderEnabled)
        assertTrue(defaults.plannedPaymentsSummaryEnabled)
        assertEquals(9, defaults.plannedPaymentsReminderHour)
        assertEquals(0, defaults.plannedPaymentsReminderMinute)
        assertFalse(defaults.autoOpenAmountKeypad)
        assertEquals("", defaults.defaultWalletId)
        assertEquals(Calendar.SUNDAY, defaults.startOfWeek)
        assertFalse(defaults.useScreenLock)
        assertTrue(defaults.hapticFeedbackEnabled)
    }

    @Test
    fun `CreditPaymentRemindMeOption resolves all three Remind Me options accurately`() {
        assertEquals(
            CreditPaymentRemindMeOption.ON_DUE_DATE,
            CreditPaymentRemindMeOption.fromKey("On the due date")
        )
        assertEquals(
            CreditPaymentRemindMeOption.ONE_DAY_BEFORE,
            CreditPaymentRemindMeOption.fromKey("1 day before")
        )
        assertEquals(
            CreditPaymentRemindMeOption.ONE_WEEK_BEFORE,
            CreditPaymentRemindMeOption.fromKey("1 week before")
        )
        assertEquals(0, CreditPaymentRemindMeOption.ON_DUE_DATE.daysBefore)
        assertEquals(1, CreditPaymentRemindMeOption.ONE_DAY_BEFORE.daysBefore)
        assertEquals(7, CreditPaymentRemindMeOption.ONE_WEEK_BEFORE.daysBefore)
    }

    @Test
    fun `resolveDefaultWallet selects saved default wallet and falls back safely if deleted or hidden`() {
        val cash = AccountEntity(
            id = "acc_cash",
            name = "Cash",
            type = AccountType.CASH,
            balance = 500.0,
            colorHex = "#10B981",
            iconName = "wallet",
            isHidden = false
        )
        val bkash = AccountEntity(
            id = "acc_bkash",
            name = "bKash",
            type = AccountType.MFS,
            balance = 1200.0,
            colorHex = "#E11D48",
            iconName = "phone",
            isHidden = false
        )
        val hiddenBank = AccountEntity(
            id = "acc_hidden",
            name = "Hidden Bank",
            type = AccountType.BANK,
            balance = 3000.0,
            colorHex = "#3B82F6",
            iconName = "bank",
            isHidden = true
        )
        val accounts = listOf(cash, bkash, hiddenBank)

        // Selects explicit saved wallet
        val selected = UserSettingsPreferences.resolveDefaultWallet(accounts, "acc_bkash")
        assertEquals("acc_bkash", selected?.id)

        // Falls back to first visible wallet if saved ID was deleted
        val fallbackDeleted = UserSettingsPreferences.resolveDefaultWallet(accounts, "acc_deleted")
        assertEquals("acc_cash", fallbackDeleted?.id)

        // Falls back to first visible wallet if saved ID is hidden
        val fallbackHidden = UserSettingsPreferences.resolveDefaultWallet(accounts, "acc_hidden")
        assertEquals("acc_cash", fallbackHidden?.id)

        // Returns null when account list is empty
        assertNull(UserSettingsPreferences.resolveDefaultWallet(emptyList(), "acc_cash"))
    }

    @Test
    fun `orderedWeekDays and computeLeadingEmptyDays rotate correctly for all 7 days of the week`() {
        val mondayOrdered = UserSettingsPreferences.orderedWeekDays(Calendar.MONDAY)
        assertEquals(7, mondayOrdered.size)
        assertEquals(Calendar.MONDAY, mondayOrdered.first().calendarDay)
        assertEquals(Calendar.SUNDAY, mondayOrdered.last().calendarDay)

        val saturdayOrdered = UserSettingsPreferences.orderedWeekDays(Calendar.SATURDAY)
        assertEquals(Calendar.SATURDAY, saturdayOrdered.first().calendarDay)
        assertEquals(Calendar.FRIDAY, saturdayOrdered.last().calendarDay)

        // If 1st of month is Wednesday (Calendar.WEDNESDAY = 4):
        // - When week starts Sunday (1): 3 leading empty days (Sun, Mon, Tue)
        // - When week starts Monday (2): 2 leading empty days (Mon, Tue)
        // - When week starts Saturday (7): 4 leading empty days (Sat, Sun, Mon, Tue)
        assertEquals(3, UserSettingsPreferences.computeLeadingEmptyDays(Calendar.WEDNESDAY, Calendar.SUNDAY))
        assertEquals(2, UserSettingsPreferences.computeLeadingEmptyDays(Calendar.WEDNESDAY, Calendar.MONDAY))
        assertEquals(4, UserSettingsPreferences.computeLeadingEmptyDays(Calendar.WEDNESDAY, Calendar.SATURDAY))

        val gridMonday = HishabPatiDatePickerStyle.buildMonthGrid(
            year = 2026,
            month = Calendar.APRIL,
            firstDayOfWeek = Calendar.MONDAY
        )
        // April 1, 2026 is a Wednesday -> 2 leading cells from March when week starts on Monday
        assertFalse(gridMonday[0].isCurrentMonth)
        assertFalse(gridMonday[1].isCurrentMonth)
        assertTrue(gridMonday[2].isCurrentMonth)
        assertEquals(1, gridMonday[2].dayOfMonth)
    }

    @Test
    fun `allWeekDays starts with Saturday followed by Sunday through Friday`() {
        val days = UserSettingsPreferences.allWeekDays
        assertEquals(7, days.size)
        assertEquals(Calendar.SATURDAY, days[0].calendarDay)
        assertEquals("Saturday", days[0].englishName)
        assertEquals(Calendar.SUNDAY, days[1].calendarDay)
        assertEquals("Sunday", days[1].englishName)
        assertEquals(Calendar.MONDAY, days[2].calendarDay)
        assertEquals("Monday", days[2].englishName)
        assertEquals(Calendar.TUESDAY, days[3].calendarDay)
        assertEquals("Tuesday", days[3].englishName)
        assertEquals(Calendar.WEDNESDAY, days[4].calendarDay)
        assertEquals("Wednesday", days[4].englishName)
        assertEquals(Calendar.THURSDAY, days[5].calendarDay)
        assertEquals("Thursday", days[5].englishName)
        assertEquals(Calendar.FRIDAY, days[6].calendarDay)
        assertEquals("Friday", days[6].englishName)
    }

    @Test
    fun `time wheel conversion helpers round-trip 12h and 24h accurately`() {
        val (h12Evening, mEvening, isPmEvening) = UserSettingsPreferences.to12HourComponents(20, 0)
        assertEquals(8, h12Evening)
        assertEquals(0, mEvening)
        assertTrue(isPmEvening)
        assertEquals(20, UserSettingsPreferences.to24Hour(h12Evening, isPmEvening))
        assertEquals("8:00 PM", UserSettingsPreferences.formatReminderTime(20, 0))

        val (h12Morning, mMorning, isPmMorning) = UserSettingsPreferences.to12HourComponents(9, 5)
        assertEquals(9, h12Morning)
        assertEquals(5, mMorning)
        assertFalse(isPmMorning)
        assertEquals(9, UserSettingsPreferences.to24Hour(h12Morning, isPmMorning))
        assertEquals("9:05 AM", UserSettingsPreferences.formatReminderTime(9, 5))
    }

    @Test
    fun `master notification toggle turns off all reminders and restores saved individual preferences when re-enabled`() {
        var state = ExtendedSettingsState()
        assertTrue(state.notificationsEnabled)

        // Customize individual reminder preferences:
        // Daily Logging ON (9:30 PM), Credit Payment OFF ("1 week before"), Statement Closing ON, Planned Payments OFF (8:15 AM)
        state = state.withDailyLoggingReminder(enabled = true, hour = 21, minute = 30)
        state = state.withCreditPaymentDueReminder(enabled = false, remindMeOption = "1 week before")
        state = state.withCreditStatementClosingReminder(enabled = true)
        state = state.withPlannedPaymentsSummary(enabled = false, hour = 8, minute = 15)

        assertTrue(state.dailyLoggingReminderEnabled)
        assertFalse(state.creditPaymentDueReminderEnabled)
        assertTrue(state.creditStatementClosingReminderEnabled)
        assertFalse(state.plannedPaymentsSummaryEnabled)

        // Turn master toggle OFF -> disables master and turns off all individual reminders while snapshotting choices
        val disabledState = state.withMasterNotificationsToggled(false)
        assertFalse(disabledState.notificationsEnabled)
        assertFalse(disabledState.dailyLoggingReminderEnabled)
        assertFalse(disabledState.creditPaymentDueReminderEnabled)
        assertFalse(disabledState.creditStatementClosingReminderEnabled)
        assertFalse(disabledState.plannedPaymentsSummaryEnabled)

        // Turn master toggle back ON -> restores exact prior individual choices (NOT enabling every reminder indiscriminately)
        val restoredState = disabledState.withMasterNotificationsToggled(true)
        assertTrue(restoredState.notificationsEnabled)
        assertTrue(restoredState.dailyLoggingReminderEnabled)
        assertEquals(21, restoredState.dailyLoggingReminderHour)
        assertEquals(30, restoredState.dailyLoggingReminderMinute)
        assertFalse(restoredState.creditPaymentDueReminderEnabled)
        assertEquals("1 week before", restoredState.creditPaymentRemindMeOption)
        assertTrue(restoredState.creditStatementClosingReminderEnabled)
        assertFalse(restoredState.plannedPaymentsSummaryEnabled)
        assertEquals(8, restoredState.plannedPaymentsReminderHour)
        assertEquals(15, restoredState.plannedPaymentsReminderMinute)
    }
}

