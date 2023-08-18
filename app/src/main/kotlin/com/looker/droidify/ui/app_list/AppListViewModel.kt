package com.looker.droidify.ui.app_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.asSequence
import com.looker.core.common.extension.asStateFlow
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.distinctMap
import com.looker.core.datastore.model.SortOrder
import com.looker.core.model.ProductItem
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.database.Database
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel
@Inject constructor(
	userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

	private val sortOrderFlow = userPreferencesRepository
		.userPreferencesFlow
		.distinctMap { it.sortOrder }

	val reposStream = Database.RepositoryAdapter
		.getAllStream()
		.asStateFlow(emptyList())

	val showUpdateAllButton = Database.ProductAdapter
		.getUpdatesStream()
		.map { it.isNotEmpty() }
		.asStateFlow(false)

	private val _sections = MutableStateFlow<ProductItem.Section>(ProductItem.Section.All)
	private val _searchQuery = MutableStateFlow("")

	private val sections: StateFlow<ProductItem.Section> = _sections.stateIn(
		initialValue = ProductItem.Section.All,
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5000)
	)
	val searchQuery: StateFlow<String> = _searchQuery.stateIn(
		initialValue = "",
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5000)
	)

	val syncConnection = Connection(SyncService::class.java)

	fun updateAll() {
		syncConnection.binder?.updateAllApps()
	}

	fun request(source: AppListFragment.Source): CursorOwner.Request {
		var mSearchQuery = ""
		var mSections: ProductItem.Section = ProductItem.Section.All
		var mOrder: SortOrder = SortOrder.NAME
		viewModelScope.launch {
			launch { searchQuery.collect { mSearchQuery = it } }
			launch { sections.collect { if (source.sections) mSections = it } }
			sortOrderFlow.collect { mOrder = it }
		}
		return when (source) {
			AppListFragment.Source.AVAILABLE -> CursorOwner.Request.ProductsAvailable(
				mSearchQuery,
				mSections,
				mOrder
			)
			AppListFragment.Source.INSTALLED -> CursorOwner.Request.ProductsInstalled(
				mSearchQuery,
				mSections,
				mOrder
			)
			AppListFragment.Source.UPDATES -> CursorOwner.Request.ProductsUpdates(
				mSearchQuery,
				mSections,
				mOrder
			)
		}
	}

	fun setSection(newSection: ProductItem.Section, perform: () -> Unit) {
		viewModelScope.launch {
			if (newSection != sections.value) {
				_sections.emit(newSection)
				launch(Dispatchers.Main) { perform() }
			}
		}
	}

	fun setSearchQuery(newSearchQuery: String, perform: () -> Unit) {
		viewModelScope.launch {
			if (newSearchQuery != searchQuery.value) {
				_searchQuery.emit(newSearchQuery)
				launch(Dispatchers.Main) { perform() }
			}
		}
	}
}