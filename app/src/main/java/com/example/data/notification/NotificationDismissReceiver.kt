package com.example.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_ID) ?: return
        val store = NotificationStore(context)
        store.dismissNotification(notifId)
    }
}
