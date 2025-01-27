package com.looker.droidify.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.droidify.sync.v2.model.AntiFeatureReason
import com.looker.droidify.sync.v2.model.ApkFileV2
import com.looker.droidify.sync.v2.model.FileV2
import com.looker.droidify.sync.v2.model.LocalizedString
import com.looker.droidify.sync.v2.model.PermissionV2
import com.looker.droidify.sync.v2.model.Tag

@Entity(
    tableName = "version",
    indices = [Index("appId")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            childColumns = ["appId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
    ],
)
data class VersionEntity(
    val added: Long,
    val whatsNew: LocalizedString,
    val versionName: String,
    val versionCode: Long,
    val maxSdkVersion: Int?,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    @Embedded("apk_")
    val apk: ApkFileV2,
    @Embedded("src_")
    val src: FileV2?,
    val features: List<String>,
    val nativeCode: List<String>,
    val permissions: List<PermissionV2>,
    val permissionsSdk23: List<PermissionV2>,
    val antiFeature: Map<Tag, AntiFeatureReason>,
    val appId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)
