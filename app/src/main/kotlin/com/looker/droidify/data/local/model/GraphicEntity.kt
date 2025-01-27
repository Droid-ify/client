package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.droidify.domain.model.Graphics
import com.looker.droidify.sync.v2.model.MetadataV2
import com.looker.droidify.sync.v2.model.locales
import com.looker.droidify.sync.v2.model.localizedValue

@Entity(
    tableName = "graphic",
    indices = [Index("appId", "locale"), Index("appId")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            childColumns = ["appId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
    ]
)
data class GraphicEntity(
    val video: String?,
    val tvBanner: String?,
    val promoGraphic: String?,
    val featureGraphic: String?,
    val locale: String,
    val appId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = -1,
)

fun MetadataV2.localizedGraphics(appId: Int): List<GraphicEntity> {
    val locales = mutableListOf<String>()
    locales.addAll(promoGraphic.locales())
    locales.addAll(featureGraphic.locales())
    locales.addAll(tvBanner.locales())
    locales.addAll(video.locales())

    return locales.map { locale ->
        GraphicEntity(
            appId = appId,
            locale = locale,
            promoGraphic = promoGraphic?.localizedValue(locale)?.name,
            featureGraphic = featureGraphic?.localizedValue(locale)?.name,
            tvBanner = tvBanner?.localizedValue(locale)?.name,
            video = video?.localizedValue(locale),
        )
    }
}

fun GraphicEntity.toGraphics() = Graphics(
    featureGraphic = featureGraphic,
    promoGraphic = promoGraphic,
    tvBanner = tvBanner,
    video = video,
)
