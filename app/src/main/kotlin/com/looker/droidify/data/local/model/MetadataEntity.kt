package com.looker.droidify.data.local.model

import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString

data class MetadataEntity(
    val packageName: String,
    val authorId: Int,
    val donateId: Int,
    val graphicId: Int,
    val screenshotId: Int,
    val linksId: Int,
    val added: Long,
    val lastUpdated: Long,
    val license: String,
    val name: LocalizedString,
    val categories: List<DefaultName>,
    val website: String?,
    val icon: LocalizedIcon?,
    val preferredSigner: String?,
    val summary: LocalizedString?,
    val description: LocalizedString?,
    val id: Int = -1,
)
