package com.looker.droidify.sync

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.looker.droidify.domain.model.Repo
import com.looker.droidify.sync.common.IndexJarValidator
import com.looker.droidify.sync.common.Izzy
import com.looker.droidify.sync.common.JsonParser
import com.looker.droidify.sync.common.downloadIndex
import com.looker.droidify.sync.common.benchmark
import com.looker.droidify.sync.common.toV2
import com.looker.droidify.sync.v1.V1Parser
import com.looker.droidify.sync.v1.V1Syncable
import com.looker.droidify.sync.v1.model.IndexV1
import com.looker.droidify.sync.v2.V2Parser
import com.looker.droidify.sync.v2.model.FileV2
import com.looker.droidify.sync.v2.model.IndexV2
import com.looker.droidify.sync.v2.model.MetadataV2
import com.looker.droidify.sync.v2.model.VersionV2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class V1SyncableTest {

    private lateinit var dispatcher: CoroutineDispatcher
    private lateinit var context: Context
    private lateinit var syncable: Syncable<IndexV1>
    private lateinit var parser: Parser<IndexV1>
    private lateinit var v2Parser: Parser<IndexV2>
    private lateinit var validator: IndexValidator
    private lateinit var repo: Repo

    @Before
    fun before() {
        context = InstrumentationRegistry.getInstrumentation().context
        dispatcher = StandardTestDispatcher()
        validator = IndexJarValidator(dispatcher)
        parser = V1Parser(dispatcher, JsonParser, validator)
        v2Parser = V2Parser(dispatcher, JsonParser)
        syncable = V1Syncable(context, FakeDownloader, dispatcher)
        repo = Izzy
    }

    @Test
    fun benchmark_sync_v1() = runTest(dispatcher) {
        val output = benchmark(10) {
            measureTimeMillis { syncable.sync(repo) }
        }
        println(output)
    }

    @Test
    fun benchmark_v1_parser() = runTest(dispatcher) {
        val file = FakeDownloader.downloadIndex(context, repo, "izzy", "index-v1.jar")
        val output = benchmark(10) {
            measureTimeMillis {
                parser.parse(
                    file = file,
                    repo = repo
                )
            }
        }
        println(output)
    }

    @Test
    fun benchmark_v1_vs_v2_parser() = runTest(dispatcher) {
        val v1File = FakeDownloader.downloadIndex(context, repo, "izzy-v1", "index-v1.jar")
        val v2File = FakeDownloader.downloadIndex(context, repo, "izzy-v2", "index-v2.json")
        val output1 = benchmark(10) {
            measureTimeMillis {
                parser.parse(
                    file = v1File,
                    repo = repo
                )
            }
        }
        val output2 = benchmark(10) {
            measureTimeMillis {
                parser.parse(
                    file = v2File,
                    repo = repo,
                )
            }
        }
        println(output1)
        println(output2)
    }

    @Test
    fun v1tov2() = runTest(dispatcher) {
        testIndexConversion("index-v1.jar", "index-v2-updated.json")
    }

    // @Test
    fun v1tov2FDroidRepo() = runTest(dispatcher) {
        testIndexConversion("fdroid-index-v1.jar", "fdroid-index-v2.json")
    }

    private suspend fun testIndexConversion(
        v1: String,
        v2: String,
        targeted: String? = null,
    ) {
        val fileV1 = FakeDownloader.downloadIndex(context, repo, "data-v1", v1)
        val fileV2 = FakeDownloader.downloadIndex(context, repo, "data-v2", v2)
        val (fingerV1, foundIndexV1) = parser.parse(fileV1, repo)
        val (fingerV2, expectedIndex) = v2Parser.parse(fileV2, repo)
        val foundIndex = foundIndexV1.toV2()
        assertEquals(fingerV2, fingerV1)
        assertNotNull(foundIndex)
        assertNotNull(expectedIndex)
        assertEquals(expectedIndex.repo.timestamp, foundIndex.repo.timestamp)
        assertEquals(expectedIndex.packages.size, foundIndex.packages.size)
        assertContentEquals(
            expectedIndex.packages.keys.sorted(),
            foundIndex.packages.keys.sorted(),
        )
        if (targeted == null) {
            expectedIndex.packages.keys.forEach { key ->
                val expectedPackage = expectedIndex.packages[key]
                val foundPackage = foundIndex.packages[key]

                println("**".repeat(25))
                println("Testing: ${expectedPackage?.metadata?.name?.get("en-US")} <$key>")

                assertNotNull(expectedPackage)
                assertNotNull(foundPackage)
                assertMetadata(expectedPackage.metadata, foundPackage.metadata)
                assertVersion(expectedPackage.versions, foundPackage.versions)
            }
        } else {
            val expectedPackage = expectedIndex.packages[targeted]
            val foundPackage = foundIndex.packages[targeted]

            println("**".repeat(25))
            println("Testing: ${expectedPackage?.metadata?.name?.get("en-US")} <$targeted>")

            assertNotNull(expectedPackage)
            assertNotNull(foundPackage)
            assertMetadata(expectedPackage.metadata, foundPackage.metadata)
            assertVersion(expectedPackage.versions, foundPackage.versions)
        }
    }
}

/*
* Cannot assert following:
* - `name` => because fdroidserver behaves weirdly
* */
private fun assertMetadata(expectedMetaData: MetadataV2, foundMetadata: MetadataV2) {
    assertEquals(expectedMetaData.preferredSigner, foundMetadata.preferredSigner)
//    assertLocalizedString(expectedMetaData.name, foundMetadata.name)
    assertLocalizedString(expectedMetaData.summary, foundMetadata.summary)
    assertLocalizedString(expectedMetaData.description, foundMetadata.description)
    assertContentEquals(expectedMetaData.categories, foundMetadata.categories)
    // Update
    assertEquals(expectedMetaData.changelog, foundMetadata.changelog)
    assertEquals(expectedMetaData.added, foundMetadata.added)
    assertEquals(expectedMetaData.lastUpdated, foundMetadata.lastUpdated)
    // Author
    assertEquals(expectedMetaData.authorEmail, foundMetadata.authorEmail)
    assertEquals(expectedMetaData.authorName, foundMetadata.authorName)
    assertEquals(expectedMetaData.authorPhone, foundMetadata.authorPhone)
    assertEquals(expectedMetaData.authorWebSite, foundMetadata.authorWebSite)
    // Donate
    assertEquals(expectedMetaData.bitcoin, foundMetadata.bitcoin)
    assertEquals(expectedMetaData.liberapay, foundMetadata.liberapay)
    assertEquals(expectedMetaData.flattrID, foundMetadata.flattrID)
    assertEquals(expectedMetaData.openCollective, foundMetadata.openCollective)
    assertEquals(expectedMetaData.litecoin, foundMetadata.litecoin)
    assertContentEquals(expectedMetaData.donate, foundMetadata.donate)
    // Source
    assertEquals(expectedMetaData.translation, foundMetadata.translation)
    assertEquals(expectedMetaData.issueTracker, foundMetadata.issueTracker)
    assertEquals(expectedMetaData.license, foundMetadata.license)
    assertEquals(expectedMetaData.sourceCode, foundMetadata.sourceCode)
    // Graphics
    assertLocalizedString(expectedMetaData.video, foundMetadata.video)
    assertLocalized(expectedMetaData.icon, foundMetadata.icon) { expected, found ->
        assertEquals(expected.name, found.name)
    }
    assertLocalized(expectedMetaData.promoGraphic, foundMetadata.promoGraphic) { expected, found ->
        assertEquals(expected.name, found.name)
    }
    assertLocalized(expectedMetaData.tvBanner, foundMetadata.tvBanner) { expected, found ->
        assertEquals(expected.name, found.name)
    }
    assertLocalized(
        expectedMetaData.featureGraphic,
        foundMetadata.featureGraphic
    ) { expected, found ->
        assertEquals(expected.name, found.name)
    }
    assertLocalized(
        expectedMetaData.screenshots?.phone,
        foundMetadata.screenshots?.phone
    ) { expected, found ->
        assertFiles(expected, found)
    }
    assertLocalized(
        expectedMetaData.screenshots?.sevenInch,
        foundMetadata.screenshots?.sevenInch
    ) { expected, found ->
        assertFiles(expected, found)
    }
    assertLocalized(
        expectedMetaData.screenshots?.tenInch,
        foundMetadata.screenshots?.tenInch
    ) { expected, found ->
        assertFiles(expected, found)
    }
    assertLocalized(
        expectedMetaData.screenshots?.tv,
        foundMetadata.screenshots?.tv
    ) { expected, found ->
        assertFiles(expected, found)
    }
    assertLocalized(
        expectedMetaData.screenshots?.wear,
        foundMetadata.screenshots?.wear
    ) { expected, found ->
        assertFiles(expected, found)
    }
}

/*
* Cannot assert following:
* - `whatsNew` => we added same changelog to all versions
* - `antiFeatures` => anti features are now version specific
* */
private fun assertVersion(
    expected: Map<String, VersionV2>,
    found: Map<String, VersionV2>,
) {
    assertEquals(expected.keys.size, found.keys.size)
    assertContentEquals(expected.keys.sorted(), found.keys.sorted().asIterable())
    expected.keys.forEach { versionHash ->
        val expectedVersion = expected[versionHash]
        val foundVersion = found[versionHash]
        assertNotNull(expectedVersion)
        assertNotNull(foundVersion)

        assertEquals(expectedVersion.added, foundVersion.added)
        assertEquals(expectedVersion.file.name, foundVersion.file.name)
        assertEquals(expectedVersion.src?.name, foundVersion.src?.name)

        val expectedMan = expectedVersion.manifest
        val foundMan = foundVersion.manifest

        assertEquals(expectedMan.versionCode, foundMan.versionCode)
        assertEquals(expectedMan.versionName, foundMan.versionName)
        assertEquals(expectedMan.maxSdkVersion, foundMan.maxSdkVersion)
        assertEquals(expectedMan.usesSdk, foundMan.usesSdk)

        assertContentEquals(
            expectedMan.features.sortedBy { it.name },
            foundMan.features.sortedBy { it.name },
        )
        assertContentEquals(expectedMan.usesPermission, foundMan.usesPermission)
        assertContentEquals(expectedMan.usesPermissionSdk23, foundMan.usesPermissionSdk23)
        assertContentEquals(expectedMan.signer?.sha256?.sorted(), foundMan.signer?.sha256?.sorted())
        assertContentEquals(expectedMan.nativecode.sorted(), foundMan.nativecode.sorted())
    }
}

private fun assertLocalizedString(
    expected: Map<String, String>?,
    found: Map<String, String>?,
    message: String? = null,
) {
    assertLocalized(expected, found) { one, two ->
        assertEquals(one, two, message)
    }
}

private fun <T> assertLocalized(
    expected: Map<String, T>?,
    found: Map<String, T>?,
    block: (expected: T, found: T) -> Unit,
) {
    if (expected == null || found == null) {
        assertEquals(expected, found)
        return
    }
    assertNotNull(expected)
    assertNotNull(found)
    assertEquals(expected.size, found.size)
    assertContentEquals(expected.keys.sorted(), found.keys.sorted().asIterable())
    expected.keys.forEach {
        if (expected[it] != null && found[it] != null) block(expected[it]!!, found[it]!!)
    }
}

private fun assertFiles(expected: List<FileV2>, found: List<FileV2>, message: String? = null) {
    // Only check name, because we cannot add sha to old index
    assertContentEquals(expected.map { it.name }, found.map { it.name }.asIterable(), message)
}
