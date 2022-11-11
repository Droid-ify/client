package com.looker.index.model.v1

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
	val antiFeatures: List<String>? = null
)


@Serializable(with = PermissionV1Serializer::class)
data class PermissionV1(
	val name: String,
	val maxSdk: Int? = null,
)

internal class PermissionV1Serializer : KSerializer<PermissionV1> {
	override val descriptor = buildClassSerialDescriptor("PermissionV1") {
		element<String>("name")
		element<Int?>("maxSdk")
	}

	override fun deserialize(decoder: Decoder): PermissionV1 {
		val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
		val jsonArray = jsonInput.decodeJsonElement().jsonArray
		if (jsonArray.size != 2) throw IllegalArgumentException()
		val name = jsonArray[0].jsonPrimitive.content
		val maxSdk = jsonArray[1].jsonPrimitive.intOrNull
		return PermissionV1(name, maxSdk)
	}

	@OptIn(ExperimentalSerializationApi::class)
	override fun serialize(encoder: Encoder, value: PermissionV1) {
		encoder.encodeCollection(JsonArray.serializer().descriptor, 2) {
			encodeStringElement(descriptor, 0, value.name)
			encodeNullableSerializableElement(descriptor, 1, Int.serializer(), value.maxSdk)
		}
	}
}