package com.looker.core.data.fdroid.sync

import com.looker.core.model.newer.Repo
import com.looker.network.Downloader
import com.looker.network.NetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fdroid.index.*
import org.fdroid.index.v1.IndexV1
import org.fdroid.index.v1.IndexV1Verifier
import org.fdroid.index.v2.Entry
import org.fdroid.index.v2.EntryVerifier
import org.fdroid.index.v2.IndexV2
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class IndexDownloaderImpl @Inject constructor(
	private val downloader: Downloader
) : IndexDownloader {

	companion object {
		private val parser = IndexParser

		private fun File.parseIndexV2(): IndexV2 = parser.parseV2(inputStream())
	}

	override suspend fun downloadIndexV1(
		repo: Repo
	): IndexV1 = withContext(Dispatchers.Default) {
		val jarFile = downloadIndexFile(repo, "index-v1.jar")
		val verifier = IndexV1Verifier(jarFile, null, repo.fingerprint)
		val index = verifier.getStreamAndVerify(parser::parseV1).second
		index
	}

	override suspend fun downloadIndexV2(
		repo: Repo
	): IndexV2 = withContext(Dispatchers.Default) {
		val file = downloadIndexFile(repo, "index-v2.json")
		file.parseIndexV2()
	}

	override suspend fun downloadIndexDiff(
		repo: Repo,
		name: String
	): IndexV2 = withContext(Dispatchers.Default) {
		val file = downloadIndexFile(repo, name)
		(file).parseIndexV2()
	}

	override suspend fun downloadEntry(
		repo: Repo
	): Entry = withContext(Dispatchers.Default) {
		val jarFile = downloadIndexFile(repo, "entry.jar")
		val verifier = EntryVerifier(jarFile, null, repo.fingerprint)
		val entry = verifier.getStreamAndVerify(parser::parseEntry).second
		entry
	}

	override suspend fun determineIndexType(repo: Repo): IndexType {
		val indexV2Exist = downloader.headCall(repo.indexUrl("entry.json"))
		return if (indexV2Exist == NetworkResponse.Success) IndexType.ENTRY
		else IndexType.INDEX_V1
	}

	private suspend fun downloadIndexFile(
		repo: Repo,
		indexParameter: String
	): File = withContext(Dispatchers.IO) {
			val tempFile = File.createTempFile(repo.name, UUID.randomUUID().toString())
			downloader.downloadToFile(
				url = repo.indexUrl(indexParameter),
				target = tempFile,
				headers = {
					if (repo.shouldAuthenticate) authentication(
						repo.authentication.username,
						repo.authentication.password
					)
					ifModifiedSince(Date(repo.versionInfo.timestamp))
				}
			)
			tempFile
		}
}