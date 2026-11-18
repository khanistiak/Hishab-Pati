package com.example.data.local

import android.content.Context
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AppLanguage
import java.util.Calendar
import java.util.Locale

data class StartOfWeekOption(
    val calendarDay: Int,
    val englishName: String,
    val banglaName: String,
    val shortEn: String,
    val shortBn: String,
    val singleEn: String,
    val singleBn: String
)

enum class CreditPaymentRemindMeOption(
    val storageKey: String,
    val englishLabel: String,
    val banglaLabel: String,
    val daysBefore: Int
) {
    ON_DUE_DATE("On the due date", "On the due date", "নির্ধারিত তারিখে", 0),
    ONE_DAY_BEFORE("1 day before", "1 day before", "১ দিন আগে", 1),
    ONE_WEEK_BEFORE("1 week before", "1 week before", "১ সপ্তাহ আগে", 7);

    companion object {
        fun fromKey(key: String): CreditPaymentRemindMeOption {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: ONE_DAY_BEFORE
        }
    }
}

data class ExtendedSettingsState(
    // Notification Manager
    val notificationsEnabled: Boolean = true,
    val dailyLoggingReminderEnabled: Boolean = true,
    val dailyLoggingReminderHour: Int = 20, // 8:00 PM
    val dailyLoggingReminderMinute: Int = 0,
    val creditPaymentDueReminderEnabled: Boolean = true,
    val creditPaymentRemindMeOption: String = CreditPaymentRemindMeOption.ONE_DAY_BEFORE.storageKey,
    val creditStatementClosingReminderEnabled: Boolean = true,
    val plannedPaymentsSummaryEnabled: Boolean = true,
    val plannedPaymentsReminderHour: Int = 9, // 9:00 AM
    val plannedPaymentsReminderMinute: Int = 0,
    val savedDailyLoggingReminderEnabled: Boolean = true,
    val savedCreditPaymentDueReminderEnabled: Boolean = true,
    val savedCreditStatementClosingReminderEnabled: Boolean = true,
    val savedPlannedPaymentsSummaryEnabled: Boolean = true,

    // General & Transaction behavior
    val enableSubcategories: Boolean = true,
    val autoOpenAmountKeypad: Boolean = false,
    val defaultWalletId: String = "",

    // Date & Time
    val startOfWeek: Int = Calendar.SUNDAY,

    // Security & System
    val useScreenLock: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true
) {
    fun withMasterNotificationsToggled(enabled: Boolean): ExtendedSettingsState {
        return if (!enabled) {
            val snapDaily = if (this.notificationsEnabled) this.dailyLoggingReminderEnabled else this.savedDailyLoggingReminderEnabled
            val snapCreditDue = if (this.notificationsEnabled) this.creditPaymentDueReminderEnabled else this.savedCreditPaymentDueReminderEnabled
            val snapStatement = if (this.notificationsEnabled) this.creditStatementClosingReminderEnabled else this.savedCreditStatementClosingReminderEnabled
            val snapPlanned = if (this.notificationsEnabled) this.plannedPaymentsSummaryEnabled else this.savedPlannedPaymentsSummaryEnabled
            copy(
                notificationsEnabled = false,
                dailyLoggingReminderEnabled = false,
                creditPaymentDueReminderEnabled = false,
                creditStatementClosingReminderEnabled = false,
                plannedPaymentsSummaryEnabled = false,
                savedDailyLoggingReminderEnabled = snapDaily,
                savedCreditPaymentDueReminderEnabled = snapCreditDue,
                savedCreditStatementClosingReminderEnabled = snapStatement,
                savedPlannedPaymentsSummaryEnabled = snapPlanned
            )
        } else {
            copy(
                notificationsEnabled = true,
                dailyLoggingReminderEnabled = this.savedDailyLoggingReminderEnabled,
                creditPaymentDueReminderEnabled = this.savedCreditPaymentDueReminderEnabled,
                creditStatementClosingReminderEnabled = this.savedCreditStatementClosingReminderEnabled,
                plannedPaymentsSummaryEnabled = this.savedPlannedPaymentsSummaryEnabled
            )
        }
    }

    fun withDailyLoggingReminder(
        enabled: Boolean = this.dailyLoggingReminderEnabled,
        hour: Int = this.dailyLoggingReminderHour,
        minute: Int = this.dailyLoggingReminderMinute
    ): ExtendedSettingsState {
        return copy(
            dailyLoggingReminderEnabled = if (this.notificationsEnabled) enabled else false,
            savedDailyLoggingReminderEnabled = enabled,
            dailyLoggingReminderHour = hour.coerceIn(0, 23),
            dailyLoggingReminderMinute = minute.coerceIn(0, 59)
        )
    }

    fun withCreditPaymentDueReminder(
        enabled: Boolean = this.creditPaymentDueReminderEnabled,
        remindMeOption: String = this.creditPaymentRemindMeOption
    ): ExtendedSettingsState {
        val normalizedOption = CreditPaymentRemindMeOption.fromKey(remindMeOption).storageKey
        return copy(
            creditPaymentDueReminderEnabled = if (this.notificationsEnabled) enabled else false,
            savedCreditPaymentDueReminderEnabled = enabled,
            creditPaymentRemindMeOption = normalizedOption
        )
    }

    fun withCreditStatementClosingReminder(
        enabled: Boolean = this.creditStatementClosingReminderEnabled
    ): ExtendedSettingsState {
        return copy(
            creditStatementClosingReminderEnabled = if (this.notificationsEnabled) enabled else false,
            savedCreditStatementClosingReminderEnabled = enabled
        )
    }

    fun withPlannedPaymentsSummary(
        enabled: Boolean = this.plannedPaymentsSummaryEnabled,
        hour: Int = this.plannedPaymentsReminderHour,
        minute: Int = this.plannedPaymentsReminderMinute
    ): ExtendedSettingsState {
        return copy(
            plannedPaymentsSummaryEnabled = if (this.notificationsEnabled) enabled else false,
            savedPlannedPaymentsSummaryEnabled = enabled,
            plannedPaymentsReminderHour = hour.coerceIn(0, 23),
            plannedPaymentsReminderMinute = minute.coerceIn(0, 59)
        )
    }
}

object UserSettingsPreferences {
    private const val PREFS_NAME = "hishab_pati_extended_settings"

    val allWeekDays: List<StartOfWeekOption> = listOf(
        StartOfWeekOption(Calendar.SATURDAY, "Saturday", "শনিবার", "Sa", "শনি", "S", "শ"),
        StartOfWeekOption(Calendar.SUNDAY, "Sunday", "রবিবার", "Su", "রবি", "S", "র"),
        StartOfWeekOption(Calendar.MONDAY, "Monday", "সোমবার", "Mo", "সোম", "M", "সো"),
        StartOfWeekOption(Calendar.TUESDAY, "Tuesday", "মঙ্গলবার", "Tu", "মঙ্গল", "T", "ম"),
        StartOfWeekOption(Calendar.WEDNESDAY, "Wednesday", "বুধবার", "We", "বুধ", "W", "বু"),
        StartOfWeekOption(Calendar.THURSDAY, "Thursday", "বৃহস্পতিবার", "Th", "বৃহঃ", "T", "বৃ"),
        StartOfWeekOption(Calendar.FRIDAY, "Friday", "শুক্রবার", "Fr", "শুক্র", "F", "শু")
    )

    fun dayName(calendarDay: Int, language: AppLanguage = AppLanguage.ENGLISH): String {
        val option = allWeekDays.firstOrNull { it.calendarDay == calendarDay }
            ?: (allWeekDays.firstOrNull { it.calendarDay == Calendar.SUNDAY } ?: allWeekDays.first())
        return if (language == AppLanguage.BANGLA) option.banglaName else option.englishName
    }

    /**
     * Returns the 7 days of the week rotated so that [startOfWeek] (`Calendar.SUNDAY`..`Calendar.SATURDAY`) is first.
     */
    fun orderedWeekDays(startOfWeek: Int): List<StartOfWeekOption> {
        val normalizedStart = if (startOfWeek in Calendar.SUNDAY..Calendar.SATURDAY) startOfWeek else Calendar.SUNDAY
        val startIndex = allWeekDays.indexOfFirst { it.calendarDay == normalizedStart }.coerceAtLeast(0)
        return List(7) { offset ->
            allWeekDays[(startIndex + offset) % 7]
        }
    }

    /**
     * Computes the number of leading empty day slots (0..6) in a month grid when the 1st of the month
     * falls on [firstDayOfMonthDayOfWeek] (`Calendar.SUNDAY`..`Calendar.SATURDAY`) and the week starts on [startOfWeek].
     */
    fun computeLeadingEmptyDays(firstDayOfMonthDayOfWeek: Int, startOfWeek: Int): Int {
        val validStart = if (startOfWeek in Calendar.SUNDAY..Calendar.SATURDAY) startOfWeek else Calendar.SUNDAY
        return (firstDayOfMonthDayOfWeek - validStart + 7) % 7
    }

    /**
     * Resolves the active default wallet from [accounts] using [savedDefaultWalletId].
     * Falls back safely to the first non-hidden wallet if the saved default is empty, hidden, or deleted.
     */
    fun resolveDefaultWallet(
        accounts: List<AccountEntity>,
        savedDefaultWalletId: String
    ): AccountEntity? {
        val activeAccounts = accounts.filter { !it.isHidden }
        if (activeAccounts.isEmpty()) return null
        return activeAccounts.firstOrNull { it.id == savedDefaultWalletId } ?: activeAccounts.first()
    }

    fun formatReminderTime(hour24: Int, minute: Int): String {
        val h = hour24.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val isPm = h >= 12
        val hour12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        val amPm = if (isPm) "PM" else "AM"
        return String.format(Locale.US, "%d:%02d %s", hour12, m, amPm)
    }

    fun to12HourComponents(hour24: Int, minute: Int): Triple<Int, Int, Boolean> {
        val h = hour24.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val isPm = h >= 12
        val hour12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return Triple(hour12, m, isPm)
    }

    fun to24Hour(hour12: Int, isPm: Boolean): Int {
        val clamped = hour12.coerceIn(1, 12)
        return when {
            isPm && clamped < 12 -> clamped + 12
            !isPm && clamped == 12 -> 0
            else -> clamped
        }
    }

    fun load(context: Context): ExtendedSettingsState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawStartOfWeek = prefs.getInt("startOfWeek", Calendar.SUNDAY)
        val validStartOfWeek = if (rawStartOfWeek in Calendar.SUNDAY..Calendar.SATURDAY) rawStartOfWeek else Calendar.SUNDAY

        val notificationsEnabled = prefs.getBoolean("notificationsEnabled", true)
        val rawDaily = prefs.getBoolean("dailyLoggingReminderEnabled", true)
        val rawCreditDue = prefs.getBoolean("creditPaymentDueReminderEnabled", true)
        val rawStatement = prefs.getBoolean("creditStatementClosingReminderEnabled", true)
        val rawPlanned = prefs.getBoolean("plannedPaymentsSummaryEnabled", true)

        val savedDaily = prefs.getBoolean("savedDailyLoggingReminderEnabled", rawDaily)
        val savedCreditDue = prefs.getBoolean("savedCreditPaymentDueReminderEnabled", rawCreditDue)
        val savedStatement = prefs.getBoolean("savedCreditStatementClosingReminderEnabled", rawStatement)
        val savedPlanned = prefs.getBoolean("savedPlannedPaymentsSummaryEnabled", rawPlanned)

        return ExtendedSettingsState(
            notificationsEnabled = notificationsEnabled,
            dailyLoggingReminderEnabled = if (notificationsEnabled) rawDaily else false,
            dailyLoggingReminderHour = prefs.getInt("dailyLoggingReminderHour", 20).coerceIn(0, 23),
            dailyLoggingReminderMinute = prefs.getInt("dailyLoggingReminderMinute", 0).coerceIn(0, 59),
            creditPaymentDueReminderEnabled = if (notificationsEnabled) rawCreditDue else false,
            creditPaymentRemindMeOption = CreditPaymentRemindMeOption.fromKey(
                prefs.getString("creditPaymentRemindMeOption", CreditPaymentRemindMeOption.ONE_DAY_BEFORE.storageKey)
                    ?: CreditPaymentRemindMeOption.ONE_DAY_BEFORE.storageKey
            ).storageKey,
            creditStatementClosingReminderEnabled = if (notificationsEnabled) rawStatement else false,
            plannedPaymentsSummaryEnabled = if (notificationsEnabled) rawPlanned else false,
            plannedPaymentsReminderHour = prefs.getInt("plannedPaymentsReminderHour", 9).coerceIn(0, 23),
            plannedPaymentsReminderMinute = prefs.getInt("plannedPaymentsReminderMinute", 0).coerceIn(0, 59),
            savedDailyLoggingReminderEnabled = savedDaily,
            savedCreditPaymentDueReminderEnabled = savedCreditDue,
            savedCreditStatementClosingReminderEnabled = savedStatement,
            savedPlannedPaymentsSummaryEnabled = savedPlanned,
            enableSubcategories = prefs.getBoolean("enableSubcategories", true),
            autoOpenAmountKeypad = prefs.getBoolean("autoOpenAmountKeypad", false),
            defaultWalletId = prefs.getString("defaultWalletId", "") ?: "",
            startOfWeek = validStartOfWeek,
            useScreenLock = prefs.getBoolean("useScreenLock", false),
            hapticFeedbackEnabled = prefs.getBoolean("hapticFeedbackEnabled", true)
        )
    }

    fun save(context: Context, state: ExtendedSettingsState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("notificationsEnabled", state.notificationsEnabled)
            .putBoolean("dailyLoggingReminderEnabled", state.dailyLoggingReminderEnabled)
            .putInt("dailyLoggingReminderHour", state.dailyLoggingReminderHour.coerceIn(0, 23))
            .putInt("dailyLoggingReminderMinute", state.dailyLoggingReminderMinute.coerceIn(0, 59))
            .putBoolean("creditPaymentDueReminderEnabled", state.creditPaymentDueReminderEnabled)
            .putString("creditPaymentRemindMeOption", state.creditPaymentRemindMeOption)
            .putBoolean("creditStatementClosingReminderEnabled", state.creditStatementClosingReminderEnabled)
            .putBoolean("plannedPaymentsSummaryEnabled", state.plannedPaymentsSummaryEnabled)
            .putInt("plannedPaymentsReminderHour", state.plannedPaymentsReminderHour.coerceIn(0, 23))
            .putInt("plannedPaymentsReminderMinute", state.plannedPaymentsReminderMinute.coerceIn(0, 59))
            .putBoolean("savedDailyLoggingReminderEnabled", state.savedDailyLoggingReminderEnabled)
            .putBoolean("savedCreditPaymentDueReminderEnabled", state.savedCreditPaymentDueReminderEnabled)
            .putBoolean("savedCreditStatementClosingReminderEnabled", state.savedCreditStatementClosingReminderEnabled)
            .putBoolean("savedPlannedPaymentsSummaryEnabled", state.savedPlannedPaymentsSummaryEnabled)
            .putBoolean("enableSubcategories", state.enableSubcategories)
            .putBoolean("autoOpenAmountKeypad", state.autoOpenAmountKeypad)
            .putString("defaultWalletId", state.defaultWalletId)
            .putInt("startOfWeek", state.startOfWeek)
            .putBoolean("useScreenLock", state.useScreenLock)
            .putBoolean("hapticFeedbackEnabled", state.hapticFeedbackEnabled)
            .apply()
    }
}
