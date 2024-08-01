package com.looker.sync.fdroid.common

import com.looker.core.domain.model.Repo
import com.looker.network.Downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

suspend fun Downloader.downloadIndex(
    repo: Repo,
    fileName: String,
    url: String,
): File = withContext(Dispatchers.IO) {
    val tempFile = File.createTempFile(repo.name, fileName)
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
            if (repo.versionInfo.timestamp > 0L) {
                ifModifiedSince(Date(repo.versionInfo.timestamp))
            }
        }
    )
    tempFile
}
