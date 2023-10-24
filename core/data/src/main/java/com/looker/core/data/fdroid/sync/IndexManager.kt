package com.looker.core.data.fdroid.sync

import com.looker.core.model.newer.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fdroid.index.IndexConverter
import org.fdroid.index.v2.EntryFileV2
import org.fdroid.index.v2.IndexV2

class IndexManager(
    private val indexDownloader: IndexDownloader,
    private val converter: IndexConverter
) {

    suspend fun getIndex(
        repos: List<Repo>
    ): Map<Repo, IndexV2?> = withContext(Dispatchers.Default) {
        repos.associate { repo ->
            when (indexDownloader.determineIndexType(repo)) {
                IndexType.INDEX_V1 -> {
                    val response = indexDownloader.downloadIndexV1(repo)
                    repo.update(
                        fingerprint = response.fingerprint,
                        timestamp = response.lastModified,
                        etag = response.etag
                    ) to converter.toIndexV2(response.index)
                }

                IndexType.ENTRY -> {
                    val response = indexDownloader.downloadEntry(repo)
                    val updatedRepo = repo.update(
                        fingerprint = response.fingerprint,
                        timestamp = response.lastModified,
                        etag = response.etag
                    )
                    if (response.lastModified == repo.versionInfo.timestamp) {
                        return@associate updatedRepo to null
                    }
                    val diff = response.index.getDiff(repo.versionInfo.timestamp)
                    updatedRepo to downloadIndexBasedOnDiff(repo, diff)
                }
            }
        }
    }

    private suspend fun downloadIndexBasedOnDiff(repo: Repo, diff: EntryFileV2?): IndexV2 =
        if (diff == null) {
            indexDownloader.downloadIndexV2(repo)
        } else {
            indexDownloader.downloadIndexDiff(repo, diff.name)
        }
}
