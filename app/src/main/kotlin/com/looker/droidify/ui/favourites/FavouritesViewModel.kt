package com.looker.droidify.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.asStateFlow
import com.looker.core.datastore.SettingsRepository
import com.looker.core.datastore.get
import com.looker.droidify.model.Product
import com.looker.droidify.database.Database
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val favouriteApps: StateFlow<List<List<Product>>> =
        settingsRepository
            .get { favouriteApps }
            .map { favourites ->
                favourites.mapNotNull { app ->
                    Database.ProductAdapter.get(app, null).ifEmpty { null }
                }
            }.asStateFlow(emptyList())

    fun updateFavourites(packageName: String) {
        viewModelScope.launch {
            settingsRepository.toggleFavourites(packageName)
        }
    }
}
