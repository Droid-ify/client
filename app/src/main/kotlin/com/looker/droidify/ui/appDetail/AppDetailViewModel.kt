package com.looker.droidify.ui.appDetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.BuildConfig
import com.looker.droidify.data.model.toPackageName
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.installer.InstallManager
import com.looker.droidify.installer.installers.isShizukuAlive
import com.looker.droidify.installer.installers.isShizukuGranted
import com.looker.droidify.installer.installers.isShizukuInstalled
import com.looker.droidify.installer.installers.isSuiAvailable
import com.looker.droidify.installer.installers.requestPermissionListener
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.installer.model.installFrom
import com.looker.droidify.model.InstalledItem
import com.looker.droidify.model.Product
import com.looker.droidify.model.Repository
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val installer: InstallManager,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
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
            flow { emit(settingsRepository.getInitial()) },
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
                addressIfUnavailable = suggestedAddress,
            )
        }.asStateFlow(AppDetailUiState())

    fun shizukuState(context: Context): ShizukuState? {
        val isSelected =
            runBlocking { settingsRepository.getInitial().installerType == InstallerType.SHIZUKU }
        if (!isSelected) return null
        val isAlive = isShizukuAlive()
        val isSuiAvailable = isSuiAvailable()
        if (isSuiAvailable) return null

        val isGranted = if (isAlive) {
            if (isShizukuGranted()) {
                true
            } else {
                runBlocking { requestPermissionListener() }
            }
        } else false
        return ShizukuState(
            isNotInstalled = !isShizukuInstalled(context),
            isNotGranted = !isGranted,
            isNotAlive = !isAlive,
        )
    }

    fun setDefaultInstaller() {
        viewModelScope.launch {
            settingsRepository.setInstallerType(InstallerType.Default)
        }
    }

    suspend fun shouldIgnoreSignature(): Boolean {
        return settingsRepository.getInitial().ignoreSignature
    }

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

data class ShizukuState(
    val isNotInstalled: Boolean,
    val isNotGranted: Boolean,
    val isNotAlive: Boolean,
) {
    val check: Boolean
        get() = isNotInstalled || isNotAlive || isNotGranted
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
