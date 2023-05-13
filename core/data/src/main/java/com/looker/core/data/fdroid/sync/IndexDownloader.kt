package com.looker.core.data.fdroid.sync

import com.looker.core.database.model.RepoEntity
import com.looker.core.model.newer.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.jar.JarFile

interface IndexDownloader {

	fun CoroutineScope.processRepos(repos: ReceiveChannel<RepoEntity>, onDownload: onDownloadListener): Job

	suspend fun downloadIndexJar(repo: RepoEntity): RepoLocJar

}

internal typealias onDownloadListener = suspend (RepoEntity, JarFile) -> Unit

data class RepoLocation(
	val url: String,
	val name: String,
	val timestamp: Long,
	val username: String,
	val password: String,
	val repo: RepoEntity
)

fun RepoEntity.toLocation() = RepoLocation(
	url = address,
	name = name["en-US"]!!,
	timestamp = timestamp,
	username = username,
	password = password,
	repo = this
)

fun RepoLocation.indexUrl(indexType: IndexType): String =
	if (url.endsWith('/')) url + indexType.jarName
	else "$url/${indexType.jarName}"

data class RepoLocJar(
	val repo: RepoLocation,
	val jar: JarFile
)