package com.looker.droidify.utility.extension

import android.app.job.JobInfo
import androidx.work.NetworkType
import com.looker.droidify.work.AutoSyncWorker.SyncConditions

fun SyncConditions.toJobNetworkType() = when (networkType) {
	NetworkType.NOT_REQUIRED -> JobInfo.NETWORK_TYPE_NONE
	NetworkType.UNMETERED -> JobInfo.NETWORK_TYPE_UNMETERED
	else -> JobInfo.NETWORK_TYPE_ANY
}
