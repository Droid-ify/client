package com.looker.droidify.ui.app_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core_datastore.UserPreferencesRepository
import com.looker.core_datastore.model.SortOrder
import com.looker.core_model.ProductItem
import com.looker.droidify.database.CursorOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel
@Inject constructor(
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

	private val initialSetup = flow {
		emit(userPreferencesRepository.fetchInitialPreferences().sortOrder)
	}

	private var lastSortOrder: SortOrder = SortOrder.UPDATED

	private val userPreferences = userPreferencesRepository.userPreferencesFlow.filter {
		it.sortOrder != lastSortOrder
	}.map {
		lastSortOrder = it.sortOrder
		it.sortOrder
	}

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

	fun request(source: AppListFragment.Source): CursorOwner.Request {
		var mSearchQuery = ""
		var mSections: ProductItem.Section = ProductItem.Section.All
		var mOrder: SortOrder = SortOrder.NAME
		viewModelScope.launch {
			launch { searchQuery.collect { mSearchQuery = it } }
			launch { sections.collect { if (source.sections) mSections = it } }
			initialSetup.collect { initialOrder ->
				mOrder = initialOrder
				userPreferences.collect {
					mOrder = it
				}
			}
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