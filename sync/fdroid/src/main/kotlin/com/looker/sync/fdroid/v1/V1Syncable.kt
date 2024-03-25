package com.looker.sync.fdroid.v1

import com.looker.core.domain.Parser
import com.looker.core.domain.Syncable
import com.looker.core.domain.model.App
import com.looker.core.domain.model.Repo
import com.looker.network.Downloader
import org.fdroid.index.v1.IndexV1

class V1Syncable(override val downloader: Downloader) : Syncable<IndexV1> {
    override val parser: Parser<IndexV1>
        get() = V1Parser()

    override suspend fun sync(): Pair<Repo, List<App>> {
        TODO("Not yet implemented")
    }

}
