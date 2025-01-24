package com.looker.droidify.sync.v2.model

/*
* FileV2 is licensed under the GPL 3.0 to FDroid Organization.
* */

import kotlinx.serialization.Serializable

@Serializable
data class FileV2(
    val name: String,
    val sha256: String? = null,
    val size: Long? = null,
)

@Serializable
data class ApkFileV2(
    val name: String,
    val sha256: String,
    val size: Long,
)
