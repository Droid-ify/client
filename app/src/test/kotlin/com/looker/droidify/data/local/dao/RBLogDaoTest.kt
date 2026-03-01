package com.looker.droidify.data.local.dao

import com.looker.droidify.data.local.BaseDatabaseTest
import com.looker.droidify.data.local.model.RBLogEntity
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RBLogDaoTest : BaseDatabaseTest() {

    private lateinit var rbLogDao: RBLogDao

    override fun initDao() {
        rbLogDao = database.rbLogDao()
    }

    private fun createLog(
        hash: String = "abc123",
        packageName: String = "com.example.app",
        timestamp: Long = 1000L,
        versionCode: Int = 100,
        reproducible: Boolean? = true,
    ) = RBLogEntity(
        hash = hash,
        repository = "https://rb.example.com",
        apkUrl = "https://example.com/app.apk",
        packageName = packageName,
        versionCode = versionCode,
        versionName = "1.0.0",
        tag = "v1.0.0",
        commit = "deadbeef",
        timestamp = timestamp,
        reproducible = reproducible,
        error = null,
    )

    @Test
    fun upsertAndGet() = runDbTest {
        val log = createLog()
        rbLogDao.upsert(listOf(log))

        val result = rbLogDao.get("com.example.app")
        assertEquals(1, result.size)
        assertEquals(log, result[0])
    }

    @Test
    fun getReturnsEmptyListWhenNoMatch() = runDbTest {
        val result = rbLogDao.get("com.example.nonexistent")
        assertTrue(result.isEmpty())
    }

    @Test
    fun upsertUpdatesExistingEntry() = runDbTest {
        val log = createLog(reproducible = false)
        rbLogDao.upsert(listOf(log))

        val updated = log.copy(reproducible = true)
        rbLogDao.upsert(listOf(updated))

        val result = rbLogDao.get("com.example.app")
        assertEquals(1, result.size)
        assertEquals(true, result[0].reproducible)
    }

    @Test
    fun getFlowEmitsUpdates() = runDbTest {
        val log = createLog()
        rbLogDao.upsert(listOf(log))

        val result = rbLogDao.getFlow("com.example.app").first()
        assertEquals(1, result.size)
        assertEquals(log, result[0])
    }

    @Test
    fun deleteRemovesEntry() = runDbTest {
        val log = createLog()
        rbLogDao.upsert(listOf(log))
        assertEquals(1, rbLogDao.get("com.example.app").size)

        rbLogDao.delete(log)
        assertTrue(rbLogDao.get("com.example.app").isEmpty())
    }

    @Test
    fun multipleLogsForSamePackage() = runDbTest {
        val log1 = createLog(hash = "hash1", timestamp = 1000L)
        val log2 = createLog(hash = "hash2", timestamp = 2000L)
        rbLogDao.upsert(listOf(log1, log2))

        val result = rbLogDao.get("com.example.app")
        assertEquals(2, result.size)
    }
}
