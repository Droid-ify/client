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
import com.looker.droidify.network.NetworkResponse
import com.looker.droidify.network.get
import com.looker.droidify.sync.JsonParser
import com.looker.droidify.utility.common.Constants
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.exceptCancellation
import com.looker.droidify.utility.common.toForegroundInfo
import com.looker.droidify.utility.notifications.createRbNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.*
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@HiltWorker
class RBLogWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val privacyRepository: PrivacyRepository,
    private val httpClient: OkHttpClient,
) : CoroutineWorker(context, params) {

    @OptIn(ExperimentalTime::class)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        setForegroundAsync(
            context.createRbNotification()
                .toForegroundInfo(Constants.NOTIFICATION_ID_RB_DOWNLOAD)
        )

        val target = Cache.getTemporaryFile(context)
        try {
            val response = httpClient.get(url = BASE_URL, target = target)
            if (response is NetworkResponse.Success) {
                val logs: Map<String, List<RBData>> =
                    JsonParser.decodeFromString<Map<String, List<RBData>>>(target.readText())
                privacyRepository.upsertRBLogs(
                    lastModified = response.lastModified ?: Date(),
                    logs = logs.toLogs()
                )
            }
            Log.i(TAG, "Fetched and saved RB Logs")
            Result.success()
        } catch (e: Exception) {
            e.exceptCancellation()
            Log.e(TAG, "Failed to fetch logs", e)
            Result.failure()
        } finally {
            withContext(NonCancellable) {
                target.delete()
            }
        }
    }

    companion object {
        private const val TAG = "RBLogWorker"
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
