package com.looker.core.common.extension

fun <T> Set<T>.updateAsMutable(block: MutableSet<T>.() -> Unit): Set<T> {
	return toMutableSet().apply(block)
}

fun <T> Collection<T>.firstIfOnly(
	block: () -> Exception = { NoSuchElementException("Collection is empty.") }
): T = firstOrNull() ?: throw block()