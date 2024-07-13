package com.looker.droidify.ui.tabsFragment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.asStateFlow
import com.looker.core.datastore.SettingsRepository
import com.looker.core.datastore.get
import com.looker.core.datastore.model.SortOrder
import com.looker.droidify.model.ProductItem
import com.looker.droidify.database.Database
import com.looker.droidify.ui.tabsFragment.TabsFragment.BackAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class TabsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val currentSection =
        savedStateHandle.getStateFlow<ProductItem.Section>(STATE_SECTION, ProductItem.Section.All)

    val sortOrder = settingsRepository
        .get { sortOrder }
        .asStateFlow(SortOrder.UPDATED)

    val allowHomeScreenSwiping = settingsRepository
        .get { homeScreenSwiping }
        .asStateFlow(false)

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

    val isSearchActionItemExpanded = MutableStateFlow(false)

    val showSections = MutableStateFlow(false)

    val backAction = combine(currentSection, isSearchActionItemExpanded, showSections, ::calcBackAction).asStateFlow(BackAction.None)

    fun setSection(section: ProductItem.Section) {
        savedStateHandle[STATE_SECTION] = section
    }

    fun setSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            settingsRepository.setSortOrder(sortOrder)
        }
    }

    private fun calcBackAction(
        currentSection: ProductItem.Section,
        isSearchActionItemExpanded: Boolean,
        showSections: Boolean,
    ): BackAction {
        return when {
            currentSection != ProductItem.Section.All -> {
                BackAction.ProductAll
            }

            isSearchActionItemExpanded -> {
                BackAction.CollapseSearchView
            }

            showSections -> {
                BackAction.HideSections
            }

            else -> {
                BackAction.None
            }
        }
    }

    companion object {
        private const val STATE_SECTION = "section"
    }
}
