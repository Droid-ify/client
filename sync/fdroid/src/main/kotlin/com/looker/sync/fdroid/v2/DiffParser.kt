package com.looker.sync.fdroid.v2

import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.sync.fdroid.Parser
import com.looker.sync.fdroid.v2.model.IndexV2Diff
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

class DiffParser(
    private val dispatcher: CoroutineDispatcher,
    private val json: Json,
) : Parser<IndexV2Diff> {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun parse(file: File, repo: Repo): Pair<Fingerprint, IndexV2Diff> =
        withContext(dispatcher) {
            val indexV2 = file.inputStream().use {
                json.decodeFromStream(IndexV2Diff.serializer(), it)
            }
            requireNotNull(repo.fingerprint) {
                "Fingerprint should not be null when parsing diff"
            } to indexV2
        }
}
