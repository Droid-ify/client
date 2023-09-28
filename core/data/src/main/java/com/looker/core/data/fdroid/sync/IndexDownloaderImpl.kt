package com.looker.core.data.fdroid.sync

import com.looker.core.common.signature.FileValidator
import com.looker.core.data.fdroid.sync.signature.EntryValidator
import com.looker.core.data.fdroid.sync.signature.IndexValidator
import com.looker.core.model.newer.Repo
import com.looker.network.Downloader
import com.looker.network.NetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fdroid.index.*
import org.fdroid.index.v1.IndexV1
import org.fdroid.index.v2.Entry
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

		private fun File.parseIndexV1(): IndexV1 = parser.parseV1(inputStream())
		private fun File.parseIndexV2(): IndexV2 = parser.parseV2(inputStream())
		private fun File.parseEntry(): Entry = parser.parseEntry(inputStream())

		private const val INDEX_V1_FILE_NAME = "index-v1.jar"
		private const val INDEX_V2_FILE_NAME = "index-v2.json"
		private const val ENTRY_FILE_NAME = "entry.jar"
	}

	override suspend fun downloadIndexV1(
		repo: Repo
	): Pair<String, IndexV1> = withContext(Dispatchers.IO) {
		var fingerprint = ""
		val validator = IndexValidator(repo) {
			fingerprint = it
		}
		val jarFile = downloadIndexFile(repo, INDEX_V1_FILE_NAME, validator)
		fingerprint to jarFile.parseIndexV1()
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
	): Pair<String, Entry> = withContext(Dispatchers.IO) {
		var fingerprint = ""
		val validator = EntryValidator(repo) {
			fingerprint = it
		}
		val jarFile = downloadIndexFile(repo, ENTRY_FILE_NAME, validator)
		fingerprint to jarFile.parseEntry()
	}

	override suspend fun determineIndexType(repo: Repo): IndexType {
		val isIndexV2 = downloader.headCall(repo.indexUrl(ENTRY_FILE_NAME))
		return if (isIndexV2 is NetworkResponse.Success) IndexType.ENTRY
		else IndexType.INDEX_V1
	}

	private suspend fun downloadIndexFile(
		repo: Repo,
		indexParameter: String,
		validator: FileValidator? = null
	): File = withContext(Dispatchers.IO) {
		val tempFile = File.createTempFile(repo.name, UUID.randomUUID().toString())
		downloader.downloadToFile(
			url = repo.indexUrl(indexParameter),
			target = tempFile,
			validator = validator,
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