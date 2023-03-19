package com.looker.core.data.fdroid.model

interface IndexFile {
	val name: String
	val sha256: String?
	val size: Long?
	val ipfsCidV1: String?

	suspend fun serialize(): String
}
