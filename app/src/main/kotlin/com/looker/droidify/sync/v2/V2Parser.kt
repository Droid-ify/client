package com.looker.droidify.sync.v2

import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.droidify.sync.Parser
import com.looker.droidify.sync.v2.model.IndexV2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class V2Parser(
    private val dispatcher: CoroutineDispatcher,
    private val json: Json,
) : Parser<IndexV2> {

    override suspend fun parse(
        file: File,
        repo: Repo,
    ): Pair<Fingerprint, IndexV2> = withContext(dispatcher) {
        requireNotNull(repo.fingerprint) {
            "Fingerprint should not be null if index v2 is being fetched"
        } to json.decodeFromString(file.readBytes().decodeToString())
    }
}
