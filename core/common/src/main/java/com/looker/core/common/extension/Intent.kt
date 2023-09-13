package com.looker.core.common.extension

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.TaskStackBuilder
import com.looker.core.common.SdkCheck
import com.looker.core.common.nullIfEmpty

inline val intentFlagCompat
	get() = if (SdkCheck.isSnowCake) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
	else PendingIntent.FLAG_UPDATE_CURRENT

fun Intent.getPendingIntent(context: Context): PendingIntent? =
	TaskStackBuilder
		.create(context)
		.addNextIntentWithParentStack(this)
		.getPendingIntent(0, intentFlagCompat)

val Intent.packageName: String?
	get() {
		val uri = data
		return when {
			uri?.scheme == "package" || uri?.scheme == "fdroid.app" -> {
				uri.schemeSpecificPart?.nullIfEmpty()
			}

			uri?.scheme == "market" && uri.host == "details" -> {
				uri["id"]?.nullIfEmpty()
			}

			uri != null && uri.scheme in httpScheme -> {
				when (uri.host) {
					in supportedExternalHosts -> uri.lastPathSegment?.nullIfEmpty()
					PERSONAL_HOST -> uri["id"]
					else -> null
				}
			}

			else -> null
		}
	}

val Intent.repoAddress: String?
	get() {
		val uri = data
		return if (uri != null && uri.scheme in httpScheme && uri.host == PERSONAL_HOST) {
			uri["repo_address"]
		} else null
	}

operator fun Uri.get(key: String): String? = getQueryParameter(key)

private const val PERSONAL_HOST = "droidify.eu.org"

private val httpScheme = setOf("http", "https")

private val supportedExternalHosts = setOf(
	"f-droid.org",
	"www.f-droid.org",
	"staging.f-droid.org",
	"apt.izzysoft.de"
)