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
import com.looker.droidify.sync.toJarScope
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
            with(jar.toJarScope<IndexV1>()) {
                when {
                    fingerprint == null -> block(
                        SyncState.JarParsing.Failure(
                            repo.id,
                            IllegalStateException("Jar entry does not contain a fingerprint")
                        )
                    )

                    repo.fingerprint != null && !repo.fingerprint.assert(fingerprint!!) -> block(
                        SyncState.JarParsing.Failure(
                            repo.id,
                            IllegalStateException("Expected fingerprint: ${repo.fingerprint}, Actual fingerprint: $fingerprint")
                        )
                    )

                    else -> block(
                        SyncState.JsonParsing.Success(
                            repo.id,
                            fingerprint!!,
                            json().toV2()
                        )
                    )
                }
            }
            jar.delete()
        } catch (t: Throwable) {
            block(SyncState.IndexDownload.Failure(repo.id, t))
        }
    }
}
