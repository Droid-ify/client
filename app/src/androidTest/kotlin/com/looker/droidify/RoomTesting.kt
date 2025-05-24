package com.looker.droidify

import android.content.Context
import com.looker.droidify.data.local.dao.AppDao
import com.looker.droidify.data.local.dao.IndexDao
import com.looker.droidify.database.Database
import com.looker.droidify.domain.model.Fingerprint
import com.looker.droidify.domain.model.Repo
import com.looker.droidify.domain.model.VersionInfo
import com.looker.droidify.index.RepositoryUpdater
import com.looker.droidify.index.RepositoryUpdater.IndexType
import com.looker.droidify.model.Repository
import com.looker.droidify.sync.FakeDownloader
import com.looker.droidify.sync.common.JsonParser
import com.looker.droidify.sync.common.assets
import com.looker.droidify.sync.common.benchmark
import com.looker.droidify.sync.common.downloadIndex
import com.looker.droidify.sync.v2.model.IndexV2
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import java.io.File
import javax.inject.Inject
import kotlin.system.measureTimeMillis
import kotlin.test.Test

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
        setupLegacy()
    }

    @Test
    fun roomBenchmark() = runTest {
        val izzy = izzyLegacy.toRepo(1)
        val insert = benchmark(1) {
            val v2File = FakeDownloader
                .downloadIndex(context, izzy, "izzy-v2", "index-v2.json")
            measureTimeMillis {
                val index = JsonParser.decodeFromString<IndexV2>(
                    v2File.readBytes().decodeToString(),
                )
                indexDao.insertIndex(
                    fingerprint = izzy.fingerprint!!,
                    index = index,
                    expectedRepoId = izzy.id,
                )
            }
        }
        val fdroid = fdroidLegacy.toRepo(2)
        val insertFDroid = benchmark(1) {
            val v2File = FakeDownloader
                .downloadIndex(context, fdroid, "fdroid-v2", "fdroid-index-v2.json")
            measureTimeMillis {
                val index = JsonParser.decodeFromString<IndexV2>(
                    v2File.readBytes().decodeToString(),
                )
                indexDao.insertIndex(
                    fingerprint = fdroid.fingerprint!!,
                    index = index,
                    expectedRepoId = fdroid.id,
                )
            }
        }
        val query = appDao.queryAppEntity("com.looker.droidify")
        println(query.first().joinToString("\n"))
        println(insert)
        println(insertFDroid)
    }

    @Test
    fun legacyBenchmark() {
        val insert = benchmark(1) {
            val createFile = File.createTempFile("index", "entry")
            val mergerFile = File.createTempFile("index", "merger")
            val jarStream = assets("izzy_index_v1.jar")
            jarStream.copyTo(createFile.outputStream())
            measureTimeMillis {
                RepositoryUpdater.processFile(
                    context = context,
                    repository = izzyLegacy,
                    indexType = IndexType.INDEX_V1,
                    unstable = false,
                    file = createFile,
                    mergerFile = mergerFile,
                    lastModified = "",
                    entityTag = "",
                    callback = { _, _, _ -> },
                )
                createFile.delete()
                mergerFile.delete()
            }
        }
        val insertFDroid = benchmark(1) {
            val createFile = File.createTempFile("index", "entry")
            val mergerFile = File.createTempFile("index", "merger")
            val jarStream = assets("fdroid_index_v1.jar")
            jarStream.copyTo(createFile.outputStream())
            measureTimeMillis {
                RepositoryUpdater.processFile(
                    context = context,
                    repository = fdroidLegacy,
                    indexType = IndexType.INDEX_V1,
                    unstable = false,
                    file = createFile,
                    mergerFile = mergerFile,
                    lastModified = "",
                    entityTag = "",
                    callback = { _, _, _ -> },
                )
                createFile.delete()
                mergerFile.delete()
            }
        }
        println(insert)
        println(insertFDroid)
    }

    private fun setupLegacy() {
        Database.init(context)
        RepositoryUpdater.init(CoroutineScope(Dispatchers.Default), FakeDownloader)
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
