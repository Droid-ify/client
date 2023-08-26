package com.looker.droidify.ui.tabs_fragment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.asStateFlow
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.getProperty
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
	private val userPreferencesRepository: UserPreferencesRepository,
	private val savedStateHandle: SavedStateHandle
) : ViewModel() {

	val currentSection = savedStateHandle.getStateFlow<ProductItem.Section>(STATE_SECTION, ProductItem.Section.All)

	val sortOrder = userPreferencesRepository
		.userPreferencesFlow
		.getProperty { sortOrder }

	val sections =
		combine(
			Database.CategoryAdapter.getAllStream(),
			Database.RepositoryAdapter.getEnabledStream()
		) { categories, repos ->
			val productCategories = categories
				.asSequence()
				.sorted()
				.map(ProductItem.Section::Category)
				.toList()

			val enabledRepositories = repos
				.map { ProductItem.Section.Repository(it.id, it.name) }
			enabledRepositories.ifEmpty { setSection(ProductItem.Section.All) }
			listOf(ProductItem.Section.All) + productCategories + enabledRepositories
		}
			.catch { it.printStackTrace() }
			.asStateFlow(emptyList())

	fun setSection(section: ProductItem.Section) {
		savedStateHandle[STATE_SECTION] = section
	}

	fun setSortOrder(sortOrder: SortOrder) {
		viewModelScope.launch {
			userPreferencesRepository.setSortOrder(sortOrder)
		}
	}

	companion object {
		private const val STATE_SECTION = "section"
	}
}