package com.looker.droidify.datastore.model

import kotlinx.serialization.Serializable

@Serializable
data class ProxyPreference(
    val type: ProxyType = ProxyType.DIRECT,
    val host: String = "localhost",
    val port: Int = 9050
) {
    fun update(
        newType: ProxyType? = null,
        newHost: String? = null,
        newPort: Int? = null
    ): ProxyPreference = copy(
        type = newType ?: type,
        host = newHost ?: host,
        port = newPort ?: port
    )
}
