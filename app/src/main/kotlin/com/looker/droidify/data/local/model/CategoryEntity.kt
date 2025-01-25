package com.looker.droidify.data.local.model

import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString

data class CategoryEntity(
    val name: LocalizedString,
    val icon: LocalizedIcon? = null,
    val description: LocalizedString? = null,
    val defaultName: DefaultName,
    val repoId: Int,
    val id: Int = -1,
)
