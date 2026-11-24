package com.example.data.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.firebase.FirestoreWalletRepository
import com.example.data.local.AppDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * WorkManager worker executing periodic automated cloud backup to user's isolated Firestore container.
 */
class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (user == null) {
            Log.w("AutoBackupWorker", "Skipping auto backup: No authenticated Google user.")
            return@withContext Result.success()
        }

        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val backupManager = JsonBackupManager(db)
            val firestoreRepository = FirestoreWalletRepository(applicationContext)

            val snapshot = backupManager.createBackupSnapshot(prettyPrint = false)
            if (!snapshot.hasUserData) {
                Log.i("AutoBackupWorker", "Skipping auto backup: Local database has no user data to upload yet.")
                return@withContext Result.success()
            }

            val result = firestoreRepository.saveCloudBackup(
                jsonString = snapshot.json,
                accountsCount = snapshot.accountsCount,
                transactionsCount = snapshot.transactionsCount,
                categoriesCount = snapshot.categoriesCount,
                debtsCount = snapshot.debtsCount,
                subcategoriesCount = snapshot.subcategoriesCount,
                budgetsCount = snapshot.budgetsCount,
                savingsGoalsCount = snapshot.savingsGoalsCount,
                backupDate = snapshot.backupDate
            )

            result.fold(
                onSuccess = { info ->
                    val prefs = applicationContext.getSharedPreferences("hishab_pati_backup_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putLong("last_cloud_backup_time", info.uploadedAtTimestamp)
                        .putLong("last_cloud_backup_size", info.fileSizeBytes)
                        .putLong("last_cloud_sync_time", info.uploadedAtTimestamp)
                        .apply()
                    Log.i(
                        "AutoBackupWorker",
                        "Auto backup succeeded for user ${user.uid} (${info.fileSizeBytes} raw bytes, ${info.compressedSizeBytes} compressed, chunks=${info.chunkCount})"
                    )
                    Result.success()
                },
                onFailure = { err ->
                    Log.e("AutoBackupWorker", "Auto backup upload failed: ${err.message}", err)
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AutoBackupWorker", "Auto backup encountered exception", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
