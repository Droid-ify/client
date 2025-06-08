package com.looker.droidify.ui.appList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.database.CursorOwner.Request.Available
import com.looker.droidify.database.CursorOwner.Request.Installed
import com.looker.droidify.database.CursorOwner.Request.Updates
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.model.ProductItem
import com.looker.droidify.model.ProductItem.Section.All
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel
@Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val skipSignatureStream = settingsRepository
        .get { ignoreSignature }
        .asStateFlow(false)

    val sortOrderFlow = settingsRepository
        .get { sortOrder }
        .asStateFlow(SortOrder.UPDATED)

    val reposStream = Database.RepositoryAdapter
        .getAllStream()
        .asStateFlow(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val showUpdateAllButton = skipSignatureStream.flatMapConcat { skip ->
        Database.ProductAdapter
            .getUpdatesStream(skip)
            .map { it.isNotEmpty() }
    }.asStateFlow(false)

    private val sections = MutableStateFlow<ProductItem.Section>(All)

    val searchQuery = MutableStateFlow("")

    val syncConnection = Connection(SyncService::class.java)

    fun updateAll() {
        viewModelScope.launch {
            syncConnection.binder?.updateAllApps()
        }
    }

    fun request(source: AppListFragment.Source): CursorOwner.Request {
        return when (source) {
            AppListFragment.Source.AVAILABLE -> Available(
                searchQuery = searchQuery.value,
                section = sections.value,
                order = sortOrderFlow.value,
            )

            AppListFragment.Source.INSTALLED -> Installed(
                searchQuery = searchQuery.value,
                section = sections.value,
                order = sortOrderFlow.value,
            )

            AppListFragment.Source.UPDATES -> Updates(
                searchQuery = searchQuery.value,
                section = sections.value,
                order = sortOrderFlow.value,
                skipSignatureCheck = skipSignatureStream.value,
            )
        }
    }

    fun setSection(newSection: ProductItem.Section, perform: () -> Unit) {
        viewModelScope.launch {
            if (newSection != sections.value) {
                sections.emit(newSection)
                launch(Dispatchers.Main) { perform() }
            }
        }
    }

    fun setSearchQuery(newSearchQuery: String, perform: () -> Unit) {
        viewModelScope.launch {
            if (newSearchQuery != searchQuery.value) {
                searchQuery.emit(newSearchQuery)
                launch(Dispatchers.Main) { perform() }
            }
        }
    }
}
