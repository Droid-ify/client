package com.looker.core.datastore.model

data class ProxyPreference(
    val type: ProxyType,
    val host: String,
    val port: Int
)
