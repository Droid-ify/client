package com.looker.core.database.model

import com.looker.core.model.newer.Package
import com.looker.core.model.newer.Permission
import com.looker.core.model.newer.toPackageName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
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
data class PackageEntity(
	val installed: Boolean,
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
	val usesPermission: List<PermissionEntity>,
	val versionCode: Long,
	val versionName: String,
	val nativeCode: List<String>,
	val features: List<String>,
	val antiFeatures: List<String>
)

@Serializable(with = PermissionEntitySerializer::class)
data class PermissionEntity(
	val name: String,
	val maxSdk: Int? = null
)

fun PackageEntity.toExternalModel(): Package = Package(
	installed = installed,
	added = added,
	apkName = apkName,
	hash = hash,
	hashType = hashType,
	minSdkVersion = minSdkVersion,
	maxSdkVersion = maxSdkVersion,
	targetSdkVersion = targetSdkVersion,
	packageName = packageName.toPackageName(),
	sig = sig,
	signer = signer,
	size = size,
	srcName = srcName,
	usesPermission = usesPermission.map(PermissionEntity::toExternalModel),
	versionCode = versionCode,
	versionName = versionName,
	nativeCode = nativeCode,
	features = features,
	antiFeatures = antiFeatures
)

fun PermissionEntity.toExternalModel(): Permission = Permission(name, maxSdk)

internal class PermissionEntitySerializer : KSerializer<PermissionEntity> {
	override val descriptor = buildClassSerialDescriptor("PermissionEntity") {
		element<String>("name")
		element<Int?>("maxSdk")
	}

	override fun deserialize(decoder: Decoder): PermissionEntity {
		val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
		val jsonArray = jsonInput.decodeJsonElement().jsonArray
		if (jsonArray.size != 2) throw IllegalArgumentException()
		val name = jsonArray[0].jsonPrimitive.content
		val maxSdk = jsonArray[1].jsonPrimitive.intOrNull
		return PermissionEntity(name, maxSdk)
	}

	@OptIn(ExperimentalSerializationApi::class)
	override fun serialize(encoder: Encoder, value: PermissionEntity) {
		encoder.encodeCollection(JsonArray.serializer().descriptor, 2) {
			encodeStringElement(descriptor, 0, value.name)
			encodeNullableSerializableElement(descriptor, 1, Int.serializer(), value.maxSdk)
		}
	}
}