package com.looker.droidify.data.local.model

import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString

data class MetadataEntity(
    val added: Long,
    val lastUpdated: Long,
    val license: String,
    val name: LocalizedString,
    val icon: LocalizedIcon?,
    val preferredSigner: String?,
    val summary: LocalizedString?,
    val description: LocalizedString?,
    val packageName: String,
    val authorId: Int,
    val repoId: Int,
    val id: Int = -1,
)
