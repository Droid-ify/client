package com.looker.core.data.fdroid.sync.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.looker.core.data.fdroid.repository.RepoRepository
import com.looker.core.datastore.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
	@Assisted private val appContext: Context,
	@Assisted workParams: WorkerParameters,
	private val userPreferencesRepository: UserPreferencesRepository,
	private val repoRepository: RepoRepository
) : CoroutineWorker(appContext, workParams) {

	override suspend fun getForegroundInfo(): ForegroundInfo =
		appContext.syncForegroundInfo()

	override suspend fun doWork(): Result {
		return try {
			Log.i(SYNC_WORK, "Start Sync")
			val unstable = userPreferencesRepository.fetchInitialPreferences().unstableUpdate
			repoRepository.syncAll(appContext, unstable)
			Result.success()
		} catch (e: Exception) {
			Log.i(SYNC_WORK, e.message.toString(), e)
			Result.failure()
		}
	}

	companion object {
		private const val SYNC_WORK = "sync_work"
		private const val SYNC_WORK_PERIODIC = "sync_work"

		fun scheduleSyncWork(context: Context, constraints: Constraints) {
			WorkManager.getInstance(context).apply {
				val work = PeriodicWorkRequestBuilder<DelegatingWorker>(12L, TimeUnit.HOURS)
					.setConstraints(constraints)
					.setInputData(SyncWorker::class.delegatedData())
					.build()
				enqueueUniquePeriodicWork(
					SYNC_WORK_PERIODIC,
					ExistingPeriodicWorkPolicy.REPLACE,
					work
				)
			}
		}

		fun startSyncWork(context: Context) {
			WorkManager.getInstance(context).apply {
				val netRequired = Constraints.Builder()
					.setRequiredNetworkType(NetworkType.CONNECTED)
					.build()
				val work = OneTimeWorkRequestBuilder<DelegatingWorker>()
					.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
					.setConstraints(netRequired)
					.setInputData(SyncWorker::class.delegatedData())
					.build()
				beginUniqueWork(
					SYNC_WORK,
					ExistingWorkPolicy.REPLACE,
					work
				).enqueue()
			}
		}
	}
}