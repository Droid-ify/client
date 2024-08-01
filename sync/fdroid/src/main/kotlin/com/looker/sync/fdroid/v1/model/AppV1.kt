package com.looker.sync.fdroid.v1.model

import kotlinx.serialization.Serializable

@Serializable
data class AppV1(
    val packageName: String,
    val icon: String? = null,
    val name: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val added: Long? = null,
    val antiFeatures: List<String> = emptyList(),
    val authorEmail: String? = null,
    val authorName: String? = null,
    val authorPhone: String? = null,
    val authorWebSite: String? = null,
    val binaries: String? = null,
    val bitcoin: String? = null,
    val categories: List<String> = emptyList(),
    val changelog: String? = null,
    val donate: String? = null,
    val flattrID: String? = null,
    val issueTracker: String? = null,
    val lastUpdated: Long? = null,
    val liberapay: String? = null,
    val liberapayID: String? = null,
    val license: String,
    val litecoin: String? = null,
    val localized: Map<String, Localized>? = null,
    val openCollective: String? = null,
    val sourceCode: String? = null,
    val suggestedVersionCode: String? = null,
    val translation: String? = null,
    val webSite: String? = null,
)

