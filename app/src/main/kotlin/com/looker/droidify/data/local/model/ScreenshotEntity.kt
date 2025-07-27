package com.looker.droidify.data.local.model

import androidx.annotation.IntDef
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import com.looker.droidify.data.model.Screenshots
import com.looker.droidify.sync.v2.model.LocalizedFiles
import com.looker.droidify.sync.v2.model.ScreenshotsV2

@Entity(
    tableName = "screenshot",
    primaryKeys = ["path", "type", "locale", "appId"],
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
data class ScreenshotEntity(
    val path: String,
    @param:ScreenshotType
    val type: Int,
    val locale: String,
    val appId: Int,
)

fun ScreenshotsV2.localizedScreenshots(appId: Int): List<ScreenshotEntity> {
    if (isNull) return emptyList()
    val screenshots = mutableListOf<ScreenshotEntity>()

    val screenshotIterator: (Int, LocalizedFiles?) -> Unit = { type, localizedFiles ->
        localizedFiles?.forEach { (locale, files) ->
            for ((path, _, _) in files) {
                screenshots.add(
                    ScreenshotEntity(
                        locale = locale,
                        appId = appId,
                        type = type,
                        path = path,
                    )
                )
            }
        }
    }
    screenshotIterator(PHONE, phone)
    screenshotIterator(SEVEN_INCH, sevenInch)
    screenshotIterator(TEN_INCH, tenInch)
    screenshotIterator(WEAR, wear)
    screenshotIterator(TV, tv)
    return screenshots
}

fun List<ScreenshotEntity>.toScreenshots(locale: String): Screenshots {
    val phone = mutableListOf<String>()
    val sevenInch = mutableListOf<String>()
    val tenInch = mutableListOf<String>()
    val wear = mutableListOf<String>()
    val tv = mutableListOf<String>()
    for (entity in this) {
        if (entity.locale != locale) continue
        when (entity.type) {
            PHONE -> phone.add(entity.path)
            SEVEN_INCH -> sevenInch.add(entity.path)
            TEN_INCH -> tenInch.add(entity.path)
            TV -> tv.add(entity.path)
            WEAR -> wear.add(entity.path)
        }
    }

    return Screenshots(
        phone = phone,
        sevenInch = sevenInch,
        tenInch = tenInch,
        wear = wear,
        tv = tv,
    )
}

@Retention(AnnotationRetention.BINARY)
@IntDef(
    PHONE,
    SEVEN_INCH,
    TEN_INCH,
    WEAR,
    TV,
)
private annotation class ScreenshotType

private const val PHONE = 0
private const val SEVEN_INCH = 1
private const val TEN_INCH = 2
private const val WEAR = 3
private const val TV = 4
