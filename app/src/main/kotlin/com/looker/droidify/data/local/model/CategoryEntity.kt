package com.looker.droidify.data.local.model

import androidx.room.Entity
import com.looker.droidify.sync.v2.model.CategoryV2
import com.looker.droidify.sync.v2.model.DefaultName

@Entity(
    tableName = "category",
    primaryKeys = ["defaultName", "locale"],
)
data class CategoryEntity(
    val icon: String?,
    val name: String,
    val description: String?,
    val locale: String,
    val defaultName: DefaultName,
)

@Entity(primaryKeys = ["repoId", "defaultName"])
data class CategoryRepoRelation(
    val repoId: Int,
    val defaultName: DefaultName,
)

@Entity(primaryKeys = ["appId", "defaultName"])
data class CategoryAppRelation(
    val appId: Int,
    val defaultName: DefaultName,
)

fun CategoryV2.categoryEntity(
    defaultName: DefaultName,
): List<CategoryEntity> {
    return name.map { (locale, localizedName) ->
        CategoryEntity(
            icon = icon[locale]?.name,
            name = localizedName,
            description = description[locale],
            defaultName = defaultName,
            locale = locale,
        )
    }
}
