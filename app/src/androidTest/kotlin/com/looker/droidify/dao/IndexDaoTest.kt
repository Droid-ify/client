package com.looker.droidify.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.looker.droidify.data.local.DroidifyDatabase
import com.looker.droidify.data.local.dao.IndexDao
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.sync.common.Izzy
import com.looker.droidify.sync.common.JsonParser
import com.looker.droidify.sync.common.assets
import com.looker.droidify.sync.common.benchmark
import com.looker.droidify.sync.v2.model.IndexV2
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class IndexDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var dispatcher: CoroutineDispatcher

    @Inject
    lateinit var database: DroidifyDatabase

    @Inject
    lateinit var dao: IndexDao

    private lateinit var index: IndexV2
    private val fingerprint: Fingerprint = Izzy.fingerprint!!

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setUp() {
        hiltRule.inject()
        dispatcher = StandardTestDispatcher()
        index = JsonParser.decodeFromStream<IndexV2>(assets("izzy_index_v2.json"))
    }

    @Test
    fun benchmark_insert_full_new() = runTest(dispatcher, timeout = 5.minutes) {
        val output =
            benchmark(5, extraMessage = "IndexDao.insertIndex – empty DB (fresh insert)") {
                database.clearAllTables()
                measureTimeMillis {
                    dao.insertIndex(
                        fingerprint = fingerprint,
                        index = index,
                    )
                }
            }
        println(output)
    }
}