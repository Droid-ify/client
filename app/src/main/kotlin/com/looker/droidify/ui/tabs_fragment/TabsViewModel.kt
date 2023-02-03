package com.looker.droidify.ui.tabs_fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.stateIn
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.distinctMap
import com.looker.core.datastore.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabsViewModel @Inject constructor(
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

	val sortOrderFlow = userPreferencesRepository
		.userPreferencesFlow
		.distinctMap { it.sortOrder }
		.stateIn(
			scope = viewModelScope,
			initial = SortOrder.ADDED
		)

	fun setSortOrder(sortOrder: SortOrder) {
		viewModelScope.launch {
			userPreferencesRepository.setSortOrder(sortOrder)
		}
	}
}