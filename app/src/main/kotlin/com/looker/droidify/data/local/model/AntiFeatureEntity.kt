package com.looker.droidify.data.local.model

import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString
import com.looker.droidify.sync.v2.model.Tag

data class AntiFeatureEntity(
    val name: LocalizedString,
    val icon: LocalizedIcon? = null,
    val description: LocalizedString? = null,
    val tag: Tag,
    val repoId: Int,
    val id: Int = -1,
)
