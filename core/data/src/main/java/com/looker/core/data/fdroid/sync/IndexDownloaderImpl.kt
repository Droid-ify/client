package com.looker.core.data.fdroid.sync

import com.looker.core.data.downloader.Downloader
import com.looker.core.data.downloader.NetworkResponse
import com.looker.core.model.newer.Repo
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import java.io.File
import java.util.Date
import java.util.UUID
import java.util.jar.JarFile
import kotlin.collections.set

class IndexDownloaderImpl(private val downloader: Downloader) : IndexDownloader {

	companion object {
		private const val WORKERS = 3
	}

	override fun CoroutineScope.processRepos(
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
			val indexType = determineIndexType(it.repo)
			val content = downloadIndexJar(it.repo, indexType)
			contents.send(content)
		}
	}

	override suspend fun determineIndexType(
		repo: Repo
	): IndexType = withContext(Dispatchers.IO) {
		val isIndexV1 =
			downloader.headCall(repo.toLocation().indexUrl(IndexType.INDEX_V2)) ==
					NetworkResponse.Error(404)
		if (isIndexV1) IndexType.INDEX_V1 else IndexType.INDEX_V2
	}

	override suspend fun downloadIndexJar(
		repo: Repo,
		indexType: IndexType
	): RepoLocJar = withContext(Dispatchers.IO) {
		val repoLocation = repo.toLocation()
		val shouldAuthenticate =
			repoLocation.username.isNotEmpty() && repoLocation.password.isNotEmpty()
		val tempFile = File.createTempFile(repoLocation.name, UUID.randomUUID().toString())
		downloader.downloadToFile(
			url = repoLocation.indexUrl(indexType),
			target = tempFile,
			headers = {
				if (shouldAuthenticate) authentication(repoLocation.username, repoLocation.password)
				ifModifiedSince(Date(repoLocation.timestamp))
			}
		)
		RepoLocJar(repoLocation, JarFile(tempFile, true))
	}
}