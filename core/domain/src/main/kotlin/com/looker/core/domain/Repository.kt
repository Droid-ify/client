package com.looker.core.domain

import com.looker.core.domain.newer.isOnion
import java.net.URL

data class Repository(
    var id: Long,
    val address: String,
    val mirrors: List<String>,
    val name: String,
    val description: String,
    val version: Int,
    val enabled: Boolean,
    val fingerprint: String,
    val lastModified: String,
    val entityTag: String,
    val updated: Long,
    val timestamp: Long,
    val authentication: String
) {

    /**
     * Remove all onion addresses and supply it as random address
     *
     * If the list only contains onion urls we will provide the default address
     */
    val randomAddress: String
        get() = (mirrors + address)
            .filter { !it.isOnion }
            .randomOrNull() ?: address

    fun edit(address: String, fingerprint: String, authentication: String): Repository {
        val isAddressChanged = this.address != address
        val isFingerprintChanged = this.fingerprint != fingerprint
        val shouldForceUpdate = isAddressChanged || isFingerprintChanged
        return copy(
            address = address,
            fingerprint = fingerprint,
            lastModified = if (shouldForceUpdate) "" else lastModified,
            entityTag = if (shouldForceUpdate) "" else entityTag,
            authentication = authentication
        )
    }

    fun update(
        mirrors: List<String>,
        name: String,
        description: String,
        version: Int,
        lastModified: String,
        entityTag: String,
        timestamp: Long
    ): Repository {
        return copy(
            mirrors = mirrors,
            name = name,
            description = description,
            version = if (version >= 0) version else this.version,
            lastModified = lastModified,
            entityTag = entityTag,
            updated = System.currentTimeMillis(),
            timestamp = timestamp
        )
    }

    fun enable(enabled: Boolean): Repository {
        return copy(enabled = enabled, lastModified = "", entityTag = "")
    }

    @Suppress("SpellCheckingInspection")
    companion object {

        fun newRepository(
            address: String,
            fingerprint: String,
            authentication: String
        ): Repository {
            val name = try {
                URL(address).let { "${it.host}${it.path}" }
            } catch (e: Exception) {
                address
            }
            return defaultRepository(address, name, "", 0, true, fingerprint, authentication)
        }

        private fun defaultRepository(
            address: String,
            name: String,
            description: String,
            version: Int = 21,
            enabled: Boolean = false,
            fingerprint: String,
            authentication: String = ""
        ): Repository {
            return Repository(
                -1, address, emptyList(), name, description, version, enabled,
                fingerprint, "", "", 0L, 0L, authentication
            )
        }

        val defaultRepositories = listOf(
            defaultRepository(
                address = "https://raw.githubusercontent.com/ThatFinnDev/fullcodesfdroid/master/repo",
                name = "FullCodeApps",
                description = "This is a repository of apps from FullCodes (and indiviual members).",
                enabled = true,
                fingerprint = "D5BF9498AEB322FEE69C042CB4645D74102D53473EE3ACE394ED5BBD19AC316E"
            )
        )

        val newlyAdded = listOf<Repository>()
    }
}
