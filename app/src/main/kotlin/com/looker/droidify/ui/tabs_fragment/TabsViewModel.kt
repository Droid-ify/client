package com.looker.droidify.ui.tabs_fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.asStateFlow
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.distinctMap
import com.looker.core.datastore.model.SortOrder
import com.looker.core.model.ProductItem
import com.looker.droidify.database.Database
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabsViewModel @Inject constructor(
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

	val sortOrderFlow = userPreferencesRepository
		.userPreferencesFlow
		.distinctMap { it.sortOrder }

	val categories =
		combine(
			Database.CategoryAdapter.getAllStream(),
			Database.RepositoryAdapter.getAllStream()
		) { categories, repos ->
			val productCategories = categories
				.asSequence()
				.sorted()
				.map(ProductItem.Section::Category)
				.toList()

			val enabledRepositories = repos
				.asSequence()
				.filter { it.enabled }
				.map { ProductItem.Section.Repository(it.id, it.name) }
				.toList()
			productCategories to enabledRepositories
		}
			.catch { it.printStackTrace() }
			.asStateFlow(emptyList<ProductItem.Section.Category>() to emptyList())

	fun setSortOrder(sortOrder: SortOrder) {
		viewModelScope.launch {
			userPreferencesRepository.setSortOrder(sortOrder)
		}
	}
}