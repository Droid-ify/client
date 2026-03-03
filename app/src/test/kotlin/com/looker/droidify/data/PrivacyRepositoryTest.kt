package com.looker.droidify.data

import app.cash.turbine.test
import com.looker.droidify.data.local.dao.DownloadStatsDao
import com.looker.droidify.data.local.dao.RBLogDao
import com.looker.droidify.data.local.model.DownloadStats
import com.looker.droidify.data.local.model.RBLogEntity
import com.looker.droidify.datastore.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrivacyRepositoryTest {

    private lateinit var rbLogDao: RBLogDao
    private lateinit var downloadStatsDao: DownloadStatsDao
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var repository: PrivacyRepository

    @BeforeEach
    fun setup() {
        rbLogDao = mockk(relaxed = true)
        downloadStatsDao = mockk(relaxed = true)
        settingsRepo = mockk(relaxed = true)
        repository = PrivacyRepository(rbLogDao, downloadStatsDao, settingsRepo)
    }

    private fun testLog(
        packageName: String = "com.example.app",
        hash: String = "abc123",
    ) = RBLogEntity(
        hash = hash,
        repository = "https://rb.example.com",
        apkUrl = "https://example.com/app.apk",
        packageName = packageName,
        versionCode = 100,
        versionName = "1.0.0",
        tag = "v1.0.0",
        commit = "deadbeef",
        timestamp = 1000L,
        reproducible = true,
        error = null,
    )

    @Test
    fun `getRBLogs returns flow from dao`() = runTest {
        val logs = listOf(testLog())
        coEvery { rbLogDao.getFlow("com.example.app") } returns flowOf(logs)

        repository.getRBLogs("com.example.app").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("com.example.app", result[0].packageName)
            awaitComplete()
        }
    }

    @Test
    fun `getLatestDownloadStats returns total from dao`() = runTest {
        coEvery { downloadStatsDao.total("com.example.app") } returns flowOf(500L)

        repository.getLatestDownloadStats("com.example.app").test {
            assertEquals(500L, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `upsertRBLogs updates settings and dao`() = runTest {
        val date = Date()
        val logs = listOf(testLog())

        repository.upsertRBLogs(date, logs)

        coVerify { settingsRepo.setRbLogLastModified(date) }
        coVerify { rbLogDao.upsert(logs) }
    }

    @Test
    fun `save delegates to download stats dao`() = runTest {
        val stats = listOf(
            DownloadStats("com.example.app", "IzzyOnDroid", 1000L, 500L),
        )

        repository.save(stats)

        coVerify { downloadStatsDao.insert(stats) }
    }
}
