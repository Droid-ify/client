package com.looker.sync.fdroid.v2.model

import kotlinx.serialization.Serializable

@Serializable
data class FileV2(
    val name: String,
    val sha256: String? = null,
    val size: Long? = null,
)
