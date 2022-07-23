package com.looker.droidify.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.looker.core_common.file.deleteOldIcons
import com.looker.core_common.file.deleteOldReleases
import com.looker.core_common.file.deletePartialFiles
import com.looker.core_common.file.deleteTemporaryFiles
import com.looker.core_datastore.UserPreferencesRepository
import kotlin.time.toJavaDuration

class CleanUpWorker(
	context: Context,
	workerParams: WorkerParameters,
	private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {
	companion object {
		private const val TAG = "CleanUpWorker"
	}

	override suspend fun doWork(): Result = try {
		val context = applicationContext
		context.deleteOldIcons()
		context.deleteOldReleases()
		context.deletePartialFiles()
		context.deleteTemporaryFiles()
		Result.success()
	} catch (e: Exception) {
		Log.e(TAG, "doWork: Failed to clean up", e)
		Result.failure()
	}

	suspend fun periodic(context: Context) {
		val workManager = WorkManager.getInstance(context)
		val interval = userPreferencesRepository.fetchInitialPreferences().cleanUpDuration
		val constraints = Constraints.Builder()
			.setRequiresBatteryNotLow(true)
			.setRequiresDeviceIdle(true)
			.build()

		val cleanCache =
			PeriodicWorkRequestBuilder<CleanUpWorker>(interval.toJavaDuration())
				.setConstraints(constraints)
				.build()

		workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, cleanCache)
		Log.d(
			TAG,
			"periodic: Scheduled periodic task and will be executed in every ${interval.inWholeHours} hours"
		)
	}

	fun force(context: Context) {
		val workManager = WorkManager.getInstance(context)
		val cleanCache = OneTimeWorkRequestBuilder<CleanUpWorker>().build()
		workManager.enqueueUniqueWork(
			"$TAG.force",
			ExistingWorkPolicy.APPEND_OR_REPLACE,
			cleanCache
		)
		Log.d(TAG, "force: Started forced cache clean up")
	}
}

