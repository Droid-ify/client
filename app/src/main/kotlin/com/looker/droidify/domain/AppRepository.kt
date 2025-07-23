package com.looker.droidify.domain

import com.looker.droidify.domain.model.App
import com.looker.droidify.domain.model.Author
import com.looker.droidify.domain.model.Package
import com.looker.droidify.domain.model.PackageName
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    fun getApp(packageName: PackageName): Flow<List<App>>

    fun getAppFromAuthor(author: Author): Flow<List<App>>

    fun getPackages(packageName: PackageName): Flow<List<Package>>

    /**
     * returns true is the app is added successfully
     * returns false if the app was already in the favourites and so it is removed
     */
    suspend fun addToFavourite(packageName: PackageName): Boolean
}
