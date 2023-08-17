package com.looker.core.data.fdroid.sync

import com.looker.core.model.newer.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fdroid.index.IndexConverter
import org.fdroid.index.v2.IndexV2

class IndexManager(
	private val indexDownloader: IndexDownloader,
	private val converter: IndexConverter
) {

	suspend fun getIndex(repos: List<Repo>): Map<Repo, IndexV2> =
		withContext(Dispatchers.Default) {
			repos.associateWith { repo ->
				when (indexDownloader.determineIndexType(repo)) {
					IndexType.INDEX_V1 -> {
						val indexV1 = indexDownloader.downloadIndexV1(repo)
						converter.toIndexV2(indexV1)
					}

					IndexType.ENTRY -> {
						val entry = indexDownloader.downloadEntry(repo)
						val diff = entry.getDiff(repo.versionInfo.timestamp)
						if (diff == null) indexDownloader.downloadIndexV2(repo)
						else indexDownloader.downloadIndexDiff(repo, diff.name)
					}
				}
			}
		}
}