package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.droidify.sync.v2.model.AntiFeatureV2
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString
import com.looker.droidify.sync.v2.model.Tag

@Entity(
    tableName = "anti_feature",
    indices = [Index("repoId")],
    foreignKeys = [
        ForeignKey(
            entity = RepoEntity::class,
            childColumns = ["repoId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
    ],
)
data class AntiFeatureEntity(
    val icon: LocalizedIcon?,
    val tag: Tag,
    val name: LocalizedString,
    val description: LocalizedString?,
    val repoId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = -1,
)

fun AntiFeatureV2.antiFeatureEntity(
    tag: Tag,
    repoId: Int,
) = AntiFeatureEntity(
    icon = icon,
    tag = tag,
    name = name,
    description = description,
    repoId = repoId,
)
