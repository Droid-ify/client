package com.looker.droidify.data

import com.looker.droidify.data.model.App
import com.looker.droidify.data.model.AppMinimal
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.sync.v2.model.Tag
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    suspend fun apps(
        sortOrder: SortOrder,
        searchQuery: String? = null,
        repoId: Int? = null,
        categoriesToInclude: List<DefaultName>? = null,
        categoriesToExclude: List<DefaultName>? = null,
        antiFeaturesToInclude: List<Tag>? = null,
        antiFeaturesToExclude: List<Tag>? = null,
    ) : List<AppMinimal>

    val categories: Flow<List<DefaultName>>

    fun getApp(packageName: PackageName): Flow<List<App>>

    /**
     * returns true is the app is added successfully
     * returns false if the app was already in the favourites and so it is removed
     */
    suspend fun addToFavourite(packageName: PackageName): Boolean
}
