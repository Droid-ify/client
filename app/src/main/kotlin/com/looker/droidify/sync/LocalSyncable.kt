package com.looker.droidify.sync

import android.content.Context
import com.looker.droidify.data.model.Repo
import com.looker.droidify.sync.common.JsonParser
import com.looker.droidify.sync.v2.V2Parser
import com.looker.droidify.sync.v2.model.IndexV2
import com.looker.droidify.utility.common.cache.Cache
import kotlinx.coroutines.Dispatchers

class LocalSyncable(
    private val context: Context,
) : Syncable<IndexV2> {
    override val parser: Parser<IndexV2>
        get() = V2Parser(Dispatchers.IO, JsonParser)

    override suspend fun sync(repo: Repo, block: (SyncState) -> Unit) {
        try {
            val file = Cache.getTemporaryFile(context).apply {
                outputStream().use {
                    block(SyncState.IndexDownload.Success(repo.id))
                    if (repo.id == 5) {
                        it.write(context.assets.open("izzy_index_v2.json").readBytes())
                    } else {
                        it.write(context.assets.open("fdroid_index_v2.json").readBytes())
                    }
                }
            }
            val (fingerprint, index) = parser.parse(file, repo)
            block(SyncState.JsonParsing.Success(repo.id, fingerprint, index))
        } catch (t: Throwable) {
            block(SyncState.JsonParsing.Failure(repo.id, t))
        }
    }
}
