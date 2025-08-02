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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce

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

    val sortOrderFlow = settingsRepository.get { sortOrder }.asStateFlow(SortOrder.UPDATED)

    @OptIn(ExperimentalCoroutinesApi::class)
    val appsState: StateFlow<List<AppMinimal>> = combine(
        searchQueryStream,
        selectedCategories,
        sortOrderFlow,
    ) { searchQuery, categories, sortOrder ->
        appRepository.apps(
            sortOrder = sortOrder,
            searchQuery = searchQuery,
            categoriesToInclude = categories.toList(),
        )
    }.asStateFlow(emptyList())

    fun toggleCategory(category: DefaultName) {
        val currentCategories = _selectedCategories.value
        _selectedCategories.value = if (currentCategories.contains(category)) {
            currentCategories - category
        } else {
            currentCategories + category
        }
    }
}
