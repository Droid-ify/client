package com.looker.droidify.data

import com.looker.droidify.data.local.dao.DownloadStatsDao
import com.looker.droidify.data.local.dao.RBLogDao
import com.looker.droidify.data.local.model.DownloadStats
import com.looker.droidify.data.local.model.RBLogEntity
import com.looker.droidify.datastore.SettingsRepository
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class PrivacyRepository(
    private val rbDao: RBLogDao,
    private val downloadStatsDao: DownloadStatsDao,
    private val settingsRepo: SettingsRepository,
) {
    private val cc = Dispatchers.IO

    fun getRBLogs(packageName: String): Flow<List<RBLogEntity>> = rbDao.getFlow(packageName)
        .flowOn(cc)

    fun getLatestDownloadStats(packageName: String): Flow<Long> =
        downloadStatsDao.total(packageName).flowOn(cc)

    suspend fun upsertRBLogs(lastModified: Date, logs: List<RBLogEntity>) {
        settingsRepo.setRbLogLastModified(lastModified)
        rbDao.upsert(logs)
    }

    suspend fun save(downloadStats: List<DownloadStats>) {
        downloadStatsDao.insert(downloadStats)
    }
}
