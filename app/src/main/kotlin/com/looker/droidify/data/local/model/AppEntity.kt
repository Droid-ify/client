package com.looker.droidify.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString
import com.looker.droidify.sync.v2.model.MetadataV2

@Entity(
    tableName = "app",
    indices = [
        Index("authorId"),
        Index("repoId"),
        Index("packageName"),
        Index("packageName", "repoId", unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = RepoEntity::class,
            childColumns = ["repoId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
        ForeignKey(
            entity = AuthorEntity::class,
            childColumns = ["authorId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
    ],
)
data class AppEntity(
    val added: Long,
    val lastUpdated: Long,
    val license: String?,
    val name: LocalizedString,
    val icon: LocalizedIcon?,
    val preferredSigner: String?,
    val summary: LocalizedString?,
    val description: LocalizedString?,
    val packageName: String,
    val authorId: Int,
    val repoId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

data class AppEntityRelations(
    @Embedded val app: AppEntity,
    @Relation(
        parentColumn = "authorId",
        entityColumn = "id",
    )
    val author: AuthorEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val links: LinksEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val graphics: List<GraphicEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val screenshots: List<ScreenshotEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId",
    )
    val version: List<VersionEntity>,
)

fun MetadataV2.appEntity(
    packageName: String,
    repoId: Int,
    authorId: Int,
) = AppEntity(
    added = added,
    lastUpdated = lastUpdated,
    license = license,
    name = name,
    icon = icon,
    preferredSigner = preferredSigner,
    summary = summary,
    description = description,
    packageName = packageName,
    authorId = authorId,
    repoId = repoId,
)
