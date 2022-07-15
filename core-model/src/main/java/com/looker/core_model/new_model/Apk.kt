package com.looker.core_model.new_model

data class Apk(
	val packageName: String,
	val size: Long,
	val apkName: String,
	val added: Long,
	val versionCode: Long,
	val versionName: String,
	val hash: String,
	val hashType: String,
	val signature: String,
	val signer: String,
	val srcName: String,
	val minSdk: Int,
	val maxSdk: Int,
	val targetSdk: String,
	val permissions: List<String>
)