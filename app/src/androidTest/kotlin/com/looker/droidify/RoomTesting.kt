package com.looker.droidify

import android.content.Context
import com.looker.droidify.data.local.dao.AppDao
import com.looker.droidify.data.local.dao.IndexDao
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.data.model.Repo
import com.looker.droidify.data.model.VersionInfo
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.model.Repository
import com.looker.droidify.sync.FakeDownloader
import com.looker.droidify.sync.common.JsonParser
import com.looker.droidify.sync.common.downloadIndex
import com.looker.droidify.sync.v2.model.IndexV2
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule

@HiltAndroidTest
class RoomTesting {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var indexDao: IndexDao

    @Inject
    lateinit var appDao: AppDao

    @Inject
    @ApplicationContext
    lateinit var context: Context

    private val defaults = Repository.defaultRepositories
    private val izzyLegacy = defaults[4]
    private val fdroidLegacy = defaults[0]

    @Before
    fun before() = runTest {
        hiltRule.inject()
        launch {
            val izzy = izzyLegacy.toRepo(1)
            val izzyFile = FakeDownloader.downloadIndex(context, izzy, "i2", "index-v2.json")
            val izzyIndex =
                JsonParser.decodeFromString<IndexV2>(izzyFile.readBytes().decodeToString())
            indexDao.insertIndex(
                fingerprint = izzy.fingerprint!!,
                index = izzyIndex,
                expectedRepoId = izzy.id,
            )
        }
//        launch {
//            val fdroid = fdroidLegacy.toRepo(2)
//            val fdroidFile =
//                FakeDownloader.downloadIndex(context, fdroid, "f2", "fdroid-index-v2.json")
//            val fdroidIndex =
//                JsonParser.decodeFromString<IndexV2>(fdroidFile.readBytes().decodeToString())
//            indexDao.insertIndex(
//                fingerprint = fdroid.fingerprint!!,
//                index = fdroidIndex,
//                expectedRepoId = fdroid.id,
//            )
//        }
    }

    @Test
    fun sortOrderTest() = runTest {
        val lastUpdatedQuery = appDao.query(sortOrder = SortOrder.UPDATED)
        var previousUpdated = Long.MAX_VALUE
        lastUpdatedQuery.forEach {
            println("Previous: $previousUpdated, Current: ${it.lastUpdated}")
            assertTrue(it.lastUpdated <= previousUpdated)
            previousUpdated = it.lastUpdated
        }

        val addedQuery = appDao.query(sortOrder = SortOrder.ADDED)
        var previousAdded = Long.MAX_VALUE
        addedQuery.forEach {
            println("Previous: $previousAdded, Current: ${it.added}")
            assertTrue(it.added <= previousAdded)
            previousAdded = it.added
        }
    }

    @Test
    fun categoryTest() = runTest {
        val categoryQuery = appDao.query(
            sortOrder = SortOrder.UPDATED,
            categoriesToInclude = listOf("Games", "Food"),
        )
        val nonCategoryQuery = appDao.query(
            sortOrder = SortOrder.UPDATED,
            categoriesToExclude = listOf("Games", "Food"),
        )
    }
}

private fun Repository.toRepo(id: Int) = Repo(
    id = id,
    enabled = enabled,
    address = address,
    name = name,
    description = description,
    fingerprint = Fingerprint(fingerprint),
    authentication = null,
    versionInfo = VersionInfo(timestamp, entityTag),
    mirrors = emptyList(),
)
