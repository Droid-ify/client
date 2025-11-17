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
    override val appId: Int,
    override val locale: String,
    val name: String,
) : LocalizedEntityData

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
    override val appId: Int,
    override val locale: String,
    val summary: String,
) : LocalizedEntityData

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
    override val appId: Int,
    override val locale: String,
    val description: String,
) : LocalizedEntityData

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
    override val appId: Int,
    override val locale: String,
    @Embedded(prefix = "icon_")
    val icon: FileV2,
) : LocalizedEntityData

sealed interface LocalizedEntityData {
    val appId: Int
    val locale: String
}

fun <T : LocalizedEntityData> List<T>.findLocale(locale: String): T {
    require(isNotEmpty()) { "List of localized data cannot be empty" }
    var bestMatch: T? = null
    var englishMatch: T? = null
    for (i in indices) {
        val match = get(i)
        val l = match.locale
        if (l == locale || l.startsWith(locale)) bestMatch = match
        if (l == "en-US" || l == "en") englishMatch = match
    }
    return bestMatch ?: englishMatch ?: first()
}
