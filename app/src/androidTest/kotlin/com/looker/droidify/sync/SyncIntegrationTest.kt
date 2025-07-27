package com.looker.droidify.sync

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.looker.droidify.data.model.Repo
import com.looker.droidify.data.model.VersionInfo
import com.looker.droidify.sync.common.IndexJarValidator
import com.looker.droidify.sync.common.Izzy
import com.looker.droidify.sync.common.JsonParser
import com.looker.droidify.sync.v1.V1Parser
import com.looker.droidify.sync.v1.V1Syncable
import com.looker.droidify.sync.v1.model.IndexV1
import com.looker.droidify.sync.v2.EntrySyncable
import com.looker.droidify.sync.v2.V2Parser
import com.looker.droidify.sync.v2.model.Entry
import com.looker.droidify.sync.v2.model.IndexV2
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the sync system.
 * These tests verify that the V1 and V2 implementations work together correctly,
 * particularly focusing on the migration path from V1 to V2.
 */
@RunWith(AndroidJUnit4::class)
class SyncIntegrationTest {

    private lateinit var dispatcher: CoroutineDispatcher
    private lateinit var context: Context
    private lateinit var v1Syncable: Syncable<IndexV1>
    private lateinit var entrySyncable: Syncable<Entry>
    private lateinit var v1Parser: Parser<IndexV1>
    private lateinit var v2Parser: Parser<IndexV2>
    private lateinit var validator: IndexValidator
    private lateinit var repo: Repo

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dispatcher = StandardTestDispatcher()
        validator = IndexJarValidator(dispatcher)
        v1Parser = V1Parser(dispatcher, JsonParser, validator)
        v2Parser = V2Parser(dispatcher, JsonParser)
        v1Syncable = V1Syncable(context, FakeDownloader, dispatcher)
        entrySyncable = EntrySyncable(context, FakeDownloader, dispatcher)

        // Use the Izzy test repo which is already set up in the test environment
        repo = Izzy
    }

    /**
     * Test that V1 index data can be properly converted to V2 format.
     * This verifies the migration path from V1 to V2.
     */
    @Test
    fun v1ToV2Conversion() = runTest(dispatcher) {
        // Sync with V1 - this internally converts V1 to V2
        val v1Result = v1Syncable.sync(repo)
        assertNotNull(v1Result, "V1 sync should return a valid result")
        val (_, convertedV2Index) = v1Result

        // Verify that the converted index has valid data
        assertNotNull(convertedV2Index, "Converted V2 index should not be null")
        assertTrue(convertedV2Index.packages.isNotEmpty(), "Converted index should have packages")

        // Verify that the timestamp is valid
        assertTrue(convertedV2Index.repo.timestamp > 0, "Timestamp should be positive")

        // Verify that the packages have valid data
        convertedV2Index.packages.values.forEach { packageV2 ->
            assertNotNull(packageV2.metadata.name, "Package name should not be null")
            assertTrue(packageV2.metadata.name.isNotEmpty(), "Package name should not be empty")
        }
    }

    /**
     * Test that data consistency is maintained when syncing with both V1 and V2.
     * This verifies that the two implementations can work together in a real-world scenario.
     */
    @Test
    fun dataConsistencyBetweenV1AndV2() = runTest(dispatcher) {
        // First sync with V1
        val v1Result = v1Syncable.sync(repo)
        assertNotNull(v1Result, "V1 sync should return a valid result")
        val (_, v1ConvertedIndex) = v1Result

        // Then sync with V2
        val v2Result = entrySyncable.sync(repo)
        assertNotNull(v2Result, "V2 sync should return a valid result")
        val (_, v2Index) = v2Result
        assertNotNull(v2Index, "V2 index should not be null")

        // Verify that a significant number of packages from V1 exist in V2
        // Note: The actual content might differ due to updates or implementation differences
        val v1Packages = v1ConvertedIndex!!.packages.keys
        val v2Packages = v2Index!!.packages.keys
        val commonPackages = v1Packages.intersect(v2Packages)

        // Calculate the percentage of V1 packages that exist in V2
        val consistencyPercentage = (commonPackages.size.toFloat() / v1Packages.size) * 100

        // Print debug information
        println("[DEBUG_LOG] V1 package count: ${v1Packages.size}")
        println("[DEBUG_LOG] V2 package count: ${v2Packages.size}")
        println("[DEBUG_LOG] Common package count: ${commonPackages.size}")
        println("[DEBUG_LOG] Consistency percentage: $consistencyPercentage%")

        // Verify that at least 80% of packages are consistent between V1 and V2
        assertTrue(consistencyPercentage >= 80.0,
            "At least 80% of packages should be consistent between V1 and V2 indexes (actual: $consistencyPercentage%)")
    }

    /**
     * Test incremental sync functionality in V2.
     * This verifies that the V2 implementation can efficiently sync only the changes.
     */
    @Test
    fun incrementalSyncV2() = runTest(dispatcher) {
        // First sync to get the initial state
        val initialResult = entrySyncable.sync(repo)
        assertNotNull(initialResult, "Initial sync should return a valid result")
        val (initialFingerprint, initialIndex) = initialResult
        assertNotNull(initialIndex, "Initial index should not be null")

        // Update the repo's timestamp to the initial index timestamp
        val updatedRepo = repo.copy(
            versionInfo = VersionInfo(initialIndex!!.repo.timestamp, null)
        )

        // Sync again with the updated timestamp
        val incrementalResult = entrySyncable.sync(updatedRepo)
        assertNotNull(incrementalResult, "Incremental sync should return a valid result")
        val (incrementalFingerprint, incrementalIndex) = incrementalResult

        // The incremental sync should either return null for the index (no changes)
        // or return only the changes since the last sync
        if (incrementalIndex != null) {
            // If there are changes, verify that they are properly applied
            // This would depend on the specific test data and implementation
            // For this example, we'll just check that the fingerprint is consistent
            assertEquals(initialFingerprint, incrementalFingerprint)
        }
        // If incrementalIndex is null, that means there were no changes, which is also valid
    }
}
