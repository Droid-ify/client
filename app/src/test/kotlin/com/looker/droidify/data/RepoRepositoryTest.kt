package com.looker.droidify.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.looker.droidify.data.encryption.Encrypted
import com.looker.droidify.data.encryption.EncryptionStorage
import com.looker.droidify.data.encryption.Key
import com.looker.droidify.data.local.droidifyDb
import com.looker.droidify.data.local.sql.DroidifyDb
import com.looker.droidify.data.model.Fingerprint
import io.mockk.every
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class RepoRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: DroidifyDb
    private lateinit var repoRepository: RepoRepository

    private val key = Key()
    private val encryptionStorage = mockk<EncryptionStorage> {
        every { key } returns flowOf(this@RepoRepositoryTest.key)
    }

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(
            JdbcSqliteDriver.IN_MEMORY,
            java.util.Properties().apply { put("foreign_keys", "true") },
        )
        DroidifyDb.Schema.create(driver)
        db = droidifyDb(driver)
        repoRepository = RepoRepository(db, encryptionStorage, Dispatchers.Unconfined)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun `insert creates enabled repository without auth`() = runTest {
        val repoId = repoRepository.insert(address = "https://repo.test/repo/")

        assertEquals(1, count("repository", "id = $repoId AND enabled = 1"))
        assertEquals(1, count("repository", "address = 'https://repo.test/repo'"))
        assertEquals(0, count("authentication"))
    }

    @Test
    fun `insert with credentials stores decryptable authentication`() = runTest {
        val repoId = repoRepository.insert(
            address = "https://repo.test/repo",
            username = "user",
            password = "secret",
        )

        assertEquals(1, count("authentication", "repoId = $repoId AND username = 'user'"))
        val (username, password, iv) = driver.executeQuery(
            identifier = null,
            sql = "SELECT username, password, initializationVector FROM authentication " +
                "WHERE repoId = $repoId",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(
                    listOf(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getBytes(2),
                    ),
                )
            },
            parameters = 0,
        ).value
        val decrypted = Encrypted(password as String).decrypt(key, iv as ByteArray)
        assertEquals("secret", decrypted)
        assertEquals("user", username)
    }

    @Test
    fun `setEnabled toggles flag`() = runTest {
        val repoId = repoRepository.insert(address = "https://repo.test/repo")

        repoRepository.setEnabled(repoId, false)

        assertEquals(1, count("repository", "id = $repoId AND enabled = 0"))
    }

    @Test
    fun `updateVersionInfo stores sync metadata`() = runTest {
        val repoId = repoRepository.insert(address = "https://repo.test/repo")

        repoRepository.updateVersionInfo(
            repoId = repoId,
            fingerprint = Fingerprint("ab".repeat(32)),
            timestamp = 1720000000000,
            etag = "etag-1",
        )

        assertEquals(
            1,
            count(
                "repository",
                "id = $repoId AND timestamp = 1720000000000 AND etag = 'etag-1' " +
                    "AND fingerprint IS NOT NULL",
            ),
        )
    }

    @Test
    fun `delete cascades to dependent rows`() = runTest {
        val repoId = repoRepository.insert(
            address = "https://repo.test/repo",
            username = "user",
            password = "secret",
        )
        db.repositoryQueries.insertMirror(
            url = "https://mirror.test/repo",
            countryCode = null,
            isPrimary = true,
            repoId = repoId,
        )

        repoRepository.delete(repoId)

        assertEquals(0, count("repository"))
        assertEquals(0, count("authentication"))
        assertEquals(0, count("mirror"))
    }

    private fun count(table: String, where: String? = null): Long {
        val sql = buildString {
            append("SELECT COUNT(*) FROM ")
            append(table)
            if (where != null) {
                append(" WHERE ")
                append(where)
            }
        }
        return driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(requireNotNull(cursor.getLong(0)))
            },
            parameters = 0,
        ).value
    }
}
