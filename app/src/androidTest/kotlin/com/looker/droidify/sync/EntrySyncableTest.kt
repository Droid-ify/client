package com.looker.droidify.sync

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.looker.droidify.domain.model.Repo
import com.looker.droidify.sync.common.IndexJarValidator
import com.looker.droidify.sync.common.Izzy
import com.looker.droidify.sync.common.JsonParser
import com.looker.droidify.sync.common.assets
import com.looker.droidify.sync.common.downloadIndex
import com.looker.droidify.sync.common.benchmark
import com.looker.droidify.sync.v2.EntryParser
import com.looker.droidify.sync.v2.EntrySyncable
import com.looker.droidify.sync.v2.model.Entry
import com.looker.droidify.sync.v2.model.IndexV2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class EntrySyncableTest {

    private lateinit var dispatcher: CoroutineDispatcher
    private lateinit var context: Context
    private lateinit var syncable: Syncable<Entry>
    private lateinit var parser: Parser<Entry>
    private lateinit var validator: IndexValidator
    private lateinit var repo: Repo
    private lateinit var newIndex: IndexV2

    /**
     * In this particular test 1 package is removed and 36 packages are updated
     */

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun before() {
        context = InstrumentationRegistry.getInstrumentation().context
        dispatcher = StandardTestDispatcher()
        validator = IndexJarValidator(dispatcher)
        parser = EntryParser(dispatcher, JsonParser, validator)
        syncable = EntrySyncable(context, FakeDownloader, dispatcher)
        newIndex = JsonParser.decodeFromStream<IndexV2>(assets("izzy_index_v2_updated.json"))
        repo = Izzy
    }

    @Test
    fun benchmark_sync_full() = runTest(dispatcher) {
        val output = benchmark(10) {
            measureTimeMillis { syncable.sync(repo) }
        }
        println(output)
    }

    @Test
    fun benchmark_entry_parser() = runTest(dispatcher) {
        val output = benchmark(10) {
            measureTimeMillis {
                parser.parse(
                    file = FakeDownloader.downloadIndex(
                        context = context,
                        repo = repo,
                        fileName = "izzy",
                        url = "entry.jar"
                    ),
                    repo = repo
                )
            }
        }
        println(output)
    }

    @Test
    fun check_if_patch_applies() = runTest(dispatcher) {
        // Downloads old index file as the index file does not exist
        val (fingerprint1, index1) = syncable.sync(repo)
        assert(index1 != null)
        // Downloads the diff as the index file exists and is older than entry version
        val (fingerprint2, index2) = syncable.sync(
            repo.copy(
                versionInfo = repo.versionInfo.copy(
                    timestamp = index1!!.repo.timestamp
                )
            )
        )
        assert(index2 != null)
        // Does not download anything
        val (fingerprint3, index3) = syncable.sync(
            repo.copy(
                versionInfo = repo.versionInfo.copy(
                    timestamp = index2!!.repo.timestamp
                )
            )
        )
        assert(index3 == null)

        // Check if all the packages are same
        assertContentEquals(newIndex.packages.keys.sorted(), index2.packages.keys.sorted())
        // Check if all the version hashes are same
        assertContentEquals(
            newIndex.packages.values.flatMap { it.versions.keys }.sorted(),
            index2.packages.values.flatMap { it.versions.keys }.sorted(),
        )

        // Check if repo antifeatures are same
        assertContentEquals(
            newIndex.repo.antiFeatures.keys.sorted(),
            index2.repo.antiFeatures.keys.sorted()
        )

        // Check if repo categories are same
        assertContentEquals(
            newIndex.repo.categories.keys.sorted(),
            index2.repo.categories.keys.sorted()
        )

        assertEquals(fingerprint1, fingerprint2)
        assertEquals(fingerprint2, fingerprint3)
    }

}
