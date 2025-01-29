package com.looker.droidify.sync.common

import kotlin.math.pow
import kotlin.math.sqrt

internal inline fun benchmark(
    repetition: Int,
    extraMessage: String? = null,
    block: () -> Long,
): String {
    val times = DoubleArray(repetition)
    repeat(repetition) { iteration ->
        System.gc()
        System.runFinalization()
        times[iteration] = block().toDouble()
    }
    val meanAndDeviation = times.culledMeanAndDeviation()
    return buildString {
        append("=".repeat(50))
        append("\n")
        if (extraMessage != null) {
            println(extraMessage)
            append("=".repeat(50))
            append("\n")
        }
        append(times.joinToString(" | "))
        append("\n")
        append("${meanAndDeviation.first} ms ± ${meanAndDeviation.second.toFloat()} ms")
        append("\n")
        append("=".repeat(50))
        append("\n")
    }
}

private fun DoubleArray.culledMeanAndDeviation(): Pair<Double, Double> {
    sort()
    return meanAndDeviation()
}

private fun DoubleArray.meanAndDeviation(): Pair<Double, Double> {
    val mean = average()
    return mean to sqrt(fold(0.0) { acc, value -> acc + (value - mean).pow(2) } / size)
}
