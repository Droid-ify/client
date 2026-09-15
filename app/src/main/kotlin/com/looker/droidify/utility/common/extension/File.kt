package com.looker.droidify.utility.common.extension

import android.content.Context
import android.net.Uri
import com.looker.droidify.utility.common.cache.Cache.Provider
import java.io.File

fun File.contentUri(context: Context, base: File = context.cacheDir): Uri = Uri.Builder()
    .scheme("content")
    .authority(Provider.AUTHORITY)
    .encodedPath(path.drop(base.path.length))
    .build()
