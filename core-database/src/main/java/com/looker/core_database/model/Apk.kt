package com.looker.core_database.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Apk(
	@SerialName("apkName") val apkName: String,
	@SerialName("size") val size: Long,
	@SerialName("added") val added: Long,
	@SerialName("versionCode") val versionCode: Long,
	@SerialName("versionName") val versionName: String = "",
	@SerialName("hash") val hash: String = "",
	@SerialName("hashType") val hashType: String = "",
	@SerialName("sig") val signature: String = "",
	@SerialName("signer") val signer: String = "",
	@SerialName("srcname") val srcName: String = "",
	@SerialName("minSdkVersion") val minSdk: Int = -1,
	@SerialName("maxSdkVersion") val maxSdk: Int = -1,
	@SerialName("targetSdkVersion") val targetSdk: Int = -1,
	@SerialName("uses-permission") val permissions: LinkedHashSet<String> = linkedSetOf(),
	@SerialName("uses-permission-sdk-23") val emptyPermission: LinkedHashSet<String> = linkedSetOf(),
	@SerialName("features") val features: List<String> = emptyList(),
	@SerialName("nativecode") val platforms: List<String> = emptyList()
) {
	fun toJson() = Json.encodeToString(this)

	companion object {
		fun fromJson(builder: Json, json: String) = builder.decodeFromString<Apk>(json)
	}
}