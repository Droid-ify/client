package com.looker.core.data.fdroid.model.v2

import kotlinx.serialization.Serializable

@Serializable
public data class RepoV2(
	val name: LocalizedTextV2 = emptyMap(),
	val icon: LocalizedFileV2 = emptyMap(),
	val address: String,
	val webBaseUrl: String? = null,
	val description: LocalizedTextV2 = emptyMap(),
	val mirrors: List<MirrorV2> = emptyList(),
	val timestamp: Long,
	val antiFeatures: Map<String, AntiFeatureV2> = emptyMap(),
	val categories: Map<String, CategoryV2> = emptyMap(),
	val releaseChannels: Map<String, ReleaseChannelV2> = emptyMap(),
) {
	public fun walkFiles(fileConsumer: (FileV2?) -> Unit) {
		icon.values.forEach { fileConsumer(it) }
		antiFeatures.values.forEach { it.icon.values.forEach { icon -> fileConsumer(icon) } }
		categories.values.forEach { it.icon.values.forEach { icon -> fileConsumer(icon) } }
	}
}
