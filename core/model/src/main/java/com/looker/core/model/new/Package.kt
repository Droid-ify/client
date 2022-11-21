package com.looker.core.model.new

data class Package(
	val added: Long,
	val apkName: String,
	val hash: String,
	val hashType: String,
	val minSdkVersion: Int,
	val maxSdkVersion: Int,
	val targetSdkVersion: Int,
	val packageName: String,
	val sig: String,
	val signer: String,
	val size: Long,
	val srcName: String,
	val usesPermission: List<Permission> = emptyList(),
	val versionCode: Long,
	val versionName: String,
	val nativeCode: List<String>,
	val features: List<String>,
	val antiFeatures: List<String>,
)

data class Permission(
	val name: String,
	val maxSdk: Int? = null
)