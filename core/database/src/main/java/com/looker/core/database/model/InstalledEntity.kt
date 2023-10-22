package com.looker.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class InstalledEntity(
    @PrimaryKey
    val packageName: String,
    val versionCode: Long,
    val signature: String
)
