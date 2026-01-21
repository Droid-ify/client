package com.looker.droidify.sync.v2

import android.content.Context
import com.looker.droidify.data.model.Repo
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
import com.looker.droidify.sync.v2.model.IndexV2Diff
import com.looker.droidify.utility.common.cache.Cache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import okhttp3.OkHttpClient

class EntrySyncable(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val dispatcher: CoroutineDispatcher,
) : Syncable<Entry> {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun sync(
        repo: Repo,
        block: (SyncState) -> Unit,
    ) = withContext(dispatcher) {
        try {
            val jar = httpClient.downloadIndex(
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
                        IllegalStateException("Empty entry v2 jar")
                    )
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
                val diffFile = httpClient.downloadIndex(
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
                val diff = async {
                    JsonParser.decodeFromString<IndexV2Diff>(diffFile.readBytes().decodeToString())
                }
                val oldIndex = async {
                    JsonParser.decodeFromString<IndexV2>(indexFile.readBytes().decodeToString())
                }
                try {
                    diff.await().patchInto(oldIndex.await()) { index ->
                        diffFile.delete()
                        Json.encodeToStream(index, indexFile.outputStream())
                    }
                } catch (t: Throwable) {
                    block(SyncState.JsonParsing.Failure(repo.id, t))
                    return@withContext
                }
            } else {
                val newIndexFile = httpClient.downloadIndex(
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
