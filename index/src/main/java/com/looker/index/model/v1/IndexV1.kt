package com.looker.index.model.v1

import kotlinx.serialization.Serializable

@Serializable
data class IndexV1(
	val repo: RepoV1,
	val requests: Requests = Requests(emptyList(), emptyList()),
	val apps: List<AppV1> = emptyList(),
	val packages: Map<String, List<PackageV1>> = emptyMap()
)

@Serializable
data class Requests(
	val install: List<String>,
	val uninstall: List<String>
)