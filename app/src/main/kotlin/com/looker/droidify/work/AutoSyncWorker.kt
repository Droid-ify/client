package com.looker.droidify.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class AutoSyncWorker @AssistedInject constructor(
	@Assisted context: Context,
	@Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
	companion object {
		const val TAG = "AutoSyncWorker"
	}

	private val syncService = Connection(
		serviceClass = SyncService::class.java,
		onBind = null,
		onUnbind = null
	)

	override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
		try {
			Log.i(TAG, "doWork: Started AutoSync")
			syncService.binder?.sync(SyncService.SyncRequest.AUTO)
			Result.success()
		} catch (e: Exception) {
			Log.e(TAG, "doWork: Failed to autoSync", e)
			Result.failure()
		}
	}

	data class SyncConditions(
		val networkType: NetworkType,
		val pluggedIn: Boolean = false,
		val batteryNotLow: Boolean = true,
		val canSync: Boolean = true
	)
}