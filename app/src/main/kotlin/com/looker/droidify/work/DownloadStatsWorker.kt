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
import com.looker.droidify.data.PrivacyRepository
import com.looker.droidify.data.local.model.DownloadStatsData
import com.looker.droidify.data.local.model.toDownloadStats
import com.looker.droidify.network.Downloader
import com.looker.droidify.network.NetworkResponse
import com.looker.droidify.utility.common.extension.tempFile
import com.looker.droidify.utility.common.generateMonthlyFileNames
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.ktor.http.HttpStatusCode
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@HiltWorker
class DownloadStatsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val privacyRepository: PrivacyRepository,
    private val downloader: Downloader,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            fetchData()
        }.fold(
            onSuccess = { filesProcessed ->
                Log.i(TAG, "Successfully processed $filesProcessed monthly files")
                Result.success(workDataOf())
            },
            onFailure = { throwable ->
                Log.e(TAG, "Failed fetching download stats", throwable)
                Result.failure(workDataOf())
            },
        )
    }

    // TODO add progress indication
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun fetchData() {
        withContext(Dispatchers.IO) {
            val existingModifiedDates = getExistingModifiedDates()
            val fileNames = generateMonthlyFileNames()
            Log.d(TAG, "Fetching ${fileNames.size} monthly files")
            val successfulResults = AtomicInt(0)
            val updatedResults = AtomicInt(0)

            fileNames.forEach { fileName ->
                context.tempFile { target ->
                    async {
                        val lastModified = existingModifiedDates[fileName]
                        downloader.downloadToFile(
                            url = BASE_URL + fileName,
                            target = target,
                            headers = {
                                if (!lastModified.isNullOrEmpty()) {
                                    ifModifiedSince(lastModified)
                                }
                            },
                        )
                    }.await().let {
                        when {
                            it is NetworkResponse.Success && it.statusCode != HttpStatusCode.NotModified.value
                                -> {
                                successfulResults.incrementAndFetch()
                                updatedResults.incrementAndFetch()
                                val downloadStats = DownloadStatsData.fromStream(
                                    target.readChannel().toInputStream(),
                                ).toDownloadStats()
                                privacyRepository.upsertDownloadStats(downloadStats)
                                it.lastModified?.let { lastModified ->
                                    privacyRepository.upsertDownloadStatsFile(
                                        fileName = fileName,
                                        lastModified = lastModified.toString(),
                                        recordsCount = downloadStats.size,
                                    )
                                }
                                Log.d(TAG, "Processed updated file: $fileName")
                            }

                            it is NetworkResponse.Success
                                -> {
                                successfulResults.incrementAndFetch()
                                Log.d(TAG, "File not modified: $fileName")
                            }

                            else -> {
                                Log.d(TAG, "Failed downloading the file: $fileName")
                            }
                        }
                    }
                }
            }
            Log.i(
                TAG,
                "Fetching download stats summary: ${successfulResults.load()}/${fileNames.size} successful, " +
                    "${updatedResults.load()} updated, ${fileNames.size - successfulResults.load()} failed",
            )
        }
    }

    private suspend fun getExistingModifiedDates(): Map<String, String> =
        withContext(Dispatchers.IO) {
            try {
                privacyRepository.loadDownloadStatsModifiedMap()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get existing modified dates", e)
                emptyMap()
            }
        }

    companion object {
        private const val TAG = "DownloadStatsWorker"
        private const val BASE_URL =
            "https://dlstats.izzyondroid.org/iod-stats-collector/stats/upstream/monthly-in-days/"

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
