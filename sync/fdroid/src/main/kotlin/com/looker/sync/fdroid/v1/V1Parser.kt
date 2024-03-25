package com.looker.sync.fdroid.v1

import com.looker.core.domain.Parser
import org.fdroid.index.v1.IndexV1
import java.io.File

class V1Parser : Parser<IndexV1> {
    override suspend fun parse(downloadedFile: File): IndexV1 {
        TODO("Not yet implemented")
    }
}
