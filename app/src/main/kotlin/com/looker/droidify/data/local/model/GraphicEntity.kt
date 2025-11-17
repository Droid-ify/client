package com.looker.droidify.data.local.model

import androidx.annotation.IntDef
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import com.looker.droidify.data.model.FilePath
import com.looker.droidify.data.model.Graphics
import com.looker.droidify.sync.v2.model.MetadataV2

@Entity(
    tableName = "graphic",
    primaryKeys = ["type", "locale", "appId"],
    indices = [Index("appId", "locale"), Index("appId")],
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
    @param:GraphicType
    val type: Int,
    val url: String,
    val locale: String,
    val appId: Int,
)

fun MetadataV2.localizedGraphics(appId: Int): List<GraphicEntity>? {
    return buildList {
        promoGraphic?.forEach { (locale, value) ->
            add(GraphicEntity(PROMO_GRAPHIC, value.name, locale, appId))
        }
        featureGraphic?.forEach { (locale, value) ->
            add(GraphicEntity(FEATURE_GRAPHIC, value.name, locale, appId))
        }
        tvBanner?.forEach { (locale, value) ->
            add(GraphicEntity(TV_BANNER, value.name, locale, appId))
        }
        video?.forEach { (locale, value) ->
            add(GraphicEntity(VIDEO, value, locale, appId))
        }
    }.ifEmpty { null }
}

fun List<GraphicEntity>.toGraphics(locale: String, baseAddress: String): Graphics {
    var featureGraphic: FilePath? = null
    var promoGraphic: FilePath? = null
    var tvBanner: FilePath? = null
    var video: FilePath? = null

    for (entity in this) {
        if (entity.locale != locale) continue
        when (entity.type) {
            FEATURE_GRAPHIC -> featureGraphic = FilePath(baseAddress, entity.url)
            PROMO_GRAPHIC -> promoGraphic = FilePath(baseAddress, entity.url)
            TV_BANNER -> tvBanner = FilePath(baseAddress, entity.url)
            VIDEO -> video = FilePath(baseAddress, entity.url)
        }
    }
    return Graphics(
        featureGraphic = featureGraphic,
        promoGraphic = promoGraphic,
        tvBanner = tvBanner,
        video = video,
    )
}

@Retention(AnnotationRetention.BINARY)
@IntDef(
    VIDEO,
    TV_BANNER,
    PROMO_GRAPHIC,
    FEATURE_GRAPHIC,
)
annotation class GraphicType

private const val VIDEO = 0
private const val TV_BANNER = 1
private const val PROMO_GRAPHIC = 2
private const val FEATURE_GRAPHIC = 3
