package com.looker.core.data.fdroid.model.v2

import com.looker.core.data.fdroid.IndexParser.json
import com.looker.core.data.fdroid.model.IndexFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class Entry(
	val timestamp: Long,
	val version: Long,
	val maxAge: Int? = null,
	val index: EntryFileV2,
	val diffs: Map<String, EntryFileV2> = emptyMap(),
) {
	/**
	 * @return the diff for the given [timestamp] or null if none exists
	 * in which case the full [index] should be used.
	 */
	fun getDiff(timestamp: Long): EntryFileV2? {
		return diffs[timestamp.toString()]
	}
}


@Serializable
data class EntryFileV2(
	override val name: String,
	override val sha256: String,
	override val size: Long,
	@SerialName("ipfsCIDv1")
	override val ipfsCidV1: String? = null,
	val numPackages: Int,
) : IndexFile {
	companion object {
		fun deserialize(string: String): EntryFileV2 {
			return json.decodeFromString(string)
		}
	}

	override suspend fun serialize(): String {
		return json.encodeToString(this)
	}
}

