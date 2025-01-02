package com.looker.sync.fdroid.v1.model

/*
* IndexV1 is licensed under the GPL 3.0 to FDroid Organization.
* */

import kotlinx.serialization.Serializable

@Serializable
data class IndexV1(
    val repo: RepoV1,
    val apps: List<AppV1> = emptyList(),
    val packages: Map<String, List<PackageV1>> = emptyMap(),
)
