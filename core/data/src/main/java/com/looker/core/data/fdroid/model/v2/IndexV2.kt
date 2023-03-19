package com.looker.core.data.fdroid.model.v2

import kotlinx.serialization.Serializable

@Serializable
public data class IndexV2(
	val repo: RepoV2,
	val packages: Map<String, PackageV2> = emptyMap(),
) {
	public fun walkFiles(fileConsumer: (FileV2?) -> Unit) {
		repo.walkFiles(fileConsumer)
		packages.values.forEach { it.walkFiles(fileConsumer) }
	}
}
