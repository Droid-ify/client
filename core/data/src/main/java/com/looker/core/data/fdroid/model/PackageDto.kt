package com.looker.core.data.fdroid.model

import com.looker.core.database.model.PackageEntity
import com.looker.core.database.model.PermissionEntity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeCollection
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class PackageDto(
	val added: Long = 0L,
	val apkName: String = "",
	val hash: String = "",
	val hashType: String = "",
	val minSdkVersion: Int = -1,
	val maxSdkVersion: Int = -1,
	val targetSdkVersion: Int = -1,
	val packageName: String = "",
	val sig: String = "",
	val signer: String = "",
	val size: Long = 0L,
	@SerialName("srcname")
	val srcName: String = "",
	@SerialName("uses-permission")
	val usesPermission: List<PermissionDto> = emptyList(),
	@SerialName("uses-permission-sdk-23")
	val usesPermission23: List<PermissionDto> = emptyList(),
	val versionCode: Long = 0L,
	val versionName: String = "",
	@SerialName("nativecode")
	val nativeCode: List<String> = emptyList(),
	val features: List<String> = emptyList(),
	val antiFeatures: List<String> = emptyList()
)

@Serializable(with = PermissionDtoSerializer::class)
data class PermissionDto(
	val name: String = "",
	val maxSdk: Int? = null
)

internal fun PackageDto.toEntity(installed: Boolean = false): PackageEntity = PackageEntity(
	installed = installed,
	added = added,
	apkName = apkName,
	hash = hash,
	hashType = hashType,
	minSdkVersion = minSdkVersion,
	maxSdkVersion = maxSdkVersion,
	targetSdkVersion = targetSdkVersion,
	packageName = packageName,
	sig = sig,
	signer = signer,
	size = size,
	srcName = srcName,
	usesPermission = (usesPermission + usesPermission23).map(PermissionDto::toEntity),
	versionCode = versionCode,
	versionName = versionName,
	nativeCode = nativeCode,
	features = features,
	antiFeatures = antiFeatures
)

internal fun PermissionDto.toEntity(): PermissionEntity = PermissionEntity(name, maxSdk)

internal class PermissionDtoSerializer : KSerializer<PermissionDto> {
	override val descriptor = buildClassSerialDescriptor("PermissionDto") {
		element<String>("name")
		element<Int?>("maxSdk")
	}

	override fun deserialize(decoder: Decoder): PermissionDto {
		val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
		val jsonArray = jsonInput.decodeJsonElement().jsonArray
		if (jsonArray.size != 2) throw IllegalArgumentException()
		val name = jsonArray[0].jsonPrimitive.content
		val maxSdk = jsonArray[1].jsonPrimitive.intOrNull
		return PermissionDto(name, maxSdk)
	}

	@OptIn(ExperimentalSerializationApi::class)
	override fun serialize(encoder: Encoder, value: PermissionDto) {
		encoder.encodeCollection(JsonArray.serializer().descriptor, 2) {
			encodeStringElement(descriptor, 0, value.name)
			encodeNullableSerializableElement(descriptor, 1, Int.serializer(), value.maxSdk)
		}
	}
}