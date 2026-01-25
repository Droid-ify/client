@file:Suppress("OPT_IN_USAGE")

package com.looker.droidify.ui.appList

import android.app.Application
import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.looker.droidify.database.AppListRow
import com.looker.droidify.database.Database
import com.looker.droidify.database.LastAccessedKeyCallback
import com.looker.droidify.database.createProductPagingSource
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.ignoreSignatureFlow
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.di.IoDispatcher
import com.looker.droidify.di.MainHandler
import com.looker.droidify.model.ProductItem
import com.looker.droidify.model.ProductItem.Section.All
import com.looker.droidify.model.Repository
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.ui.appList.AppListFragment.Source
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import com.looker.droidify.R.string as stringRes

private const val KEY_LAST_KEY: String = "KEY_LAST_KEY"
private const val KEY_SEARCH_QUERY: String = "KEY_SEARCH_QUERY"
private const val KEY_STATE: String = "KEY_STATE"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = AppListViewModel.Factory::class)
class AppListViewModel
@AssistedInject constructor(
    application: Application,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
    @MainHandler
    mainHandler: Handler,
    @IoDispatcher
    ioDispatcher: CoroutineDispatcher,
    @Assisted
    source: Source,
) : ViewModel() {

    @JvmField
    internal var lastAccessedKey: Int? = null

    private val listForceReloadFlow: MutableSharedFlow<Long> = MutableSharedFlow()

    private val ignoreSignatureFlow: StateFlow<Boolean> = settingsRepository
        .ignoreSignatureFlow()
        .asStateFlow(false)

    private val sections: MutableStateFlow<ProductItem.Section> = MutableStateFlow(All)

    private val searchQueryFlow: MutableStateFlow<String>

    private data class AppListParams(
        @JvmField
        val searchQuery: String,
        @JvmField
        val sections: ProductItem.Section,
        @JvmField
        val sortOrder: SortOrder,
        @JvmField
        val ignoreSignature: Boolean,
    )

    private data class PagerParams(
        @JvmField
        val state: AppListParams,
        @JvmField
        val timestamp: Long,
        @JvmField
        val repos: List<Repository>,
    )

    @JvmField
    val showUpdateAllButton: StateFlow<Boolean> = ignoreSignatureFlow.flatMapLatest { skip ->
        Database.ProductAdapter
            .getUpdatesStream(skip)
    }.asStateFlow(false)

    @JvmField
    val syncConnection: Connection<SyncService.Binder, SyncService> = Connection(SyncService::class.java)

    @JvmField
    val listFlow: Flow<PagingData<AppListRow>>

    init {
        val saveState = savedStateHandle.get<Bundle>(KEY_STATE)
        lastAccessedKey = saveState?.getInt(KEY_LAST_KEY)
        searchQueryFlow = MutableStateFlow(saveState?.getString(KEY_SEARCH_QUERY).orEmpty())

        savedStateHandle.setSavedStateProvider(KEY_STATE) {
            Bundle(2).apply {
                val lastKey = lastAccessedKey
                if (lastKey is Int) {
                    putInt(KEY_LAST_KEY, lastKey)
                }

                val searchQuery = searchQueryFlow.value
                if (searchQuery.isNotBlank()) {
                    putString(KEY_SEARCH_QUERY, searchQuery)
                }
            }
        }

        val sortOrderFlow: Flow<SortOrder> = settingsRepository.get { sortOrder }

        val state: Flow<AppListParams> = combine(
            sortOrderFlow,
            sections,
            searchQueryFlow.debounce { 200L },
            ignoreSignatureFlow,
        ) { sortOrder, section, query, ignoreSignature ->
            AppListParams(
                searchQuery = query,
                sections = section,
                sortOrder = sortOrder,
                ignoreSignature = ignoreSignature,
            )
        }.distinctUntilChanged()

        val lastAccessedKeyCallback = LastAccessedKeyCallback {
            lastAccessedKey = it
        }

        listFlow = combine(
            state,
            listForceReloadFlow.onStart { emit(0L) },
            Database.RepositoryAdapter
                .getAllStream(),
        ) { state, timestamp, repos ->
            PagerParams(state, timestamp, repos)
        }.distinctUntilChanged().flowOn(ioDispatcher).flatMapLatest { params ->
            val state = params.state

            Pager(
                config = PagingConfig(
                    pageSize = 60,
                    enablePlaceholders = false,
                ),
                initialKey = lastAccessedKey,
                pagingSourceFactory = {
                    val emptyText = when {
                        state.searchQuery.isNotEmpty() -> {
                            application.getString(stringRes.no_matching_applications_found)
                        }

                        else -> when (source) {
                            Source.AVAILABLE -> application.getString(stringRes.no_applications_available)
                            Source.INSTALLED -> application.getString(stringRes.no_applications_installed)
                            Source.UPDATES -> application.getString(stringRes.all_applications_up_to_date)
                        }
                    }

                    val repositories = params.repos

                    when (source) {
                        Source.AVAILABLE -> createProductPagingSource(
                            installed = false,
                            updates = false,
                            searchQuery = state.searchQuery,
                            section = state.sections,
                            sortOrder = state.sortOrder,
                            skipSignatureCheck = state.ignoreSignature,
                            repositories = repositories,
                            emptyText = emptyText,
                            mainHandler = mainHandler,
                            ioDispatcher = ioDispatcher,
                            lastAccessedKeyCallback = lastAccessedKeyCallback,
                        )

                        Source.INSTALLED -> createProductPagingSource(
                            installed = true,
                            updates = false,
                            searchQuery = state.searchQuery,
                            section = state.sections,
                            sortOrder = state.sortOrder,
                            skipSignatureCheck = state.ignoreSignature,
                            repositories = repositories,
                            emptyText = emptyText,
                            mainHandler = mainHandler,
                            ioDispatcher = ioDispatcher,
                            lastAccessedKeyCallback = lastAccessedKeyCallback,
                        )

                        Source.UPDATES -> createProductPagingSource(
                            installed = true,
                            updates = true,
                            searchQuery = state.searchQuery,
                            section = state.sections,
                            sortOrder = state.sortOrder,
                            skipSignatureCheck = state.ignoreSignature,
                            repositories = repositories,
                            emptyText = emptyText,
                            mainHandler = mainHandler,
                            ioDispatcher = ioDispatcher,
                            lastAccessedKeyCallback = lastAccessedKeyCallback,
                        )
                    }
                },
            ).flow
        }.cachedIn(viewModelScope)
    }

    fun updateAll() {
        viewModelScope.launch {
            syncConnection.binder?.updateAllApps()
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            listForceReloadFlow.emit(System.currentTimeMillis())
        }
    }

    fun setSection(newSection: ProductItem.Section) {
        viewModelScope.launch {
            sections.emit(newSection)
        }
    }

    fun setSearchQuery(newSearchQuery: String) {
        viewModelScope.launch {
            searchQueryFlow.emit(newSearchQuery)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(source: Source): AppListViewModel
    }
}
