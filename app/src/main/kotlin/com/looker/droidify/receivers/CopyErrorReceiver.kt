package com.looker.droidify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.looker.droidify.utility.common.extension.copyToClipboard
import com.looker.droidify.utility.common.extension.notificationManager

class CopyErrorReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COPY_ERROR = "com.looker.droidify.intent.action.COPY_ERROR"
        const val EXTRA_ERROR_DETAILS = "error_details"
        const val EXTRA_NOTIFICATION_TAG = "notification_tag"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COPY_ERROR) {
            val errorDetails = intent.getStringExtra(EXTRA_ERROR_DETAILS) ?: return
            val notificationTag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG)
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

            context.copyToClipboard(errorDetails)
            if (notificationTag != null && notificationId != -1) {
                context.notificationManager?.cancel(notificationTag, notificationId)
            }
        }
    }
}
