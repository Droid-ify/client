package com.looker.core.common

class Singleton<T> {
    private var value: T? = null

    /**
     * Updates the [value] if its null else it is returned
     */
    fun getOrUpdate(block: () -> T): T = value ?: kotlin.run {
        value = block()
        value!!
    }
}
