package com.looker.sync.fdroid.v1

import com.looker.core.domain.Parser
import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.sync.fdroid.IndexJarValidator
import com.looker.sync.fdroid.IndexValidator
import com.looker.sync.fdroid.utils.toJarFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

class V1Parser(
    private val dispatcher: CoroutineDispatcher
) : Parser<String> {

    private val validator: IndexValidator = IndexJarValidator(dispatcher)

    override suspend fun parse(
        file: File,
        repo: Repo,
    ): Pair<Fingerprint, String> = withContext(dispatcher) {
        val jar = file.toJarFile()
        val entry = jar.getJarEntry("index-v1.json")
        val indexV1 = jar.getInputStream(entry).use {

        }
        val validatedFingerprint: Fingerprint = validator.validate(entry, repo.fingerprint)
        validatedFingerprint to "indexV1"
    }
}
