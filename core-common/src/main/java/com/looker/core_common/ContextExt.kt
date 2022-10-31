package com.looker.core_common

import android.app.NotificationManager
import android.content.Context

inline val Context.notificationManager: NotificationManager
	get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager