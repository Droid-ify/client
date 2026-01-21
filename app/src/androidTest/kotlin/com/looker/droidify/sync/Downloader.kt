package com.looker.droidify.sync

import android.content.Context
import com.looker.droidify.data.model.Repo
import com.looker.droidify.network.ProgressListener
import com.looker.droidify.sync.common.assets
import com.looker.droidify.utility.common.cache.Cache
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/**
 * Creates a fake OkHttpClient for testing that intercepts and provides test assets.
 */
val FakeDownloader: OkHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val url = chain.request().url.toString()
        throw UnsupportedOperationException("FakeDownloader: Use fakeDownloadToFile for URL: $url")
    }
    .build()

/**
 * Test helper extension function that downloads from test assets.
 * Simulates the downloadIndex function for tests.
 */
suspend fun OkHttpClient.downloadIndex(
    context: Context,
    repo: Repo,
    fileName: String,
    url: String,
    diff: Boolean = false,
    onProgress: ProgressListener? = null,
): File = withContext(Dispatchers.IO) {
    val indexFile = Cache.getIndexFile(context, "repo_${repo.id}_$fileName")

    if (url.endsWith("fail")) {
        error("You asked for it")
    }

    val index = when {
        url.endsWith("fdroid-index-v1.jar") -> assets("fdroid_index_v1.jar")
        url.endsWith("fdroid-index-v1.json") -> assets("fdroid_index_v1.json")
        url.endsWith("fdroid-index-v2.json") -> assets("fdroid_index_v2.json")
        url.endsWith("index-v1.jar") -> assets("izzy_index_v1.jar")
        url.endsWith("index-v2.json") -> assets("izzy_index_v2.json")
        url.endsWith("index-v2-updated.json") -> assets("izzy_index_v2_updated.json")
        url.endsWith("entry.jar") -> assets("izzy_entry.jar")
        url.endsWith("/diff/1725731263000.json") -> assets("izzy_diff.json")
        // Just in case we try these in future
        url.endsWith("index-v1.json") -> assets("izzy_index_v1.json")
        url.endsWith("entry.json") -> assets("izzy_entry.json")
        else -> error("Unknown URL: $url")
    }
    index.writeTo(indexFile)
    indexFile
}

private suspend infix fun InputStream.writeTo(file: File) = withContext(Dispatchers.IO) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytesRead = read(buffer)
    file.outputStream().use { output ->
        while (bytesRead != -1) {
            ensureActive()
            output.write(buffer, 0, bytesRead)
            bytesRead = read(buffer)
        }
        output.flush()
    }
}
