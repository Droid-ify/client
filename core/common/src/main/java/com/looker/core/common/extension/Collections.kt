package com.looker.core.common.extension

fun <T> Set<T>.updateAsMutable(block: MutableSet<T>.() -> Unit): Set<T> {
	return toMutableSet().apply(block)
}

fun <T> Collection<T>.firstIfSingular(
	block: (size: Int) -> Exception = { NoSuchElementException("Collection size: $it, Expected: 1") }
): T = if (size == 1) first() else throw block(size)