package com.looker.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.looker.core.data.fdroid.repository.RepoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
	@Assisted private val appContext: Context,
	@Assisted workerParams: WorkerParameters,
	private val repoRepository: RepoRepository
) : CoroutineWorker(appContext, workerParams) {

	override suspend fun getForegroundInfo(): ForegroundInfo = appContext.syncForegroundInfo()

	override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
		val syncedSuccessfully = async { repoRepository.sync() }
		if (syncedSuccessfully.await()) Result.success() else Result.retry()
	}

	companion object {

	}
}