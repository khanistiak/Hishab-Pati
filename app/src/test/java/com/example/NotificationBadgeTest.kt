package com.example

import android.content.SharedPreferences
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.DebtType
import com.example.data.notification.NotificationStore
import com.example.ui.screens.notifications.generateRealNotifications
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for Home Screen notification badge logic:
 * 1. Fresh install state (no notifications) -> badge condition (unreadCount > 0) is false.
 * 2. Unread alerts exist -> unreadCount > 0 -> badge condition is true.
 * 3. Marking notification as read -> unreadCount drops to 0 -> badge condition is false.
 * 4. Marking all as read -> unreadCount is 0 -> badge condition is false.
 * 5. Clearing / dismissing all -> unreadCount is 0 -> badge condition is false.
 */
class NotificationBadgeTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var notificationStore: NotificationStore

    @Before
    fun setup() {
        fakePrefs = FakeSharedPreferences()
        notificationStore = NotificationStore(injectedPrefs = fakePrefs)
    }

    @Test
    fun testFreshInstallHasNoNotificationsAndBadgeIsHidden() {
        val notifications = generateRealNotifications(
            budgets = emptyList(),
            debts = emptyList(),
            accounts = emptyList(),
            transactions = emptyList(),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = notificationStore
        )

        val unreadCount = notifications.count { !it.isRead }
        val showBadge = unreadCount > 0

        assertEquals(0, unreadCount)
        assertFalse("Badge should not show on fresh install", showBadge)
    }

    @Test
    fun testUnreadNotificationsTriggersBadge() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 2) // Due in 2 days

        val debt = DebtEntity(
            id = "debt_1",
            personName = "Tanvir Ahmed",
            amount = 5000.0,
            dueDate = "2026-11-02",
            type = DebtType.LENT,
            isSettled = false
        )

        val notifications = generateRealNotifications(
            budgets = emptyList(),
            debts = listOf(debt),
            accounts = emptyList(),
            transactions = emptyList(),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = notificationStore
        )

        val unreadCount = notifications.count { !it.isRead }
        val showBadge = unreadCount > 0

        assertTrue("Should have at least 1 notification", notifications.isNotEmpty())
        assertTrue("Unread count should be > 0", unreadCount > 0)
        assertTrue("Badge should show when unread notifications exist", showBadge)
    }

    @Test
    fun testMarkAsReadHidesBadge() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 2)

        val debt = DebtEntity(
            id = "debt_due_soon",
            personName = "Tanvir Ahmed",
            amount = 5000.0,
            dueDate = "2026-11-02",
            type = DebtType.LENT,
            isSettled = false
        )

        val notificationsInitial = generateRealNotifications(
            budgets = emptyList(),
            debts = listOf(debt),
            accounts = emptyList(),
            transactions = emptyList(),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = notificationStore
        )

        assertEquals(1, notificationsInitial.count { !it.isRead })

        // Mark as read
        val notifId = notificationsInitial.first().id
        notificationStore.markAsRead(notifId)

        // Re-generate notifications with updated store
        val notificationsAfterRead = generateRealNotifications(
            budgets = emptyList(),
            debts = listOf(debt),
            accounts = emptyList(),
            transactions = emptyList(),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = notificationStore
        )

        val unreadCount = notificationsAfterRead.count { !it.isRead }
        val showBadge = unreadCount > 0

        assertEquals(0, unreadCount)
        assertFalse("Badge should hide after marking notification as read", showBadge)
    }

    @Test
    fun testDismissAllHidesBadge() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 2)

        val debt = DebtEntity(
            id = "debt_due_soon",
            personName = "Tanvir Ahmed",
            amount = 5000.0,
            dueDate = "2026-11-02",
            type = DebtType.LENT,
            isSettled = false
        )

        val notificationsInitial = generateRealNotifications(
            budgets = emptyList(),
            debts = listOf(debt),
            accounts = emptyList(),
            transactions = emptyList(),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = notificationStore
        )

        assertTrue(notificationsInitial.isNotEmpty())

        // Clear all notifications
        notificationStore.dismissAll(notificationsInitial.map { it.id })

        val notificationsAfterDismiss = generateRealNotifications(
            budgets = emptyList(),
            debts = listOf(debt),
            accounts = emptyList(),
            transactions = emptyList(),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = notificationStore
        )

        val unreadCount = notificationsAfterDismiss.count { !it.isRead }
        val showBadge = unreadCount > 0

        assertEquals(0, unreadCount)
        assertFalse("Badge should hide after dismissing all notifications", showBadge)
    }

    @Test
    fun testOpenAndClearDismissesNotificationImmediately() {
        val debt = DebtEntity(
            id = "debt_tap_test",
            personName = "Mahmud",
            amount = 3000.0,
            dueDate = "2026-11-05",
            type = DebtType.LENT,
            isSettled = false
        )

        val notifs = generateRealNotifications(
            budgets = emptyList(),
            debts = listOf(debt),
            accounts = emptyList(),
            transactions = emptyList(),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = notificationStore
        )

        assertEquals(1, notifs.size)
        val notifId = notifs.first().id

        // Tap simulation: mark as read and dismiss from store
        notificationStore.markAsRead(notifId)
        notificationStore.dismissNotification(notifId)

        val notifsAfterTap = generateRealNotifications(
            budgets = emptyList(),
            debts = listOf(debt),
            accounts = emptyList(),
            transactions = emptyList(),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = notificationStore
        )

        assertTrue("Tapped notification should be immediately removed from active list", notifsAfterTap.isEmpty())
        assertTrue("Store must remember notification was read", notificationStore.isRead(notifId))
        assertTrue("Store must remember notification was dismissed", notificationStore.isDismissed(notifId))
    }

    @Test
    fun testClearAllRemovesAllNotifications() {
        val debt1 = DebtEntity(
            id = "d1",
            personName = "Ali",
            amount = 2000.0,
            dueDate = "2026-11-03",
            type = DebtType.LENT,
            isSettled = false
        )
        val debt2 = DebtEntity(
            id = "d2",
            personName = "Karim",
            amount = 4000.0,
            dueDate = "2026-11-04",
            type = DebtType.LENT,
            isSettled = false
        )

        val initial = generateRealNotifications(
            budgets = emptyList(),
            debts = listOf(debt1, debt2),
            accounts = emptyList(),
            transactions = emptyList(),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = notificationStore
        )

        assertEquals(2, initial.size)

        // Clear All action
        notificationStore.dismissAll(initial.map { it.id })

        val afterClearAll = generateRealNotifications(
            budgets = emptyList(),
            debts = listOf(debt1, debt2),
            accounts = emptyList(),
            transactions = emptyList(),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = notificationStore
        )

        assertEquals(0, afterClearAll.size)
    }

    @Test
    fun testDestinationTypesAssignedCorrectly() {
        val debt = DebtEntity(
            id = "debt_target",
            personName = "Tanvir",
            amount = 5000.0,
            dueDate = "2026-11-02",
            type = DebtType.LENT,
            isSettled = false
        )
        val ccAccount = AccountEntity(
            id = "cc_target",
            name = "EBL MasterCard",
            type = AccountType.CREDIT_CARD,
            balance = -15000.0
        )
        val tx = TransactionEntity(
            id = "tx_large_1",
            title = "MacBook Pro",
            category = "Electronics",
            amount = 120000.0,
            type = com.example.data.model.TransactionType.EXPENSE,
            accountId = "cc_target",
            timestamp = System.currentTimeMillis() - 1000L
        )

        val notifs = generateRealNotifications(
            budgets = emptyList(),
            debts = listOf(debt),
            accounts = listOf(ccAccount),
            transactions = listOf(tx),
            currencySymbol = "৳",
            language = AppLanguage.ENGLISH,
            notificationStore = notificationStore
        )

        val billNotif = notifs.firstOrNull { it.id == "bill_${ccAccount.id}" }
        val debtNotif = notifs.firstOrNull { it.id == "debt_alert_${debt.id}" }
        val txNotif = notifs.firstOrNull { it.id == "tx_large_${tx.id}" }

        org.junit.Assert.assertNotNull("Credit card bill notification should be generated", billNotif)
        assertEquals(com.example.ui.screens.notifications.NotificationDestinationType.BILL, billNotif?.destinationType)

        org.junit.Assert.assertNotNull("Debt alert should be generated", debtNotif)
        assertEquals(com.example.ui.screens.notifications.NotificationDestinationType.DEBT, debtNotif?.destinationType)

        org.junit.Assert.assertNotNull("Large transaction notification should be generated", txNotif)
        assertEquals(com.example.ui.screens.notifications.NotificationDestinationType.TRANSACTION, txNotif?.destinationType)
    }
}

/**
 * In-memory FakeSharedPreferences for fast JVM unit testing of notification store.
 */
class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any>()
    private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (data[key] as? Set<String>)?.toMutableSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = data.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(this)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listener?.let { listeners.add(it) }
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listeners.remove(listener)
    }

    fun notifyChanged(key: String?) {
        listeners.forEach { it.onSharedPreferenceChanged(this, key) }
    }

    private class FakeEditor(private val parent: FakeSharedPreferences) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { temp[key ?: ""] = values?.toSet(); return this }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
        override fun remove(key: String?): SharedPreferences.Editor { temp[key ?: ""] = null; return this }
        override fun clear(): SharedPreferences.Editor { clear = true; return this }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) parent.data.clear()
            temp.forEach { (k, v) ->
                if (v == null) parent.data.remove(k) else parent.data[k] = v
                parent.notifyChanged(k)
            }
            temp.clear()
        }
    }
}
