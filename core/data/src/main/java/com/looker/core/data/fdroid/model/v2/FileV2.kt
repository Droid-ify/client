package com.looker.core.data.fdroid.model.v2

import com.looker.core.data.fdroid.IndexParser.json
import com.looker.core.data.fdroid.model.IndexFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString


@Serializable
data class FileV2(
	override val name: String,
	override val sha256: String? = null,
	override val size: Long? = null,
	@SerialName("ipfsCIDv1")
	override val ipfsCidV1: String? = null,
) : IndexFile {
	companion object {
		@JvmStatic
		fun deserialize(string: String?): FileV2? {
			if (string == null) return null
			return json.decodeFromString(string)
		}

		@JvmStatic
		fun fromPath(path: String): FileV2 = FileV2(path)
	}

	override suspend fun serialize(): String {
		return json.encodeToString(this)
	}
}

typealias LocalizedTextV2 = Map<String, String>
typealias LocalizedFileV2 = Map<String, FileV2>
typealias LocalizedFileListV2 = Map<String, List<FileV2>>

@Serializable
data class MirrorV2(
	val url: String,
	val location: String? = null,
)

@Serializable
data class AntiFeatureV2(
	val icon: LocalizedFileV2 = emptyMap(),
	val name: LocalizedTextV2,
	val description: LocalizedTextV2 = emptyMap(),
)

@Serializable
data class CategoryV2(
	val icon: LocalizedFileV2 = emptyMap(),
	val name: LocalizedTextV2,
	val description: LocalizedTextV2 = emptyMap(),
)

@Serializable
data class ReleaseChannelV2(
	val name: LocalizedTextV2,
	val description: LocalizedTextV2 = emptyMap(),
)
