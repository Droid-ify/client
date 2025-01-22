package com.looker.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "screenshots",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = CASCADE,
        ),
    ],
    indices = [Index("appId", "locale")],
)
data class ScreenshotEntity(
    val locale: String,
    val phone: List<String>,
    val sevenInch: List<String>,
    val tenInch: List<String>,
    val tv: List<String>,
    val wear: List<String>,
    val appId: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Long = -1L,
)
