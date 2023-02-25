package com.looker.core.data.fdroid.sync

import android.content.Context
import com.looker.core.common.cache.Cache
import com.looker.core.model.newer.Repo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.ifModifiedSince
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.*
import java.util.jar.JarFile

private val client = HttpClient(OkHttp) { expectSuccess = true }

private const val WORKERS = 3

internal fun CoroutineScope.processRepos(
	context: Context,
	repos: ReceiveChannel<Repo>,
	onDownload: suspend (Repo, JarFile) -> Unit
) = launch {
	val locations = Channel<RepoLocation>()
	val contents = Channel<RepoLocJar>(1)
	repeat(WORKERS) { worker(locations, contents) }
	downloader(context, repos, locations, contents, onDownload)
}

private fun CoroutineScope.downloader(
	context: Context,
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
				val repoLocation = repo.toLocation(context)
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
		val content = downloadIndexJar(it)
		contents.send(content)
	}
}

internal suspend fun downloadIndexJar(repoLocation: RepoLocation): RepoLocJar = coroutineScope {
	val shouldAuthenticate =
		repoLocation.username.isNotEmpty() && repoLocation.password.isNotEmpty()
	val request = HttpRequestBuilder().apply {
		url(repoLocation.url)
		ifModifiedSince(Date(repoLocation.timestamp))
		if (shouldAuthenticate) basicAuth(repoLocation.username, repoLocation.password)
	}
	val response = try {
		client.get(request)
	} catch (e: Exception) {
		throw RepoSyncFailedException(e.message.toString())
	}
	val tempFile = Cache.getTemporaryFile(repoLocation.context)
	val result = response.body<ByteArray>()
	tempFile.writeBytes(result)
	RepoLocJar(repoLocation, JarFile(tempFile, true))
}

data class RepoLocation(
	val url: String,
	val context: Context,
	val timestamp: Long,
	val username: String,
	val password: String
)

fun Repo.toLocation(context: Context) = RepoLocation(
	url = "$address/index-v1.jar",
	context = context,
	timestamp = timestamp,
	username = username,
	password = password
)

data class RepoLocJar(
	val repo: RepoLocation,
	val jar: JarFile
)