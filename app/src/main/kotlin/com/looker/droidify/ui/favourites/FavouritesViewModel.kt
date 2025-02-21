package com.looker.droidify.ui.favourites

import androidx.lifecycle.ViewModel
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.model.Product
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val favouriteApps: StateFlow<List<List<Product>>> =
        settingsRepository
            .get { favouriteApps }
            .map { favourites ->
                favourites.mapNotNull { app ->
                    Database.ProductAdapter.get(app, null).ifEmpty { null }
                }
            }.asStateFlow(emptyList())

}
