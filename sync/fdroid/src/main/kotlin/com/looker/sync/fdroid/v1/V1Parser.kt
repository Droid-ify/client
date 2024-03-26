package com.looker.sync.fdroid.v1

import com.looker.core.domain.Parser
import com.looker.core.domain.model.Fingerprint
import org.fdroid.index.v1.IndexV1
import java.io.File

class V1Parser : Parser<Pair<Fingerprint, IndexV1>> {
    override suspend fun parse(downloadedFile: File): Pair<Fingerprint, IndexV1> {
        TODO("Not yet implemented")
    }
}
