package com.looker.droidify.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.work.di.DelegatingWorker
import com.looker.droidify.work.di.delegatedData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

@HiltWorker
class AutoSyncWorker @AssistedInject constructor(
	@Assisted context: Context,
	@Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
	companion object {
		const val TAG = "AutoSyncWorker"

		val periodicWorkBuilder =
			PeriodicWorkRequestBuilder<DelegatingWorker>(12.hours.toJavaDuration())
				.setInputData(AutoSyncWorker::class.delegatedData())
	}

	private val syncService = Connection(
		serviceClass = SyncService::class.java,
		onBind = null,
		onUnbind = null
	)

	override suspend fun doWork(): Result = try {
		Log.i(CleanUpWorker.TAG, "doWork: Started AutoSync")
		syncService.binder?.sync(SyncService.SyncRequest.AUTO)
		Result.success()
	} catch (e: Exception) {
		Log.e(TAG, "doWork: Failed to autoSync", e)
		Result.failure()
	}

	data class SyncConditions(
		val networkType: NetworkType,
		val pluggedIn: Boolean = false,
		val batteryNotLow: Boolean = true,
		val canSync: Boolean = true
	)
}