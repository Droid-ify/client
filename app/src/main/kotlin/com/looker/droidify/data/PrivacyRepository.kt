package com.looker.droidify.data

import com.looker.droidify.data.local.dao.RBLogDao
import com.looker.droidify.data.local.model.RBLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PrivacyRepository(val rbDao: RBLogDao) {
    private val cc = Dispatchers.IO
    private val jcc = Dispatchers.IO + SupervisorJob()

    fun getRBLogs(packageName: String): Flow<List<RBLogEntity>> = rbDao.getFlow(packageName)
        .flowOn(cc)

    suspend fun upsertRBLogs(vararg logs: RBLogEntity) {
        withContext(jcc) {
            rbDao.upsert(*logs)
        }
    }
}
