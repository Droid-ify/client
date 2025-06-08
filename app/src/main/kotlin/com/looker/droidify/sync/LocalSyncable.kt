package com.looker.droidify.sync

import android.content.Context
import com.looker.droidify.domain.model.Fingerprint
import com.looker.droidify.domain.model.Repo
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

    override suspend fun sync(repo: Repo): Pair<Fingerprint, IndexV2?> {
        val file = Cache.getTemporaryFile(context).apply {
            outputStream().use {
                it.write(context.assets.open("izzy_index_v2.json").readBytes())
            }
        }
        return parser.parse(file, repo)
    }
}
