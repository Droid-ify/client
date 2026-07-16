package com.looker.droidify.sync.v2

import android.content.Context
import android.util.Log
import com.looker.droidify.data.model.Repo
import com.looker.droidify.network.Downloader
import com.looker.droidify.network.percentBy
import com.looker.droidify.network.validation.invalid
import com.looker.droidify.sync.JsonParser
import com.looker.droidify.sync.SyncState
import com.looker.droidify.sync.Syncable
import com.looker.droidify.sync.common.ENTRY_V2_NAME
import com.looker.droidify.sync.common.INDEX_V2_NAME
import com.looker.droidify.sync.common.downloadIndex
import com.looker.droidify.sync.toJarScope
import com.looker.droidify.sync.v2.model.Entry
import com.looker.droidify.sync.v2.model.IndexV2
import com.looker.droidify.sync.v2.model.IndexV2Merger
import com.looker.droidify.utility.common.cache.Cache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi

class EntrySyncable(
    private val context: Context,
    private val downloader: Downloader,
    private val dispatcher: CoroutineDispatcher,
) : Syncable<Entry> {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun sync(
        repo: Repo,
        block: (SyncState) -> Unit,
    ) = withContext(dispatcher) {
        try {
            val jar = downloader.downloadIndex(
                context = context,
                repo = repo,
                url = repo.address.removeSuffix("/") + "/$ENTRY_V2_NAME",
                fileName = ENTRY_V2_NAME,
                onProgress = { bytes, total ->
                    val percent = (bytes percentBy total)
                    block(SyncState.IndexDownload.Progress(repo.id, percent))
                },
            )
            if (jar.length() == 0L) {
                block(
                    SyncState.IndexDownload.Failure(
                        repo.id,
                        IllegalStateException("Empty entry v2 jar"),
                    ),
                )
                return@withContext
            } else {
                block(SyncState.IndexDownload.Success(repo.id))
            }
            val (fingerprint, entry) = try {
                with(jar.toJarScope<Entry>()) {
                    val output = json()
                    val jarFingerprint = fingerprint
                        ?: invalid("Jar entry does not contain a fingerprint")

                    if (repo.fingerprint != null && !repo.fingerprint.assert(jarFingerprint)) {
                        invalid("Expected fingerprint: ${repo.fingerprint}, Actual fingerprint: $jarFingerprint")
                    }

                    (repo.fingerprint ?: jarFingerprint) to output
                }
            } catch (t: Throwable) {
                block(SyncState.JarParsing.Failure(repo.id, t))
                return@withContext
            } finally {
                jar.delete()
            }
            block(SyncState.JarParsing.Success(repo.id, fingerprint))
            val diffRef = entry.getDiff(repo.versionInfo?.timestamp)
            if (diffRef == null) {
                block(SyncState.JsonParsing.Success(repo.id, fingerprint, null))
                return@withContext
            }
            val indexPath = repo.address.removeSuffix("/") + diffRef.name
            val indexFile = Cache.getIndexFile(context, "repo_${repo.id}_$INDEX_V2_NAME")
            val indexV2 = if (diffRef != entry.index && indexFile.exists()) {
                val diffFile = downloader.downloadIndex(
                    context = context,
                    repo = repo,
                    url = indexPath,
                    fileName = "diff_${repo.versionInfo?.timestamp}.json",
                    diff = true,
                    onProgress = { bytes, total ->
                        val percent = (bytes percentBy total)
                        block(SyncState.IndexDownload.Progress(repo.id, percent))
                    },
                )
                try {
                    indexFile
                        .takeIf { it.exists() && it.length() > 0 }
                        ?.let { indexFile ->
                            IndexV2Merger(indexFile).use { merger ->
                                merger.processDiff(
                                    diffFile.inputStream(),
                                ).let {
                                    Log.d(
                                        "EntrySyncable",
                                        "merged diff file $diffFile, success = $it, indexFile = $indexFile.",
                                    )
                                }
                            }
                            JsonParser.decodeFromString<IndexV2>(
                                indexFile.readBytes().decodeToString(),
                            )
                        }
                } catch (t: Throwable) {
                    block(SyncState.JsonParsing.Failure(repo.id, t))
                    return@withContext
                }
            } else {
                val newIndexFile = downloader.downloadIndex(
                    context = context,
                    repo = repo,
                    url = indexPath,
                    fileName = INDEX_V2_NAME,
                    onProgress = { bytes, total ->
                        val percent = (bytes percentBy total)
                        block(SyncState.IndexDownload.Progress(repo.id, percent))
                    },
                )
                try {
                    JsonParser.decodeFromString<IndexV2>(newIndexFile.readBytes().decodeToString())
                } catch (t: Throwable) {
                    block(SyncState.JsonParsing.Failure(repo.id, t))
                    return@withContext
                }
            }
            block(SyncState.JsonParsing.Success(repo.id, fingerprint, indexV2))
        } catch (t: Throwable) {
            block(SyncState.IndexDownload.Failure(repo.id, t))
        }
    }
}
