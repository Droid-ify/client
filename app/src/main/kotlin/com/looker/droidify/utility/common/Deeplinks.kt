package com.looker.droidify.utility.common

import android.content.Intent
import com.looker.droidify.utility.common.extension.get

const val LEGACY_HOST = "droidify.eu.org"

const val PERSONAL_HOST = "droidify.app"

fun shareUrl(packageName: String, repoAddress: String) =
    "https://droidify.app/app/?id=${packageName}&repo_address=${repoAddress}"

private val httpScheme = arrayOf("http", "https")
private val fdroidRepoScheme = arrayOf("fdroidrepo", "fdroidrepos")

private val supportedExternalHosts = arrayOf(
    "f-droid.org",
    "www.f-droid.org",
    "staging.f-droid.org",
    "apt.izzysoft.de",
)

fun Intent.deeplinkType(): DeeplinkType? = when {
    data?.scheme == "package" || data?.scheme == "fdroid.app" -> {
        val packageName = data?.schemeSpecificPart?.nullIfEmpty()
            ?: invalidDeeplink("Invalid packageName: $data")
        DeeplinkType.AppDetail(packageName)
    }

    data?.scheme in fdroidRepoScheme -> {
        val repoAddress =
            if (data?.scheme.equals("fdroidrepos")) {
                dataString!!.replaceFirst("fdroidrepos", "https")
            } else if (data?.scheme.equals("fdroidrepo")) {
                dataString!!.replaceFirst("fdroidrepo", "https")
            } else {
                invalidDeeplink("No repo address: $data")
            }
        DeeplinkType.AddRepository(repoAddress)
    }

    data?.scheme == "market" && data?.host == "details" -> {
        val packageName =
            data["id"]?.nullIfEmpty() ?: invalidDeeplink("Invalid packageName: $data")
        DeeplinkType.AppDetail(packageName)
    }

    data != null && data?.scheme in httpScheme -> {
        when (data?.host) {
            PERSONAL_HOST, LEGACY_HOST -> {
                val repoAddress = data["repo_address"]
                if (data?.path == "/app/") {
                    val packageName =
                        data["id"] ?: invalidDeeplink("Invalid packageName: $data")
                    DeeplinkType.AppDetail(packageName, repoAddress)
                } else {
                    invalidDeeplink("Unknown intent path: ${data?.path}, Data: $data")
                }
            }

            in supportedExternalHosts -> {
                val packageName = data?.lastPathSegment?.nullIfEmpty()
                    ?: invalidDeeplink("Invalid packageName: $data")
                DeeplinkType.AppDetail(packageName)
            }

            else -> null
        }
    }

    else -> null
}

val Intent.getInstallPackageName: String?
    get() = if (data?.scheme == "package") data?.schemeSpecificPart?.nullIfEmpty() else null

class InvalidDeeplink(override val message: String?) : IllegalStateException(message)

@Suppress("NOTHING_TO_INLINE")
private inline fun invalidDeeplink(message: String): Nothing = throw InvalidDeeplink(message)

sealed interface DeeplinkType {

    class AddRepository(val address: String) : DeeplinkType

    class AppDetail(val packageName: String, val repoAddress: String? = null) : DeeplinkType
}
