package com.looker.sync.fdroid.v2

import android.content.Context
import com.looker.core.common.cache.Cache
import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.network.Downloader
import com.looker.sync.fdroid.Parser
import com.looker.sync.fdroid.Syncable
import com.looker.sync.fdroid.common.ENTRY_V2_NAME
import com.looker.sync.fdroid.common.INDEX_V2_NAME
import com.looker.sync.fdroid.common.IndexJarValidator
import com.looker.sync.fdroid.common.JsonParser
import com.looker.sync.fdroid.common.downloadIndex
import com.looker.sync.fdroid.v2.model.Entry
import com.looker.sync.fdroid.v2.model.IndexV2
import com.looker.sync.fdroid.v2.model.IndexV2Diff
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
            json = JsonParser.parser,
            validator = IndexJarValidator(dispatcher),
        )

    private val indexParser: Parser<IndexV2> = V2Parser(
        dispatcher = dispatcher,
        json = JsonParser.parser,
    )

    private val diffParser: Parser<IndexV2Diff> = DiffParser(
        dispatcher = dispatcher,
        json = JsonParser.parser,
    )

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun sync(repo: Repo): Pair<Fingerprint, IndexV2?> =
        withContext(dispatcher) {
            // example https://apt.izzysoft.de/fdroid/repo/entry.json
            val jar = downloader.downloadIndex(
                context = context,
                repo = repo,
                url = repo.address.removeSuffix("/") + "/$ENTRY_V2_NAME",
                fileName = ENTRY_V2_NAME
            )
            val (fingerprint, entry) = parser.parse(jar, repo)
            jar.delete()
            val index = entry.getDiff(repo.versionInfo.timestamp)
            // Already latest
                ?: return@withContext fingerprint to null
            val indexPath = repo.address.removeSuffix("/") + index.name
            val indexFile = Cache.getIndexFile(context, "repo_${repo.id}_$INDEX_V2_NAME")
            val indexV2 = if (index != entry.index && indexFile.exists()) {
                // example https://apt.izzysoft.de/fdroid/repo/diff/1725372028000.json
                val diffFile = downloader.downloadIndex(
                    context = context,
                    repo = repo,
                    url = indexPath,
                    fileName = "diff_${repo.versionInfo.timestamp}.json",
                    diff = true,
                )
                // TODO: Maybe parse in parallel
                diffParser.parse(diffFile, repo).second.let {
                    diffFile.delete()
                    it.patchInto(indexParser.parse(indexFile, repo).second) { index ->
                        Json.encodeToStream(index, indexFile.outputStream())
                    }
                }
            } else {
                // example https://apt.izzysoft.de/fdroid/repo/index-v2.json
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
