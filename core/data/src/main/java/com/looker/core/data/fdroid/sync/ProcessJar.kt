package com.looker.core.data.fdroid.sync

import com.looker.core.data.fdroid.model.IndexV1
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlin.coroutines.resume

suspend fun JarFile.getIndexV1(): IndexV1 = suspendCancellableCoroutine { cont ->
	val indexEntry = getEntry(IndexType.INDEX_V1.contentName) as JarEntry
	val indexFile = IndexV1.decodeFromInputStream(getInputStream(indexEntry))
	cont.resume(indexFile)
}

internal enum class IndexType(val jarName: String, val contentName: String) {
	INDEX_V1("index-v1.jar", "index-v1.json")
}