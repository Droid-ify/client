package com.looker.sync.fdroid

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.looker.core.domain.model.Repo
import com.looker.sync.fdroid.common.IndexJarValidator
import com.looker.sync.fdroid.common.Izzy
import com.looker.sync.fdroid.common.JsonParser
import com.looker.sync.fdroid.common.downloadIndex
import com.looker.sync.fdroid.common.memory
import com.looker.sync.fdroid.common.toV2
import com.looker.sync.fdroid.v1.V1Parser
import com.looker.sync.fdroid.v1.V1Syncable
import com.looker.sync.fdroid.v1.model.IndexV1
import com.looker.sync.fdroid.v2.V2Parser
import com.looker.sync.fdroid.v2.model.FileV2
import com.looker.sync.fdroid.v2.model.IndexV2
import com.looker.sync.fdroid.v2.model.MetadataV2
import com.looker.sync.fdroid.v2.model.VersionV2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.math.exp
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

    /**
     * In this particular test 1 package is removed and 36 packages are updated
     */

    @Before
    fun before() {
        context = InstrumentationRegistry.getInstrumentation().context
        dispatcher = StandardTestDispatcher()
        validator = IndexJarValidator(dispatcher)
        parser = V1Parser(dispatcher, JsonParser.parser, validator)
        v2Parser = V2Parser(dispatcher, JsonParser.parser)
        syncable = V1Syncable(context, FakeDownloader, dispatcher)
        repo = Izzy
    }

    @Test
    fun benchmark_sync_v1() = runTest(dispatcher) {
        memory(10) {
            measureTimeMillis { syncable.sync(repo) }
        }
    }


    @Test
    fun benchmark_v1_parser() = runTest(dispatcher) {
        memory(10) {
            measureTimeMillis {
                parser.parse(
                    file = FakeDownloader.downloadIndex(
                        context = context,
                        repo = repo,
                        fileName = "izzy",
                        url = "index-v1.jar"
                    ),
                    repo = repo
                )
            }
        }
    }

    @Test
    fun v1tov2() = runTest(dispatcher) {
        testIndexConversion("index-v1.jar", "index-v2-updated.json")
    }

    @Test
    fun v1tov2FDroidRepo() = runTest(dispatcher) {
        testIndexConversion("fdroid-index-v1.json", "fdroid-index-v2.json", "com.looker.droidify")
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

private fun assertMetadata(meta1: MetadataV2, meta2: MetadataV2) {
    assertEquals(meta1.preferredSigner, meta2.preferredSigner)
    assertLocalizedString(meta1.name, meta2.name)
    assertLocalizedString(meta1.summary, meta2.summary)
    assertLocalizedString(meta1.description, meta2.description)
    assertContentEquals(meta1.categories, meta2.categories)
    // Update
    assertEquals(meta1.changelog, meta2.changelog)
    assertEquals(meta1.added, meta2.added)
    assertEquals(meta1.lastUpdated, meta2.lastUpdated)
    // Author
    assertEquals(meta1.authorEmail, meta2.authorEmail)
    assertEquals(meta1.authorName, meta2.authorName)
    assertEquals(meta1.authorPhone, meta2.authorPhone)
    assertEquals(meta1.authorWebsite, meta2.authorWebsite)
    // Donate
    assertEquals(meta1.bitcoin, meta2.bitcoin)
    assertEquals(meta1.liberapay, meta2.liberapay)
    assertEquals(meta1.flattrID, meta2.flattrID)
    assertEquals(meta1.openCollective, meta2.openCollective)
    assertEquals(meta1.litecoin, meta2.litecoin)
    assertContentEquals(meta1.donate, meta2.donate)
    // Source
    assertEquals(meta1.translation, meta2.translation)
    assertEquals(meta1.issueTracker, meta2.issueTracker)
    assertEquals(meta1.license, meta2.license)
    assertEquals(meta1.sourceCode, meta2.sourceCode)
    // Graphics
    assertLocalizedString(meta1.video, meta2.video)
    assertLocalized(meta1.icon, meta2.icon) { expected, found ->
        assertEquals(expected.name, found.name)
    }
    assertLocalized(meta1.promoGraphic, meta2.promoGraphic) { expected, found ->
        assertEquals(expected.name, found.name)
    }
    assertLocalized(meta1.tvBanner, meta2.tvBanner) { expected, found ->
        assertEquals(expected.name, found.name)
    }
    assertLocalized(meta1.featureGraphic, meta2.featureGraphic) { expected, found ->
        assertEquals(expected.name, found.name)
    }
    assertLocalized(meta1.screenshots?.phone, meta2.screenshots?.phone) { expected, found ->
        assertFiles(expected, found)
    }
    assertLocalized(meta1.screenshots?.sevenInch, meta2.screenshots?.sevenInch) { expected, found ->
        assertFiles(expected, found)
    }
    assertLocalized(meta1.screenshots?.tenInch, meta2.screenshots?.tenInch) { expected, found ->
        assertFiles(expected, found)
    }
    assertLocalized(meta1.screenshots?.tv, meta2.screenshots?.tv) { expected, found ->
        assertFiles(expected, found)
    }
    assertLocalized(meta1.screenshots?.wear, meta2.screenshots?.wear) { expected, found ->
        assertFiles(expected, found)
    }
}

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
//         We are knowingly adding same whats new to all versions
//        assertLocalizedString(expected.whatsNew, found.whatsNew)

//         We cannot assure this too, since they started adding version specific anti-features
//        assertLocalized(
//            expected.antiFeatures,
//            found.antiFeatures
//        ) { antiFeatureExpected, antiFeatureFound ->
//            println(antiFeatureExpected)
//            println(antiFeatureFound)
//            assertLocalizedString(antiFeatureExpected, antiFeatureFound)
//        }
        // TODO: fix
        // assertContentEquals(expected.signer?.sha256, found.signer?.sha256)

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
) {
    assertLocalized(expected, found) { one, two ->
        assertEquals(one, two)
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

private fun assertFiles(expected: List<FileV2>, found: List<FileV2>) {
    // Only check name, because we cannot add sha to old index
    assertContentEquals(expected.map { it.name }, found.map { it.name }.asIterable())
}
