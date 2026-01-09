package com.looker.droidify.data.local.dao

import com.looker.droidify.data.local.BaseDatabaseTest
import com.looker.droidify.data.local.model.InstalledEntity
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InstalledDaoTest : BaseDatabaseTest() {

    private lateinit var installedDao: InstalledDao

    override fun initDao() {
        installedDao = database.installedDao()
    }

    @Test
    fun insertAndGetInstalledEntity() = runDbTest {
        // Given
        val entity = InstalledEntity(
            packageName = "com.example.app",
            version = "1.0.0",
            versionCode = 100,
            signature = "abcdef123456"
        )

        // When
        installedDao.insert(entity)
        val result = installedDao.get("com.example.app")

        // Then
        assertEquals(entity, result)
    }

    @Test
    fun getReturnsNullWhenPackageNotFound() = runDbTest {
        // When
        val result = installedDao.get("com.example.nonexistent")

        // Then
        assertNull(result)
    }

    @Test
    fun streamReturnsFlowOfInstalledEntity() = runDbTest {
        // Given
        val entity = InstalledEntity(
            packageName = "com.example.app",
            version = "1.0.0",
            versionCode = 100,
            signature = "abcdef123456"
        )
        installedDao.insert(entity)

        // When
        val result = installedDao.stream("com.example.app").first()

        // Then
        assertEquals(entity, result)
    }

    @Test
    fun streamAllReturnsFlowOfAllInstalledEntities() = runDbTest {
        // Given
        val entity1 = InstalledEntity(
            packageName = "com.example.app1",
            version = "1.0.0",
            versionCode = 100,
            signature = "abcdef123456"
        )
        val entity2 = InstalledEntity(
            packageName = "com.example.app2",
            version = "2.0.0",
            versionCode = 200,
            signature = "ghijkl789012"
        )
        installedDao.insertAll(listOf(entity1, entity2))

        // When
        val result = installedDao.streamAll().first()

        // Then
        assertEquals(2, result.size)
        assertEquals(listOf(entity1, entity2), result)
    }

    @Test
    fun insertAllInsertsMultipleEntities() = runDbTest {
        // Given
        val entity1 = InstalledEntity(
            packageName = "com.example.app1",
            version = "1.0.0",
            versionCode = 100,
            signature = "abcdef123456"
        )
        val entity2 = InstalledEntity(
            packageName = "com.example.app2",
            version = "2.0.0",
            versionCode = 200,
            signature = "ghijkl789012"
        )

        // When
        installedDao.insertAll(listOf(entity1, entity2))
        val result1 = installedDao.get("com.example.app1")
        val result2 = installedDao.get("com.example.app2")

        // Then
        assertEquals(entity1, result1)
        assertEquals(entity2, result2)
    }

    @Test
    fun replaceAllReplacesAllEntities() = runDbTest {
        // Given
        val entity1 = InstalledEntity(
            packageName = "com.example.app1",
            version = "1.0.0",
            versionCode = 100,
            signature = "abcdef123456"
        )
        val entity2 = InstalledEntity(
            packageName = "com.example.app2",
            version = "2.0.0",
            versionCode = 200,
            signature = "ghijkl789012"
        )
        installedDao.insertAll(listOf(entity1, entity2))

        val entity3 = InstalledEntity(
            packageName = "com.example.app3",
            version = "3.0.0",
            versionCode = 300,
            signature = "mnopqr345678"
        )

        // When
        installedDao.replaceAll(listOf(entity3))
        val result = installedDao.streamAll().first()

        // Then
        assertEquals(1, result.size)
        assertEquals(entity3, result[0])
        assertNull(installedDao.get("com.example.app1"))
        assertNull(installedDao.get("com.example.app2"))
    }

    @Test
    fun deleteRemovesEntity() = runDbTest {
        // Given
        val entity = InstalledEntity(
            packageName = "com.example.app",
            version = "1.0.0",
            versionCode = 100,
            signature = "abcdef123456"
        )
        installedDao.insert(entity)

        // When
        val deleteCount = installedDao.delete("com.example.app")
        val result = installedDao.get("com.example.app")

        // Then
        assertEquals(1, deleteCount)
        assertNull(result)
    }

    @Test
    fun deleteAllRemovesAllEntities() = runDbTest {
        // Given
        val entity1 = InstalledEntity(
            packageName = "com.example.app1",
            version = "1.0.0",
            versionCode = 100,
            signature = "abcdef123456"
        )
        val entity2 = InstalledEntity(
            packageName = "com.example.app2",
            version = "2.0.0",
            versionCode = 200,
            signature = "ghijkl789012"
        )
        installedDao.insertAll(listOf(entity1, entity2))

        // When
        installedDao.deleteAll()
        val result = installedDao.streamAll().first()

        // Then
        assertEquals(0, result.size)
    }
}
