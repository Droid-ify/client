package com.looker.droidify.compose.appList

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import com.looker.droidify.data.AppRepository
import com.looker.droidify.data.model.AppMinimal
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@HiltViewModel
@OptIn(FlowPreview::class)
class AppListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val searchQuery = TextFieldState("")
    private val searchQueryStream = snapshotFlow { searchQuery.text.toString() }.debounce(300)

    val categories = appRepository.categories.asStateFlow(emptyList())

    private val _selectedCategories = MutableStateFlow<Set<DefaultName>>(emptySet())
    val selectedCategories: StateFlow<Set<DefaultName>> = _selectedCategories

    // Favourites state
    private val _favouritesOnly = MutableStateFlow(false)
    val favouritesOnly: StateFlow<Boolean> = _favouritesOnly
    private val favouriteApps: Flow<Set<String>> = settingsRepository.get { favouriteApps }.distinctUntilChanged()

    val sortOrderFlow = settingsRepository.get { sortOrder }.asStateFlow(SortOrder.UPDATED)

    @OptIn(ExperimentalCoroutinesApi::class)
    val appsState: StateFlow<List<AppMinimal>> = combine(
        searchQueryStream,
        selectedCategories,
        sortOrderFlow,
        favouritesOnly,
        favouriteApps,
    ) { searchQuery, categories, sortOrder, favOnly, favSet ->
        val items = appRepository.apps(
            sortOrder = sortOrder,
            searchQuery = searchQuery,
            categoriesToInclude = categories.toList(),
        )
        if (favOnly) items.filter { it.packageName.name in favSet } else items
    }.asStateFlow(emptyList())

    fun toggleCategory(category: DefaultName) {
        val currentCategories = _selectedCategories.value
        _selectedCategories.value = if (currentCategories.contains(category)) {
            currentCategories - category
        } else {
            currentCategories + category
        }
    }

    fun toggleFavouritesOnly() {
        _favouritesOnly.value = !_favouritesOnly.value
    }
}
