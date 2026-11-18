package com.example.data.notification

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistent store for Notification dismissed state and read state.
 * Uses SharedPreferences to ensure dismissed and read states persist across app restarts and screen transitions.
 * Exposes a reactive `updates` StateFlow to trigger immediate UI badge and list recalculations.
 */
class NotificationStore(
    context: Context? = null,
    injectedPrefs: SharedPreferences? = null
) {
    private val prefs: SharedPreferences = injectedPrefs
        ?: context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ?: error("Context or SharedPreferences must be provided")

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        notifyUpdated()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    fun getDismissedIds(): Set<String> {
        return prefs.getStringSet(KEY_DISMISSED_IDS, emptySet()) ?: emptySet()
    }

    fun dismissNotification(id: String) {
        val current = getDismissedIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet(KEY_DISMISSED_IDS, current).apply()
        notifyUpdated()
    }

    fun dismissAll(ids: Collection<String>) {
        val current = getDismissedIds().toMutableSet()
        current.addAll(ids)
        prefs.edit()
            .putStringSet(KEY_DISMISSED_IDS, current)
            .putBoolean(KEY_ALL_CLEARED, true)
            .putLong(KEY_CLEARED_TIMESTAMP, System.currentTimeMillis())
            .apply()
        notifyUpdated()
    }

    fun isAllCleared(): Boolean {
        return prefs.getBoolean(KEY_ALL_CLEARED, false)
    }

    fun getReadIds(): Set<String> {
        return prefs.getStringSet(KEY_READ_IDS, emptySet()) ?: emptySet()
    }

    fun markAsRead(id: String) {
        val current = getReadIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet(KEY_READ_IDS, current).apply()
        notifyUpdated()
    }

    fun markAllAsRead(ids: Collection<String>) {
        val current = getReadIds().toMutableSet()
        current.addAll(ids)
        prefs.edit().putStringSet(KEY_READ_IDS, current).apply()
        notifyUpdated()
    }

    fun isDismissed(id: String): Boolean {
        return getDismissedIds().contains(id)
    }

    fun isRead(id: String): Boolean {
        return getReadIds().contains(id)
    }

    fun clearHistory() {
        prefs.edit().clear().apply()
        notifyUpdated()
    }

    companion object {
        private const val PREFS_NAME = "hishab_notifications_store"
        private const val KEY_DISMISSED_IDS = "dismissed_notification_ids"
        private const val KEY_READ_IDS = "read_notification_ids"
        private const val KEY_ALL_CLEARED = "is_all_cleared"
        private const val KEY_CLEARED_TIMESTAMP = "cleared_timestamp"

        private val _updates = MutableStateFlow(0)
        val updates = _updates.asStateFlow()

        fun notifyUpdated() {
            _updates.value++
        }
    }
}
