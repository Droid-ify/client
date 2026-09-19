package com.looker.droidify.sync.common

import android.content.Context
import com.looker.droidify.data.model.Repo
import com.looker.droidify.network.Downloader
import com.looker.droidify.network.ProgressListener
import com.looker.droidify.network.header.authentication
import com.looker.droidify.network.header.ifModifiedSince
import com.looker.droidify.utility.common.cache.Cache
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun Downloader.downloadIndex(
    context: Context,
    repo: Repo,
    fileName: String,
    url: String,
    diff: Boolean = false,
    onProgress: ProgressListener? = null,
): File = withContext(Dispatchers.IO) {
    val indexFile = Cache.getIndexFile(context, "repo_${repo.id}_$fileName")
    downloadToFile(
        url = url,
        target = indexFile,
        block = onProgress,
        headers = {
            authentication(repo.authentication)
            if (!diff) ifModifiedSince(repo.versionInfo)
        },
    )
    indexFile
}

const val INDEX_V1_NAME = "index-v1.jar"
const val ENTRY_V2_NAME = "entry.jar"
const val INDEX_V2_NAME = "index-v2.json"
