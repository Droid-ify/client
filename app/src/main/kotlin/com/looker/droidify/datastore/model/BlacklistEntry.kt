package com.looker.droidify.datastore.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class BlacklistEntry(
    val id: String = UUID.randomUUID().toString(),
    val packagePattern: String = "",
    val appNamePattern: String = "",
)
