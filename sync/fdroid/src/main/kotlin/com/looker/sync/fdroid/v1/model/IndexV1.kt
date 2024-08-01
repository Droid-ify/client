package com.looker.sync.fdroid.v1.model

import kotlinx.serialization.Serializable

@Serializable
data class IndexV1(
    val repo: RepoV1,
    val apps: List<AppV1> = emptyList(),
    val packages: Map<String, List<PackageV1>> = emptyMap(),
)
