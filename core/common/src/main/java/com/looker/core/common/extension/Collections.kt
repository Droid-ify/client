package com.looker.core.common.extension

fun <T> Set<T>.updateAsMutable(block: MutableSet<T>.() -> Unit): Set<T> {
	return toMutableSet().apply(block)
}