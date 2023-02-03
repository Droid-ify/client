package com.looker.core.data.fdroid.sync

import java.util.jar.JarFile

data class RepoSyncResult(
	val jarFile: JarFile,
	val mirrors: List<String>,
	val name: String,
	val description: String,
	val version: Int,
	val timestamp: Long
)

class RepoSyncFailedException(override val message: String) : Exception(message)