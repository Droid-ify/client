package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.droidify.domain.model.Links
import com.looker.droidify.sync.v2.model.MetadataV2

@Entity(
    tableName = "link",
    indices = [Index("appId")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            childColumns = ["appId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        )
    ]
)
data class LinksEntity(
    val changelog: String?,
    val issueTracker: String?,
    val translation: String?,
    val sourceCode: String?,
    val webSite: String?,
    val appId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

fun MetadataV2.linkEntity(appId: Int) = LinksEntity(
    appId = appId,
    changelog = changelog,
    issueTracker = issueTracker,
    translation = translation,
    sourceCode = sourceCode,
    webSite = webSite,
)

fun LinksEntity.toLinks() = Links(
    changelog = changelog,
    issueTracker = issueTracker,
    translation = translation,
    sourceCode = sourceCode,
    webSite = webSite,
)
