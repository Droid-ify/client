package com.looker.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.looker.core.datastore.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
	@Assisted private val appContext: Context,
	@Assisted workerParams: WorkerParameters,
	private val userPreferencesRepository: UserPreferencesRepository
): CoroutineWorker(appContext, workerParams) {

	override suspend fun getForegroundInfo(): ForegroundInfo = appContext.syncForegroundInfo()

	override suspend fun doWork(): Result {
		TODO("Not yet implemented")
	}



}