package com.looker.core.common

import android.content.Intent
import com.looker.core.common.extension.get

private const val PERSONAL_HOST = "droidify.eu.org"

private val httpScheme = arrayOf("http", "https")
private val fdroidRepoScheme = arrayOf("fdroidrepo", "fdroidrepos")

private val supportedExternalHosts = arrayOf(
    "f-droid.org",
    "www.f-droid.org",
    "staging.f-droid.org",
    "apt.izzysoft.de"
)

val Intent.deeplinkType: DeeplinkType?
    get() = when {
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
                PERSONAL_HOST -> {
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

    data class AddRepository(val address: String) : DeeplinkType

    data class AppDetail(val packageName: String, val repoAddress: String? = null) : DeeplinkType
}
