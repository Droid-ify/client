package com.looker.sync.fdroid.v1.model

import kotlinx.serialization.Serializable

@Serializable
data class RepoV1(
    val address: String,
    val icon: String,
    val name: String,
    val description: String,
    val timestamp: Long,
    val version: Int,
    val mirrors: List<String> = emptyList(),
)
