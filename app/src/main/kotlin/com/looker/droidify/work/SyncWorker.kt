package com.looker.droidify.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.looker.droidify.R
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.utility.common.createNotificationChannel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repoRepository: RepoRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val KEY_REPO_ID = "repo_id"
        private const val KEY_TRIGGER = "trigger"
        private const val TRIGGER_USER = "user"
        private const val TRIGGER_PERIODIC = "periodic"

        private val defaultConstraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueueUserSync(context: Context, repoId: Int? = null) {
            val data = Data.Builder()
                .putString(KEY_TRIGGER, TRIGGER_USER)
                .apply { if (repoId != null) putInt(KEY_REPO_ID, repoId) }
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(data)
                .setConstraints(defaultConstraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()

            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(
                    uniqueWorkName = "$TAG.user",
                    existingWorkPolicy = ExistingWorkPolicy.KEEP,
                    request = request,
                )
            Log.i(TAG, "User sync enqueued (repoId=$repoId)")
        }

        fun syncRepo(context: Context, repoId: Int) {
            enqueueUserSync(context, repoId)
        }

        fun schedulePeriodicSync(context: Context, repeatInterval: Duration) {
            val data = Data.Builder()
                .putString(KEY_TRIGGER, TRIGGER_PERIODIC)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(repeatInterval.toJavaDuration())
                .setInputData(data)
                .setConstraints(defaultConstraints)
                .addTag(TAG)
                .build()

            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    uniqueWorkName = TAG,
                    existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
                    request = request,
                )
            Log.i(TAG, "Periodic sync scheduled every $repeatInterval")
        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            Log.i(TAG, "All sync work cancelled")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repoId = if (inputData.hasKeyWithValueOfType(KEY_REPO_ID, Int::class.java)) {
            inputData.getInt(KEY_REPO_ID, -1).takeIf { it >= 0 }
        } else null
        try {
            val success = if (repoId != null) {
                val repo = repoRepository.getRepo(repoId)
                if (repo != null) {
                    setForeground(createForegroundInfo(repo.name, 0))
                    repoRepository.sync(repo)
                } else {
                    Log.w(TAG, "Repo not found for id=$repoId; falling back to syncAll")
                    repoRepository.syncAll()
                }
            } else {
                repoRepository.syncAll()
            }
            if (success) {
                Log.i(TAG, "Sync completed successfully (repoId=$repoId)")
                Result.success()
            } else {
                Log.w(TAG, "Sync reported failure (repoId=$repoId)")
                Result.retry()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Sync failed with exception", t)
            Result.retry()
        }
    }

    private fun createForegroundInfo(name: String, percent: Int): ForegroundInfo {
        val id = "sync_channel"
        val title = "Syncing: $name"
        val cancel = applicationContext.getString(R.string.cancel)
        val intent = WorkManager
            .getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        applicationContext.createNotificationChannel(
            id = id,
            name = "Sync channel",
            showBadge = true,
        )

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setProgress(100, percent, percent == -1)
            .setSmallIcon(R.drawable.ic_sync)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ForegroundInfo(124, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(124, notification)
        }
    }
}
