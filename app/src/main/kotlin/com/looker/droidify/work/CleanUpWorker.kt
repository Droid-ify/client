package com.looker.droidify.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.looker.core_common.cache.Cache
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

@HiltWorker
class CleanUpWorker @AssistedInject constructor(
	@Assisted context: Context,
	@Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
	companion object {
		const val TAG = "CleanUpWorker"

		private val constraints = Constraints.Builder()
			.setRequiresBatteryNotLow(true)
			.build()

		// TODO: Use variable time durations
		val periodicWork =
			PeriodicWorkRequestBuilder<DelegatingWorker>(12.hours.toJavaDuration())
				.setConstraints(constraints)
				.setInputData(CleanUpWorker::class.delegatedData())
				.build()

		val forceWork =
			OneTimeWorkRequestBuilder<DelegatingWorker>()
				.setInputData(CleanUpWorker::class.delegatedData())
				.build()
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

