package com.looker.sync.fdroid.v1

import android.content.Context
import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.network.Downloader
import com.looker.sync.fdroid.Parser
import com.looker.sync.fdroid.Syncable
import com.looker.sync.fdroid.common.INDEX_V1_NAME
import com.looker.sync.fdroid.common.IndexJarValidator
import com.looker.sync.fdroid.common.JsonParser
import com.looker.sync.fdroid.common.downloadIndex
import com.looker.sync.fdroid.common.toV2
import com.looker.sync.fdroid.v1.model.IndexV1
import com.looker.sync.fdroid.v2.model.IndexV2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class V1Syncable(
    private val context: Context,
    private val downloader: Downloader,
    private val dispatcher: CoroutineDispatcher,
) : Syncable<IndexV1> {
    override val parser: Parser<IndexV1>
        get() = V1Parser(
            dispatcher = dispatcher,
            json = JsonParser.parser,
            validator = IndexJarValidator(dispatcher),
        )

    override suspend fun sync(repo: Repo): Pair<Fingerprint, IndexV2> =
        withContext(dispatcher) {
            val jar = downloader.downloadIndex(
                context = context,
                repo = repo,
                url = repo.address.removeSuffix("/") + "/$INDEX_V1_NAME",
                fileName = INDEX_V1_NAME,
            )
            val (fingerprint, indexV1) = parser.parse(jar, repo)
            jar.delete()
            fingerprint to indexV1.toV2()
        }
}
