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
import com.looker.core.common.cache.Cache
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@HiltWorker
class CleanUpWorker @AssistedInject constructor(
	@Assisted context: Context,
	@Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
	companion object {
		const val TAG = "CleanUpWorker"

		fun removeAllSchedules(context: Context) {
			val workManager = WorkManager.getInstance(context)
			workManager.cancelUniqueWork(TAG)
		}

		fun scheduleCleanup(context: Context, duration: Duration) {
			val workManager = WorkManager.getInstance(context)
			val cleanup = PeriodicWorkRequestBuilder<DelegatingWorker>(duration.toJavaDuration())
				.setInputData(CleanUpWorker::class.delegatedData())
				.build()

			workManager.enqueueUniquePeriodicWork(
				TAG,
				ExistingPeriodicWorkPolicy.REPLACE,
				cleanup
			)
			Log.d(TAG, "Periodic work enqueued with duration: $duration")
		}

		fun force(context: Context) {
			val cleanup = OneTimeWorkRequestBuilder<DelegatingWorker>()
				.setInputData(CleanUpWorker::class.delegatedData())
				.build()

			val workManager = WorkManager.getInstance(context)
			workManager.enqueueUniqueWork(
				"$TAG.force",
				ExistingWorkPolicy.KEEP,
				cleanup
			)
			Log.d(TAG, "Forced cleanup enqueued")
		}
	}

	override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
		try {
			Log.i(TAG, "doWork: Started Cleanup")
			val context = applicationContext
			Cache.cleanup(context)
			Result.success()
		} catch (e: Exception) {
			Log.e(TAG, "doWork: Failed to clean up", e)
			Result.failure()
		}
	}
}