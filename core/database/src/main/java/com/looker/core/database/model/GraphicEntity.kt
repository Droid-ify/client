package com.looker.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "graphics",
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
data class GraphicEntity(
    val locale: String,
    val featureGraphic: String,
    val promoGraphic: String,
    val tvBanner: String,
    val video: String,
    val appId: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Long = -1L,
)
