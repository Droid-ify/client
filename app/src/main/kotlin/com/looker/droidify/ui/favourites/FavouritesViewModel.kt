package com.looker.droidify.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.distinctMap
import com.looker.core.model.Product
import com.looker.droidify.database.Database
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class FavouritesViewModel @Inject constructor(
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

	val favouriteApps: StateFlow<List<List<Product>>> =
		userPreferencesRepository.userPreferencesFlow
			.distinctMap { it.favouriteApps }
			.map { favourites ->
				favourites.map { app ->
					Database.ProductAdapter.get(app, null)
				}
			}.stateIn(
				scope = viewModelScope,
				started = SharingStarted.WhileSubscribed(5_000),
				initialValue = emptyList()
			)

	fun updateFavourites(packageName: String) {
		viewModelScope.launch {
			userPreferencesRepository.toggleFavourites(packageName)
		}
	}
}