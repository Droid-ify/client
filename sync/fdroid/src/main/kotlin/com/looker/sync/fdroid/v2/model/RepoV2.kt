package com.looker.sync.fdroid.v2.model

import kotlinx.serialization.Serializable

@Serializable
data class RepoV2(
    val address: String,
    val icon: LocalizedIcon? = null,
    val name: LocalizedString = emptyMap(),
    val description: LocalizedString = emptyMap(),
    val antiFeatures: Map<String, AntiFeatureV2> = emptyMap(),
    val categories: Map<String, CategoryV2> = emptyMap(),
    val mirrors: List<MirrorV2> = emptyList(),
    val timestamp: Long,
)

@Serializable
data class MirrorV2(
    val url: String,
    val isPrimary: Boolean? = null,
    val location: String? = null
)

@Serializable
data class CategoryV2(
    val icon: LocalizedIcon = emptyMap(),
    val name: LocalizedString,
    val description: LocalizedString = emptyMap(),
)

@Serializable
data class AntiFeatureV2(
    val icon: LocalizedIcon = emptyMap(),
    val name: LocalizedString,
    val description: LocalizedString = emptyMap(),
)
