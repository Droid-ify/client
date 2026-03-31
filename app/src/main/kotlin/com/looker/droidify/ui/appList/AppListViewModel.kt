package com.looker.droidify.ui.appList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class AppListViewModel
@Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val skipSignatureStream = settingsRepository
        .get { ignoreSignature }
        .asStateFlow(false)

    private val sortOrderFlow = settingsRepository
        .get { sortOrder }
        .asStateFlow(SortOrder.UPDATED)

    private val sections = MutableStateFlow<ProductItem.Section>(All)

    val state = combine(
        skipSignatureStream,
        sortOrderFlow,
        sections,
    ) { skipSignature, sortOrder, section ->
        AppListState(
            sections = section,
            sortOrder = sortOrder,
            skipSignatureCheck = skipSignature,
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

    fun setSection(newSection: ProductItem.Section) {
        viewModelScope.launch {
            sections.emit(newSection)
        }
    }
}

data class AppListState(
    val sections: ProductItem.Section = All,
    val sortOrder: SortOrder = SortOrder.UPDATED,
    val skipSignatureCheck: Boolean = false,
) {
    fun toRequest(source: AppListFragment.Source, searchQuery: String) = when (source) {
        AppListFragment.Source.AVAILABLE -> Available(
            searchQuery = searchQuery,
            section = sections,
            order = sortOrder,
            skipSignatureCheck = skipSignatureCheck,
        )

        AppListFragment.Source.INSTALLED -> Installed(
            searchQuery = searchQuery,
            order = sortOrder,
            skipSignatureCheck = skipSignatureCheck,
        )

        AppListFragment.Source.UPDATES -> Updates(
            searchQuery = searchQuery,
            order = sortOrder,
            skipSignatureCheck = skipSignatureCheck,
        )
    }
}
