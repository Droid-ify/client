package com.looker.sync.fdroid.v2

import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.sync.fdroid.IndexValidator
import com.looker.sync.fdroid.Parser
import com.looker.sync.fdroid.utils.toJarFile
import com.looker.sync.fdroid.v2.model.Entry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class EntryParser(
    private val dispatcher: CoroutineDispatcher,
    private val json: Json,
    private val validator: IndexValidator,
) : Parser<Entry> {

    override suspend fun parse(
        file: File,
        repo: Repo,
    ): Pair<Fingerprint, Entry> = withContext(dispatcher) {
        val jar = file.toJarFile()
        val entry = jar.getJarEntry("entry.json")
        val entryString = jar.getInputStream(entry).use {
            it.readBytes().decodeToString()
        }
        validator.validate(entry, repo.fingerprint) to json.decodeFromString(entryString)
    }
}
