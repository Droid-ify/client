package com.looker.core.data.fdroid.sync

import com.looker.core.model.newer.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.jar.JarFile

interface SyncProcessor {

	fun CoroutineScope.processRepos(repos: ReceiveChannel<Repo>, onDownload: onDownloadListener): Job

	suspend fun determineIndexType(repo: Repo): IndexType

	suspend fun downloadIndexJar(repo: Repo, indexType: IndexType): RepoLocJar
}

typealias onDownloadListener = suspend (Repo, JarFile) -> Unit

data class RepoLocation(
	val url: String,
	val name: String,
	val timestamp: Long,
	val username: String,
	val password: String,
	val repo: Repo
)
fun Repo.toLocation() = RepoLocation(
	url = address,
	name = name,
	timestamp = versionInfo.timestamp,
	username = authentication.username,
	password = authentication.password,
	repo = this
)

fun RepoLocation.indexUrl(indexType: IndexType): String =
	if (url.endsWith('/')) url + indexType.jarName
	else "$url/${indexType.jarName}"

data class RepoLocJar(
	val repo: RepoLocation,
	val jar: JarFile
)