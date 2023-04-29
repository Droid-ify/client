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
			if (host == "f-droid.org"
				|| host.endsWith(".f-droid.org")
				|| host == "apt.izzysoft.de"
			) {
				uri.lastPathSegment?.nullIfEmpty()
			} else {
				null
			}
		}

		else -> null
	}
}