package com.looker.droidify.index

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.looker.droidify.model.Repository
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
@SmallTest
class RepositoryUpdaterTest {

    private lateinit var context: Context
    private lateinit var repository: Repository

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
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
    fun processFile() {
        testRepetition(1) {
            val createFile = File.createTempFile("index", "entry")
            val mergerFile = File.createTempFile("index", "merger")
            val jarStream = context.resources.assets.open("index-v1.jar")
            jarStream.copyTo(createFile.outputStream())
            process(createFile, mergerFile)
        }
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

    private inline fun testRepetition(repetition: Int, block: () -> Long) {
        val times = (1..repetition).map {
            System.gc()
            System.runFinalization()
            block().toDouble()
        }
        val meanAndDeviation = times.culledMeanAndDeviation()
        println(times)
        println("${meanAndDeviation.first} ± ${meanAndDeviation.second}")
    }
}

private fun List<Double>.culledMeanAndDeviation(): Pair<Double, Double> = when {
    isEmpty() -> Double.NaN to Double.NaN
    size == 1 || size == 2 -> this.meanAndDeviation()
    else -> sorted().subList(1, size - 1).meanAndDeviation()
}

private fun List<Double>.meanAndDeviation(): Pair<Double, Double> {
    val mean = average()
    return mean to sqrt(fold(0.0) { acc, value -> acc + (value - mean).squared() } / size)
}

private fun Double.squared() = this * this
