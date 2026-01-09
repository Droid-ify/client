package com.looker.droidify.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.looker.droidify.sync.v2.model.CategoryV2
import com.looker.droidify.sync.v2.model.DefaultName

@Entity(
    tableName = "category",
    primaryKeys = ["defaultName", "locale"],
    indices = [Index("defaultName")]
)
data class CategoryEntity(
    val icon: String?,
    val name: String,
    val description: String?,
    val locale: String,
    val defaultName: DefaultName,
)

@Entity(
    tableName = "category_repo_relation",
    primaryKeys = ["id", "defaultName"],
    foreignKeys = [
        ForeignKey(
            entity = RepoEntity::class,
            childColumns = ["id"],
            parentColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CategoryRepoRelation(
    @ColumnInfo("id")
    val repoId: Int,
    val defaultName: DefaultName,
)

@Entity(
    tableName = "category_app_relation",
    primaryKeys = ["id", "defaultName"],
    indices = [Index("defaultName")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            childColumns = ["id"],
            parentColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CategoryAppRelation(
    @ColumnInfo("id")
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

// FIXME: This is a garbage algorithm
fun List<CategoryEntity>.filterLocalized(locale: String): List<CategoryEntity> =
    filter { it.locale == locale }.ifEmpty {
        filter { it.locale == "en-US" }.ifEmpty {
            filter { it.locale == "en" }
        }
    }
