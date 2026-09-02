package com.looker.droidify.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.looker.droidify.assets
import com.looker.droidify.data.local.droidifyDb
import com.looker.droidify.data.local.sql.DroidifyDb
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.sync.JsonParser
import com.looker.droidify.sync.v2.model.IndexV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

class IndexRepositoryBenchmark {

    private val fingerprint = Fingerprint("ab".repeat(32))

    @Test
    fun `benchmark fdroid index import`() = benchmark("fdroid_index_v2.json")

    @Test
    fun `benchmark izzy index import`() = benchmark("izzy_index_v2.json")

    private fun benchmark(asset: String) {
        val (index, parseTime) = measureTimedValue {
            JsonParser.decodeFromString<IndexV2>(
                assets(asset)!!.readBytes().decodeToString(),
            )
        }

        repeat(WARMUP) { insertIndex(index) }
        val timings = List(ITERATIONS) { insertIndex(index) }
        report(asset, index.packages.size, parseTime, timings)
    }

    private fun insertIndex(index: IndexV2): Duration {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.use { driver ->
            DroidifyDb.Schema.create(driver)
            val db = droidifyDb(driver)
            val repository = IndexRepository(db, Dispatchers.Unconfined)
            val repoId = insertBareRepo(db)
            return measureTime {
                runBlocking { repository.insertIndex(repoId, fingerprint, index) }
            }
        }
    }

    private fun insertBareRepo(db: DroidifyDb): Long {
        db.repositoryQueries.insertRepo(
            address = "https://repo.test/repo",
            webBaseUrl = null,
            fingerprint = null,
            etag = null,
            timestamp = null,
            enabled = true,
        )
        return db.repositoryQueries.lastInsertRowId().executeAsOne()
    }

    private fun report(name: String, packages: Int, parse: Duration, timings: List<Duration>) {
        val ms = timings.map { it.inWholeMicroseconds / 1000.0 }.sorted()
        val n = ms.size
        val mean = ms.average()
        val stddev = sqrt(ms.sumOf { (it - mean).pow(2) } / (n - 1))
        val ci95 = t95(n - 1) * stddev / sqrt(n.toDouble())
        val median = percentile(ms, 50.0)

        println(
            """
            [$name] $packages packages, parse ${parse.fmt()}
              runs    $n (+$WARMUP warmup)
              mean    ${mean.fmt()} ± ${ci95.fmt()} (95% CI)
              stddev  ${stddev.fmt()} (cv ${"%.1f".format(stddev / mean * 100)}%)
              median  ${median.fmt()}  p90 ${percentile(ms, 90.0).fmt()}
              range   ${ms.first().fmt()} … ${ms.last().fmt()}
              rate    ${"%.0f".format(packages / (median / 1000))} packages/s (median)
            """.trimIndent(),
        )
    }

    /** Linear-interpolated percentile over a sorted sample. */
    private fun percentile(sorted: List<Double>, p: Double): Double {
        val rank = p / 100 * (sorted.size - 1)
        val low = rank.toInt()
        val high = minOf(low + 1, sorted.size - 1)
        return sorted[low] + (sorted[high] - sorted[low]) * (rank - low)
    }

    /** Two-tailed Student's t critical value at 95% confidence. */
    private fun t95(df: Int): Double = when {
        df <= 1 -> 12.706
        df == 2 -> 4.303
        df == 3 -> 3.182
        df == 4 -> 2.776
        df == 5 -> 2.571
        df == 6 -> 2.447
        df == 7 -> 2.365
        df == 8 -> 2.306
        df == 9 -> 2.262
        df <= 15 -> 2.131
        df <= 30 -> 2.042
        else -> 1.960
    }

    private fun Double.fmt(): String =
        if (this >= 1000) "%.3f s".format(this / 1000) else "%.1f ms".format(this)

    private fun Duration.fmt(): String = (inWholeMicroseconds / 1000.0).fmt()

    private companion object {
        const val WARMUP = 2
        const val ITERATIONS = 10
    }
}
