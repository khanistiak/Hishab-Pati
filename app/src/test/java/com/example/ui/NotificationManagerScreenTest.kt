package com.example.ui

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ExtendedSettingsState
import com.example.data.local.UserSettingsPreferences
import com.example.data.model.ThemeMode
import com.example.ui.screens.settings.NotificationManagerScreen
import com.example.ui.screens.settings.hasSystemNotificationPermission
import com.example.ui.screens.settings.shouldRequestRuntimeNotificationPermission
import com.example.ui.theme.MyWalletBackground
import com.example.ui.theme.MyWalletTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationManagerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        // Reset preferences to clean defaults before each test
        UserSettingsPreferences.save(application, ExtendedSettingsState())
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Shadows.shadowOf(notificationManager).setNotificationsEnabled(true)
    }

    private fun scrollToTag(tag: String) {
        composeTestRule.onNodeWithTag("notification_manager_lazy_column")
            .performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun masterToggle_disablesAllIndividualRemindersWhenTurnedOff_andRestoresSavedChoicesWhenTurnedBackOn() {
        val viewModel = MainViewModel(application)

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                MyWalletBackground(isDark = false) {
                    NotificationManagerScreen(
                        viewModel = viewModel,
                        onBack = {},
                        permissionStateOverride = true
                    )
                }
            }
        }

        // Initially, master toggle and all 4 reminders are ON
        scrollToTag("toggle_master_notifications")
        composeTestRule.onNodeWithTag("toggle_master_notifications")
            .assertIsDisplayed()
            .assertIsOn()

        scrollToTag("toggle_daily_logging_reminder")
        composeTestRule.onNodeWithTag("toggle_daily_logging_reminder")
            .assertIsOn()
            .assertIsEnabled()

        scrollToTag("toggle_credit_payment_due_reminder")
        composeTestRule.onNodeWithTag("toggle_credit_payment_due_reminder")
            .assertIsOn()
            .assertIsEnabled()

        scrollToTag("toggle_statement_closing_reminder")
        composeTestRule.onNodeWithTag("toggle_statement_closing_reminder")
            .assertIsOn()
            .assertIsEnabled()

        scrollToTag("toggle_planned_payments_summary")
        composeTestRule.onNodeWithTag("toggle_planned_payments_summary")
            .assertIsOn()
            .assertIsEnabled()

        // Customize individual preferences: turn OFF Credit Payment Due Reminder and Planned Payments Summary,
        // keep Daily Logging Reminder and Statement Closing Reminder ON
        composeTestRule.onNodeWithTag("toggle_planned_payments_summary").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("toggle_planned_payments_summary").assertIsOff()

        scrollToTag("toggle_credit_payment_due_reminder")
        composeTestRule.onNodeWithTag("toggle_credit_payment_due_reminder").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("toggle_credit_payment_due_reminder").assertIsOff()

        // Scroll back to top and turn OFF the master toggle
        scrollToTag("toggle_master_notifications")
        composeTestRule.onNodeWithTag("toggle_master_notifications").performClick()
        composeTestRule.waitForIdle()

        // Verify master toggle is OFF and all individual toggles are OFF and disabled
        composeTestRule.onNodeWithTag("toggle_master_notifications").assertIsOff()

        scrollToTag("toggle_daily_logging_reminder")
        composeTestRule.onNodeWithTag("toggle_daily_logging_reminder")
            .assertIsOff()
            .assertIsNotEnabled()
        scrollToTag("row_daily_reminder_time")
        composeTestRule.onNodeWithTag("row_daily_reminder_time").assertIsNotEnabled()

        scrollToTag("toggle_credit_payment_due_reminder")
        composeTestRule.onNodeWithTag("toggle_credit_payment_due_reminder")
            .assertIsOff()
            .assertIsNotEnabled()
        scrollToTag("row_credit_remind_me")
        composeTestRule.onNodeWithTag("row_credit_remind_me").assertIsNotEnabled()

        scrollToTag("toggle_statement_closing_reminder")
        composeTestRule.onNodeWithTag("toggle_statement_closing_reminder")
            .assertIsOff()
            .assertIsNotEnabled()

        scrollToTag("toggle_planned_payments_summary")
        composeTestRule.onNodeWithTag("toggle_planned_payments_summary")
            .assertIsOff()
            .assertIsNotEnabled()
        scrollToTag("row_planned_reminder_time")
        composeTestRule.onNodeWithTag("row_planned_reminder_time").assertIsNotEnabled()

        // Turn master toggle back ON -> must restore Daily Logging (ON), Credit Payment Due (OFF),
        // Statement Closing (ON), and Planned Payments (OFF)
        scrollToTag("toggle_master_notifications")
        composeTestRule.onNodeWithTag("toggle_master_notifications").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("toggle_master_notifications").assertIsOn()

        scrollToTag("toggle_daily_logging_reminder")
        composeTestRule.onNodeWithTag("toggle_daily_logging_reminder")
            .assertIsOn()
            .assertIsEnabled()

        scrollToTag("toggle_credit_payment_due_reminder")
        composeTestRule.onNodeWithTag("toggle_credit_payment_due_reminder")
            .assertIsOff()
            .assertIsEnabled()

        scrollToTag("toggle_statement_closing_reminder")
        composeTestRule.onNodeWithTag("toggle_statement_closing_reminder")
            .assertIsOn()
            .assertIsEnabled()

        scrollToTag("toggle_planned_payments_summary")
        composeTestRule.onNodeWithTag("toggle_planned_payments_summary")
            .assertIsOff()
            .assertIsEnabled()
    }

    @Test
    fun individualSelectors_timePickerAndRemindMeBottomSheet_updateAndPersistPreferences() {
        val viewModel = MainViewModel(application)

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                MyWalletBackground(isDark = false) {
                    NotificationManagerScreen(
                        viewModel = viewModel,
                        onBack = {},
                        permissionStateOverride = true
                    )
                }
            }
        }

        // 1. Test Daily Logging Reminder time selector
        scrollToTag("row_daily_reminder_time")
        composeTestRule.onNodeWithText("8:00 PM").assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_daily_reminder_time")
            .assertHasClickAction()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("select_time_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wheel_option_9").performClick()
        composeTestRule.onNodeWithTag("time_picker_save_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("9:00 PM").assertIsDisplayed()
        assertEquals(21, viewModel.extendedSettings.value.dailyLoggingReminderHour)
        assertEquals(0, viewModel.extendedSettings.value.dailyLoggingReminderMinute)

        // 2. Test Credit Payment Due Reminder "Remind Me" bottom sheet selector
        scrollToTag("row_credit_remind_me")
        composeTestRule.onNodeWithText("1 day before").assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_credit_remind_me")
            .assertHasClickAction()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("remind_me_bottom_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("remind_me_option_one_week_before").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("1 week before").assertIsDisplayed()
        assertEquals("1 week before", viewModel.extendedSettings.value.creditPaymentRemindMeOption)

        // 3. Test Planned Payments Summary time selector
        scrollToTag("row_planned_reminder_time")
        composeTestRule.onNodeWithText("9:00 AM").assertIsDisplayed()
        composeTestRule.onNodeWithTag("row_planned_reminder_time")
            .assertHasClickAction()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("select_time_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wheel_option_10").performClick()
        composeTestRule.onNodeWithTag("time_picker_save_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("10:00 AM").assertIsDisplayed()
        assertEquals(10, viewModel.extendedSettings.value.plannedPaymentsReminderHour)
        assertEquals(0, viewModel.extendedSettings.value.plannedPaymentsReminderMinute)

        // 4. Verify persistence across leaving the page and restarting the app
        val reloadedFromDisk = UserSettingsPreferences.load(application)
        assertEquals(21, reloadedFromDisk.dailyLoggingReminderHour)
        assertEquals("1 week before", reloadedFromDisk.creditPaymentRemindMeOption)
        assertEquals(10, reloadedFromDisk.plannedPaymentsReminderHour)

        val restartedViewModel = MainViewModel(application)
        assertEquals(21, restartedViewModel.extendedSettings.value.dailyLoggingReminderHour)
        assertEquals("1 week before", restartedViewModel.extendedSettings.value.creditPaymentRemindMeOption)
        assertEquals(10, restartedViewModel.extendedSettings.value.plannedPaymentsReminderHour)
    }

    @Test
    fun permissionDeniedAndRevokedStates_showBlockedExplanation_disableControls_andHandleGrantAction() {
        val viewModel = MainViewModel(application)
        var permissionGranted by mutableStateOf(false)
        var permissionRequestCount = 0

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.DARK) {
                MyWalletBackground(isDark = true) {
                    NotificationManagerScreen(
                        viewModel = viewModel,
                        onBack = {},
                        permissionStateOverride = permissionGranted,
                        onRequestPermissionOverride = {
                            permissionRequestCount++
                            permissionGranted = true
                        }
                    )
                }
            }
        }

        // When permission is denied:
        // - Master toggle must NOT falsely show as enabled
        // - Explanation card and Grant Permission button are displayed
        // - Individual reminder toggles are off and disabled
        scrollToTag("toggle_master_notifications")
        composeTestRule.onNodeWithTag("toggle_master_notifications").assertIsOff()
        composeTestRule.onNodeWithTag("notifications_disabled_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notifications_blocked_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_grant_notification_permission").assertIsDisplayed()

        scrollToTag("toggle_daily_logging_reminder")
        composeTestRule.onNodeWithTag("toggle_daily_logging_reminder")
            .assertIsOff()
            .assertIsNotEnabled()
        scrollToTag("row_daily_reminder_time")
        composeTestRule.onNodeWithTag("row_daily_reminder_time").assertIsNotEnabled()

        // Clicking Grant Permission invokes the permission flow and enables notifications
        scrollToTag("master_notification_card")
        composeTestRule.onNodeWithTag("btn_grant_notification_permission").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, permissionRequestCount)
        composeTestRule.onNodeWithTag("notifications_disabled_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("toggle_master_notifications").assertIsOn()

        scrollToTag("toggle_daily_logging_reminder")
        composeTestRule.onNodeWithTag("toggle_daily_logging_reminder")
            .assertIsOn()
            .assertIsEnabled()

        // Simulate permission revoked in Android Settings while app was configured as enabled
        permissionGranted = false
        composeTestRule.waitForIdle()

        // Page must immediately show notifications as blocked and never falsely display as enabled
        scrollToTag("master_notification_card")
        composeTestRule.onNodeWithTag("toggle_master_notifications").assertIsOff()
        composeTestRule.onNodeWithTag("notifications_disabled_card").assertIsDisplayed()

        scrollToTag("toggle_daily_logging_reminder")
        composeTestRule.onNodeWithTag("toggle_daily_logging_reminder")
            .assertIsOff()
            .assertIsNotEnabled()
    }

    @Test
    fun androidSystemPermissionChecks_handleRuntimePermissionOnApi33Plus_andChannelStateOnOlderVersions() {
        val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Android 13+ (API 33+): requires both POST_NOTIFICATIONS and NotificationManager enabled
        Shadows.shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Shadows.shadowOf(notificationManager).setNotificationsEnabled(true)

        assertFalse(hasSystemNotificationPermission(application, sdkInt = Build.VERSION_CODES.TIRAMISU))
        assertTrue(shouldRequestRuntimeNotificationPermission(application, sdkInt = Build.VERSION_CODES.TIRAMISU))

        // 2. Android 12L and below (API < 33): does NOT require runtime POST_NOTIFICATIONS prompt,
        //    relies on NotificationManagerCompat.areNotificationsEnabled()
        assertTrue(hasSystemNotificationPermission(application, sdkInt = Build.VERSION_CODES.S_V2))
        assertFalse(shouldRequestRuntimeNotificationPermission(application, sdkInt = Build.VERSION_CODES.S_V2))

        // 3. If notifications are blocked at OS level on Android 12L, hasSystemNotificationPermission returns false
        Shadows.shadowOf(notificationManager).setNotificationsEnabled(false)
        assertFalse(hasSystemNotificationPermission(application, sdkInt = Build.VERSION_CODES.S_V2))

        // 4. Granting both on Android 13+ returns true and no longer requests runtime prompt
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Shadows.shadowOf(notificationManager).setNotificationsEnabled(true)
        assertTrue(hasSystemNotificationPermission(application, sdkInt = Build.VERSION_CODES.TIRAMISU))
        assertFalse(shouldRequestRuntimeNotificationPermission(application, sdkInt = Build.VERSION_CODES.TIRAMISU))
    }

    @Test
    fun persistence_preservesMasterToggleOffAndRestoresPriorChoicesAfterAppRestart() {
        val viewModel1 = MainViewModel(application)
        // Configure custom reminder choices
        viewModel1.setDailyLoggingReminder(enabled = false, hour = 22, minute = 15)
        viewModel1.setCreditPaymentDueReminder(enabled = true, remindMeOption = "On the due date")
        viewModel1.setCreditStatementClosingReminder(enabled = false)
        viewModel1.setPlannedPaymentsSummary(enabled = true, hour = 7, minute = 45)

        // Turn master toggle OFF
        viewModel1.toggleNotifications(false)
        assertFalse(viewModel1.extendedSettings.value.notificationsEnabled)
        assertFalse(viewModel1.extendedSettings.value.dailyLoggingReminderEnabled)
        assertFalse(viewModel1.extendedSettings.value.creditPaymentDueReminderEnabled)
        assertFalse(viewModel1.extendedSettings.value.creditStatementClosingReminderEnabled)
        assertFalse(viewModel1.extendedSettings.value.plannedPaymentsSummaryEnabled)

        // Simulate app restart with a brand new MainViewModel reading from SharedPreferences
        val viewModelAfterRestart = MainViewModel(application)
        assertFalse(viewModelAfterRestart.extendedSettings.value.notificationsEnabled)
        assertFalse(viewModelAfterRestart.extendedSettings.value.dailyLoggingReminderEnabled)
        assertFalse(viewModelAfterRestart.extendedSettings.value.creditPaymentDueReminderEnabled)
        assertFalse(viewModelAfterRestart.extendedSettings.value.creditStatementClosingReminderEnabled)
        assertFalse(viewModelAfterRestart.extendedSettings.value.plannedPaymentsSummaryEnabled)

        // Turn master toggle back ON after app restart -> restores exact saved choices
        viewModelAfterRestart.toggleNotifications(true)
        val restored = viewModelAfterRestart.extendedSettings.value
        assertTrue(restored.notificationsEnabled)
        assertFalse(restored.dailyLoggingReminderEnabled)
        assertEquals(22, restored.dailyLoggingReminderHour)
        assertEquals(15, restored.dailyLoggingReminderMinute)
        assertTrue(restored.creditPaymentDueReminderEnabled)
        assertEquals("On the due date", restored.creditPaymentRemindMeOption)
        assertFalse(restored.creditStatementClosingReminderEnabled)
        assertTrue(restored.plannedPaymentsSummaryEnabled)
        assertEquals(7, restored.plannedPaymentsReminderHour)
        assertEquals(45, restored.plannedPaymentsReminderMinute)
    }
}
