package com.looker.droidify.data.local.dao

import com.looker.droidify.data.local.BaseDatabaseTest
import com.looker.droidify.data.local.model.RepoEntity
import com.looker.droidify.data.model.Fingerprint
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RepoDaoTest : BaseDatabaseTest() {

    private lateinit var repoDao: RepoDao
    private lateinit var indexDao: IndexDao

    override fun initDao() {
        repoDao = database.repoDao()
        indexDao = database.indexDao()
    }

    private suspend fun insertTestRepo(
        address: String = "https://f-droid.org/repo",
        fingerprint: String = "ABC123",
        timestamp: Long? = 1000L,
    ): Int {
        val entity = RepoEntity(
            address = address,
            webBaseUrl = null,
            fingerprint = Fingerprint(fingerprint),
            timestamp = timestamp,
        )
        return indexDao.upsertRepo(entity)
    }

    @Test
    fun insertAndGetRepo() = runDbTest {
        val id = insertTestRepo()
        val result = repoDao.getRepo(id)

        assertNotNull(result)
        assertEquals("https://f-droid.org/repo", result!!.address)
        assertEquals(Fingerprint("ABC123"), result.fingerprint)
    }

    @Test
    fun getRepoReturnsNullWhenNotFound() = runDbTest {
        val result = repoDao.getRepo(999)
        assertNull(result)
    }

    @Test
    fun streamReturnsAllRepos() = runDbTest {
        insertTestRepo(address = "https://repo1.org/repo", fingerprint = "FP1")
        insertTestRepo(address = "https://repo2.org/repo", fingerprint = "FP2")

        val repos = repoDao.stream().first()
        assertEquals(2, repos.size)
    }

    @Test
    fun repoFlowEmitsUpdatedRepo() = runDbTest {
        val id = insertTestRepo()
        val repo = repoDao.repo(id).first()

        assertNotNull(repo)
        assertEquals("https://f-droid.org/repo", repo!!.address)
    }

    @Test
    fun deleteRemovesRepo() = runDbTest {
        val id = insertTestRepo()
        assertNotNull(repoDao.getRepo(id))

        repoDao.delete(id)
        assertNull(repoDao.getRepo(id))
    }

    @Test
    fun resetTimestampSetsTimestampToNull() = runDbTest {
        val id = insertTestRepo(timestamp = 5000L)
        val before = repoDao.getRepo(id)
        assertEquals(5000L, before!!.timestamp)

        repoDao.resetTimestamp(id)
        val after = repoDao.getRepo(id)
        assertNull(after!!.timestamp)
    }

    @Test
    fun getAddressByIdsReturnsCorrectMapping() = runDbTest {
        val id1 = insertTestRepo(address = "https://repo1.org", fingerprint = "FP1")
        val id2 = insertTestRepo(address = "https://repo2.org", fingerprint = "FP2")

        val result = repoDao.getAddressByIds(listOf(id1, id2))
        assertEquals(2, result.size)
        assertEquals("https://repo1.org", result[id1])
        assertEquals("https://repo2.org", result[id2])
    }
}
