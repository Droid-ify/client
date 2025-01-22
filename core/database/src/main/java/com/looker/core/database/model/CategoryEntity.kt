package com.looker.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.core.database.utils.localizedValue
import com.looker.core.domain.model.Category

@Entity(
    tableName = "category",
    foreignKeys = [
        ForeignKey(
            entity = RepoEntity::class,
            parentColumns = ["id"],
            childColumns = ["repoId"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("tag")],
)
data class CategoryEntity(
    val tag: String,
    val icon: LocalizedString,
    val name: LocalizedString,
    val description: LocalizedString,
    val repoId: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Long = -1L,
)

private fun CategoryEntity.toCategory(locale: String) =
    Category(
        id = id,
        name = name.localizedValue(locale) ?: "",
        icon = icon.localizedValue(locale) ?: "",
        description = description.localizedValue(locale) ?: ""
    )

fun Collection<CategoryEntity>.toCategoryList(locale: String): List<Category> =
    map { it.toCategory(locale) }
