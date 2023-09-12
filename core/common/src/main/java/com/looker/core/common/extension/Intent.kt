package com.looker.core.common.extension

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

fun Intent.getPackageName(): String? {
	val uri = data
	return when {
		uri?.scheme == "package" || uri?.scheme == "fdroid.app" -> {
			uri.schemeSpecificPart?.nullIfEmpty()
		}

		uri?.scheme == "market" && uri.host == "details" -> {
			uri.getQueryParameter("id")?.nullIfEmpty()
		}

		uri != null && uri.scheme in setOf("http", "https") -> {
			val host = uri.host.orEmpty()
			if (host in hosts) {
				if (host == PERSONAL_HOST) uri.getQueryParameter("id")
				else uri.lastPathSegment?.nullIfEmpty()
			} else null
		}

		else -> null
	}
}

fun Intent.getRepoAddress(): String? {
	val uri = data
	return if (uri != null && uri.scheme in setOf("http", "https") && uri.host == PERSONAL_HOST) {
		uri.getQueryParameter("repo_address")
	} else null
}

private const val PERSONAL_HOST = "droidify.eu.org"

private val hosts = setOf(
	PERSONAL_HOST,
	"f-droid.org",
	"www.f-droid.org",
	"staging.f-droid.org",
	"apt.izzysoft.de"
)