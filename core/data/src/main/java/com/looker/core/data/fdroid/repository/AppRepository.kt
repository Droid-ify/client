package com.looker.core.data.fdroid.repository

import com.looker.core.model.newer.App
import com.looker.core.model.newer.Author
import com.looker.core.model.newer.Package
import com.looker.core.model.newer.PackageName
import kotlinx.coroutines.flow.Flow

interface AppRepository {

	fun getApps(): Flow<List<App>>

	fun getApp(packageName: PackageName): Flow<List<App>>

	fun getAppFromAuthor(author: Author): Flow<List<App>>

	fun getPackages(packageName: PackageName): Flow<List<Package>>

}