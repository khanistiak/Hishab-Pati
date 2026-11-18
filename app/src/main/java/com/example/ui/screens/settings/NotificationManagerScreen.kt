package com.example.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.CreditPaymentRemindMeOption
import com.example.data.local.UserSettingsPreferences
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.TranslucentCard
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.screens.transaction.CategorySelectionSheetStyle
import com.example.ui.screens.transaction.SheetHeaderCircleActionButton
import com.example.ui.util.AppHaptics
import kotlinx.coroutines.launch
import java.util.Locale

private enum class TimePickerTarget {
    NONE,
    DAILY_LOGGING,
    PLANNED_PAYMENTS
}

/**
 * Checks whether Android system notification permission is currently granted.
 * - On Android 13+ (API 33 / TIRAMISU), checks both `POST_NOTIFICATIONS` runtime permission
 *   and `NotificationManagerCompat.areNotificationsEnabled()`.
 * - On Android 12L and below (API < 33), where `POST_NOTIFICATIONS` runtime permission does not
 *   exist, checks `NotificationManagerCompat.areNotificationsEnabled()`.
 */
fun hasSystemNotificationPermission(
    context: Context,
    sdkInt: Int = Build.VERSION.SDK_INT
): Boolean {
    val runtimeGranted = if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    val channelEnabled = try {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    } catch (_: Exception) {
        true
    }
    return runtimeGranted && channelEnabled
}

/**
 * Returns true if the current Android version supports and requires the runtime
 * `POST_NOTIFICATIONS` permission prompt.
 */
fun shouldRequestRuntimeNotificationPermission(
    context: Context,
    sdkInt: Int = Build.VERSION.SDK_INT
): Boolean {
    if (sdkInt < Build.VERSION_CODES.TIRAMISU) {
        return false
    }
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) != PackageManager.PERMISSION_GRANTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationManagerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    permissionStateOverride: Boolean? = null,
    onRequestPermissionOverride: (() -> Unit)? = null
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val settings by viewModel.settings.collectAsState()
    val extendedSettings by viewModel.extendedSettings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBangla = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val hapticsEnabled = extendedSettings.hapticFeedbackEnabled
    val appNotificationsEnabled = extendedSettings.notificationsEnabled

    var systemPermissionGranted by remember {
        mutableStateOf(permissionStateOverride ?: hasSystemNotificationPermission(context))
    }

    LaunchedEffect(permissionStateOverride) {
        if (permissionStateOverride != null) {
            systemPermissionGranted = permissionStateOverride
        } else {
            systemPermissionGranted = hasSystemNotificationPermission(context)
        }
    }

    DisposableEffect(lifecycleOwner, context, permissionStateOverride) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                if (permissionStateOverride == null) {
                    systemPermissionGranted = hasSystemNotificationPermission(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val nowPermitted = granted && hasSystemNotificationPermission(context)
        systemPermissionGranted = nowPermitted
        if (nowPermitted) {
            viewModel.toggleNotifications(true)
            viewModel.userNotificationMessage.value = if (isBangla) {
                "নোটিফিকেশন অনুমতি প্রদান করা হয়েছে"
            } else {
                "Notifications enabled"
            }
        } else {
            openAppNotificationSettings(context)
        }
    }

    val requestOrOpenPermissionFlow: () -> Unit = {
        if (onRequestPermissionOverride != null) {
            onRequestPermissionOverride()
        } else if (shouldRequestRuntimeNotificationPermission(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!hasSystemNotificationPermission(context)) {
            openAppNotificationSettings(context)
        } else {
            systemPermissionGranted = true
            viewModel.toggleNotifications(true)
        }
    }

    val areNotificationsActive = systemPermissionGranted && appNotificationsEnabled

    var activeTimePickerTarget by remember { mutableStateOf(TimePickerTarget.NONE) }
    var showRemindMeSheet by remember { mutableStateOf(false) }

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.NOTIFICATION_MANAGER
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 6.dp,
        bottomNavClearance = 36.dp
    )

    val dailyReminderTimeFormatted = remember(
        extendedSettings.dailyLoggingReminderHour,
        extendedSettings.dailyLoggingReminderMinute
    ) {
        UserSettingsPreferences.formatReminderTime(
            hour24 = extendedSettings.dailyLoggingReminderHour,
            minute = extendedSettings.dailyLoggingReminderMinute
        )
    }

    val plannedReminderTimeFormatted = remember(
        extendedSettings.plannedPaymentsReminderHour,
        extendedSettings.plannedPaymentsReminderMinute
    ) {
        UserSettingsPreferences.formatReminderTime(
            hour24 = extendedSettings.plannedPaymentsReminderHour,
            minute = extendedSettings.plannedPaymentsReminderMinute
        )
    }

    val remindMeDisplayLabel = remember(extendedSettings.creditPaymentRemindMeOption, isBangla) {
        val opt = CreditPaymentRemindMeOption.fromKey(extendedSettings.creditPaymentRemindMeOption)
        if (isBangla) opt.banglaLabel else opt.englishLabel
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("notification_manager_screen")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    scrollLayout.backdropState,
                    listState,
                    scrollLayout.contentGraphicsLayer
                )
                .testTag("notification_manager_lazy_column"),
            contentPadding = scrollLayout.contentPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Prominent Master Notification Control Card near the top of the page, below the header
            item(key = "master_notification_control") {
                MasterNotificationControlCard(
                    isBangla = isBangla,
                    isDark = isDark,
                    systemPermissionGranted = systemPermissionGranted,
                    appNotificationsEnabled = appNotificationsEnabled,
                    areNotificationsActive = areNotificationsActive,
                    onMasterToggleChanged = { wantEnabled ->
                        AppHaptics.performToggle(view, hapticsEnabled)
                        if (!systemPermissionGranted) {
                            requestOrOpenPermissionFlow()
                        } else {
                            viewModel.toggleNotifications(wantEnabled)
                        }
                    },
                    onGrantPermissionClick = {
                        AppHaptics.performTap(view, hapticsEnabled)
                        requestOrOpenPermissionFlow()
                    }
                )
            }

            // Section Group Label for Reminder Categories
            item(key = "reminder_categories_header") {
                Text(
                    text = if (isBangla) "অনুস্মারক এবং সতর্কতা" else "REMINDERS & ALERTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp)
                )
            }

            // 1. Daily Expense Reminder Card
            item(key = "card_daily_expense_reminder") {
                NotificationSectionCard(
                    headerIcon = Icons.AutoMirrored.Filled.EventNote,
                    headerTitle = if (isBangla) "দৈনিক ব্যয়ের অনুস্মারক" else "Daily Expense Reminder",
                    accentColor = Color(0xFF6366F1),
                    isDark = isDark,
                    areNotificationsActive = areNotificationsActive
                ) {
                    NotificationToggleRow(
                        title = if (isBangla) "দৈনিক লগিং অনুস্মারক" else "Daily Logging Reminder",
                        subtitle = if (isBangla) {
                            "আজ কোনো লেনদেন লগ না করা হলে একটি দৈনিক অনুস্মারক পান"
                        } else {
                            "Receive a daily reminder if no transactions have been logged today"
                        },
                        checked = areNotificationsActive && extendedSettings.dailyLoggingReminderEnabled,
                        enabled = areNotificationsActive,
                        onCheckedChange = { enabled ->
                            if (!areNotificationsActive) return@NotificationToggleRow
                            AppHaptics.performToggle(view, hapticsEnabled)
                            viewModel.setDailyLoggingReminder(enabled = enabled)
                        },
                        isDark = isDark,
                        testTag = "toggle_daily_logging_reminder"
                    )

                    NotificationCardDivider(isDark = isDark)

                    NotificationValueRow(
                        title = if (isBangla) "অনুস্মারকের সময়" else "Reminder Time",
                        value = dailyReminderTimeFormatted,
                        enabled = areNotificationsActive && extendedSettings.dailyLoggingReminderEnabled,
                        isDark = isDark,
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            activeTimePickerTarget = TimePickerTarget.DAILY_LOGGING
                        },
                        testTag = "row_daily_reminder_time"
                    )
                }
            }

            // 2. Credit Payment Due Date Card
            item(key = "card_credit_payment_due") {
                NotificationSectionCard(
                    headerIcon = Icons.Default.CreditCard,
                    headerTitle = if (isBangla) "ক্রেডিট পেমেন্ট পরিশোধের তারিখ" else "Credit Payment Due Date",
                    accentColor = Color(0xFFF59E0B),
                    isDark = isDark,
                    areNotificationsActive = areNotificationsActive
                ) {
                    NotificationToggleRow(
                        title = if (isBangla) "পেমেন্ট পরিশোধের অনুস্মারক" else "Payment Due Reminder",
                        subtitle = if (isBangla) {
                            "আপনার ক্রেডিট কার্ডের বিল পরিশোধের নির্ধারিত তারিখের আগে বিজ্ঞপ্তি পান"
                        } else {
                            "Get notified before your credit card payments are due"
                        },
                        checked = areNotificationsActive && extendedSettings.creditPaymentDueReminderEnabled,
                        enabled = areNotificationsActive,
                        onCheckedChange = { enabled ->
                            if (!areNotificationsActive) return@NotificationToggleRow
                            AppHaptics.performToggle(view, hapticsEnabled)
                            viewModel.setCreditPaymentDueReminder(enabled = enabled)
                        },
                        isDark = isDark,
                        testTag = "toggle_credit_payment_due_reminder"
                    )

                    NotificationCardDivider(isDark = isDark)

                    NotificationValueRow(
                        title = if (isBangla) "আমাকে মনে করিয়ে দিন" else "Remind Me",
                        value = remindMeDisplayLabel,
                        enabled = areNotificationsActive && extendedSettings.creditPaymentDueReminderEnabled,
                        isDark = isDark,
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            showRemindMeSheet = true
                        },
                        testTag = "row_credit_remind_me"
                    )
                }
            }

            // 3. Credit Statement Date Card
            item(key = "card_credit_statement_date") {
                NotificationSectionCard(
                    headerIcon = Icons.AutoMirrored.Filled.ReceiptLong,
                    headerTitle = if (isBangla) "ক্রেডিট স্টেটমেন্ট তারিখ" else "Credit Statement Date",
                    accentColor = Color(0xFF8B5CF6),
                    isDark = isDark,
                    areNotificationsActive = areNotificationsActive
                ) {
                    NotificationToggleRow(
                        title = if (isBangla) "স্টেটমেন্ট ক্লোজিং অনুস্মারক" else "Statement Closing Reminder",
                        subtitle = if (isBangla) {
                            "আপনার বিলিং সাইকেল শেষ হওয়ার দিনে বিজ্ঞপ্তি পান"
                        } else {
                            "Get notified on the day your billing cycle closes"
                        },
                        checked = areNotificationsActive && extendedSettings.creditStatementClosingReminderEnabled,
                        enabled = areNotificationsActive,
                        onCheckedChange = { enabled ->
                            if (!areNotificationsActive) return@NotificationToggleRow
                            AppHaptics.performToggle(view, hapticsEnabled)
                            viewModel.setCreditStatementClosingReminder(enabled = enabled)
                        },
                        isDark = isDark,
                        testTag = "toggle_statement_closing_reminder"
                    )
                }
            }

            // 4. Planned Payments Alert Card
            item(key = "card_planned_payments_alert") {
                NotificationSectionCard(
                    headerIcon = Icons.Default.Schedule,
                    headerTitle = if (isBangla) "পরিকল্পিত পেমেন্ট সতর্কতা" else "Planned Payments Alert",
                    accentColor = Color(0xFF10B981),
                    isDark = isDark,
                    areNotificationsActive = areNotificationsActive
                ) {
                    NotificationToggleRow(
                        title = if (isBangla) "পরিকল্পিত পেমেন্ট সারাংশ" else "Planned Payments Summary",
                        subtitle = if (isBangla) {
                            "আজকের জন্য নির্ধারিত পরিকল্পিত পেমেন্টগুলোর একটি সারাংশ পান"
                        } else {
                            "Receive a summary of planned payments scheduled for today"
                        },
                        checked = areNotificationsActive && extendedSettings.plannedPaymentsSummaryEnabled,
                        enabled = areNotificationsActive,
                        onCheckedChange = { enabled ->
                            if (!areNotificationsActive) return@NotificationToggleRow
                            AppHaptics.performToggle(view, hapticsEnabled)
                            viewModel.setPlannedPaymentsSummary(enabled = enabled)
                        },
                        isDark = isDark,
                        testTag = "toggle_planned_payments_summary"
                    )

                    NotificationCardDivider(isDark = isDark)

                    NotificationValueRow(
                        title = if (isBangla) "অনুস্মারকের সময়" else "Reminder Time",
                        value = plannedReminderTimeFormatted,
                        enabled = areNotificationsActive && extendedSettings.plannedPaymentsSummaryEnabled,
                        isDark = isDark,
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            activeTimePickerTarget = TimePickerTarget.PLANNED_PAYMENTS
                        },
                        testTag = "row_planned_reminder_time"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Fixed Frosted-Glass Top Header matching Home, Plan, Theme, and Settings
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "notification_manager_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF262B3D) else Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("notification_manager_back_button"),
                    onClick = {
                        AppHaptics.performTap(view, hapticsEnabled)
                        onBack()
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isBangla) "পেছনে" else "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = if (isBangla) "নোটিফিকেশন ম্যানেজার" else "Notification Manager",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp
                    )
                )
            }
        }
    }

    // Select Time Modal Dialog
    if (activeTimePickerTarget != TimePickerTarget.NONE) {
        val initialHour24 = when (activeTimePickerTarget) {
            TimePickerTarget.DAILY_LOGGING -> extendedSettings.dailyLoggingReminderHour
            TimePickerTarget.PLANNED_PAYMENTS -> extendedSettings.plannedPaymentsReminderHour
            TimePickerTarget.NONE -> 20
        }
        val initialMinute = when (activeTimePickerTarget) {
            TimePickerTarget.DAILY_LOGGING -> extendedSettings.dailyLoggingReminderMinute
            TimePickerTarget.PLANNED_PAYMENTS -> extendedSettings.plannedPaymentsReminderMinute
            TimePickerTarget.NONE -> 0
        }

        SelectTimeWheelDialog(
            initialHour24 = initialHour24,
            initialMinute = initialMinute,
            isBangla = isBangla,
            isDark = isDark,
            hapticsEnabled = hapticsEnabled,
            onDismiss = { activeTimePickerTarget = TimePickerTarget.NONE },
            onSave = { selectedHour24, selectedMinute ->
                when (activeTimePickerTarget) {
                    TimePickerTarget.DAILY_LOGGING -> {
                        viewModel.setDailyLoggingReminder(
                            enabled = extendedSettings.dailyLoggingReminderEnabled,
                            hour = selectedHour24,
                            minute = selectedMinute
                        )
                    }
                    TimePickerTarget.PLANNED_PAYMENTS -> {
                        viewModel.setPlannedPaymentsSummary(
                            enabled = extendedSettings.plannedPaymentsSummaryEnabled,
                            hour = selectedHour24,
                            minute = selectedMinute
                        )
                    }
                    TimePickerTarget.NONE -> {}
                }
                activeTimePickerTarget = TimePickerTarget.NONE
            }
        )
    }

    // Remind Me Bottom Sheet
    if (showRemindMeSheet) {
        RemindMeBottomSheet(
            selectedOptionKey = extendedSettings.creditPaymentRemindMeOption,
            isBangla = isBangla,
            isDark = isDark,
            hapticsEnabled = hapticsEnabled,
            onSelectOption = { option ->
                viewModel.setCreditPaymentDueReminder(
                    enabled = extendedSettings.creditPaymentDueReminderEnabled,
                    remindMeOption = option.storageKey
                )
                showRemindMeSheet = false
            },
            onDismiss = { showRemindMeSheet = false }
        )
    }
}

private fun openAppNotificationSettings(context: Context) {
    try {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
        }
    }
}

@Composable
private fun MasterNotificationControlCard(
    isBangla: Boolean,
    isDark: Boolean,
    systemPermissionGranted: Boolean,
    appNotificationsEnabled: Boolean,
    areNotificationsActive: Boolean,
    onMasterToggleChanged: (Boolean) -> Unit,
    onGrantPermissionClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val warningColor = Color(0xFFF59E0B)

    val cardBorder = when {
        !systemPermissionGranted -> BorderStroke(1.25.dp, warningColor.copy(alpha = if (isDark) 0.45f else 0.35f))
        areNotificationsActive -> BorderStroke(1.25.dp, primaryColor.copy(alpha = if (isDark) 0.38f else 0.24f))
        else -> null
    }

    val badgeColor = when {
        !systemPermissionGranted -> warningColor
        areNotificationsActive -> primaryColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusSubtitle = when {
        !systemPermissionGranted -> if (isBangla) {
            "সিস্টেম অনুমতি প্রদান করা হয়নি — নোটিফিকেশন অবরুদ্ধ"
        } else {
            "Notifications are blocked — system permission required"
        }
        areNotificationsActive -> if (isBangla) {
            "আপনার সংরক্ষিত অনুস্মারক এবং পেমেন্ট সতর্কতা সক্রিয় আছে"
        } else {
            "Your saved reminders and payment alerts are active"
        }
        else -> if (isBangla) {
            "অ্যাপের সকল নোটিফিকেশন এবং অনুস্মারক বন্ধ আছে"
        } else {
            "All app notifications and reminders are turned off"
        }
    }

    TranslucentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("master_notification_card"),
        cornerRadius = 22.dp,
        elevation = 2.dp,
        border = cardBorder
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .toggleable(
                        value = areNotificationsActive,
                        enabled = true,
                        role = Role.Switch,
                        onValueChange = onMasterToggleChanged
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("toggle_master_notifications"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (areNotificationsActive) {
                                Icons.Default.NotificationsActive
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBangla) "বিজ্ঞপ্তি" else "Notifications",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = if (!systemPermissionGranted) {
                                if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = areNotificationsActive,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = primaryColor,
                        uncheckedThumbColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                        uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                        uncheckedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                    )
                )
            }

            if (!systemPermissionGranted) {
                NotificationCardDivider(isDark = isDark)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("notifications_disabled_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF282119) else Color(0xFFFFFBEB),
                    border = BorderStroke(
                        width = 1.dp,
                        color = warningColor.copy(alpha = if (isDark) 0.35f else 0.28f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) {
                                    "নোটিফিকেশন অনুমতি প্রয়োজন"
                                } else {
                                    "Notification Permission Required"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                ),
                                modifier = Modifier.testTag("notifications_blocked_title")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isBangla) {
                                "অ্যান্ড্রয়েড সিস্টেম সেটিংসে নোটিফিকেশন অনুমতি বন্ধ আছে। আপনার দৈনিক ব্যয়, ক্রেডিট কার্ডের নির্ধারিত তারিখ এবং পরিকল্পিত পেমেন্টের সময়মতো অনুস্মারক পেতে অনুমতি প্রদান করুন।"
                            } else {
                                "Android notification permission is currently denied or blocked. Grant permission to receive timely reminders for your daily expenses, credit card due dates, and planned payments."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF78350F)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onGrantPermissionClick,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_grant_notification_permission")
                        ) {
                            Text(
                                text = if (isBangla) "অনুমতি দিন" else "Grant Permission",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSectionCard(
    headerIcon: ImageVector,
    headerTitle: String,
    accentColor: Color,
    isDark: Boolean,
    areNotificationsActive: Boolean,
    content: @Composable () -> Unit
) {
    val headerStripBg = if (isDark) {
        accentColor.copy(alpha = 0.12f)
    } else {
        accentColor.copy(alpha = 0.08f)
    }
    val headerTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)

    TranslucentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .alpha(if (areNotificationsActive) 1f else 0.72f),
        cornerRadius = 22.dp,
        elevation = if (areNotificationsActive) 2.dp else 0.5.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Section Header Strip matching the app's rounded badge & card language
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                    .background(headerStripBg)
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = accentColor.copy(alpha = if (isDark) 0.22f else 0.15f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = headerTextColor
                        )
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDark: Boolean,
    testTag: String
) {
    val activeTrackColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.5.sp,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    }
                )
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeTrackColor,
                uncheckedThumbColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                uncheckedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                disabledCheckedThumbColor = Color.White.copy(alpha = 0.7f),
                disabledCheckedTrackColor = activeTrackColor.copy(alpha = 0.4f),
                disabledUncheckedThumbColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                disabledUncheckedTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                disabledUncheckedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            )
        )
    }
}

@Composable
private fun NotificationValueRow(
    title: String,
    value: String,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = if (enabled) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (enabled) {
                primaryColor.copy(alpha = if (isDark) 0.18f else 0.10f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = if (enabled) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationCardDivider(isDark: Boolean) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = if (isDark) Color(0x16FFFFFF) else Color(0x0D000000)
    )
}

/**
 * Custom wheel/column time picker dialog styled to match Hishab Pati's dialogs.
 */
@Composable
fun SelectTimeWheelDialog(
    initialHour24: Int,
    initialMinute: Int,
    isBangla: Boolean,
    isDark: Boolean,
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (hour24: Int, minute: Int) -> Unit
) {
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val (initHour12, initMin, initIsPm) = remember(initialHour24, initialMinute) {
        UserSettingsPreferences.to12HourComponents(initialHour24, initialMinute)
    }

    var selectedHour12 by remember(initHour12) { mutableIntStateOf(initHour12) }
    var selectedMinute by remember(initMin) { mutableIntStateOf(initMin) }
    var isPm by remember(initIsPm) { mutableStateOf(initIsPm) }

    val dialogBg = if (isDark) Color(0xFF1A1E2E) else MaterialTheme.colorScheme.surface
    val pillHighlightColor = primaryColor.copy(alpha = if (isDark) 0.22f else 0.12f)
    val cancelBtnColor = if (isDark) Color(0xFF262E42) else Color(0xFFF1F5F9)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 380.dp)
                .testTag("select_time_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = dialogBg,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(
                width = 1.25.dp,
                color = if (isDark) {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = primaryColor.copy(alpha = if (isDark) 0.20f else 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "সময় নির্বাচন করুন" else "Select Time",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = if (isBangla) "অনুস্মারকের সময় নির্ধারণ করুন" else "Choose your preferred reminder time",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3-Column Wheel Picker (Hour 1..12, :, Minute 00..59, AM/PM)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hour Wheel (1..12)
                    val hours = remember { (1..12).toList() }
                    WheelColumnPicker(
                        items = hours.map { it.toString() },
                        selectedIndex = (selectedHour12 - 1).coerceIn(0, 11),
                        onSelectedIndexChanged = { idx ->
                            val newHour = hours[idx]
                            if (selectedHour12 != newHour) {
                                selectedHour12 = newHour
                                AppHaptics.performTick(view, hapticsEnabled)
                            }
                        },
                        pillHighlightColor = pillHighlightColor,
                        accentColor = primaryColor,
                        modifier = Modifier
                            .width(82.dp)
                            .testTag("time_picker_hour_wheel")
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    // Minute Wheel (00..59)
                    val minutes = remember { (0..59).map { String.format(Locale.US, "%02d", it) } }
                    WheelColumnPicker(
                        items = minutes,
                        selectedIndex = selectedMinute.coerceIn(0, 59),
                        onSelectedIndexChanged = { idx ->
                            if (selectedMinute != idx) {
                                selectedMinute = idx
                                AppHaptics.performTick(view, hapticsEnabled)
                            }
                        },
                        pillHighlightColor = pillHighlightColor,
                        accentColor = primaryColor,
                        modifier = Modifier
                            .width(82.dp)
                            .testTag("time_picker_minute_wheel")
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // AM / PM Column
                    val periods = remember { listOf("AM", "PM") }
                    WheelColumnPicker(
                        items = periods,
                        selectedIndex = if (isPm) 1 else 0,
                        onSelectedIndexChanged = { idx ->
                            val newIsPm = idx == 1
                            if (isPm != newIsPm) {
                                isPm = newIsPm
                                AppHaptics.performTick(view, hapticsEnabled)
                            }
                        },
                        pillHighlightColor = pillHighlightColor,
                        accentColor = primaryColor,
                        modifier = Modifier
                            .width(82.dp)
                            .testTag("time_picker_ampm_wheel")
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Cancel & Save Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cancelBtnColor,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("time_picker_cancel_button")
                    ) {
                        Text(
                            text = if (isBangla) "বাতিল" else "Cancel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            val hour24 = UserSettingsPreferences.to24Hour(selectedHour12, isPm)
                            onSave(hour24, selectedMinute)
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("time_picker_save_button")
                    ) {
                        Text(
                            text = if (isBangla) "সংরক্ষণ" else "Save",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelColumnPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    pillHighlightColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val rowHeight = 44.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()
    var hasUserScrolled by remember { mutableStateOf(false) }

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                selectedIndex
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
                }?.index?.coerceIn(0, items.lastIndex) ?: selectedIndex
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            hasUserScrolled = true
        } else if (hasUserScrolled && centeredIndex in items.indices) {
            onSelectedIndexChanged(centeredIndex)
            hasUserScrolled = false
        }
    }

    Box(
        modifier = modifier.height(rowHeight * 5),
        contentAlignment = Alignment.Center
    ) {
        // Center selection pill background matching app's primary accent
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = pillHighlightColor
        ) {}

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = rowHeight * 2),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val isSelected = index == selectedIndex
                val distance = kotlin.math.abs(index - selectedIndex)
                val alpha = when (distance) {
                    0 -> 1f
                    1 -> 0.65f
                    else -> 0.40f
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clickable {
                            hasUserScrolled = false
                            onSelectedIndexChanged(index)
                            coroutineScope.launch {
                                listState.scrollToItem(index)
                            }
                        }
                        .testTag("wheel_option_${items[index]}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isSelected) 18.sp else 16.sp,
                            color = if (isSelected) {
                                accentColor
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                            }
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Bottom sheet for selecting Credit Payment Due Date reminder offset
 * ("On the due date", "1 day before", "1 week before") styled to match Hishab Pati's bottom sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindMeBottomSheet(
    selectedOptionKey: String,
    isBangla: Boolean,
    isDark: Boolean,
    hapticsEnabled: Boolean,
    onSelectOption: (CreditPaymentRemindMeOption) -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBgUnselected = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val cardBgSelected = if (isDark) Color(0xFF0F253E) else Color(0xFFEFF6FF)
    val borderUnselected = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val accentColor = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .testTag("remind_me_bottom_sheet")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBangla) "আমাকে মনে করিয়ে দিন" else "Remind Me",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = textPrimary
                        )
                    )
                    Text(
                        text = if (isBangla) {
                            "ক্রেডিট কার্ডের বিল পরিশোধের অনুস্মারক কখন পেতে চান তা বেছে নিন"
                        } else {
                            "Choose when to receive credit card payment due alerts"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    )
                }

                SheetHeaderCircleActionButton(
                    onClick = {
                        AppHaptics.performTap(view, hapticsEnabled)
                        onDismiss()
                    },
                    icon = CategorySelectionSheetStyle.HighContrastCloseIcon,
                    contentDescription = if (isBangla) "বন্ধ করুন" else "Close",
                    isDark = isDark,
                    testTag = "btn_close_remind_me_sheet"
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CreditPaymentRemindMeOption.entries.forEach { option ->
                    val isSelected = option.storageKey.equals(selectedOptionKey, ignoreCase = true)
                    Surface(
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            onSelectOption(option)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) cardBgSelected else cardBgUnselected,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accentColor else borderUnselected
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("remind_me_option_${option.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isBangla) option.banglaLabel else option.englishLabel,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = if (isSelected) accentColor else textPrimary
                                )
                            )

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    AppHaptics.performTap(view, hapticsEnabled)
                                    onSelectOption(option)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = accentColor,
                                    unselectedColor = textSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
