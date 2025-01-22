package com.looker.droidify.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.datastore.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class CleanUpWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "CleanUpWorker"

        fun removeAllSchedules(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(TAG)
        }

        fun scheduleCleanup(context: Context, duration: Duration) {
            val workManager = WorkManager.getInstance(context)
            val cleanup = PeriodicWorkRequestBuilder<CleanUpWorker>(duration.toJavaDuration())
                .build()

            workManager.enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                cleanup
            )
            Log.i(TAG, "Periodic work enqueued with duration: $duration")
        }

        fun force(context: Context) {
            val cleanup = OneTimeWorkRequestBuilder<CleanUpWorker>()
                .build()

            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                "$TAG.force",
                ExistingWorkPolicy.KEEP,
                cleanup
            )
            Log.i(TAG, "Forced cleanup enqueued")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "doWork: Started Cleanup")
            settingsRepository.setCleanupInstant()
            Cache.cleanup(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.i(TAG, "doWork: Failed to clean up", e)
            Result.failure()
        }
    }
}
