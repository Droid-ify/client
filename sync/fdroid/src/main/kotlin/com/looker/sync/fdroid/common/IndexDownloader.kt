package com.looker.sync.fdroid.common

import android.content.Context
import com.looker.core.common.cache.Cache
import com.looker.core.domain.model.Repo
import com.looker.network.Downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

suspend fun Downloader.downloadIndex(
    context: Context,
    repo: Repo,
    fileName: String,
    url: String,
    diff: Boolean = false,
): File = withContext(Dispatchers.IO) {
    val tempFile = Cache.getIndexFile(context, "repo_${repo.id}_$fileName")
    downloadToFile(
        url = url,
        target = tempFile,
        headers = {
            if (repo.shouldAuthenticate) {
                authentication(
                    repo.authentication.username,
                    repo.authentication.password
                )
            }
            if (repo.versionInfo.timestamp > 0L && !diff) {
                ifModifiedSince(Date(repo.versionInfo.timestamp))
            }
        }
    )
    tempFile
}

const val INDEX_V1_NAME = "index-v1.jar"
const val ENTRY_V2_NAME = "entry.jar"
const val INDEX_V2_NAME = "index-v2.json"
