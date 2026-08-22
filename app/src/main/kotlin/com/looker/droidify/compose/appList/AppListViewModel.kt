package com.looker.droidify.compose.appList

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import com.looker.droidify.data.model.AppMinimal
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.sync.v2.model.DefaultName
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val searchQuery = TextFieldState("")

    // TODO(sqldelight): reimplement with SQLDelight-backed repository
    val categories: StateFlow<List<DefaultName>> = MutableStateFlow(emptyList())

    private val _selectedCategories = MutableStateFlow<Set<DefaultName>>(emptySet())
    val selectedCategories: StateFlow<Set<DefaultName>> = _selectedCategories

    // Favourites state
    private val _favouritesOnly = MutableStateFlow(false)
    val favouritesOnly: StateFlow<Boolean> = _favouritesOnly

    val sortOrderFlow = settingsRepository.get { sortOrder }.asStateFlow(SortOrder.UPDATED)

    // TODO(sqldelight): reimplement with SQLDelight-backed repository
    val appsState: StateFlow<List<AppMinimal>> = MutableStateFlow(emptyList())

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
