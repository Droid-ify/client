package com.looker.sync.fdroid.v1.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
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
    val antiFeatures: List<String>? = null,

)

@Serializable(PermissionV1Serializer::class)
data class PermissionV1(
    val name: String,
    val maxSdk: Int? = null,
)

internal class PermissionV1Serializer : KSerializer<PermissionV1> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PermissionV1") {
            element<String>("name")
            element<Int?>("maxSdk")
        }

    override fun deserialize(decoder: Decoder): PermissionV1 {
        decoder as? JsonDecoder ?: error("Not a JSON")
        val array: JsonArray = decoder.decodeJsonElement().jsonArray
        require(array.size == 2) { "Permission array is invalid: $array" }
        require(array[0].jsonPrimitive.isString) { "Name is not the first element in permission: $array" }
        val name: String = array[0].jsonPrimitive.content
        val maxSdk: Int? = array[1].jsonPrimitive.intOrNull
        return PermissionV1(name, maxSdk)
    }

    override fun serialize(encoder: Encoder, permission: PermissionV1) {
        encoder.encodeCollection(JsonArray.serializer().descriptor, 2) {
            encodeStringElement(descriptor, 0, permission.name)
            encodeNullableSerializableElement(descriptor, 1, Int.serializer(), permission.maxSdk)
        }
    }
}
