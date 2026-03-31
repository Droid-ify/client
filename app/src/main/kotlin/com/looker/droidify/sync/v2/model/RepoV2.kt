package com.looker.droidify.sync.v2.model
/*
* RepoV2, AntiFeatureV2, CategoryV2, MirrorV2 are licensed under the GPL 3.0 to FDroid Organization.
* */
import kotlinx.serialization.Serializable

@Serializable
data class RepoV2(
    val address: String,
    val webBaseUrl: String? = null,
    val icon: LocalizedIcon? = null,
    val name: LocalizedString = emptyMap(),
    val description: LocalizedString = emptyMap(),
    val antiFeatures: Map<Tag, AntiFeatureV2> = emptyMap(),
    val categories: Map<DefaultName, CategoryV2> = emptyMap(),
    val mirrors: List<MirrorV2> = emptyList(),
    val timestamp: Long,
)

@Serializable
data class MirrorV2(
    val url: String,
    val isPrimary: Boolean? = null,
    val countryCode: String? = null,
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
