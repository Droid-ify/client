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
import com.looker.droidify.data.local.model.DownloadStatsData
import com.looker.droidify.data.local.model.DownloadStatsData.Companion.toEpochMillis
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.network.Downloader
import com.looker.droidify.network.NetworkResponse
import com.looker.droidify.utility.common.Constants
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.exceptCancellation
import com.looker.droidify.utility.common.generateMonthlyFileNames
import com.looker.droidify.utility.common.toForegroundInfo
import com.looker.droidify.utility.notifications.createDownloadStatsNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.ktor.http.HttpStatusCode
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@HiltWorker
class DownloadStatsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val privacyRepository: PrivacyRepository,
    private val settingsRepo: SettingsRepository,
    private val downloader: Downloader,
) : CoroutineWorker(context, params) {

    val downloadSemaphores = Semaphore(2)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            setForegroundAsync(
                context
                    .createDownloadStatsNotification()
                    .toForegroundInfo(Constants.NOTIFICATION_ID_STATS_DOWNLOAD)
            )
            fetchData()
            Log.i(TAG, "Successfully processed download stats monthly files")
            Result.success()
        } catch (e: Exception) {
            e.exceptCancellation()
            Log.e(TAG, "Failed fetching download stats", e)
            Result.failure()
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun fetchData() = withContext(Dispatchers.IO) {
        supervisorScope {
            val lastModified = settingsRepo.getInitial().lastModifiedDownloadStats
            val fileNames = ConcurrentLinkedQueue(generateMonthlyFileNames(lastModified))
            val successfulResults = AtomicInt(0)
            val updatedResults = AtomicInt(0)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()

            Log.d(TAG, "Fetching ${fileNames.size} monthly files")
            while (fileNames.isNotEmpty()) {
                launch {
                    downloadSemaphores.withPermit {
                        val fileName = fileNames.poll() ?: return@withPermit
                        val target = Cache.getTemporaryFile(context)

                        Log.i(TAG, "Downloading $fileName")
                        val response = downloadFile(fileName, target)
                        Log.i(TAG, "Downloaded $fileName with $response")

                        if (response is NetworkResponse.Success) {
                            successfulResults.incrementAndFetch()
                            val isModified = response.statusCode != HttpStatusCode.NotModified.value
                            if (isModified) {
                                processDownloadStats(
                                    response = response,
                                    fileName = fileName,
                                    target = target
                                )
                                updatedResults.incrementAndFetch()
                            }
                        }
                        target.delete()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalAtomicApi::class, ExperimentalTime::class)
    private suspend fun processDownloadStats(
        response: NetworkResponse.Success,
        fileName: String,
        target: File,
    ) {
        Log.i(TAG, "Processing $fileName")
        val downloadStats = target.inputStream().use {
            DownloadStatsData.fromStream(it)
                .toDownloadStats(fileName.substringBefore('.').toEpochMillis())
        }
        privacyRepository.save(downloadStats)
        settingsRepo.updateLastModifiedDownloadStats(response.lastModified ?: Date())
        Log.d(TAG, "Processed updated file: $fileName")
    }

    private suspend fun downloadFile(
        fileName: String,
        target: File,
    ): NetworkResponse = downloader.downloadToFile(
        url = IZZY_STATS_MONTHLY + fileName,
        target = target,
    )

    companion object {
        private const val TAG = "DownloadStatsWorker"

        private const val IZZY_STATS_MONTHLY =
            "https://dlstats.izzyondroid.org/iod-stats-collector/stats/basic/monthly/"

        fun fetchDownloadStats(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                "download_stats",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<DownloadStatsWorker>().build(),
            )
        }
    }
}
