package com.looker.droidify.data.local.dao

import com.looker.droidify.data.local.BaseDatabaseTest
import com.looker.droidify.data.local.model.DownloadStats
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStatsDaoTest : BaseDatabaseTest() {

    private lateinit var downloadStatsDao: DownloadStatsDao

    override fun initDao() {
        downloadStatsDao = database.downloadStatsDao()
    }

    private fun createStats(
        packageName: String = "com.example.app",
        source: String = "IzzyOnDroid",
        timestamp: Long = 1000L,
        downloads: Long = 500L,
    ) = DownloadStats(
        packageName = packageName,
        source = source,
        timestamp = timestamp,
        downloads = downloads,
    )

    @Test
    fun insertAndQueryTotal() = runDbTest {
        val stats = createStats(downloads = 100)
        downloadStatsDao.insert(listOf(stats))

        val total = downloadStatsDao.total("com.example.app").first()
        assertEquals(100L, total)
    }

    @Test
    fun totalSumsMultipleEntries() = runDbTest {
        val stats1 = createStats(timestamp = 1000L, downloads = 100)
        val stats2 = createStats(timestamp = 2000L, downloads = 200)
        downloadStatsDao.insert(listOf(stats1, stats2))

        val total = downloadStatsDao.total("com.example.app").first()
        assertEquals(300L, total)
    }

    @Test
    fun totalSinceFiltersOlderEntries() = runDbTest {
        val old = createStats(timestamp = 500L, downloads = 100)
        val recent = createStats(timestamp = 1500L, downloads = 200)
        downloadStatsDao.insert(listOf(old, recent))

        val total = downloadStatsDao.totalSince("com.example.app", since = 1000L).first()
        assertEquals(200L, total)
    }

    @Test
    fun totalReturnsZeroForUnknownPackage() = runDbTest {
        val total = downloadStatsDao.total("com.example.nonexistent").first()
        assertEquals(0L, total)
    }

    @Test
    fun insertReplacesOnConflict() = runDbTest {
        val original = createStats(downloads = 100)
        downloadStatsDao.insert(listOf(original))

        val updated = createStats(downloads = 999)
        downloadStatsDao.insert(listOf(updated))

        val total = downloadStatsDao.total("com.example.app").first()
        assertEquals(999L, total)
    }

    @Test
    fun multiplePackagesTrackedIndependently() = runDbTest {
        val stats1 = createStats(packageName = "com.app.one", downloads = 100)
        val stats2 = createStats(packageName = "com.app.two", downloads = 200)
        downloadStatsDao.insert(listOf(stats1, stats2))

        assertEquals(100L, downloadStatsDao.total("com.app.one").first())
        assertEquals(200L, downloadStatsDao.total("com.app.two").first())
    }
}
