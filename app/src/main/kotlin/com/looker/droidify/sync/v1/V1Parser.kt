package com.looker.droidify.sync.v1

import com.looker.droidify.domain.model.Fingerprint
import com.looker.droidify.domain.model.Repo
import com.looker.droidify.sync.IndexValidator
import com.looker.droidify.sync.Parser
import com.looker.droidify.sync.utils.toJarFile
import com.looker.droidify.sync.v1.model.IndexV1
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class V1Parser(
    private val dispatcher: CoroutineDispatcher,
    private val json: Json,
    private val validator: IndexValidator,
) : Parser<IndexV1> {

    override suspend fun parse(
        file: File,
        repo: Repo,
    ): Pair<Fingerprint, IndexV1> = withContext(dispatcher) {
        return@withContext file.toJarFile().use { jar ->
            val entry = jar.getJarEntry("index-v1.json")
            val indexString = jar.getInputStream(entry).use {
                it.readBytes().decodeToString()
            }
            validator.validate(entry, repo.fingerprint) to json.decodeFromString(indexString)
        }
    }
}
