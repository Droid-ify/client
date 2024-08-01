package com.looker.sync.fdroid

import com.looker.core.domain.model.Fingerprint
import com.looker.network.DataSize
import com.looker.sync.fdroid.common.Izzy
import com.looker.sync.fdroid.common.getResource
import com.looker.sync.fdroid.common.toV2
import com.looker.sync.fdroid.v1.V1Parser
import com.looker.sync.fdroid.v2.V2Parser
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import java.util.jar.JarEntry
import kotlin.time.measureTime

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class V1ParserTest {

    private val dispatcher = StandardTestDispatcher()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val validator = object : IndexValidator {
        override suspend fun validate(
            jarEntry: JarEntry,
            expectedFingerprint: Fingerprint?
        ): Fingerprint {
            return expectedFingerprint ?: Fingerprint("0".repeat(64))
        }

    }
    private val v1Parser = V1Parser(dispatcher, json, validator)
    private val v2Parser = V2Parser(dispatcher, json)
    private val jarFile = getResource("izzy_index_v1.jar")
    private val v2JsonFile = getResource("izzy_index_v2.json")
    private val repo = Izzy

    @Test
    fun `parse v1 json and compare with v2`() = runTest(dispatcher) {
        requireNotNull(jarFile)
        requireNotNull(v2JsonFile)
        memory {
            val (_, indexV1) = v1Parser.parse(jarFile, repo)
            val convertedIndex = indexV1.toV2()
            val (_, indexV2) = v2Parser.parse(v2JsonFile, repo)
            assertEquals(indexV2.packages.size, convertedIndex.packages.size)
            assertIterableEquals(
                indexV2.packages.keys.sorted(),
                convertedIndex.packages.keys.sorted()
            )
        }
    }
}

private inline fun memory(
    block: () -> Unit,
) {
    val runtime = Runtime.getRuntime()
    println("=".repeat(50))
    val initial = runtime.freeMemory()
    val time = measureTime {
        block()
    }
    val final = runtime.freeMemory()
    println("Time Taken: ${time}, Usage: ${DataSize(initial - final)} / ${DataSize(runtime.maxMemory())}")
    println("=".repeat(50))
    println()
}
