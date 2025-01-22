package com.looker.droidify.sync.v1.model

/*
* PackageV1, PermissionV1 are licensed under the GPL 3.0 to FDroid Organization.
* */

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageV1(
    val added: Long? = null,
    val apkName: String,
    val hash: String,
    val hashType: String,
    val minSdkVersion: Int? = null,
    val maxSdkVersion: Int? = null,
    val targetSdkVersion: Int? = minSdkVersion,
    val packageName: String,
    val sig: String? = null,
    val signer: String? = null,
    val size: Long,
    @SerialName("srcname")
    val srcName: String? = null,
    @SerialName("uses-permission")
    val usesPermission: List<PermissionV1> = emptyList(),
    @SerialName("uses-permission-sdk-23")
    val usesPermission23: List<PermissionV1> = emptyList(),
    val versionCode: Long? = null,
    val versionName: String,
    @SerialName("nativecode")
    val nativeCode: List<String>? = null,
    val features: List<String>? = null,
    val antiFeatures: List<String>? = null,
)

typealias PermissionV1 = Array<String?>

val PermissionV1.name: String get() = first()!!
val PermissionV1.maxSdk: Int? get() = getOrNull(1)?.toInt()
