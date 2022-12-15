package com.looker.droidify.sync

import android.app.job.JobInfo
import androidx.work.NetworkType

data class SyncPreference(
	val networkType: NetworkType,
	val pluggedIn: Boolean = false,
	val batteryNotLow: Boolean = true,
	val canSync: Boolean = true
)

fun SyncPreference.toJobNetworkType() = when (networkType) {
	NetworkType.NOT_REQUIRED -> JobInfo.NETWORK_TYPE_NONE
	NetworkType.UNMETERED -> JobInfo.NETWORK_TYPE_UNMETERED
	else -> JobInfo.NETWORK_TYPE_ANY
}
