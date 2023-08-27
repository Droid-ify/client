package com.looker.core.common.extension

fun <T> Set<T>.updateAsMutable(block: MutableSet<T>.() -> Unit): Set<T> {
	return toMutableSet().apply(block)
}

fun <T> List<T>.updateAsMutable(block: MutableList<T>.() -> Unit): List<T> {
	return toMutableList().apply(block)
}
