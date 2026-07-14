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
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.network.Downloader
import com.looker.droidify.network.NetworkResponse
import com.looker.droidify.network.header.ifModifiedSince
import com.looker.droidify.sync.JsonParser
import com.looker.droidify.utility.common.Constants
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.exceptCancellation
import com.looker.droidify.utility.common.toForegroundInfo
import com.looker.droidify.utility.notifications.createRbNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.time.ExperimentalTime

@HiltWorker
class RBLogWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val privacyRepository: PrivacyRepository,
    private val settingsRepository: SettingsRepository,
    private val downloader: Downloader,
) : CoroutineWorker(context, params) {

    @OptIn(ExperimentalTime::class)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getInitial()
        if (!settings.rbLogsEnabled) {
            Log.i(TAG, "Reproducibility logs disabled, skipping")
            return@withContext Result.success()
        }
        val izzy = Database.RepositoryAdapter.getAll()
            .firstOrNull { it.fingerprint.equals(IZZY_FINGERPRINT, ignoreCase = true) }
        val mirrors = izzy
            ?.let { (listOf(it.address) + it.mirrors).distinct() }
            ?: emptyList()
        if (mirrors.isEmpty()) {
            Log.i(TAG, "IzzyOnDroid repo not found, skipping")
            return@withContext Result.success()
        }

        val start = settings.rbLogMirrorIndex.coerceIn(0, mirrors.lastIndex)
        val ordered = mirrors.drop(start) + mirrors.take(start)
        val next = if (start + 1 < mirrors.size) start + 1 else 0
        settingsRepository.setRbLogMirrorIndex(next)

        val target = Cache.getTemporaryFile(context)
        try {
            val lastModified = settings.lastRbLogFetch
            for (mirror in ordered) {
                val url = mirror.rbLogUrl()
                val response = downloader.downloadToFile(
                    url = url,
                    target = target,
                    headers = {
                        if (lastModified != null) ifModifiedSince(Date(lastModified))
                    },
                )
                if (response !is NetworkResponse.Success) {
                    Log.w(TAG, "Failed to fetch rb logs from $url, trying next mirror")
                    continue
                }

                // Means it is modified, 304: Not-Modified
                if (response.statusCode != 304) {
                    val notification = context.createRbNotification()
                    setForegroundAsync(notification.toForegroundInfo(Constants.NOTIFICATION_ID_RB_DOWNLOAD))

                    val logs = JsonParser
                        .decodeFromString<Map<String, List<RBData>>>(target.readText())
                        .toLogs()

                    privacyRepository.upsertRBLogs(
                        lastModified = response.lastModified ?: Date(),
                        logs = logs,
                    )
                    Log.i(TAG, "Fetched, parsed and saved RB Logs from $url")
                }
                return@withContext Result.success()
            }
            Log.e(TAG, "All ${ordered.size} mirror(s) failed")
            Result.failure()
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

        private const val IZZY_FINGERPRINT =
            "3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A"

        // Available builders: "izzy.json" and "bt443.json" (Ben).
        private const val RB_LOG_PATH = "rbtlogs/izzy.json"

        private fun String.rbLogUrl(): String =
            removeSuffix("/").removeSuffix("repo").removeSuffix("/") + "/$RB_LOG_PATH"

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
