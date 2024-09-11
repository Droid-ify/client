package com.looker.sync.fdroid.common

import com.looker.network.DataSize
import kotlin.time.measureTime

internal inline fun memory(
    extraMessage: String? = null,
    block: () -> Unit,
) {
    val runtime = Runtime.getRuntime()
    if (extraMessage != null) {
        println("=".repeat(50))
        println(extraMessage)
    }
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
