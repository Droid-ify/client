package com.looker.droidify.service

import android.content.Context
import androidx.annotation.StringRes
import com.looker.droidify.R.string.connection_error_DESC
import com.looker.droidify.R.string.could_not_download_FORMAT
import com.looker.droidify.R.string.http_error_DESC
import com.looker.droidify.R.string.io_error_DESC
import com.looker.droidify.R.string.socket_error_DESC
import com.looker.droidify.R.string.unknown_error_DESC
import com.looker.droidify.network.Downloader
import com.looker.droidify.network.NetworkResponse
import com.looker.droidify.network.percentBy
import com.looker.droidify.utility.common.cache.Cache
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

private const val MAX_PARALLEL_DOWNLOADS = 1

class DownloadManager(
    private val context: Context,
    private val downloader: Downloader,
) {

    sealed interface Status {
        data object Queue : Status
        data object Downloading : Status
        sealed interface Finished : Status {
            data object Success : Finished
            data class Error(@get:StringRes val titleResId: Int, val message: Int?) : Finished
        }
    }

    // percent 0..100
    val progress = ConcurrentHashMap<String, Int>()
    val tasks = ConcurrentHashMap<String, Status>()

    private val semaphore = Semaphore(MAX_PARALLEL_DOWNLOADS)

    suspend fun enqueue(
        key: String,
        url: String,
        fileName: String,
        authentication: String,
    ): Boolean {
        if (Cache.getReleaseFile(context, fileName).exists()) return false
        tasks[key] = Status.Queue

        semaphore.withPermit {
            download(key, url, fileName, authentication)
        }
        return true
    }

    fun cancel(key: String) {
        tasks.remove(key)
    }

    fun cancelAll() {
        tasks.keys.forEach(::cancel)
    }

    private suspend fun download(
        key: String,
        url: String,
        fileName: String,
        authentication: String,
    ) {
        tasks[key] = Status.Downloading
        val target = Cache.getPartialReleaseFile(context, fileName)

        val response = downloader.downloadToFile(
            url = url,
            target = target,
            headers = { if (authentication.isNotEmpty()) authentication(authentication) },
        ) { read, total ->
            progress[key] = read.value percentBy total?.value
        }
        progress.remove(key)
        when (response) {
            is NetworkResponse.Success -> {
                val releaseFile = Cache.getReleaseFile(context, fileName)
                target.renameTo(releaseFile)
                tasks[key] = Status.Finished.Success
            }

            is NetworkResponse.Error -> {
                val descRes = when (response) {
                    is NetworkResponse.Error.ConnectionTimeout -> connection_error_DESC
                    is NetworkResponse.Error.Http -> http_error_DESC
                    is NetworkResponse.Error.IO -> io_error_DESC
                    is NetworkResponse.Error.SocketTimeout -> socket_error_DESC
                    is NetworkResponse.Error.Unknown -> unknown_error_DESC
                }
                tasks[key] = Status.Finished.Error(could_not_download_FORMAT, descRes)
            }
        }
    }
}
