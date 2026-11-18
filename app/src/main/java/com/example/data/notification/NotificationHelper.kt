package com.example.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.screens.notifications.AppNotification

object NotificationHelper {

    const val CHANNEL_ID = "hishab_pati_notifications"
    private const val CHANNEL_NAME = "Hishab Pati Alerts & Reminders"
    private const val CHANNEL_DESC = "Bill reminders, budget alerts, and transaction notifications"

    const val ACTION_NOTIFICATION_CLICK = "com.example.ACTION_NOTIFICATION_CLICK"
    const val ACTION_NOTIFICATION_DISMISSED = "com.example.ACTION_NOTIFICATION_DISMISSED"

    const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    const val EXTRA_DESTINATION_TYPE = "EXTRA_DESTINATION_TYPE"
    const val EXTRA_TARGET_ENTITY_ID = "EXTRA_TARGET_ENTITY_ID"
    const val EXTRA_TARGET_SEARCH_QUERY = "EXTRA_TARGET_SEARCH_QUERY"
    const val EXTRA_TARGET_AMOUNT = "EXTRA_TARGET_AMOUNT"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun postSystemNotification(
        context: Context,
        notif: AppNotification,
        isBn: Boolean
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Tap Intent -> Open app and navigate to the target screen
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_NOTIFICATION_CLICK
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, notif.id)
            putExtra(EXTRA_DESTINATION_TYPE, notif.destinationType.name)
            putExtra(EXTRA_TARGET_ENTITY_ID, notif.targetEntityId)
            putExtra(EXTRA_TARGET_SEARCH_QUERY, notif.targetSearchQuery)
            putExtra(EXTRA_TARGET_AMOUNT, notif.targetAmount ?: 0.0)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notif.id.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Intent -> BroadcastReceiver to update NotificationStore
        val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            action = ACTION_NOTIFICATION_DISMISSED
            putExtra(EXTRA_NOTIFICATION_ID, notif.id)
        }

        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            notif.id.hashCode() + 100000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isBn) notif.titleBn else notif.title
        val message = if (isBn) notif.messageBn else notif.message

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setAutoCancel(true)

        try {
            manager.notify(notif.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun cancelNotification(context: Context, notifId: String) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(notifId.hashCode())
    }

    fun cancelAllNotifications(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancelAll()
    }

    fun syncSystemNotifications(
        context: Context,
        notifications: List<AppNotification>,
        isBn: Boolean,
        enabled: Boolean
    ) {
        if (!enabled) {
            cancelAllNotifications(context)
            return
        }

        // Only post notifications that are active and not read
        val activeNotifs = notifications.filter { !it.isRead }
        activeNotifs.forEach { notif ->
            postSystemNotification(context, notif, isBn)
        }
    }
}
