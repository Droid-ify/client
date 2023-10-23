package com.looker.droidify.ui.appDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.asStateFlow
import com.looker.core.common.toPackageName
import com.looker.core.datastore.SettingsRepository
import com.looker.core.model.InstalledItem
import com.looker.core.model.Product
import com.looker.core.model.Repository
import com.looker.droidify.BuildConfig
import com.looker.droidify.database.Database
import com.looker.installer.InstallManager
import com.looker.installer.model.InstallState
import com.looker.installer.model.installFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val installer: InstallManager,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val packageName: String = requireNotNull(savedStateHandle[ARG_PACKAGE_NAME])

    private val repoAddress: StateFlow<String?> =
        savedStateHandle.getStateFlow(ARG_REPO_ADDRESS, null)

    val installerState: StateFlow<InstallState?> =
        installer.state.mapNotNull { stateMap ->
            stateMap[packageName.toPackageName()]
        }.asStateFlow(null)

    val state =
        combine(
            Database.ProductAdapter.getStream(packageName),
            Database.RepositoryAdapter.getAllStream(),
            Database.InstalledAdapter.getStream(packageName),
            repoAddress,
            flow { emit(settingsRepository.fetchInitialPreferences()) }
        ) { products, repositories, installedItem, suggestedAddress, initialSettings ->
            val idAndRepos = repositories.associateBy { it.id }
            val filteredProducts = products.filter { product ->
                idAndRepos[product.repositoryId] != null
            }
            AppDetailUiState(
                products = filteredProducts,
                repos = repositories,
                installedItem = installedItem,
                isFavourite = packageName in initialSettings.favouriteApps,
                allowIncompatibleVersions = initialSettings.incompatibleVersions,
                isSelf = packageName == BuildConfig.APPLICATION_ID,
                addressIfUnavailable = suggestedAddress
            )
        }.asStateFlow(AppDetailUiState())

    fun setFavouriteState() {
        viewModelScope.launch {
            settingsRepository.toggleFavourites(packageName)
        }
    }

    fun installPackage(packageName: String, fileName: String) {
        viewModelScope.launch {
            installer install (packageName installFrom fileName)
        }
    }

    fun uninstallPackage() {
        viewModelScope.launch {
            installer uninstall packageName.toPackageName()
        }
    }

    fun removeQueue() {
        viewModelScope.launch {
            installer remove packageName.toPackageName()
        }
    }

    companion object {
        const val ARG_PACKAGE_NAME = "package_name"
        const val ARG_REPO_ADDRESS = "repo_address"
    }
}

data class AppDetailUiState(
    val products: List<Product> = emptyList(),
    val repos: List<Repository> = emptyList(),
    val installedItem: InstalledItem? = null,
    val isSelf: Boolean = false,
    val isFavourite: Boolean = false,
    val allowIncompatibleVersions: Boolean = false,
    val addressIfUnavailable: String? = null,
)
