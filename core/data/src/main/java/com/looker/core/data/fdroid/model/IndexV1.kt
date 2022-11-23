package com.looker.core.data.fdroid.model

import kotlinx.serialization.Serializable

@Serializable
data class IndexV1(
	val repo: RepoDto,
	val requests: Requests = Requests(emptyList(), emptyList()),
	val apps: List<AppDto> = emptyList(),
	val packages: Map<String, List<PackageDto>> = emptyMap(),
)

@Serializable
data class Requests(
	val install: List<String>,
	val uninstall: List<String>,
)