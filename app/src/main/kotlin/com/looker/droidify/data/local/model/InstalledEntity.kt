package com.looker.droidify.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.looker.droidify.model.InstalledItem

@Entity("installed")
data class InstalledEntity(
    @PrimaryKey
    val packageName: String,
    val version: String,
    val versionCode: Long,
    val signature: String
)

/**
 * Extension function to convert from domain model to entity
 */
fun InstalledItem.toEntity(): InstalledEntity = InstalledEntity(
    packageName = packageName,
    version = version,
    versionCode = versionCode,
    signature = signature
)

/**
 * Extension function to convert from entity to domain model
 */
fun InstalledEntity.toDomain(): InstalledItem = InstalledItem(
    packageName = packageName,
    version = version,
    versionCode = versionCode,
    signature = signature
)
