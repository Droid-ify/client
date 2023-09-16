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

		private const val INDEX_V1_FILE_NAME = "index-v1.jar"
		private const val INDEX_V2_FILE_NAME = "index-v2.json"
		private const val ENTRY_FILE_NAME = "entry.jar"
	}

	override suspend fun downloadIndexV1(
		repo: Repo
	): IndexV1 = withContext(Dispatchers.Default) {
		val jarFile = downloadIndexFile(repo, INDEX_V1_FILE_NAME)
		val verifier = IndexV1Verifier(jarFile, null, repo.fingerprint)
		verifier.getStreamAndVerify(parser::parseV1).second
	}

	override suspend fun downloadIndexV2(
		repo: Repo
	): IndexV2 = withContext(Dispatchers.Default) {
		val file = downloadIndexFile(repo, INDEX_V2_FILE_NAME)
		file.parseIndexV2()
	}

	override suspend fun downloadIndexDiff(
		repo: Repo,
		name: String
	): IndexV2 = withContext(Dispatchers.Default) {
		val file = downloadIndexFile(repo, name)
		file.parseIndexV2()
	}

	override suspend fun downloadEntry(
		repo: Repo
	): Entry = withContext(Dispatchers.Default) {
		val jarFile = downloadIndexFile(repo, ENTRY_FILE_NAME)
		val verifier = EntryVerifier(jarFile, null, repo.fingerprint)
		verifier.getStreamAndVerify(parser::parseEntry).second
	}

	override suspend fun determineIndexType(repo: Repo): IndexType {
		val indexV2Exist = downloader.headCall(repo.indexUrl(ENTRY_FILE_NAME))
		return if (indexV2Exist is NetworkResponse.Success) IndexType.ENTRY
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