package com.looker.downloader.model

import android.content.Context
import com.looker.core.common.cache.Cache

data class DownloadItem(
	val url: String,
	val fileName: String,
	val headerInfo: HeaderInfo = HeaderInfo()
)

fun DownloadItem.toLocation(context: Context) = DownloadLocation(
	file = Cache.getPartialReleaseFile(context, fileName),
	url = url,
	headerInfo = headerInfo
)
