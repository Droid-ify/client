package com.looker.droidify.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.droidify.data.model.ApkFile
import com.looker.droidify.data.model.Manifest
import com.looker.droidify.data.model.Package
import com.looker.droidify.data.model.Permission
import com.looker.droidify.data.model.Platforms
import com.looker.droidify.data.model.SDKs
import com.looker.droidify.network.DataSize
import com.looker.droidify.sync.v2.model.ApkFileV2
import com.looker.droidify.sync.v2.model.FileV2
import com.looker.droidify.sync.v2.model.LocalizedString
import com.looker.droidify.sync.v2.model.PackageV2
import com.looker.droidify.sync.v2.model.PermissionV2
import com.looker.droidify.sync.v2.model.localizedValue

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
    val appId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

fun PackageV2.versionEntities(appId: Int): Map<VersionEntity, List<AntiFeatureAppRelation>> {
    return versions.map { (_, version) ->
        VersionEntity(
            added = version.added,
            whatsNew = version.whatsNew.mapValues { (locale, value) -> value.replace("\n", "<br/>") },
            versionName = version.manifest.versionName,
            versionCode = version.manifest.versionCode,
            maxSdkVersion = version.manifest.maxSdkVersion,
            minSdkVersion = version.manifest.usesSdk?.minSdkVersion ?: -1,
            targetSdkVersion = version.manifest.usesSdk?.targetSdkVersion ?: -1,
            apk = version.file,
            src = version.src,
            features = version.manifest.features.map { it.name },
            nativeCode = version.manifest.nativecode,
            permissions = version.manifest.usesPermission,
            permissionsSdk23 = version.manifest.usesPermissionSdk23,
            appId = appId,
        ) to version.antiFeatures.map { (tag, reason) ->
            AntiFeatureAppRelation(
                tag = tag,
                reason = reason,
                appId = appId,
                versionCode = version.manifest.versionCode,
            )
        }
    }.toMap()
}

fun List<VersionEntity>.toPackages(
    locale: String,
    installed: InstalledEntity?,
) = map { version ->
    Package(
        id = version.id.toLong(),
        installed = installed != null && installed.versionCode == version.versionCode,
        added = version.added,
        apk = ApkFile(
            name = version.apk.name,
            hash = version.apk.sha256,
            size = DataSize(version.apk.size),
        ),
        platforms = Platforms(version.nativeCode),
        features = version.features,
        antiFeatures = emptyList(), // This would need to be populated from AntiFeatureAppRelation
        manifest = Manifest(
            versionCode = version.versionCode,
            versionName = version.versionName,
            usesSDKs = SDKs(
                min = version.minSdkVersion,
                max = version.maxSdkVersion ?: -1,
                target = version.targetSdkVersion,
            ),
            signer = emptySet(), // This would need to be populated from somewhere
            permissions = version.permissions.map {
                Permission(
                    name = it.name,
                    sdKs = SDKs(
                        min = -1, // PermissionV2 doesn't have minSdkVersion
                        max = it.maxSdkVersion ?: -1,
                        target = -1,
                    ),
                )
            },
        ),
        whatsNew = version.whatsNew.localizedValue(locale) ?: "",
    )
}
