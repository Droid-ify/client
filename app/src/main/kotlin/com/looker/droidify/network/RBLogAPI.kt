package com.looker.droidify.network

import android.util.Log
import com.looker.droidify.data.local.model.RBData
import com.looker.droidify.data.local.model.RBLogEntity
import com.looker.droidify.data.local.model.RBLogs
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class RBLogAPI {
    private val onionProxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 9050))
    var proxy: Proxy? = null
        set(value) {
            if (field != value) {
                field = value
            }
        }

    private fun getProxy(onion: Boolean) = if (onion) onionProxy else proxy

    val client = HttpClient(OkHttp) {
        engine {
            config {
                // TODO eventually change timeouts
                connectTimeout(30L, TimeUnit.SECONDS)
                readTimeout(30L, TimeUnit.SECONDS)
                writeTimeout(30L, TimeUnit.SECONDS)
                proxy(getProxy(false))
                retryOnConnectionFailure(true)
            }
        }
    }

    suspend fun getIndex(): Map<String, List<RBData>> {
        val request = HttpRequestBuilder().apply {
            // TODO add setting for RBLog provider
            url("https://codeberg.org/IzzyOnDroid/rbtlog/raw/branch/izzy/log/index.json")
            headers {
                // TODO append(HttpHeaders.IfModifiedSince,)
            }
        }

        val result = client.get(request)
        if (!result.status.isSuccess())
            Log.w(this::javaClass.name, "getIndex() failed: ${result.status}")

        return when {
            result.status.isSuccess() -> {
                // TODO save result.headers["Last-Modified"].orEmpty()
                RBLogs.fromJson(result.bodyAsText())
            }

            else                      -> emptyMap()
        }
    }
}

fun RBData.toLog(hash: String): RBLogEntity = RBLogEntity(
    hash = hash,
    repository = repository,
    apk_url = apk_url,
    appid = appid,
    version_code = version_code,
    version_name = version_name,
    tag = tag,
    commit = commit,
    timestamp = timestamp,
    reproducible = reproducible,
    error = error
)

fun Map<String, List<RBData>>.toLogs(): List<RBLogEntity> {
    return this.flatMap { (hash, data) ->
        data.map { it.toLog(hash) }
    }
}
