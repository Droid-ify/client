package com.looker.droidify.sync.v2

import android.content.Context
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.data.model.Repo
import com.looker.droidify.network.Downloader
import com.looker.droidify.sync.Parser
import com.looker.droidify.sync.Syncable
import com.looker.droidify.sync.common.ENTRY_V2_NAME
import com.looker.droidify.sync.common.INDEX_V2_NAME
import com.looker.droidify.sync.common.IndexJarValidator
import com.looker.droidify.sync.common.JsonParser
import com.looker.droidify.sync.common.downloadIndex
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

class EntrySyncable(
    private val context: Context,
    private val downloader: Downloader,
    private val dispatcher: CoroutineDispatcher,
) : Syncable<Entry> {
    override val parser: Parser<Entry>
        get() = EntryParser(
            dispatcher = dispatcher,
            json = JsonParser,
            validator = IndexJarValidator(dispatcher),
        )

    private val indexParser: Parser<IndexV2> = V2Parser(
        dispatcher = dispatcher,
        json = JsonParser,
    )

    private val diffParser: Parser<IndexV2Diff> = DiffParser(
        dispatcher = dispatcher,
        json = JsonParser,
    )

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun sync(
        repo: Repo,
    ): Pair<Fingerprint, IndexV2?>? = withContext(dispatcher) {
        val jar = downloader.downloadIndex(
            context = context,
            repo = repo,
            url = repo.address.removeSuffix("/") + "/$ENTRY_V2_NAME",
            fileName = ENTRY_V2_NAME,
        )
        if (jar.length() == 0L) return@withContext null
        val (fingerprint, entry) = parser.parse(jar, repo)
        jar.delete()
        val index = entry.getDiff(repo.versionInfo.timestamp)
        // Already latest
            ?: return@withContext fingerprint to null
        val indexPath = repo.address.removeSuffix("/") + index.name
        val indexFile = Cache.getIndexFile(context, "repo_${repo.id}_$INDEX_V2_NAME")
        val indexV2 = if (index != entry.index && indexFile.exists()) {
            val diffFile = downloader.downloadIndex(
                context = context,
                repo = repo,
                url = indexPath,
                fileName = "diff_${repo.versionInfo.timestamp}.json",
                diff = true,
            )
            val diff = async { diffParser.parse(diffFile, repo).second }
            val oldIndex = async { indexParser.parse(indexFile, repo).second }
            diff.await().patchInto(oldIndex.await()) { index ->
                diffFile.delete()
                Json.encodeToStream(index, indexFile.outputStream())
            }
        } else {
            val newIndexFile = downloader.downloadIndex(
                context = context,
                repo = repo,
                url = indexPath,
                fileName = INDEX_V2_NAME,
            )
            indexParser.parse(newIndexFile, repo).second
        }
        fingerprint to indexV2
    }
}
