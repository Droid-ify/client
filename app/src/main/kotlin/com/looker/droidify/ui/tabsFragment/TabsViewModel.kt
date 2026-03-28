package com.looker.droidify.ui.tabsFragment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.model.ProductItem
import com.looker.droidify.ui.tabsFragment.TabsFragment.BackAction
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val currentSection =
        savedStateHandle.getStateFlow<ProductItem.Section>(STATE_SELECTED_SECTION, ProductItem.Section.All)

    val searchQuery = savedStateHandle.getStateFlow(STATE_SEARCH_QUERY, "")

    val sortOrder = settingsRepository
        .get { sortOrder }
        .asStateFlow(SortOrder.UPDATED)

    val allowHomeScreenSwiping = settingsRepository
        .get { homeScreenSwiping }
        .asStateFlow(false)

    val sections =
        combine(
            Database.CategoryAdapter.getAllStream(),
            Database.RepositoryAdapter.getEnabledStream(),
        ) { categories, repos ->
            val productCategories = categories
                .asSequence()
                .sorted()
                .map(ProductItem.Section::Category)
                .toList()

            val enabledRepositories = repos
                .map { ProductItem.Section.Repository(it.id, it.name) }
            listOf(ProductItem.Section.All) + productCategories + enabledRepositories
        }
            .catch { it.printStackTrace() }
            .asStateFlow(emptyList())

    val isSearchActionItemExpanded = MutableStateFlow(
        savedStateHandle[STATE_SEARCH_ACTION_ITEM_EXPANDED] ?: false,
    )

    val showSections = MutableStateFlow(false)

    val backAction = combine(
        currentSection,
        isSearchActionItemExpanded,
        showSections,
    ) { currentSection, isSearchActionItemExpanded, showSections ->
        when {
            currentSection != ProductItem.Section.All -> BackAction.ProductAll
            isSearchActionItemExpanded -> BackAction.CollapseSearchView
            showSections -> BackAction.HideSections
            else -> BackAction.None
        }
    }.asStateFlow(BackAction.None)

    init {
        viewModelScope.launch {
            isSearchActionItemExpanded.collect { isExpanded ->
                savedStateHandle[STATE_SEARCH_ACTION_ITEM_EXPANDED] = isExpanded
            }
        }
        viewModelScope.launch {
            combine(currentSection, sections) { section, availableSections ->
                section to availableSections
            }.collect { (section, availableSections) ->
                if (availableSections.isNotEmpty() && section !in availableSections) {
                    setSection(ProductItem.Section.All)
                }
            }
        }
    }

    fun setSection(section: ProductItem.Section) {
        savedStateHandle[STATE_SELECTED_SECTION] = section
    }

    fun setSearchQuery(query: String) {
        savedStateHandle[STATE_SEARCH_QUERY] = query
    }

    fun setSearchActionItemExpanded(expanded: Boolean) {
        isSearchActionItemExpanded.value = expanded
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
        private const val STATE_SELECTED_SECTION = "selected_section"
        private const val STATE_SEARCH_QUERY = "search_query"
        private const val STATE_SEARCH_ACTION_ITEM_EXPANDED = "search_action_item_expanded"
    }
}
