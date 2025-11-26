package com.looker.droidify.index

import com.looker.droidify.assets
import com.looker.droidify.sync.JsonParser
import com.looker.droidify.sync.v2.model.IndexV2
import com.looker.droidify.sync.v2.model.PackageV2
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class IndexValuesTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `test values in index v2`() = runTest(dispatcher) {
        val izzy = assets("izzy_index_v2_updated.json")!!
        val fdroid = assets("fdroid_index_v2.json")!!
        val izzyIndex: IndexV2 = JsonParser.decodeFromString(izzy.readBytes().decodeToString())
        val fdroidIndex: IndexV2 = JsonParser.decodeFromString(fdroid.readBytes().decodeToString())

        var hits = 0
        var total = 0

        val nativeCode = mutableSetOf<String>()
        val performTest: (PackageV2) -> Unit = { data ->
            data.versions.forEach { t, u ->
                hits++
                nativeCode.addAll(u.manifest.nativecode)
            }
            total++
        }

        izzyIndex.packages.forEach { (packageName, data) ->
//            println("Testing on Izzy $packageName")
            performTest(data)
        }
        fdroidIndex.packages.forEach { (packageName, data) ->
//            println("Testing on FDroid $packageName")
            performTest(data)
        }
        println(nativeCode)
        println("Hits: %d, %.2f%%".format(hits, hits * 100 / total.toFloat()))
    }
}
