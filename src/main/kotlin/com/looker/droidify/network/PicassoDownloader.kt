package com.looker.droidify.network

import android.content.Context
import android.net.Uri
import android.view.View
import com.looker.droidify.entity.Product
import com.looker.droidify.entity.Repository
import com.looker.droidify.utility.extension.text.nullIfEmpty
import okhttp3.Cache
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

object PicassoDownloader {
    private const val HOST_ICON = "icon"
    private const val HOST_SCREENSHOT = "screenshot"
    private const val QUERY_ADDRESS = "address"
    private const val QUERY_AUTHENTICATION = "authentication"
    private const val QUERY_PACKAGE_NAME = "packageName"
    private const val QUERY_ICON = "icon"
    private const val QUERY_METADATA_ICON = "metadataIcon"
    private const val QUERY_LOCALE = "locale"
    private const val QUERY_DEVICE = "device"
    private const val QUERY_SCREENSHOT = "screenshot"
    private const val QUERY_DPI = "dpi"

    private val supportedDpis = listOf(120, 160, 240, 320, 480, 640)

    class Factory(cacheDir: File) : Call.Factory {
        private val cache = Cache(cacheDir, 50_000_000L)

        override fun newCall(request: okhttp3.Request): Call {
            return when (request.url.host) {
                HOST_ICON -> {
                    val address = request.url.queryParameter(QUERY_ADDRESS)?.nullIfEmpty()
                    val authentication = request.url.queryParameter(QUERY_AUTHENTICATION)
                    val path = run {
                        val packageName =
                            request.url.queryParameter(QUERY_PACKAGE_NAME)?.nullIfEmpty()
                        val icon = request.url.queryParameter(QUERY_ICON)?.nullIfEmpty()
                        val metadataIcon =
                            request.url.queryParameter(QUERY_METADATA_ICON)?.nullIfEmpty()
                        val dpi = request.url.queryParameter(QUERY_DPI)?.nullIfEmpty()
                        when {
                            icon != null -> "${if (dpi != null) "icons-$dpi" else "icons"}/$icon"
                            packageName != null && metadataIcon != null -> "$packageName/$metadataIcon"
                            else -> null
                        }
                    }
                    if (address == null || path == null) {
                        Downloader.createCall(request.newBuilder(), "", null)
                    } else {
                        Downloader.createCall(
                            request.newBuilder().url(
                                address.toHttpUrl()
                                    .newBuilder().addPathSegments(path).build()
                            ), authentication.orEmpty(), cache
                        )
                    }
                }
                HOST_SCREENSHOT -> {
                    val address = request.url.queryParameter(QUERY_ADDRESS)
                    val authentication = request.url.queryParameter(QUERY_AUTHENTICATION)
                    val packageName = request.url.queryParameter(QUERY_PACKAGE_NAME)
                    val locale = request.url.queryParameter(QUERY_LOCALE)
                    val device = request.url.queryParameter(QUERY_DEVICE)
                    val screenshot = request.url.queryParameter(QUERY_SCREENSHOT)
                    if (screenshot.isNullOrEmpty() || address.isNullOrEmpty()) {
                        Downloader.createCall(request.newBuilder(), "", null)
                    } else {
                        Downloader.createCall(
                            request.newBuilder().url(
                                address.toHttpUrl()
                                    .newBuilder().addPathSegment(packageName.orEmpty())
                                    .addPathSegment(locale.orEmpty())
                                    .addPathSegment(device.orEmpty())
                                    .addPathSegment(screenshot.orEmpty()).build()
                            ),
                            authentication.orEmpty(), cache
                        )
                    }
                }
                else -> {
                    Downloader.createCall(request.newBuilder(), "", null)
                }
            }
        }
    }

    fun createScreenshotUri(
        repository: Repository,
        packageName: String,
        screenshot: Product.Screenshot
    ): Uri {
        return Uri.Builder().scheme("https").authority(HOST_SCREENSHOT)
            .appendQueryParameter(QUERY_ADDRESS, repository.address)
            .appendQueryParameter(QUERY_AUTHENTICATION, repository.authentication)
            .appendQueryParameter(QUERY_PACKAGE_NAME, packageName)
            .appendQueryParameter(QUERY_LOCALE, screenshot.locale)
            .appendQueryParameter(
                QUERY_DEVICE, when (screenshot.type) {
                    Product.Screenshot.Type.PHONE -> "phoneScreenshots"
                    Product.Screenshot.Type.SMALL_TABLET -> "sevenInchScreenshots"
                    Product.Screenshot.Type.LARGE_TABLET -> "tenInchScreenshots"
                }
            )
            .appendQueryParameter(QUERY_SCREENSHOT, screenshot.path)
            .build()
    }

    fun createIconUri(
        view: View,
        packageName: String,
        icon: String,
        metadataIcon: String,
        repository: Repository
    ): Uri {
        val size = (view.layoutParams.let { min(it.width, it.height) } /
                view.resources.displayMetrics.density).roundToInt()
        return createIconUri(view.context, packageName, icon, metadataIcon, size, repository)
    }

    private fun createIconUri(
        context: Context, packageName: String, icon: String, metadataIcon: String,
        targetSizeDp: Int, repository: Repository
    ): Uri {
        return Uri.Builder().scheme("https").authority(HOST_ICON)
            .appendQueryParameter(QUERY_ADDRESS, repository.address)
            .appendQueryParameter(QUERY_AUTHENTICATION, repository.authentication)
            .appendQueryParameter(QUERY_PACKAGE_NAME, packageName)
            .appendQueryParameter(QUERY_ICON, icon)
            .appendQueryParameter(QUERY_METADATA_ICON, metadataIcon)
            .apply {
                if (repository.version >= 11) {
                    val displayDpi = context.resources.displayMetrics.densityDpi
                    val requiredDpi = displayDpi * targetSizeDp / 48
                    val iconDpi = supportedDpis.find { it >= requiredDpi } ?: supportedDpis.last()
                    appendQueryParameter(QUERY_DPI, iconDpi.toString())
                }
            }
            .build()
    }
}
