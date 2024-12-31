package com.looker.sync.fdroid.common

import kotlin.math.pow
import kotlin.math.sqrt

internal inline fun memory(
    repetition: Int,
    extraMessage: String? = null,
    block: () -> Long,
) {
    if (extraMessage != null) {
        println("=".repeat(50))
        println(extraMessage)
        println("=".repeat(50))
    }
    val times = DoubleArray(repetition)
    repeat(repetition) { iteration ->
        System.gc()
        System.runFinalization()
        times[iteration] = block().toDouble()
    }
    val meanAndDeviation = times.culledMeanAndDeviation()
    println("=".repeat(50))
    println(times.joinToString(" | "))
    println("${meanAndDeviation.first} ms ± ${meanAndDeviation.second.toFloat()} ms")
    println("=".repeat(50))
    println()
}

private fun DoubleArray.culledMeanAndDeviation(): Pair<Double, Double> {
    sort()
    return meanAndDeviation()
}

private fun DoubleArray.meanAndDeviation(): Pair<Double, Double> {
    val mean = average()
    return mean to sqrt(fold(0.0) { acc, value -> acc + (value - mean).pow(2) } / size)
}
