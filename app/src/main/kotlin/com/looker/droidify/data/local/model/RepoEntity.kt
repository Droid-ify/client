package com.looker.droidify.data.local.model

import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString

data class RepoEntity(
    val icon: LocalizedIcon? = null,
    val address: String,
    val name: LocalizedString,
    val description: LocalizedString,
    val timestamp: Long,
    val id: Int = -1,
)
