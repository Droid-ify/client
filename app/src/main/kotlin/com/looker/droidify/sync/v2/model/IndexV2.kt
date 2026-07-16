package com.looker.droidify.sync.v2.model

/*
* IndexV2, RepoV2 are licensed under the GPL 3.0 to FDroid Organization.
* */

import kotlinx.serialization.Serializable

@Serializable
data class IndexV2(
    val repo: RepoV2,
    val packages: Map<String, PackageV2>,
)
