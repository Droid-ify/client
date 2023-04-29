package com.looker.core.data.fdroid.sync

import com.looker.core.model.newer.Repo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import io.ktor.http.ifModifiedSince
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import java.util.jar.JarFile

private val client = HttpClient(OkHttp)

private const val WORKERS = 3

internal fun CoroutineScope.processRepos(
	repos: ReceiveChannel<Repo>,
	onDownload: suspend (Repo, JarFile) -> Unit
) = launch {
	val locations = Channel<RepoLocation>()
	val contents = Channel<RepoLocJar>(1)
	repeat(WORKERS) { worker(locations, contents) }
	downloader(repos, locations, contents, onDownload)
}

private fun CoroutineScope.downloader(
	repos: ReceiveChannel<Repo>,
	locations: SendChannel<RepoLocation>,
	contents: ReceiveChannel<RepoLocJar>,
	onDownload: suspend (Repo, JarFile) -> Unit
) = launch {
	val requested = mutableMapOf<RepoLocation, MutableList<Repo>>()
	while (true) {
		select<Unit> {
			contents.onReceive { (location, jar) ->
				val repoMutableList = requested.remove(location)!!
				repoMutableList.forEach { onDownload(it, jar) }
			}
			repos.onReceive { repo ->
				val repoLocation = repo.toLocation()
				val repoList = requested[repoLocation]
				if (repoList == null) {
					requested[repoLocation] = mutableListOf(repo)
					locations.send(repoLocation)
				} else {
					repoList.add(repo)
				}
			}
		}
	}
}

private fun CoroutineScope.worker(
	items: ReceiveChannel<RepoLocation>,
	contents: SendChannel<RepoLocJar>
) = launch {
	items.consumeEach {
		val indexType = determineIndexType(it)
		val content = downloadIndexJar(it, indexType)
		contents.send(content)
	}
}

internal suspend fun determineIndexType(
	repoLocation: RepoLocation
): IndexType = withContext(Dispatchers.IO) {
	val isIndexV1 = client.head(
		repoLocation.indexUrl(IndexType.INDEX_V2)
	).status == HttpStatusCode.NotFound
	if (isIndexV1) IndexType.INDEX_V1 else IndexType.INDEX_V2
}

internal suspend fun downloadIndexJar(
	repoLocation: RepoLocation,
	indexType: IndexType
): RepoLocJar = withContext(Dispatchers.IO) {
	val shouldAuthenticate =
		repoLocation.username.isNotEmpty() && repoLocation.password.isNotEmpty()
	val request = HttpRequestBuilder().apply {
		url(repoLocation.indexUrl(indexType))
		ifModifiedSince(Date(repoLocation.timestamp))
		if (shouldAuthenticate) basicAuth(repoLocation.username, repoLocation.password)
	}
	val response = client.get(request)
	val tempFile = File.createTempFile(repoLocation.name, UUID.randomUUID().toString())
	val result = response.body<ByteArray>()
	tempFile.writeBytes(result)
	RepoLocJar(repoLocation, JarFile(tempFile, true))
}

data class RepoLocation(
	val url: String,
	val name: String,
	val timestamp: Long,
	val username: String,
	val password: String
)

fun Repo.toLocation() = RepoLocation(
	url = address,
	name = name,
	timestamp = versionInfo.timestamp,
	username = authentication.username,
	password = authentication.password
)

fun RepoLocation.indexUrl(indexType: IndexType): String =
	if (url.endsWith('/')) url + indexType.jarName
	else "$url/${indexType.jarName}"

data class RepoLocJar(
	val repo: RepoLocation,
	val jar: JarFile
)