package com.example.data.backup

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit

enum class AutoBackupFrequency(val labelEn: String, val labelBn: String) {
    OFF("Off", "বন্ধ"),
    DAILY("Daily", "দৈনিক"),
    WEEKLY("Weekly", "সাপ্তাহিক")
}

object AutoBackupScheduler {
    const val UNIQUE_WORK_NAME = "hishab_pati_cloud_auto_backup"
    private const val TAG = "AutoBackupScheduler"

    private fun getWorkManagerSafely(context: Context): WorkManager? {
        return try {
            WorkManager.getInstance(context)
        } catch (e: IllegalStateException) {
            try {
                val config = Configuration.Builder().build()
                WorkManager.initialize(context, config)
                WorkManager.getInstance(context)
            } catch (ex: Exception) {
                Log.w(TAG, "WorkManager could not be initialized in this environment: ${ex.message}")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to obtain WorkManager instance: ${e.message}")
            null
        }
    }

    /**
     * Schedules periodic auto-backup with WorkManager.
     * Enforces that the user is authenticated and frequency is not OFF.
     * Uses ExistingPeriodicWorkPolicy.UPDATE to prevent duplicate schedules and overlapping jobs.
     */
    fun schedule(context: Context, frequency: AutoBackupFrequency) {
        val workManager = getWorkManagerSafely(context) ?: return
        val currentUser = Firebase.auth.currentUser

        if (frequency == AutoBackupFrequency.OFF || currentUser == null) {
            Log.d(TAG, "Cancelling auto-backup work (frequency: $frequency, user: ${currentUser?.uid})")
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val (intervalDays, timeUnit) = when (frequency) {
            AutoBackupFrequency.DAILY -> Pair(1L, TimeUnit.DAYS)
            AutoBackupFrequency.WEEKLY -> Pair(7L, TimeUnit.DAYS)
            AutoBackupFrequency.OFF -> return
        }

        val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalDays, timeUnit)
            .setConstraints(constraints)
            .build()

        Log.i(TAG, "Enqueuing unique periodic backup: $frequency for user: ${currentUser.uid}")
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /**
     * Cancels any active auto-backup periodic work (e.g., on sign-out).
     */
    fun cancel(context: Context) {
        Log.d(TAG, "Cancelling scheduled auto-backup work")
        val workManager = getWorkManagerSafely(context) ?: return
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
