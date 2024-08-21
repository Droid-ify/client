package com.looker.sync.fdroid.v1

import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.sync.fdroid.IndexValidator
import com.looker.sync.fdroid.Parser
import com.looker.sync.fdroid.utils.toJarFile
import com.looker.sync.fdroid.v1.model.IndexV1
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

class V1Parser(
    private val dispatcher: CoroutineDispatcher,
    private val json: Json,
    private val validator: IndexValidator,
) : Parser<IndexV1> {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun parse(
        file: File,
        repo: Repo,
    ): Pair<Fingerprint, IndexV1> = withContext(dispatcher) {
        val jar = file.toJarFile()
        val entry = jar.getJarEntry("index-v1.json")
        val indexV1 = jar.getInputStream(entry).use {
            json.decodeFromStream(IndexV1.serializer(), it)
        }
        val validatedFingerprint: Fingerprint = validator.validate(entry, repo.fingerprint)
        validatedFingerprint to indexV1
    }
}
