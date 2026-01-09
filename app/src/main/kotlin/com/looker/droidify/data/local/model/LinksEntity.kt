package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import com.looker.droidify.data.model.Links
import com.looker.droidify.sync.v2.model.MetadataV2

@Entity(
    tableName = "link",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            childColumns = ["appId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
    ],
)
data class LinksEntity(
    val changelog: String?,
    val issueTracker: String?,
    val translation: String?,
    val sourceCode: String?,
    val webSite: String?,
    @PrimaryKey
    val appId: Int,
)

private fun MetadataV2.isLinkNull(): Boolean {
    return changelog == null &&
        issueTracker == null &&
        translation == null &&
        sourceCode == null &&
        webSite == null
}

fun MetadataV2.linkEntity(appId: Int) = if (!isLinkNull()) {
    LinksEntity(
        appId = appId,
        changelog = changelog,
        issueTracker = issueTracker,
        translation = translation,
        sourceCode = sourceCode,
        webSite = webSite,
    )
} else null

fun LinksEntity.toLinks() = Links(
    changelog = changelog,
    issueTracker = issueTracker,
    translation = translation,
    sourceCode = sourceCode,
    webSite = webSite,
)
