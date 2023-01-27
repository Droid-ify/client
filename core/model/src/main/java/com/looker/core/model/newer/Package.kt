package com.looker.core.model.newer

data class Package(
	val installed: Boolean,
	val added: Long,
	val apkName: String,
	val hash: String,
	val hashType: String,
	val minSdkVersion: Int,
	val maxSdkVersion: Int,
	val targetSdkVersion: Int,
	val packageName: PackageName,
	val sig: String,
	val signer: String,
	val size: Long,
	val srcName: String,
	val usesPermission: List<Permission>,
	val versionCode: Long,
	val versionName: String,
	val nativeCode: List<String>,
	val features: List<String>,
	val antiFeatures: List<String>
)

data class InstalledPackage(
	val added: Long,
	val targetSdkVersion: Int,
	val packageName: PackageName,
	val versionCode: Long,
	val versionName: String,
	val size: Long
)

fun Package.toInstalled(): InstalledPackage? = if (installed) InstalledPackage(
	added = added,
	targetSdkVersion = targetSdkVersion,
	packageName = packageName,
	versionCode = versionCode,
	versionName = versionName,
	size = size
) else null

data class Permission(
	val name: String,
	val maxSdk: Int? = null
)