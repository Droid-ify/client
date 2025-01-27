package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.droidify.domain.model.Screenshots
import com.looker.droidify.sync.v2.model.LocalizedFiles
import com.looker.droidify.sync.v2.model.ScreenshotsV2

@Entity(
    tableName = "screenshot",
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
data class ScreenshotEntity(
    val path: String,
    val type: ScreenshotType,
    val locale: String,
    val appId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = -1,
)

enum class ScreenshotType {
    Phone, SevenInch, TenInch, Wear, Tv
}

fun ScreenshotsV2.localizedScreenshots(appId: Int): List<ScreenshotEntity> {
    if (isNull) return emptyList()
    val screenshots = mutableListOf<ScreenshotEntity>()

    val screenshotIterator: (ScreenshotType, LocalizedFiles) -> Unit = { type, localizedFiles ->
        localizedFiles.forEach { (locale, files) ->
            for ((path, _, _) in files) {
                val entity = ScreenshotEntity(
                    locale = locale,
                    appId = appId,
                    type = type,
                    path = path,
                )
                screenshots.add(entity)
            }
        }
    }

    phone?.let { screenshotIterator(ScreenshotType.Phone, it) }
    sevenInch?.let { screenshotIterator(ScreenshotType.SevenInch, it) }
    tenInch?.let { screenshotIterator(ScreenshotType.TenInch, it) }
    wear?.let { screenshotIterator(ScreenshotType.Wear, it) }
    tv?.let { screenshotIterator(ScreenshotType.Tv, it) }

    return screenshots
}

fun List<ScreenshotEntity>.toScreenshots(): Screenshots {
    val screenshotMap = HashMap<ScreenshotType, MutableList<String>>(5)
    ScreenshotType.entries.forEach { type ->
        screenshotMap[type] = mutableListOf()
    }
    for (entity in this) {
        screenshotMap[entity.type]!!.add(entity.path)
    }

    return Screenshots(
        phone = screenshotMap[ScreenshotType.Phone]!!,
        sevenInch = screenshotMap[ScreenshotType.SevenInch]!!,
        tenInch = screenshotMap[ScreenshotType.TenInch]!!,
        wear = screenshotMap[ScreenshotType.Wear]!!,
        tv = screenshotMap[ScreenshotType.Tv]!!,
    )
}
