package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.droidify.sync.v2.model.AntiFeatureV2
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString
import com.looker.droidify.sync.v2.model.Tag

@Entity(
    tableName = "anti_feature",
    indices = [Index("tag")],
)
data class AntiFeatureEntity(
    val icon: LocalizedIcon?,
    val tag: Tag,
    val name: LocalizedString,
    val description: LocalizedString?,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

fun AntiFeatureV2.antiFeatureEntity(
    tag: Tag,
) = AntiFeatureEntity(
    icon = icon,
    tag = tag,
    name = name,
    description = description,
)
