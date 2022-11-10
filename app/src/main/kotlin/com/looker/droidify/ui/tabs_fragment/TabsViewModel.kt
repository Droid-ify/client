package com.looker.droidify.ui.tabs_fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabsViewModel @Inject constructor(
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

	val initialSetup = flow {
		emit(userPreferencesRepository.fetchInitialPreferences().sortOrder)
	}

	private var lastSortOrder: SortOrder = SortOrder.UPDATED

	val userPreferences = userPreferencesRepository.userPreferencesFlow.filter {
		it.sortOrder != lastSortOrder
	}.map {
		lastSortOrder = it.sortOrder
		it.sortOrder
	}

	fun setSortOrder(sortOrder: SortOrder) {
		viewModelScope.launch {
			userPreferencesRepository.setSortOrder(sortOrder)
		}
	}

}