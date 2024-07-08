package com.looker.sync.fdroid.v1

import com.looker.core.common.extension.toJarFile
import com.looker.core.domain.Parser
import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.sync.fdroid.IndexValidator
import com.looker.sync.fdroid.IndexJarValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.fdroid.index.IndexParser
import org.fdroid.index.parseV1
import org.fdroid.index.v1.IndexV1
import java.io.File

class V1Parser(
    private val dispatcher: CoroutineDispatcher
) : Parser<IndexV1> {

    private val validator: IndexValidator = IndexJarValidator(dispatcher)

    override suspend fun parse(
        file: File,
        repo: Repo,
    ): Pair<Fingerprint, IndexV1> = withContext(dispatcher) {
        val jar = file.toJarFile()
        val entry = jar.getJarEntry("index-v1.json")
        val indexV1 = jar.getInputStream(entry).use(IndexParser::parseV1)
        validator.validate(entry, repo.fingerprint) to indexV1
    }
}
