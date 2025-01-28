package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.droidify.domain.model.Graphics
import com.looker.droidify.sync.v2.model.MetadataV2

@Entity(
    tableName = "graphic",
    indices = [Index("appId", "locale")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            childColumns = ["appId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
    ],
)
data class GraphicEntity(
    val type: GraphicType,
    val url: String,
    val locale: String,
    val appId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

enum class GraphicType {
    Video,
    TvBanner,
    PromoGraphic,
    FeatureGraphic,
}

fun MetadataV2.localizedGraphics(appId: Int): List<GraphicEntity>? {
    return buildList {
        promoGraphic?.forEach { (locale, value) ->
            add(GraphicEntity(GraphicType.PromoGraphic, value.name, locale, appId))
        }
        featureGraphic?.forEach { (locale, value) ->
            add(GraphicEntity(GraphicType.FeatureGraphic, value.name, locale, appId))
        }
        tvBanner?.forEach { (locale, value) ->
            add(GraphicEntity(GraphicType.TvBanner, value.name, locale, appId))
        }
        video?.forEach { (locale, value) ->
            add(GraphicEntity(GraphicType.FeatureGraphic, value, locale, appId))
        }
    }.ifEmpty { null }
}

fun List<GraphicEntity>.toGraphics(): Graphics {
    var featureGraphic: String? = null
    var promoGraphic: String? = null
    var tvBanner: String? = null
    var video: String? = null

    for (entity in this) {
        when (entity.type) {
            GraphicType.FeatureGraphic -> featureGraphic = entity.url
            GraphicType.PromoGraphic -> promoGraphic = entity.url
            GraphicType.TvBanner -> tvBanner = entity.url
            GraphicType.Video -> video = entity.url
        }
    }
    return Graphics(
        featureGraphic = featureGraphic,
        promoGraphic = promoGraphic,
        tvBanner = tvBanner,
        video = video,
    )
}
