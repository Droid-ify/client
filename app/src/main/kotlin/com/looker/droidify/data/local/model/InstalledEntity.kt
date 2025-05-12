package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("installed")
data class InstalledEntity(
    val versionCode: String,
    val versionName: String,
    val signature: String,
    @PrimaryKey
    val packageName: String,
)
