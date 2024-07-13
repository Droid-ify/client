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
import com.looker.core.domain.RepoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workParams: WorkerParameters,
    private val repoRepository: RepoRepository
) : CoroutineWorker(appContext, workParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        appContext.syncForegroundInfo()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(SYNC_WORK, "Start Sync")
        setForegroundAsync(appContext.syncForegroundInfo())
        val isSuccess = try {
            repoRepository.syncAll()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure()
        }
        if (isSuccess) Result.success() else Result.failure()
    }

    companion object {
        private const val SYNC_WORK = "sync_work"

        fun cancelSyncWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK)
        }

        fun scheduleSyncWork(context: Context, constraints: Constraints) {
            WorkManager.getInstance(context).apply {
                val work = PeriodicWorkRequestBuilder<DelegatingWorker>(12L, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .setInputData(SyncWorker::class.delegatedData())
                    .build()
                enqueueUniquePeriodicWork(SYNC_WORK, ExistingPeriodicWorkPolicy.REPLACE, work)
            }
        }

        fun startSyncWork(context: Context) {
            WorkManager.getInstance(context).apply {
                val netRequired = Constraints(
                    requiredNetworkType = NetworkType.CONNECTED
                )
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
