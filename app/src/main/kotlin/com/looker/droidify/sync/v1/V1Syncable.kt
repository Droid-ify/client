package com.looker.droidify.sync.v1

import android.content.Context
import com.looker.droidify.data.model.Repo
import com.looker.droidify.network.Downloader
import com.looker.droidify.network.percentBy
import com.looker.droidify.sync.SyncState
import com.looker.droidify.sync.Syncable
import com.looker.droidify.sync.common.INDEX_V1_NAME
import com.looker.droidify.sync.common.downloadIndex
import com.looker.droidify.sync.common.toV2
import com.looker.droidify.sync.parseJson
import com.looker.droidify.sync.utils.toJarFile
import com.looker.droidify.sync.v1.model.IndexV1
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class V1Syncable(
    private val context: Context,
    private val downloader: Downloader,
    private val dispatcher: CoroutineDispatcher,
) : Syncable<IndexV1> {
    override suspend fun sync(repo: Repo, block: (SyncState) -> Unit) = withContext(dispatcher) {
        try {
            val jar = downloader.downloadIndex(
                context = context,
                repo = repo,
                url = repo.address.removeSuffix("/") + "/$INDEX_V1_NAME",
                fileName = INDEX_V1_NAME,
                onProgress = { bytes, total ->
                    val percent = (bytes percentBy total)
                    block(SyncState.IndexDownload.Progress(repo.id, percent))
                },
            )
            if (jar.length() == 0L) {
                block(
                    SyncState.IndexDownload.Failure(
                        repo.id,
                        IllegalStateException("Empty v1 index jar")
                    )
                )
                return@withContext
            } else {
                block(SyncState.IndexDownload.Success(repo.id))
            }
            val (fingerprint, indexV1) = try {
                jar.toJarFile().parseJson<IndexV1>(repo.fingerprint)
            } catch (t: Exception) {
                block(SyncState.JarParsing.Failure(repo.id, t))
                return@withContext
            } finally {
                jar.delete()
            }
            block(SyncState.JarParsing.Success(repo.id, fingerprint))
            val indexV2 = try {
                indexV1.toV2()
            } catch (t: Throwable) {
                block(SyncState.JsonParsing.Failure(repo.id, t))
                return@withContext
            }
            block(SyncState.JsonParsing.Success(repo.id, fingerprint, indexV2))
        } catch (t: Throwable) {
            block(SyncState.IndexDownload.Failure(repo.id, t))
        }
    }
}
