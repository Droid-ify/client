package com.looker.droidify.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import com.looker.droidify.sync.v2.model.FileV2

@Entity(
    tableName = "localized_app_name",
    primaryKeys = ["appId", "locale"],
    indices = [Index("appId"), Index("locale")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = CASCADE,
        ),
    ],
)
data class LocalizedAppNameEntity(
    val appId: Int,
    val locale: String,
    val name: String,
)

@Entity(
    tableName = "localized_app_summary",
    primaryKeys = ["appId", "locale"],
    indices = [Index("appId"), Index("locale")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = CASCADE,
        ),
    ],
)
data class LocalizedAppSummaryEntity(
    val appId: Int,
    val locale: String,
    val summary: String,
)

@Entity(
    tableName = "localized_app_description",
    primaryKeys = ["appId", "locale"],
    indices = [Index("appId"), Index("locale")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = CASCADE,
        ),
    ],
)
data class LocalizedAppDescriptionEntity(
    val appId: Int,
    val locale: String,
    val description: String,
)

@Entity(
    tableName = "localized_app_icon",
    primaryKeys = ["appId", "locale"],
    indices = [Index("appId"), Index("locale")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = CASCADE,
        ),
    ],
)
data class LocalizedAppIconEntity(
    val appId: Int,
    val locale: String,
    @Embedded(prefix = "icon_")
    val icon: FileV2,
)
