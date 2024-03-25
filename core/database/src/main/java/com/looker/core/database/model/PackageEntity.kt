package com.looker.core.database.model

import com.looker.core.database.utils.localizedValue
import com.looker.core.domain.model.ApkFile
import com.looker.core.domain.model.Manifest
import com.looker.core.domain.model.Package
import com.looker.core.domain.model.Permission
import com.looker.core.domain.model.Platforms
import com.looker.core.domain.model.SDKs
import kotlinx.serialization.Serializable

@Serializable
data class PackageEntity(
    val added: Long,
    val apkName: String,
    val hash: String,
    val hashType: String,
    val minSdkVersion: Int,
    val maxSdkVersion: Int,
    val targetSdkVersion: Int,
    val sig: String,
    val signer: String,
    val size: Long,
    val srcName: String,
    val usesPermission: List<PermissionEntity>,
    val versionCode: Long,
    val versionName: String,
    val nativeCode: List<String>,
    val features: List<String>,
    val antiFeatures: List<String>,
    val whatsNew: LocalizedString
)

@Serializable
data class PermissionEntity(
    val name: String,
    val minSdk: Int? = null,
    val maxSdk: Int? = null
)

fun PackageEntity.toExternal(locale: String, installed: Boolean): Package = Package(
    installed = installed,
    added = added,
    apk = ApkFile(
        name = apkName,
        hash = hash,
        size = size
    ),
    manifest = Manifest(
        versionCode = versionCode,
        versionName = versionName,
        usesSDKs = SDKs(minSdkVersion, targetSdkVersion),
        signer = setOf(signer),
        permissions = usesPermission.map(PermissionEntity::toExternalModel)
    ),
    platforms = Platforms(nativeCode),
    features = features,
    antiFeatures = antiFeatures,
    whatsNew = whatsNew.localizedValue(locale) ?: ""
)

fun List<PackageEntity>.toExternal(
    locale: String,
    installed: (PackageEntity) -> Boolean
): List<Package> = map { it.toExternal(locale, installed(it)) }

fun PermissionEntity.toExternalModel(): Permission = Permission(
    name = name,
    sdKs = SDKs(min = minSdk ?: -1, max = maxSdk ?: -1)
)
