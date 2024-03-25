package com.looker.core.common.extension

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.TaskStackBuilder
import com.looker.core.common.SdkCheck

fun intent(action: String, block: Intent.() -> Unit = {}): Intent {
    return Intent(action).apply(block)
}

inline val intentFlagCompat
    get() = if (SdkCheck.isSnowCake) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }

fun Intent.toPendingIntent(context: Context): PendingIntent? =
    TaskStackBuilder
        .create(context)
        .addNextIntentWithParentStack(this)
        .getPendingIntent(0, intentFlagCompat)

operator fun Uri?.get(key: String): String? = this?.getQueryParameter(key)
