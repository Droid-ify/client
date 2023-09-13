package com.looker.core.common

import android.content.Intent
import com.looker.core.common.extension.get

private const val PERSONAL_HOST = "droidify.eu.org"

private val httpScheme = setOf("http", "https")
private val fdroidRepoScheme = setOf("fdroidrepo", "fdroidrepos")

private val supportedExternalHosts = setOf(
	"f-droid.org",
	"www.f-droid.org",
	"staging.f-droid.org",
	"apt.izzysoft.de"
)

val Intent.deeplinkType: DeeplinkType?
	get() = when {
		data?.scheme == "package" || data?.scheme == "fdroid.app" -> {
			val packageName = data?.schemeSpecificPart?.nullIfEmpty()
				?: throw InvalidDeeplink("Invalid packageName: $data")
			DeeplinkType.AppDetail(packageName)
		}

		data?.scheme in fdroidRepoScheme -> {
			val repoAddress =
				if (data?.scheme.equals("fdroidrepos")) {
					dataString!!.replaceFirst("fdroidrepos", "https")
				} else if (data?.scheme.equals("fdroidrepo")) {
					dataString!!.replaceFirst("fdroidrepo", "https")
				} else throw InvalidDeeplink("No repo address: $data")
			DeeplinkType.AddRepository(repoAddress)
		}

		data?.scheme == "market" && data?.host == "details" -> {
			val packageName =
				data["id"]?.nullIfEmpty() ?: throw InvalidDeeplink("Invalid packageName: $data")
			DeeplinkType.AppDetail(packageName)
		}

		data != null && data?.scheme in httpScheme -> {
			when (data?.host) {
				PERSONAL_HOST -> {
					val repoAddress = data["repo_address"]
					if (data?.path == "/app/") {
						val packageName =
							data["id"] ?: throw InvalidDeeplink("Invalid packageName: $data")
						DeeplinkType.AppDetail(packageName, repoAddress)
					} else throw InvalidDeeplink("Unknown intent path: ${data?.path}, Data: $data")
				}

				in supportedExternalHosts -> {
					val packageName = data?.lastPathSegment?.nullIfEmpty()
						?: throw InvalidDeeplink("Invalid packageName: $data")
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

sealed interface DeeplinkType {

	data class AddRepository(val address: String) : DeeplinkType

	data class AppDetail(val packageName: String, val repoAddress: String? = null) : DeeplinkType

}