package com.looker.droidify.data

import app.cash.turbine.test
import com.looker.droidify.data.local.dao.InstalledDao
import com.looker.droidify.data.local.model.InstalledEntity
import com.looker.droidify.model.InstalledItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InstalledRepositoryTest {

    private lateinit var installedDao: InstalledDao
    private lateinit var repository: InstalledRepository

    @BeforeEach
    fun setup() {
        installedDao = mockk(relaxed = true)
        repository = InstalledRepository(installedDao)
    }

    private fun testEntity(
        packageName: String = "com.example.app",
        version: String = "1.0.0",
        versionCode: Long = 100,
        signature: String = "abc123",
    ) = InstalledEntity(
        packageName = packageName,
        version = version,
        versionCode = versionCode,
        signature = signature,
    )

    @Test
    fun `get returns domain model when entity exists`() = runTest {
        coEvery { installedDao.get("com.example.app") } returns testEntity()

        val result = repository.get("com.example.app")

        assertNotNull(result)
        assertEquals("com.example.app", result!!.packageName)
        assertEquals("1.0.0", result.version)
        assertEquals(100L, result.versionCode)
    }

    @Test
    fun `get returns null when entity does not exist`() = runTest {
        coEvery { installedDao.get("com.example.nonexistent") } returns null

        val result = repository.get("com.example.nonexistent")
        assertNull(result)
    }

    @Test
    fun `getStream emits mapped domain model`() = runTest {
        coEvery { installedDao.stream("com.example.app") } returns flowOf(testEntity())

        repository.getStream("com.example.app").test {
            val item = awaitItem()
            assertNotNull(item)
            assertEquals("com.example.app", item!!.packageName)
            awaitComplete()
        }
    }

    @Test
    fun `getStream emits null when entity not found`() = runTest {
        coEvery { installedDao.stream("com.example.missing") } returns flowOf(null)

        repository.getStream("com.example.missing").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getAllStream emits mapped list`() = runTest {
        val entities = listOf(
            testEntity(packageName = "com.app.one"),
            testEntity(packageName = "com.app.two"),
        )
        coEvery { installedDao.streamAll() } returns flowOf(entities)

        repository.getAllStream().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertEquals("com.app.one", items[0].packageName)
            assertEquals("com.app.two", items[1].packageName)
            awaitComplete()
        }
    }

    @Test
    fun `put delegates to dao insert`() = runTest {
        val item = InstalledItem(
            packageName = "com.example.app",
            version = "2.0.0",
            versionCode = 200,
            signature = "def456",
        )

        repository.put(item)

        coVerify {
            installedDao.insert(
                match {
                    it.packageName == "com.example.app" &&
                            it.version == "2.0.0" &&
                            it.versionCode == 200L
                },
            )
        }
    }

    @Test
    fun `putAll delegates to dao replaceAll`() = runTest {
        val items = listOf(
            InstalledItem("com.app.one", "1.0", 1, "sig1"),
            InstalledItem("com.app.two", "2.0", 2, "sig2"),
        )

        repository.putAll(items)

        coVerify {
            installedDao.replaceAll(match { it.size == 2 })
        }
    }

    @Test
    fun `delete delegates to dao and returns count`() = runTest {
        coEvery { installedDao.delete("com.example.app") } returns 1

        val result = repository.delete("com.example.app")

        assertEquals(1, result)
        coVerify { installedDao.delete("com.example.app") }
    }
}
