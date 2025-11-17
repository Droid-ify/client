package com.looker.droidify.sync.v2

import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.data.model.Repo
import com.looker.droidify.sync.Parser
import com.looker.droidify.sync.v2.model.IndexV2Diff
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class DiffParser(
    private val dispatcher: CoroutineDispatcher,
    private val json: Json,
) : Parser<IndexV2Diff> {

    override suspend fun parse(
        file: File,
        repo: Repo
    ): Pair<Fingerprint, IndexV2Diff> = withContext(dispatcher) {
        requireNotNull(repo.fingerprint) {
            "Fingerprint should not be null when parsing diff"
        } to json.decodeFromString(file.readBytes().decodeToString())
    }
}
