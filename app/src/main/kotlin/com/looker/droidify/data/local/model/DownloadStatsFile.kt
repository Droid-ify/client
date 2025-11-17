package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["fileName"], unique = true),
        Index(value = ["fileName", "lastModified"]),
    ],
)
data class DownloadStatsFile(
    @PrimaryKey
    val fileName: String,
    val lastModified: String,
    val lastFetched: Long = System.currentTimeMillis(),
    val fetchSuccess: Boolean = true,
    val fileSize: Long? = null,
    val recordsCount: Int? = null,
)
