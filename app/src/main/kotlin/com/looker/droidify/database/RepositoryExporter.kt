package com.looker.droidify.database

import android.content.Context
import android.net.Uri
import com.looker.core.common.Exporter
import com.looker.core.common.extension.Json
import com.looker.core.common.extension.parseDictionary
import com.looker.core.common.extension.writeDictionary
import com.looker.core.di.ApplicationScope
import com.looker.core.di.IoDispatcher
import com.looker.core.model.Repository
import com.looker.droidify.utility.serialization.repository
import com.looker.droidify.utility.serialization.serialize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryExporter @Inject constructor (
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : Exporter<List<Repository>> {
    override suspend fun saveToFile(item: List<Repository>, target: Uri) {
        scope.launch(ioDispatcher) {
            val stream = context.contentResolver.openOutputStream(target)
            Json.factory.createGenerator(stream).use { generator ->
                item.forEach {
                    generator.writeDictionary(it::serialize)
                }
            }
        }
    }

    override suspend fun readFromFile(target: Uri): List<Repository> = withContext(ioDispatcher) {
        val list = mutableListOf<Repository>()
        val stream = context.contentResolver.openInputStream(target)
        Json.factory.createParser(stream).use {
            val repo = it.parseDictionary {
                repository()
            }
            list.add(repo)
        }
        list
    }
}
