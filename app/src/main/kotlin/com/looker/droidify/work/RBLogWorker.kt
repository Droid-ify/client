package com.looker.droidify.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.looker.droidify.data.PrivacyRepository
import com.looker.droidify.data.local.model.RBData
import com.looker.droidify.data.local.model.toLogs
import com.looker.droidify.network.Downloader
import com.looker.droidify.sync.JsonParser
import com.looker.droidify.utility.common.extension.tempFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class RBLogWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val privacyRepository: PrivacyRepository,
    private val downloader: Downloader,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            fetchLogs()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                Log.e("RBLogWorker", "Failed to fetch logs", it)
                Result.failure()
            },
        )
    }

    private suspend fun fetchLogs() {
        withContext(Dispatchers.IO) {
            context.tempFile { target ->
                downloader.downloadToFile(url = BASE_URL, target = target)
                val logs: Map<String, List<RBData>> = JsonParser.decodeFromString(target.readText())
                privacyRepository.upsertRBLogs(logs.toLogs())
            }
        }
    }

    companion object {
        private const val BASE_URL =
            "https://codeberg.org/IzzyOnDroid/rbtlog/raw/branch/izzy/log/index.json"

        fun fetchRBLogs(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                "rb_index",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RBLogWorker>().build(),
            )
        }
    }
}
