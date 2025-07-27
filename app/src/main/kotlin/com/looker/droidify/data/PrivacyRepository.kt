package com.looker.droidify.data

import com.looker.droidify.data.local.dao.RBLogDao
import com.looker.droidify.data.local.model.RBLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class PrivacyRepository(val rbDao: RBLogDao) {
    private val cc = Dispatchers.IO

    fun getRBLogs(packageName: String): Flow<List<RBLogEntity>> = rbDao.getFlow(packageName)
        .flowOn(cc)

    suspend fun upsertRBLogs(logs: List<RBLogEntity>) {
        rbDao.upsert(logs)
    }
}
