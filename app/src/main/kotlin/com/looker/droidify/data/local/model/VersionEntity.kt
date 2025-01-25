package com.looker.droidify.data.local.model

import com.looker.droidify.sync.v2.model.AntiFeatureReason
import com.looker.droidify.sync.v2.model.ApkFileV2
import com.looker.droidify.sync.v2.model.FileV2
import com.looker.droidify.sync.v2.model.LocalizedString
import com.looker.droidify.sync.v2.model.PermissionV2
import com.looker.droidify.sync.v2.model.Tag

data class VersionEntity(
    val added: Long,
    val whatsNew: LocalizedString,
    val versionName: String,
    val versionCode: Long,
    val maxSdkVersion: Int?,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val apk: ApkFileV2,
    val src: FileV2?,
    val features: List<String>,
    val nativeCode: List<String>,
    val permissions: List<PermissionV2>,
    val permissionsSdk23: List<PermissionV2>,
    val antiFeature: Map<Tag, AntiFeatureReason>,
    val packageId: Int,
    val id: Int = -1,
)
