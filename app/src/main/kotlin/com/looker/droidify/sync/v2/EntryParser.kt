package com.looker.droidify.sync.v2

import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.data.model.Repo
import com.looker.droidify.sync.IndexValidator
import com.looker.droidify.sync.Parser
import com.looker.droidify.sync.utils.toJarFile
import com.looker.droidify.sync.v2.model.Entry
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

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
