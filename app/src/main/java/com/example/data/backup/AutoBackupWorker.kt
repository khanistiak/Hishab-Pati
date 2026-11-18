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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * WorkManager worker executing periodic automated cloud backup to user's isolated Firestore container.
 */
class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val user = Firebase.auth.currentUser
        if (user == null) {
            Log.w("AutoBackupWorker", "Skipping auto backup: No authenticated Google user.")
            return@withContext Result.success()
        }

        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val backupManager = JsonBackupManager(db)
            val firestoreRepository = FirestoreWalletRepository(applicationContext)

            val json = backupManager.createBackupJson()
            val accountsCount = db.accountDao().getAllAccounts().first().size
            val transactionsCount = db.transactionDao().getAllTransactions().first().size
            val categoriesCount = db.categoryDao().getAllCategories().first().size
            val debtsCount = db.debtDao().getAllDebts().first().size

            val result = firestoreRepository.saveCloudBackup(
                jsonString = json,
                accountsCount = accountsCount,
                transactionsCount = transactionsCount,
                categoriesCount = categoriesCount,
                debtsCount = debtsCount
            )

            result.fold(
                onSuccess = {
                    val now = System.currentTimeMillis()
                    val size = json.toByteArray(Charsets.UTF_8).size.toLong()
                    val prefs = applicationContext.getSharedPreferences("backup_restore_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putLong("last_cloud_backup_time", now)
                        .putLong("last_cloud_backup_size", size)
                        .apply()
                    Log.i("AutoBackupWorker", "Auto backup succeeded for user ${user.uid} ($size bytes) at $now")
                    Result.success()
                },
                onFailure = { err ->
                    Log.e("AutoBackupWorker", "Auto backup upload failed: ${err.message}", err)
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
            )
        } catch (e: Exception) {
            Log.e("AutoBackupWorker", "Auto backup encountered exception", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
