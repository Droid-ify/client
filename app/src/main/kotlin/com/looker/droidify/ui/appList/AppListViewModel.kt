package com.looker.droidify.ui.appList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.database.CursorOwner.Request.Available
import com.looker.droidify.database.CursorOwner.Request.Installed
import com.looker.droidify.database.CursorOwner.Request.Updates
import com.looker.droidify.database.Database
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.installer.InstallManager
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.model.ProductItem
import com.looker.droidify.model.ProductItem.Section.All
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel
@Inject constructor(
    settingsRepository: SettingsRepository,
    installManager: InstallManager,
) : ViewModel() {

    val installStates: StateFlow<Map<PackageName, InstallState>> = installManager.state

    private val skipSignatureStream = settingsRepository
        .get { ignoreSignature }
        .asStateFlow(false)

    private val sortOrderFlow = settingsRepository
        .get { sortOrder }
        .asStateFlow(SortOrder.UPDATED)

    private val sections = MutableStateFlow<ProductItem.Section>(All)

    val searchQuery = MutableStateFlow("")

    val state = combine(
        sortOrderFlow,
        sections,
        searchQuery,
    ) { sortOrder, section, query ->
        AppListState(
            searchQuery = query,
            sections = section,
            sortOrder = sortOrder,
        )
    }.asStateFlow(AppListState())

    val reposStream = Database.RepositoryAdapter
        .getAllStream()
        .asStateFlow(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val showUpdateAllButton = skipSignatureStream.flatMapLatest { skip ->
        Database.ProductAdapter
            .getUpdatesStream(skip)
            .map { it.isNotEmpty() }
    }.asStateFlow(false)

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
            )
        }
    }

    fun setSection(newSection: ProductItem.Section) {
        viewModelScope.launch {
            sections.emit(newSection)
        }
    }

    fun setSearchQuery(newSearchQuery: String) {
        viewModelScope.launch {
            searchQuery.emit(newSearchQuery)
        }
    }
}

data class AppListState(
    val searchQuery: String = "",
    val sections: ProductItem.Section = All,
    val sortOrder: SortOrder = SortOrder.UPDATED,
)
