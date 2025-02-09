package com.looker.droidify.data.local.model

import androidx.room.Entity
import com.looker.droidify.sync.v2.model.AntiFeatureV2
import com.looker.droidify.sync.v2.model.Tag

@Entity(
    tableName = "anti_feature",
    primaryKeys = ["tag", "locale"],
)
data class AntiFeatureEntity(
    val icon: String?,
    val name: String,
    val description: String?,
    val locale: String,
    val tag: Tag,
)

@Entity(primaryKeys = ["repoId", "tag"])
data class AntiFeatureRepoRelation(
    val repoId: Int,
    val tag: Tag,
)

fun AntiFeatureV2.antiFeatureEntity(
    tag: Tag,
): List<AntiFeatureEntity> {
    return name.map { (locale, localizedName) ->
        AntiFeatureEntity(
            icon = icon[locale]?.name,
            name = localizedName,
            description = description[locale],
            tag = tag,
            locale = locale,
        )
    }
}
