package com.looker.core.data.fdroid.sync

import com.looker.core.common.signature.FileValidator
import com.looker.core.data.fdroid.sync.signature.EntryValidator
import com.looker.core.data.fdroid.sync.signature.IndexValidator
import com.looker.core.model.newer.Repo
import com.looker.network.Downloader
import com.looker.network.NetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fdroid.index.IndexParser
import org.fdroid.index.parseV2
import org.fdroid.index.v1.IndexV1
import org.fdroid.index.v2.Entry
import org.fdroid.index.v2.IndexV2
import java.io.File
import java.io.InputStream
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class IndexDownloaderImpl @Inject constructor(
    private val downloader: Downloader
) : IndexDownloader {

    companion object {
        private val parser = IndexParser

        private fun InputStream.parseIndexV2(): IndexV2 = parser.parseV2(this)

        private const val INDEX_V1_FILE_NAME = "index-v1.jar"
        private const val INDEX_V2_FILE_NAME = "index-v2.json"
        private const val ENTRY_FILE_NAME = "entry.jar"
    }

    override suspend fun downloadIndexV1(
        repo: Repo
    ): IndexDownloadResponse<IndexV1> = withContext(Dispatchers.IO) {
        var repoFingerprint: String? = null
        var fileIndex: IndexV1? = null
        val validator = IndexValidator(repo) { index, fingerprint ->
            repoFingerprint = fingerprint
            fileIndex = index
        }
        val (_, response) = downloadIndexFile(repo, INDEX_V1_FILE_NAME, validator)
        if (repoFingerprint == null || fileIndex == null || repoFingerprint?.isBlank() == true || response is NetworkResponse.Error)
            throw IllegalStateException("Fingerprint: $repoFingerprint, Index: $fileIndex")
        IndexDownloadResponse(
            index = fileIndex!!,
            fingerprint = repoFingerprint!!,
            lastModified = fileIndex?.repo?.timestamp,
            etag = (response as NetworkResponse.Success).etag
        )
    }

    override suspend fun downloadIndexV2(
        repo: Repo
    ): IndexV2 = withContext(Dispatchers.Default) {
        val (file, _) = downloadIndexFile(repo, INDEX_V2_FILE_NAME)
        file.inputStream().parseIndexV2()
    }

    override suspend fun downloadIndexDiff(
        repo: Repo,
        name: String
    ): IndexV2 = withContext(Dispatchers.Default) {
        val (file, _) = downloadIndexFile(repo, name)
        file.inputStream().parseIndexV2()
    }

    override suspend fun downloadEntry(
        repo: Repo
    ): IndexDownloadResponse<Entry> = withContext(Dispatchers.IO) {
        var repoFingerprint: String? = null
        var fileEntry: Entry? = null
        val validator = EntryValidator(repo) { entry, fingerprint ->
            repoFingerprint = fingerprint
            fileEntry = entry
        }
        val (_, response) = downloadIndexFile(repo, ENTRY_FILE_NAME, validator)
        if (repoFingerprint == null || fileEntry == null || repoFingerprint?.isBlank() == true || response is NetworkResponse.Error.Validation)
            throw IllegalStateException("Empty Fingerprint")
        IndexDownloadResponse(
            index = fileEntry!!,
            fingerprint = repoFingerprint!!,
            lastModified = fileEntry?.timestamp,
            etag = (response as NetworkResponse.Success).etag
        )
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
    ): Pair<File, NetworkResponse> = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile(repo.name, UUID.randomUUID().toString())
        val response = downloader.downloadToFile(
            url = repo.indexUrl(indexParameter),
            target = tempFile,
            validator = validator,
            headers = {
                if (repo.shouldAuthenticate) authentication(
                    repo.authentication.username,
                    repo.authentication.password
                )
                if (repo.versionInfo.timestamp > 0L) ifModifiedSince(Date(repo.versionInfo.timestamp))
            }
        )
        tempFile to response
    }
}
