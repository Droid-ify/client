package com.looker.droidify.sync.v1.model

/*
* RepoV1 is licensed under the GPL 3.0 to FDroid Organization.
* */

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
