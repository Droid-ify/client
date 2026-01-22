package com.looker.droidify.model

data class InstalledItem(
    @JvmField
    val packageName: String,
    @JvmField
    val version: String,
    @JvmField
    val versionCode: Long,
    @JvmField
    val signature: String
)
