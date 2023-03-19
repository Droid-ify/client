package com.looker.core.data.fdroid.model.v2.stream

import kotlinx.serialization.json.JsonObject

interface IndexV2DiffStreamReceiver {

	/**
	 * Receives the diff for the [RepoV2] from the index stream.
	 */
	fun receiveRepoDiff(version: Long, repoJsonObject: JsonObject)

	/**
	 * Receives one diff for a [MetadataV2] from the index stream.
	 * This is called once for each package in the index diff.
	 *
	 * If the given [packageJsonObject] is null, the package should be removed.
	 */
	fun receivePackageMetadataDiff(packageName: String, packageJsonObject: JsonObject?)

	/**
	 * Receives the diff for all versions of the give n [packageName]
	 * as a map of versions IDs to the diff [JsonObject].
	 * This is called once for each package in the index diff (if versions have changed).
	 *
	 * If an entry in the given [versionsDiffMap] is null,
	 * the version with that ID should be removed.
	 */
	fun receiveVersionsDiff(packageName: String, versionsDiffMap: Map<String, JsonObject?>?)

	/**
	 * Called when the stream has been processed to its end.
	 */
	fun onStreamEnded()

}
