@file:OptIn(ExperimentalStdlibApi::class)

package com.looker.droidify.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.looker.droidify.data.local.droidifyDb
import com.looker.droidify.data.local.sql.DroidifyDb
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.sync.v2.model.AntiFeatureV2
import com.looker.droidify.sync.v2.model.ApkFileV2
import com.looker.droidify.sync.v2.model.CategoryV2
import com.looker.droidify.sync.v2.model.FeatureV2
import com.looker.droidify.sync.v2.model.FileV2
import com.looker.droidify.sync.v2.model.IndexV2
import com.looker.droidify.sync.v2.model.ManifestV2
import com.looker.droidify.sync.v2.model.MetadataV2
import com.looker.droidify.sync.v2.model.MirrorV2
import com.looker.droidify.sync.v2.model.PackageV2
import com.looker.droidify.sync.v2.model.PermissionV2
import com.looker.droidify.sync.v2.model.RepoV2
import com.looker.droidify.sync.v2.model.ScreenshotsV2
import com.looker.droidify.sync.v2.model.UsesSdkV2
import com.looker.droidify.sync.v2.model.VersionV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IndexRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: DroidifyDb
    private lateinit var indexRepository: IndexRepository

    private val fingerprint = Fingerprint("ab".repeat(32))

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        DroidifyDb.Schema.create(driver)
        db = droidifyDb(driver)
        indexRepository = IndexRepository(db, Dispatchers.Unconfined)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun `insertIndex populates all tables`() = runTest {
        val repoId = insertBareRepo()

        indexRepository.insertIndex(repoId, fingerprint, testIndex(), etag = "etag-1")

        assertEquals(1, count("app"))
        assertEquals(1, count("localized_app"))
        assertEquals(1, count("localized_repo"))
        assertEquals(1, count("author"))
        assertEquals(1, count("links"))
        assertEquals(2, count("mirror"))
        assertEquals(1, count("category"))
        assertEquals(1, count("category_repo_relation"))
        assertEquals(1, count("category_app_relation"))
        assertEquals(1, count("anti_feature"))
        assertEquals(1, count("anti_feature_repo_relation"))
        assertEquals(1, count("anti_features_app_relation"))
        assertEquals(1, count("version"))
        assertEquals(2, count("permission"))
        assertEquals(1, count("feature"))
        assertEquals(1, count("native_code"))
        assertEquals(2, count("screenshot"))
        // 1 feature graphic + 1 video
        assertEquals(2, count("graphic"))
        // 1 regular + bitcoin + liberapay
        assertEquals(3, count("donate"))

        val appId = db.appQueries.selectAppId("com.example.app", repoId).executeAsOne()
        val versionId = db.versionQueries
            .selectVersionId(appId, "12".repeat(32).hexToByteArray())
            .executeAsOne()
        assertNotNull(versionId)
    }

    @Test
    fun `insertIndex updates repository version info`() = runTest {
        val repoId = insertBareRepo()

        indexRepository.insertIndex(repoId, fingerprint, testIndex(), etag = "etag-1")

        assertEquals(
            1,
            count(
                "repository",
                where = "timestamp = 1720000000000 AND etag = 'etag-1' " +
                    "AND fingerprint IS NOT NULL",
            ),
        )
    }

    @Test
    fun `insertIndex twice is idempotent`() = runTest {
        val repoId = insertBareRepo()

        indexRepository.insertIndex(repoId, fingerprint, testIndex())
        indexRepository.insertIndex(repoId, fingerprint, testIndex())

        assertEquals(1, count("app"))
        assertEquals(1, count("author"))
        assertEquals(1, count("version"))
        assertEquals(2, count("permission"))
        assertEquals(2, count("mirror"))
        assertEquals(2, count("screenshot"))
        assertEquals(3, count("donate"))
    }

    @Test
    fun `version without usesSdk falls back to defaults`() = runTest {
        val repoId = insertBareRepo()
        val index = testIndex { manifest ->
            manifest.copy(usesSdk = null)
        }

        indexRepository.insertIndex(repoId, fingerprint, index)

        assertEquals(1, count("version", where = "minSdkVersion = 1 AND targetSdkVersion = 1"))
    }

    private fun insertBareRepo(): Long {
        db.repositoryQueries.insertRepo(
            address = "https://repo.test/repo",
            webBaseUrl = null,
            fingerprint = null,
            etag = null,
            timestamp = null,
            enabled = true,
        )
        return db.repositoryQueries.lastInsertRowId().executeAsOne()
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

    private fun testIndex(
        manifestTransform: (ManifestV2) -> ManifestV2 = { it },
    ): IndexV2 = IndexV2(
        repo = RepoV2(
            address = "https://repo.test/repo",
            name = mapOf("en-US" to "Test Repo"),
            description = mapOf("en-US" to "Test repository"),
            icon = mapOf("en-US" to FileV2("/icons/repo.png", "cd".repeat(32), 128)),
            mirrors = listOf(
                MirrorV2("https://repo.test/repo", isPrimary = true),
                MirrorV2("https://mirror.test/repo", countryCode = "US"),
            ),
            categories = mapOf(
                "Internet" to CategoryV2(name = mapOf("en-US" to "Internet")),
            ),
            antiFeatures = mapOf(
                "Ads" to AntiFeatureV2(name = mapOf("en-US" to "Advertising")),
            ),
            timestamp = 1720000000000,
        ),
        packages = mapOf(
            "com.example.app" to PackageV2(
                metadata = MetadataV2(
                    name = mapOf("en-US" to "Example"),
                    summary = mapOf("en-US" to "An example app"),
                    description = mapOf("en-US" to "A longer description"),
                    icon = mapOf("en-US" to FileV2("/icon.png", "ef".repeat(32), 64)),
                    added = 1710000000000,
                    lastUpdated = 1719000000000,
                    authorName = "Author",
                    authorEmail = "author@example.com",
                    categories = listOf("Internet"),
                    donate = listOf("https://donate.example.com"),
                    bitcoin = "bc1qexample",
                    liberapay = "example",
                    license = "GPL-3.0-only",
                    sourceCode = "https://git.example.com",
                    preferredSigner = "ab".repeat(32),
                    featureGraphic = mapOf("en-US" to FileV2("/fg.png")),
                    video = mapOf("en-US" to "https://video.example.com"),
                    screenshots = ScreenshotsV2(
                        phone = mapOf("en-US" to listOf(FileV2("/s1.png"), FileV2("/s2.png"))),
                    ),
                ),
                versions = mapOf(
                    "12".repeat(32) to VersionV2(
                        added = 1719000000000,
                        file = ApkFileV2("/app.apk", "12".repeat(32), 1024),
                        whatsNew = mapOf("en-US" to "Release notes"),
                        manifest = manifestTransform(
                            ManifestV2(
                                versionName = "1.0",
                                versionCode = 100,
                                usesSdk = UsesSdkV2(23, 34),
                                usesPermission = listOf(
                                    PermissionV2("android.permission.INTERNET"),
                                ),
                                usesPermissionSdk23 = listOf(
                                    PermissionV2("android.permission.BLUETOOTH", 30),
                                ),
                                features = listOf(FeatureV2("android.hardware.camera")),
                                nativecode = listOf("arm64-v8a"),
                            ),
                        ),
                        antiFeatures = mapOf("Tracking" to mapOf("en-US" to "Has tracking")),
                    ),
                ),
            ),
        ),
    )
}
