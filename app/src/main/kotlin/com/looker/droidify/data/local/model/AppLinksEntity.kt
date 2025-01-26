package com.looker.droidify.data.local.model

import com.looker.droidify.domain.model.Links
import com.looker.droidify.sync.v2.model.MetadataV2

data class AppLinksEntity(
    val changelog: String?,
    val issueTracker: String?,
    val translation: String?,
    val sourceCode: String?,
    val webSite: String?,
    val id: Int = -1,
)

fun MetadataV2.linkEntity() = AppLinksEntity(
    changelog = changelog,
    issueTracker = issueTracker,
    translation = translation,
    sourceCode = sourceCode,
    webSite = webSite,
)

fun AppLinksEntity.toLinks() = Links(
    changelog = changelog,
    issueTracker = issueTracker,
    translation = translation,
    sourceCode = sourceCode,
    webSite = webSite,
)
