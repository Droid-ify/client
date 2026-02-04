package com.looker.droidify.sync

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.looker.droidify.data.model.Repo
import com.looker.droidify.sync.common.Izzy
import com.looker.droidify.sync.common.benchmark
import com.looker.droidify.sync.common.downloadIndex
import com.looker.droidify.sync.common.toV2
import com.looker.droidify.sync.v1.V1Syncable
import com.looker.droidify.sync.v1.model.IndexV1
import com.looker.droidify.sync.v2.model.FileV2
import com.looker.droidify.sync.v2.model.IndexV2
import com.looker.droidify.sync.v2.model.MetadataV2
import com.looker.droidify.sync.v2.model.PackageV2
import com.looker.droidify.sync.v2.model.VersionV2
import kotlin.system.measureTimeMillis
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V1SyncableTest {

    private lateinit var dispatcher: CoroutineDispatcher
    private lateinit var context: Context
    private lateinit var syncable: Syncable<IndexV1>
    private lateinit var repo: Repo

    @Before
    fun before() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dispatcher = StandardTestDispatcher()
        syncable = V1Syncable(context, FakeDownloader, dispatcher)
        repo = Izzy
    }

    @Test
    fun benchmark_sync_v1() = runTest(dispatcher) {
        val output = benchmark(10) {
            measureTimeMillis { syncable.sync(repo) { /* no-op */ } }
        }
        println(output)
    }

    @Test
    fun benchmark_v1_parser() = runTest(dispatcher) {
        val file = FakeDownloader.downloadIndex(context, repo, "izzy", "index-v1.jar")
        val output = benchmark(10) {
            measureTimeMillis {
                with(file.toJarScope<IndexV1>()) { json() }
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
                with(v1File.toJarScope<IndexV1>()) { json() }
            }
        }
        val output2 = benchmark(10) {
            measureTimeMillis {
                JsonParser.decodeFromString<IndexV2>(v2File.readBytes().decodeToString())
            }
        }
        println(output1)
        println(output2)
    }

    @Test
    fun v1tov2() = runTest(dispatcher) {
        testIndexConversion("index-v1.jar", "index-v2-updated.json")
    }

    @Test
    fun targetPropertyV1Test() = runTest(dispatcher) {
        val v1IzzyFile =
            FakeDownloader.downloadIndex(context, repo, "izzy-v1", "index-v1.jar")
        val v1FdroidFile =
            FakeDownloader.downloadIndex(context, repo, "fdroid-v1", "fdroid-index-v1.jar")

        val t = with(v1IzzyFile.toJarScope<IndexV1>()) { json() }
        val set = hashSetOf<String>()
        t.packages.flatMap { it.value }.forEach {
            set.add(it.hashType)
        }
        val k = with(v1FdroidFile.toJarScope<IndexV1>()) { json() }
        k.packages.flatMap { it.value }.forEach {
            set.add(it.hashType)
        }
        println("Types: ${set.joinToString()}")
    }

    @Test
    fun targetPropertyTest() = runTest(dispatcher) {
        val v2IzzyFile =
            FakeDownloader.downloadIndex(context, repo, "izzy-v2", "index-v2-updated.json")
        val v2FdroidFile =
            FakeDownloader.downloadIndex(context, repo, "fdroid-v2", "fdroid-index-v2.json")
        val v2Izzy = JsonParser.decodeFromString<IndexV2>(v2IzzyFile.readBytes().decodeToString())
        val v2Fdroid =
            JsonParser.decodeFromString<IndexV2>(v2FdroidFile.readBytes().decodeToString())

        val performTest: (PackageV2) -> Unit = { data ->
            print("lib: ")
            println(data.metadata.liberapay)
            print("donate: ")
            println(data.metadata.donate)
            print("bit: ")
            println(data.metadata.bitcoin)
            print("flattr: ")
            println(data.metadata.flattrID)
            print("Open: ")
            println(data.metadata.openCollective)
            print("LiteCoin: ")
            println(data.metadata.litecoin)
        }

        v2Izzy.packages.forEach { (packageName, data) ->
            println("Testing on Izzy $packageName")
            performTest(data)
        }
        v2Fdroid.packages.forEach { (packageName, data) ->
            println("Testing on FDroid $packageName")
            performTest(data)
        }
    }

    private suspend fun testIndexConversion(
        v1: String,
        v2: String,
        targeted: String? = null,
    ) {
        val fileV1 = FakeDownloader.downloadIndex(context, repo, "data-v1", v1)
        val fileV2 = FakeDownloader.downloadIndex(context, repo, "data-v2", v2)
        val foundIndexV1 = with(fileV1.toJarScope<IndexV1>()) { json() }
        val expectedIndex =
            JsonParser.decodeFromString<IndexV2>(fileV2.readBytes().decodeToString())
        val foundIndex = foundIndexV1.toV2()
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
        assertEquals(expectedVersion.file.sha256, foundVersion.file.sha256)
        assertEquals(expectedVersion.file.size, foundVersion.file.size)
        assertEquals(expectedVersion.file.name, foundVersion.file.name)
        assertEquals(expectedVersion.src?.name, foundVersion.src?.name)

        val expectedMan = expectedVersion.manifest
        val foundMan = foundVersion.manifest

        assertEquals(expectedMan.versionCode, foundMan.versionCode)
        assertEquals(expectedMan.versionName, foundMan.versionName)
        assertEquals(expectedMan.maxSdkVersion, foundMan.maxSdkVersion)
        assertNotNull(expectedMan.usesSdk)
        assertNotNull(foundMan.usesSdk)
        assertEquals(expectedMan.usesSdk, foundMan.usesSdk)
        assertTrue(expectedMan.usesSdk.minSdkVersion >= 1)
        assertTrue(expectedMan.usesSdk.targetSdkVersion >= 1)
        assertTrue(foundMan.usesSdk.minSdkVersion >= 1)
        assertTrue(foundMan.usesSdk.targetSdkVersion >= 1)

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
