package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString

@Entity(tableName = "category")
data class CategoryEntity(
    val icon: LocalizedIcon?,
    val name: LocalizedString,
    val description: LocalizedString?,
    val defaultName: DefaultName,
    @PrimaryKey(autoGenerate = true)
    val id: Int = -1,
)

data class RepoCategoryCrossRef(
    val repoId: Int,
    val categoryId: Int,
)

data class MetadataCategoryCrossRef(
    val appId: Int,
    val categoryId: Int,
)
