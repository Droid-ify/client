package com.looker.droidify.sync

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.data.model.Repo
import com.looker.droidify.sync.common.Izzy
import com.looker.droidify.sync.common.assets
import com.looker.droidify.sync.common.benchmark
import com.looker.droidify.sync.common.downloadIndex
import com.looker.droidify.sync.utils.toJarFile
import com.looker.droidify.sync.v2.EntrySyncable
import com.looker.droidify.sync.v2.model.Entry
import com.looker.droidify.sync.v2.model.IndexV2
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntrySyncableTest {

    private suspend fun syncAndGet(
        syncable: Syncable<Entry>,
        repo: Repo,
    ): Pair<Fingerprint, IndexV2?>? {
        var finger: Fingerprint? = null
        var index: IndexV2? = null
        syncable.sync(repo) { state ->
            if (state is SyncState.JsonParsing.Success) {
                finger = state.fingerprint
                index = state.index
            }
        }
        return finger?.let { it to index }
    }

    private lateinit var dispatcher: CoroutineDispatcher
    private lateinit var context: Context
    private lateinit var syncable: Syncable<Entry>
    private lateinit var repo: Repo
    private lateinit var newIndex: IndexV2

    /**
     * In this particular test 1 package is removed and 36 packages are updated
     */

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun before() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dispatcher = StandardTestDispatcher()
        syncable = EntrySyncable(context, FakeDownloader, dispatcher)
        newIndex = JsonParser.decodeFromStream<IndexV2>(assets("izzy_index_v2_updated.json"))
        repo = Izzy
    }

    @Test
    fun benchmark_sync_full() = runTest(dispatcher) {
        val output = benchmark(10) {
            measureTimeMillis { syncable.sync(repo) { /* no-op */ } }
        }
        println(output)
    }

    @Test
    fun benchmark_entry_parser() = runTest(dispatcher) {
        val output = benchmark(10) {
            measureTimeMillis {
                FakeDownloader
                    .downloadIndex(
                        context = context,
                        repo = repo,
                        fileName = "izzy",
                        url = "entry.jar"
                    )
                    .toJarFile()
                    .parseJson<Entry>(repo.fingerprint)
            }
        }
        println(output)
    }

    @Test
    fun check_if_patch_applies() = runTest(dispatcher) {
        // Downloads old index file as the index file does not exist
        val (fingerprint1, index1) = syncAndGet(syncable, repo) ?: fail("Result should not be null")
        assert(index1 != null)
        // Downloads the diff as the index file exists and is older than entry version
        val (fingerprint2, index2) = syncAndGet(
            syncable,
            repo.copy(
                versionInfo = repo.versionInfo?.copy(
                    timestamp = index1!!.repo.timestamp
                )
            )
        ) ?: fail("Result should not be null")
        assert(index2 != null)
        val index2NonNull = requireNotNull(index2)
        // Does not download anything
        val (fingerprint3, index3) = syncAndGet(
            syncable,
            repo.copy(
                versionInfo = repo.versionInfo?.copy(
                    timestamp = index2NonNull.repo.timestamp
                )
            )
        ) ?: fail("Result should not be null")
        assert(index3 == null)

        // Check if all the packages are same
        assertContentEquals(newIndex.packages.keys.sorted(), index2NonNull.packages.keys.sorted())
        // Check if all the version hashes are same
        assertContentEquals(
            newIndex.packages.values.flatMap { it.versions.keys }.sorted(),
            index2NonNull.packages.values.flatMap { it.versions.keys }.sorted(),
        )

        // Check if repo antifeatures are same
        assertContentEquals(
            newIndex.repo.antiFeatures.keys.sorted(),
            index2NonNull.repo.antiFeatures.keys.sorted()
        )

        // Check if repo categories are same
        assertContentEquals(
            newIndex.repo.categories.keys.sorted(),
            index2NonNull.repo.categories.keys.sorted()
        )

        assertEquals(fingerprint1, fingerprint2)
        assertEquals(fingerprint2, fingerprint3)
    }

}
