package com.looker.sync.fdroid.v2.model

import kotlinx.serialization.Serializable

@Serializable
data class IndexV2(
    val repo: RepoV2,
    val packages: Map<String, PackageV2>
)
