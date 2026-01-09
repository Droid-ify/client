package com.looker.droidify.index

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.looker.droidify.database.Database
import com.looker.droidify.model.Repository
import com.looker.droidify.sync.FakeDownloader
import com.looker.droidify.sync.common.Izzy
import com.looker.droidify.sync.common.benchmark
import com.looker.droidify.sync.common.downloadIndex
import java.io.File
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class RepositoryUpdaterTest {

    private lateinit var context: Context
    private lateinit var repository: Repository

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Database.init(context)
        RepositoryUpdater.init(CoroutineScope(Dispatchers.Default), FakeDownloader)
        repository = Repository(
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

    @Test
    fun processFile() = runTest {
        val output = benchmark(1) {
            val mergerFile = File.createTempFile("index", "merger")
            val index = FakeDownloader.downloadIndex(context, Izzy, "index", "index-v1.jar")
            process(index, mergerFile)
        }
        println(output)
    }

    private fun process(file: File, merger: File) = measureTimeMillis {
        RepositoryUpdater.processFile(
            context = context,
            repository = repository,
            indexType = RepositoryUpdater.IndexType.INDEX_V1,
            unstable = false,
            file = file,
            mergerFile = merger,
            lastModified = "",
            entityTag = "",
            callback = { stage, current, total ->

            },
        )
    }
}
