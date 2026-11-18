package com.example.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import com.example.data.notification.NotificationStore
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.util.Calendar

enum class NotificationCategory {
    ALL,
    UNREAD,
    BILLS,
    INSIGHTS,
    ALERTS
}

enum class NotificationDestinationType {
    BUDGET,
    DEBT,
    TRANSACTION,
    WALLET,
    BILL,
    INSIGHTS,
    GENERAL
}

data class AppNotification(
    val id: String,
    val title: String,
    val titleBn: String,
    val message: String,
    val messageBn: String,
    val timeAgo: String,
    val timeAgoBn: String,
    val group: String, // "YESTERDAY", "EARLIER"
    val groupBn: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color,
    val category: NotificationCategory,
    var isRead: Boolean,
    val destinationType: NotificationDestinationType = NotificationDestinationType.GENERAL,
    val targetEntityId: String? = null,
    val targetSearchQuery: String? = null,
    val targetAmount: Double? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationStore = remember { viewModel.notificationStore }
    val storeUpdates by NotificationStore.updates.collectAsState()

    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val currencySymbol = settings?.currencySymbol ?: "৳"

    val budgets by viewModel.budgets.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var selectedFilter by remember { mutableStateOf(NotificationCategory.ALL) }
    var isNavigating by remember { mutableStateOf(false) }

    val extendedSettings by viewModel.extendedSettings.collectAsState()

    // Real notifications derived exclusively from actual user data (empty on fresh install)
    val realNotifications = remember(budgets, debts, accounts, transactions, currencySymbol, language, storeUpdates, extendedSettings) {
        generateRealNotifications(
            budgets = budgets,
            debts = debts,
            accounts = accounts,
            transactions = transactions,
            currencySymbol = currencySymbol,
            language = language,
            notificationStore = notificationStore,
            extendedSettings = extendedSettings
        )
    }

    // Active mutable notification list filtered by persisted dismissed IDs
    val notifications = remember(realNotifications) {
        mutableStateListOf<AppNotification>().apply {
            addAll(realNotifications)
        }
    }

    val unreadCount = notifications.count { !it.isRead }

    val filteredNotifications = remember(selectedFilter, notifications.toList()) {
        when (selectedFilter) {
            NotificationCategory.ALL -> notifications
            NotificationCategory.UNREAD -> notifications.filter { !it.isRead }
            NotificationCategory.BILLS -> notifications.filter { it.category == NotificationCategory.BILLS }
            NotificationCategory.INSIGHTS -> notifications.filter { it.category == NotificationCategory.INSIGHTS }
            NotificationCategory.ALERTS -> notifications.filter { it.category == NotificationCategory.ALERTS }
        }
    }

    val groupedNotifications = filteredNotifications.groupBy { if (isBn) it.groupBn else it.group }

    // Interactive Notification Click Handler: opens target record and removes immediately
    fun handleNotificationClick(notif: AppNotification) {
        if (isNavigating) return
        isNavigating = true

        // 1. Mark as read AND dismissed in persistent store
        notificationStore.markAsRead(notif.id)
        notificationStore.dismissNotification(notif.id)

        // 2. Remove from active list immediately
        notifications.removeAll { it.id == notif.id }

        // 3. Cancel corresponding Android system notification
        com.example.data.notification.NotificationHelper.cancelNotification(context, notif.id)

        // 4. Navigate to destination
        viewModel.navigateToNotificationDestination(
            destinationType = notif.destinationType,
            targetEntityId = notif.targetEntityId,
            targetSearchQuery = notif.targetSearchQuery,
            targetAmount = notif.targetAmount
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.size(42.dp),
                    onClick = onBack
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isBn) "পেছনে" else "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = AppStrings.t("notifications", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Clear All Button (top bar)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("btn_clear_all_notifications"),
                    onClick = {
                        if (notifications.isEmpty()) {
                            viewModel.userNotificationMessage.value = if (isBn) {
                                "মুছে ফেলার মতো কোনো নোটিফিকেশন নেই"
                            } else {
                                "No notifications to clear"
                            }
                        } else {
                            val allIds = notifications.map { it.id }
                            notificationStore.dismissAll(allIds)
                            notifications.clear()
                            com.example.data.notification.NotificationHelper.cancelAllNotifications(context)
                            viewModel.userNotificationMessage.value = if (isBn) {
                                "সব নোটিফিকেশন মুছে ফেলা হয়েছে"
                            } else {
                                "All notifications cleared"
                            }
                        }
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isBn) "সব মুছুন" else "Clear all notifications",
                            tint = ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Settings gear icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.size(42.dp),
                    onClick = { viewModel.openSubScreen(SubScreen.SETTINGS) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Notification Settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips Bar
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isSelected = selectedFilter == NotificationCategory.ALL
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = NotificationCategory.ALL },
                        label = { Text(AppStrings.t("all", language), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                item {
                    val isSelected = selectedFilter == NotificationCategory.UNREAD
                    val unreadLabel = if (isBn) {
                        "অপঠিত • ${CurrencyFormatter.convertToBanglaDigits(unreadCount.toString())}"
                    } else {
                        "Unread • $unreadCount"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = NotificationCategory.UNREAD },
                        label = { Text(unreadLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                item {
                    val isSelected = selectedFilter == NotificationCategory.BILLS
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = NotificationCategory.BILLS },
                        label = { Text(AppStrings.t("bills", language), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                item {
                    val isSelected = selectedFilter == NotificationCategory.INSIGHTS
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = NotificationCategory.INSIGHTS },
                        label = { Text(AppStrings.t("insights", language), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                item {
                    val isSelected = selectedFilter == NotificationCategory.ALERTS
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = NotificationCategory.ALERTS },
                        label = { Text(AppStrings.t("alerts", language), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Empty State
            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp, vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isBn) "কোনো নোটিফিকেশন নেই" else "No Notifications",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 17.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isBn) {
                                "সব নোটিফিকেশন পড়া বা মুছে ফেলা হয়েছে। নতুন নোটিফিকেশন আসলে এখানে দেখতে পাবেন।"
                            } else {
                                "You're all caught up! New alerts, reminders, and updates will appear here."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Notifications List with Swipe-to-Dismiss (Requirement 3)
                val listState = viewModel.rememberScreenLazyListState(
                    screenOverride = SubScreen.NOTIFICATIONS
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notifications_lazy_column"),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    groupedNotifications.forEach { (groupTitle, list) ->
                        item(key = "header_$groupTitle") {
                            Text(
                                text = groupTitle,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.1.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }

                        items(list, key = { it.id }) { notif ->
                            // Responsive SwipeToDismissBox implementation
                            val dismissState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance -> totalDistance * 0.35f },
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.StartToEnd ||
                                        dismissValue == SwipeToDismissBoxValue.EndToStart
                                    ) {
                                        notificationStore.dismissNotification(notif.id)
                                        notifications.removeAll { it.id == notif.id }
                                        com.example.data.notification.NotificationHelper.cancelNotification(context, notif.id)
                                        viewModel.userNotificationMessage.value = if (isBn) {
                                            "নোটিফিকেশন সরানো হয়েছে"
                                        } else {
                                            "Notification dismissed"
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection
                                    if (direction != SwipeToDismissBoxValue.Settled) {
                                        val isDismissingToEnd = direction == SwipeToDismissBoxValue.StartToEnd
                                        val alignment = if (isDismissingToEnd) Alignment.CenterStart else Alignment.CenterEnd

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(ExpenseRed)
                                                .clickable {
                                                    notificationStore.dismissNotification(notif.id)
                                                    notifications.removeAll { it.id == notif.id }
                                                    com.example.data.notification.NotificationHelper.cancelNotification(context, notif.id)
                                                    viewModel.userNotificationMessage.value = if (isBn) {
                                                        "নোটিফিকেশন সরানো হয়েছে"
                                                    } else {
                                                        "Notification dismissed"
                                                    }
                                                }
                                                .padding(horizontal = 24.dp),
                                            contentAlignment = alignment
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isDismissingToEnd) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = if (isBn) "মুছে ফেলুন" else "Delete",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Text(
                                                        text = if (isBn) "মুছে ফেলুন" else "Delete",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    )
                                                } else {
                                                    Text(
                                                        text = if (isBn) "মুছে ফেলুন" else "Delete",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = if (isBn) "মুছে ফেলুন" else "Delete",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize())
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .testTag("notification_swipe_${notif.id}")
                            ) {
                                NotificationCard(
                                    notif = notif,
                                    isBn = isBn,
                                    onClick = { handleNotificationClick(notif) }
                                )
                            }
                        }
                    }

                    // Clear All Button below notification list (Requirement 3)
                    if (notifications.isNotEmpty()) {
                        item(key = "clear_all_footer") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        val allIds = notifications.map { it.id }
                                        notificationStore.dismissAll(allIds)
                                        notifications.clear()
                                        com.example.data.notification.NotificationHelper.cancelAllNotifications(context)
                                        viewModel.userNotificationMessage.value = if (isBn) {
                                            "সব নোটিফিকেশন মুছে ফেলা হয়েছে"
                                        } else {
                                            "All notifications cleared"
                                        }
                                    }
                                    .testTag("btn_clear_all_notifications_bottom")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBn) "সব নোটিফিকেশন মুছুন" else "Clear All Notifications",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed,
                                            fontSize = 15.sp
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notif: AppNotification,
    isBn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (notif.isRead) 1.dp else 2.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("notification_card_${notif.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Notification Icon Circle
            Surface(
                shape = CircleShape,
                color = notif.iconBgColor,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = notif.icon,
                        contentDescription = null,
                        tint = notif.iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBn) notif.titleBn else notif.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (notif.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (notif.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isBn) notif.messageBn else notif.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (notif.isRead) 0.55f else 0.75f),
                        lineHeight = 17.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isBn) notif.timeAgoBn else notif.timeAgo,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                )
            }

            // Blue Unread Indicator Dot
            if (!notif.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6))
                )
            }
        }
    }
}

/**
 * Derives legitimate notifications strictly from actual user entities in the database.
 * On a fresh install, all parameters are empty lists, returning an empty list so the clean empty-state is displayed.
 */
fun generateRealNotifications(
    budgets: List<BudgetEntity>,
    debts: List<DebtEntity>,
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    language: AppLanguage,
    notificationStore: NotificationStore,
    extendedSettings: com.example.data.local.ExtendedSettingsState = com.example.data.local.ExtendedSettingsState()
): List<AppNotification> {
    val results = mutableListOf<AppNotification>()
    val dismissedIds = notificationStore.getDismissedIds()
    val readIds = notificationStore.getReadIds()

    // 1. Budget Alerts: Triggered when actual category expenses reach >= 80% of monthly limit
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val monthStart = calendar.timeInMillis

    for (budget in budgets) {
        val spent = transactions
            .filter {
                it.type == TransactionType.EXPENSE &&
                    it.timestamp >= monthStart &&
                    it.category.equals(budget.categoryName, ignoreCase = true)
            }
            .sumOf { it.amount }

        if (budget.monthlyLimit > 0.0 && spent >= budget.monthlyLimit * 0.8) {
            val ratio = spent / budget.monthlyLimit
            val pct = (ratio * 100).toInt()
            val notifId = "budget_alert_${budget.id}"
            if (!dismissedIds.contains(notifId)) {
                results.add(
                    AppNotification(
                        id = notifId,
                        title = "${budget.categoryName} budget alert: $pct% reached",
                        titleBn = "${budget.categoryName} বাজেট সতর্কতা: ${CurrencyFormatter.convertToBanglaDigits(pct.toString())}% ব্যবহৃত",
                        message = "You've spent ${CurrencyFormatter.format(spent, currencySymbol)} of your ${CurrencyFormatter.format(budget.monthlyLimit, currencySymbol)} monthly limit for this category.",
                        messageBn = "আপনি এই ক্যাটাগরিতে আপনার মাসিক সীমার ${CurrencyFormatter.format(spent, currencySymbol, language = AppLanguage.BANGLA)} খরচ করেছেন।",
                        timeAgo = "This Month",
                        timeAgoBn = "এই মাস",
                        group = "TODAY",
                        groupBn = "আজ",
                        icon = Icons.Default.Lightbulb,
                        iconColor = Color(0xFFF59E0B),
                        iconBgColor = Color(0xFFFEF3C7),
                        category = NotificationCategory.ALERTS,
                        isRead = readIds.contains(notifId),
                        destinationType = NotificationDestinationType.BUDGET,
                        targetSearchQuery = budget.categoryName,
                        targetEntityId = budget.id
                    )
                )
            }
        }
    }

    // 2. Bill Reminders: Triggered when user has Credit Card accounts with outstanding balances and Payment Due Reminder is enabled
    if (extendedSettings.creditPaymentDueReminderEnabled) {
        val remindMeLabel = extendedSettings.creditPaymentRemindMeOption
        for (acc in accounts) {
            if (!acc.isHidden && acc.type == AccountType.CREDIT_CARD) {
                val absBal = kotlin.math.abs(acc.balance)
                if (absBal > 0.0) {
                    val notifId = "bill_${acc.id}"
                    if (!dismissedIds.contains(notifId)) {
                        results.add(
                            AppNotification(
                                id = notifId,
                                title = "${acc.name} Bill Reminder",
                                titleBn = "${acc.name} বিল অনুস্মারক",
                                message = "Outstanding balance: ${CurrencyFormatter.format(absBal, currencySymbol)} ($remindMeLabel).",
                                messageBn = "মোট বকেয়া: ${CurrencyFormatter.format(absBal, currencySymbol, language = AppLanguage.BANGLA)}।",
                                timeAgo = "Bill Due",
                                timeAgoBn = "বিল অনুস্মারক",
                                group = "TODAY",
                                groupBn = "আজ",
                                icon = Icons.Default.CalendarMonth,
                                iconColor = Color(0xFFEF4444),
                                iconBgColor = Color(0xFFFEE2E2),
                                category = NotificationCategory.BILLS,
                                isRead = readIds.contains(notifId),
                                destinationType = NotificationDestinationType.BILL,
                                targetSearchQuery = acc.name,
                                targetEntityId = acc.id
                            )
                        )
                    }
                }
            }
        }
    }

    // 3. Debt Alerts: Triggered for real unsettled debts
    for (debt in debts) {
        if (!debt.isSettled) {
            val remaining = debt.amount - debt.paidAmount
            if (remaining > 0.0) {
                val notifId = "debt_alert_${debt.id}"
                if (!dismissedIds.contains(notifId)) {
                    val isLent = debt.type == DebtType.LENT
                    val title = if (isLent) "Upcoming Debt Collection: ${debt.personName}" else "Debt Payment Reminder: ${debt.personName}"
                    val titleBn = if (isLent) "আসন্ন ঋণ সংগ্রহ: ${debt.personName}" else "ঋণ পরিশোধের অনুস্মারক: ${debt.personName}"
                    val message = if (debt.dueDate.isNotBlank()) {
                        "Remaining ${CurrencyFormatter.format(remaining, currencySymbol)} due on ${debt.dueDate}."
                    } else {
                        "Outstanding debt of ${CurrencyFormatter.format(remaining, currencySymbol)} with ${debt.personName}."
                    }
                    val messageBn = if (debt.dueDate.isNotBlank()) {
                        "${debt.dueDate}-এ অবশিষ্ট ${CurrencyFormatter.format(remaining, currencySymbol, language = AppLanguage.BANGLA)} পরিশোধযোগ্য।"
                    } else {
                        "${debt.personName}-এর সাথে ${CurrencyFormatter.format(remaining, currencySymbol, language = AppLanguage.BANGLA)} বকেয়া রয়েছে।"
                    }

                    results.add(
                        AppNotification(
                            id = notifId,
                            title = title,
                            titleBn = titleBn,
                            message = message,
                            messageBn = messageBn,
                            timeAgo = if (debt.dueDate.isNotBlank()) debt.dueDate else "Active",
                            timeAgoBn = if (debt.dueDate.isNotBlank()) debt.dueDate else "সক্রিয়",
                            group = "EARLIER",
                            groupBn = "পূর্বে",
                            icon = Icons.Default.Payments,
                            iconColor = Color(0xFF10B981),
                            iconBgColor = Color(0xFFD1FAE5),
                            category = NotificationCategory.ALERTS,
                            isRead = readIds.contains(notifId),
                            destinationType = NotificationDestinationType.DEBT,
                            targetSearchQuery = debt.personName,
                            targetEntityId = debt.id
                        )
                    )
                }
            }
        }
    }

    // 4. Large Expense Alerts: Expenses >= 10,000 from the last 7 days
    val now = System.currentTimeMillis()
    val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000

    val largeExpenses = transactions
        .filter { it.type == TransactionType.EXPENSE && it.amount >= 10000.0 && it.timestamp >= sevenDaysAgo }
        .sortedByDescending { it.timestamp }
        .take(3)

    for (tx in largeExpenses) {
        val notifId = "tx_large_${tx.id}"
        if (!dismissedIds.contains(notifId)) {
            val formattedDate = DateFormatter.formatDateGroup(tx.timestamp, language)
            val formattedDateBn = DateFormatter.formatDateGroup(tx.timestamp, AppLanguage.BANGLA)
            results.add(
                AppNotification(
                    id = notifId,
                    title = "Large expense alert recorded",
                    titleBn = "বড় ব্যয়ের সতর্কতা রেকর্ড করা হয়েছে",
                    message = "${CurrencyFormatter.format(tx.amount, currencySymbol)} recorded for ${tx.category}.",
                    messageBn = "${tx.category}-এ ${CurrencyFormatter.format(tx.amount, currencySymbol, language = AppLanguage.BANGLA)} খরচ রেকর্ড হয়েছে।",
                    timeAgo = formattedDate,
                    timeAgoBn = formattedDateBn,
                    group = "EARLIER",
                    groupBn = "পূর্বে",
                    icon = Icons.Default.WarningAmber,
                    iconColor = Color(0xFFF97316),
                    iconBgColor = Color(0xFFFFEDD5),
                    category = NotificationCategory.ALERTS,
                    isRead = readIds.contains(notifId),
                    destinationType = NotificationDestinationType.TRANSACTION,
                    targetSearchQuery = tx.category,
                    targetAmount = tx.amount
                )
            )
        }
    }

    // 5. Salary / Income notifications: Income transactions from last 7 days
    val salaryTransactions = transactions
        .filter {
            it.type == TransactionType.INCOME &&
                (it.category.contains("Salary", ignoreCase = true) ||
                    it.title.contains("Salary", ignoreCase = true) ||
                    it.category.contains("বেতন", ignoreCase = true)) &&
                it.timestamp >= sevenDaysAgo
        }
        .sortedByDescending { it.timestamp }
        .take(2)

    for (tx in salaryTransactions) {
        val notifId = "tx_salary_${tx.id}"
        if (!dismissedIds.contains(notifId)) {
            val formattedDate = DateFormatter.formatDateGroup(tx.timestamp, language)
            val formattedDateBn = DateFormatter.formatDateGroup(tx.timestamp, AppLanguage.BANGLA)
            val titleName = if (tx.title.isNotBlank()) tx.title else tx.category
            results.add(
                AppNotification(
                    id = notifId,
                    title = "$titleName Credited",
                    titleBn = "$titleName জমা হয়েছে",
                    message = "${CurrencyFormatter.format(tx.amount, currencySymbol)} salary credited.",
                    messageBn = "${CurrencyFormatter.format(tx.amount, currencySymbol, language = AppLanguage.BANGLA)} বেতন জমা হয়েছে।",
                    timeAgo = formattedDate,
                    timeAgoBn = formattedDateBn,
                    group = "EARLIER",
                    groupBn = "পূর্বে",
                    icon = Icons.Default.TrendingUp,
                    iconColor = IncomeGreen,
                    iconBgColor = Color(0xFFDCFCE7),
                    category = NotificationCategory.INSIGHTS,
                    isRead = readIds.contains(notifId),
                    destinationType = NotificationDestinationType.TRANSACTION,
                    targetSearchQuery = tx.category,
                    targetAmount = tx.amount
                )
            )
        }
    }

    return results
}
