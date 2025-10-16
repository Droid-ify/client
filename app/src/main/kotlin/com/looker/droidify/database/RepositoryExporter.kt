package com.looker.droidify.database

import android.content.Context
import android.net.Uri
import com.fasterxml.jackson.core.JsonToken
import com.looker.droidify.utility.common.Exporter
import com.looker.droidify.utility.common.extension.Json
import com.looker.droidify.utility.common.extension.forEach
import com.looker.droidify.utility.common.extension.forEachKey
import com.looker.droidify.utility.common.extension.parseDictionary
import com.looker.droidify.utility.common.extension.writeArray
import com.looker.droidify.utility.common.extension.writeDictionary
import com.looker.droidify.di.ApplicationScope
import com.looker.droidify.di.IoDispatcher
import com.looker.droidify.model.Repository
import com.looker.droidify.utility.serialization.repository
import com.looker.droidify.utility.serialization.serialize
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class RepositoryExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : Exporter<List<Repository>> {
    override suspend fun export(item: List<Repository>, target: Uri) {
        scope.launch(ioDispatcher) {
            val stream = context.contentResolver.openOutputStream(target, "wt")
            Json.factory.createGenerator(stream).use { generator ->
                generator.writeDictionary {
                    writeArray("repositories") {
                        item.map {
                            it.copy(
                                id = -1,
                                mirrors = if (it.enabled) it.mirrors else emptyList(),
                                lastModified = "",
                                entityTag = ""
                            )
                        }.forEach { repo ->
                            writeDictionary {
                                repo.serialize(this)
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun import(target: Uri): List<Repository> = withContext(ioDispatcher) {
        val list = mutableListOf<Repository>()
        val stream = context.contentResolver.openInputStream(target)
        Json.factory.createParser(stream).use { generator ->
            generator?.parseDictionary {
                forEachKey {
                    if (it.array("repositories")) {
                        forEach(JsonToken.START_OBJECT) {
                            val repo = repository()
                            list.add(repo)
                        }
                    }
                }
            }
        }
        list
    }
}
