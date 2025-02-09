package com.looker.droidify

import android.content.Context
import com.looker.droidify.data.local.dao.AppDao
import com.looker.droidify.data.local.dao.RepoDao
import com.looker.droidify.database.Database
import com.looker.droidify.index.RepositoryUpdater
import com.looker.droidify.index.RepositoryUpdater.IndexType
import com.looker.droidify.model.Repository
import com.looker.droidify.sync.FakeDownloader
import com.looker.droidify.sync.common.Izzy
import com.looker.droidify.sync.common.JsonParser
import com.looker.droidify.sync.common.assets
import com.looker.droidify.sync.common.benchmark
import com.looker.droidify.sync.common.downloadIndex
import com.looker.droidify.sync.v2.model.IndexV2
import com.looker.droidify.utility.common.extension.windowed
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    lateinit var repoDao: RepoDao

    @Inject
    lateinit var appDao: AppDao

    @Inject
    @ApplicationContext
    lateinit var context: Context

    val repo = Izzy
    lateinit var legacyRepo: Repository

    @Before
    fun before() = runTest {
        hiltRule.inject()
        setupLegacy()
    }

    @Test
    fun roomBenchmark() = runTest {
        val insert = benchmark(1, "Insert Bunch") {
            val v2File = FakeDownloader
                .downloadIndex(context, repo, "izzy-v2", "index-v2.json")
            measureTimeMillis {
                val index = JsonParser.decodeFromString<IndexV2>(
                    v2File.readBytes().decodeToString(),
                )
                val id = repoDao.insertRepo(
                    fingerprint = repo.fingerprint!!,
                    repo = index.repo,
                    username = repo.authentication?.username,
                    password = repo.authentication?.password,
                    id = repo.id,
                )
                index.packages.windowed(500) {
                    appDao.upsertMetadata(
                        repoId = id,
                        metadatas = it,
                    )
                }
            }
        }
        val queryAll = benchmark(5, "Stream of apps") {
            measureTimeMillis {
                appDao.stream()
            }
        }
        val queryOne = benchmark(5, "Query app") {
            measureTimeMillis {
                appDao.queryAppEntity("com.looker.droidify")
            }
        }
        println(insert)
        println(queryAll)
        println(queryOne)
    }

    @Test
    fun legacyBenchmark() {
        val insert = benchmark(1, "Insert") {
            val createFile = File.createTempFile("index", "entry")
            val mergerFile = File.createTempFile("index", "merger")
            val jarStream = assets("izzy_index_v1.jar")
            jarStream.copyTo(createFile.outputStream())
            measureTimeMillis {
                RepositoryUpdater.processFile(
                    context = context,
                    repository = legacyRepo,
                    indexType = IndexType.INDEX_V1,
                    unstable = false,
                    file = createFile,
                    mergerFile = mergerFile,
                    lastModified = "",
                    entityTag = "",
                    callback = { _, _, _ -> },
                )
            }
        }
        val queryAll = benchmark(5, "Stream of apps") {
            measureTimeMillis {
                Database.ProductAdapter.getAll()
            }
        }
        val queryOne = benchmark(5, "Query app") {
            measureTimeMillis {
                Database.ProductAdapter.get("com.looker.droidify", null)
            }
        }
        println(insert)
        println(queryAll)
        println(queryOne)
    }

    private fun setupLegacy() {
        Database.init(context)
        RepositoryUpdater.init(CoroutineScope(Dispatchers.Default), FakeDownloader)
        legacyRepo = Repository(
            id = 15,
            address = "https://apt.izzysoft.de/fdroid/repo",
            mirrors = emptyList(),
            name = "IzzyOnDroid F-Droid Repo",
            description = "",
            version = 20002,
            enabled = true,
            fingerprint = "3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A",
            lastModified = "",
            entityTag = "",
            updated = 1735315749835,
            timestamp = 1725352450000,
            authentication = "",
        )
    }
}
