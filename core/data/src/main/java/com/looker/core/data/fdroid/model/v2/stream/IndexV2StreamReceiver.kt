package com.looker.core.data.fdroid.model.v2.stream

import com.looker.core.data.fdroid.model.v2.PackageV2
import com.looker.core.data.fdroid.model.v2.RepoV2

interface IndexV2StreamReceiver {

	/**
	 * Receives the [RepoV2] from the index stream.
	 * Attention: This might get called after receiving packages.
	 */
	fun receive(repo: RepoV2, version: Long, certificate: String)

	/**
	 * Receives one [PackageV2] from the index stream.
	 * This is called once for each package in the index.
	 */
	fun receive(packageName: String, p: PackageV2)

	/**
	 * Called when the stream has been processed to its end.
	 */
	fun onStreamEnded()

}
