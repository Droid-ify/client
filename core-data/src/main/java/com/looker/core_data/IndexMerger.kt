package com.looker.core_data

import com.looker.core_database.model.Apk
import com.looker.core_database.model.App
import java.io.Closeable

interface IndexMerger: Closeable {

	suspend fun addApps(apps: List<App>)

	suspend fun addApks(apks: List<Apk>)

}