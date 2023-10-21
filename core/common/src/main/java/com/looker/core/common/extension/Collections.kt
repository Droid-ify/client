package com.looker.core.common.extension

inline fun <K, E> Map<K, E>.updateAsMutable(block: MutableMap<K, E>.() -> Unit): Map<K, E> {
    return toMutableMap().apply(block)
}

inline fun <T> Set<T>.updateAsMutable(block: MutableSet<T>.() -> Unit): Set<T> {
    return toMutableSet().apply(block)
}

inline fun <T> MutableSet<T>.addAndCompute(item: T, block: (isAdded: Boolean) -> Unit): Boolean =
    add(item).apply { block(this) }

inline fun <T> List<T>.updateAsMutable(block: MutableList<T>.() -> Unit): List<T> {
    return toMutableList().apply(block)
}
