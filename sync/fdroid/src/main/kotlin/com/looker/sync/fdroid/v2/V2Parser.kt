package com.looker.sync.fdroid.v2

import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.sync.fdroid.Parser
import com.looker.sync.fdroid.v2.model.IndexV2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

class V2Parser(
    private val dispatcher: CoroutineDispatcher,
    private val json: Json,
) : Parser<IndexV2> {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun parse(
        file: File,
        repo: Repo,
    ): Pair<Fingerprint, IndexV2> = withContext(dispatcher) {
        val indexV2 = file.inputStream().use {
            json.decodeFromStream(IndexV2.serializer(), it)
        }
        requireNotNull(repo.fingerprint) {
            "Fingerprint should not be null if index v2 is being fetched"
        } to indexV2
    }
}
