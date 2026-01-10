package com.looker.droidify.data

import com.looker.droidify.data.local.dao.DownloadStatsDao
import com.looker.droidify.data.local.dao.DownloadStatsFileDao
import com.looker.droidify.data.local.dao.RBLogDao
import com.looker.droidify.data.local.model.DownloadStats
import com.looker.droidify.data.local.model.DownloadStatsFile
import com.looker.droidify.data.local.model.RBLogEntity
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.utility.common.extension.exceptCancellation
import com.looker.droidify.utility.common.getIsoDateOfMonthsAgo
import com.looker.droidify.utility.common.isoDateToInt
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class PrivacyRepository(
    private val rbDao: RBLogDao,
    private val downloadStatsDao: DownloadStatsDao,
    private val dsFileDao: DownloadStatsFileDao,
    private val settingsRepo: SettingsRepository,
) {
    private val cc = Dispatchers.IO

    fun getRBLogs(packageName: String): Flow<List<RBLogEntity>> = rbDao.getFlow(packageName)
        .flowOn(cc)

    fun getDownloadStats(packageName: String): Flow<List<DownloadStats>> =
        downloadStatsDao.getFlow(packageName)
            .flowOn(cc)

    fun getLatestDownloadStats(packageName: String): Flow<List<DownloadStats>> =
        downloadStatsDao.getFlowSince(packageName, getIsoDateOfMonthsAgo(3).isoDateToInt())
            .flowOn(cc)

    suspend fun loadDownloadStatsModifiedMap(): Map<String, String> =
        try {
            dsFileDao.getLastModifiedDates()
        } catch (e: Exception) {
            e.exceptCancellation()
            emptyMap()
        }

    suspend fun upsertRBLogs(lastModified: Date, logs: List<RBLogEntity>) {
        settingsRepo.setRbLogLastModified(lastModified)
        rbDao.upsert(logs)
    }

    suspend fun upsertDownloadStats(downloadStats: Collection<DownloadStats>) {
        downloadStatsDao.multipleUpserts(downloadStats)
    }

    suspend fun upsertDownloadStatsFile(
        fileName: String,
        lastModified: String,
        fileSize: Long? = null,
        recordsCount: Int? = null,
    ) {
        val metadata = DownloadStatsFile(
            fileName = fileName,
            lastModified = lastModified,
            lastFetched = System.currentTimeMillis(),
            fetchSuccess = true,
            fileSize = fileSize,
            recordsCount = recordsCount,
        )
        dsFileDao.upsert(metadata)
    }
}
