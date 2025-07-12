package com.looker.droidify.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.looker.droidify.domain.PrivacyRepository
import com.looker.droidify.network.RBLogAPI
import com.looker.droidify.network.toLogs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class RBLogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val rbAPI: RBLogAPI,
    private val privacyRepository: PrivacyRepository,
    //TODO private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            fetchLogs()
        }.fold(
            onSuccess = {
                Result.success()
            },
            onFailure = {
                Log.e(this::javaClass.name, "Failed fetching exodus trackers", it)
                Result.failure(workDataOf("exception" to it.message))
            },
        )
    }

    private suspend fun fetchLogs() {
        withContext(Dispatchers.IO) {
            val logs = rbAPI.getIndex()
            privacyRepository.upsertRBLogs(*logs.toLogs().toTypedArray())
        }
    }

    companion object {
        fun fetchRBLogs(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                "rb_index",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RBLogWorker>()
                    .build(),
            )
        }
    }
}
