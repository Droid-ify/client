package com.looker.sync.fdroid.v2

import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.sync.fdroid.Parser
import com.looker.sync.fdroid.v2.model.IndexV2
import java.io.File

class DiffParser: Parser<IndexV2> {
    override suspend fun parse(file: File, repo: Repo): Pair<Fingerprint, IndexV2> {
        TODO("Not yet implemented")
    }
}
