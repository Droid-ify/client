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
import com.looker.droidify.network.NetworkResponse
import com.looker.droidify.sync.JsonParser
import com.looker.droidify.utility.common.Constants
import com.looker.droidify.utility.common.extension.tempFile
import com.looker.droidify.utility.common.toForegroundInfo
import com.looker.droidify.utility.notifications.createRbNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class RBLogWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val privacyRepository: PrivacyRepository,
    private val downloader: Downloader,
) : CoroutineWorker(context, params) {

    @OptIn(ExperimentalTime::class)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        setForegroundAsync(
            context.createRbNotification()
                .toForegroundInfo(Constants.NOTIFICATION_ID_RB_DOWNLOAD)
        )

        runCatching {
            context.tempFile { target ->
                val response = downloader.downloadToFile(url = BASE_URL, target = target)
                if (response is NetworkResponse.Success) {
                    val logs: Map<String, List<RBData>> =
                        JsonParser.decodeFromString<Map<String, List<RBData>>>(target.readText())
                    privacyRepository.upsertRBLogs(
                        lastModified = response.lastModified
                            ?: Date(Clock.System.now().toEpochMilliseconds()),
                        logs = logs.toLogs()
                    )
                }
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                Log.e("RBLogWorker", "Failed to fetch logs", it)
                Result.failure()
            },
        )
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
