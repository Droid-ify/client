package com.looker.droidify.data.local.model

data class MirrorEntity(
    val url: String,
    val countryCode: String,
    val isPrimary: Boolean,
    val repoId: Int,
    val id: Int = -1,
)
