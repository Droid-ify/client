package com.looker.droidify.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.asStateFlow
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.getProperty
import com.looker.core.model.Product
import com.looker.droidify.database.Database
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

	val favouriteApps: StateFlow<List<List<Product>>> =
		userPreferencesRepository.userPreferencesFlow
			.getProperty { favouriteApps }
			.map { favourites ->
				favourites.mapNotNull { app ->
					Database.ProductAdapter.get(app, null).ifEmpty { null }
				}
			}.asStateFlow(emptyList())

	fun updateFavourites(packageName: String) {
		viewModelScope.launch {
			userPreferencesRepository.toggleFavourites(packageName)
		}
	}
}