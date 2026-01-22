package com.looker.droidify.ui.appDetail

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.BuildConfig
import com.looker.droidify.data.PrivacyRepository
import com.looker.droidify.data.local.model.RBLogEntity
import com.looker.droidify.data.model.toPackageName
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.di.DefaultDispatcher
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
import com.looker.droidify.utility.common.extension.getLauncherActivities
import com.looker.droidify.utility.common.extension.isSystemApplication
import com.looker.droidify.utility.extension.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    application: Application,
    private val installer: InstallManager,
    private val settingsRepository: SettingsRepository,
    @DefaultDispatcher
    defaultDispatcher: CoroutineDispatcher,
    privacyRepository: PrivacyRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val packageName: String = requireNotNull(savedStateHandle[ARG_PACKAGE_NAME])

    private val repoAddress: StateFlow<String?> =
        savedStateHandle.getStateFlow(ARG_REPO_ADDRESS, null)

    val installerState: StateFlow<InstallState?> =
        installer.state.mapNotNull { stateMap ->
            stateMap[packageName.toPackageName()]
        }.asStateFlow(null)

    private val _state =
        combine(
            Database.ProductAdapter.getStream(packageName),
            Database.RepositoryAdapter.getAllStream(),
            Database.InstalledAdapter.getStream(packageName),
            privacyRepository.getRBLogs(packageName),
            privacyRepository.getLatestDownloadStats(packageName),
            repoAddress,
            flow { emit(settingsRepository.getInitial()) },
        ) { products, repositories, installedItem, rbLogs, downloads, suggestedAddress, initialSettings ->
            val idAndRepos = repositories.associateBy { it.id }
            val filteredProducts = products.filter { product ->
                idAndRepos[product.repositoryId] != null
            }

            val isSelf = packageName == BuildConfig.APPLICATION_ID

            AppDetailUiState(
                products = filteredProducts.mapNotNull { product ->
                    val requiredRepo = repositories.find { it.id == product.repositoryId }
                    requiredRepo?.let { product to it }
                },
                rbLogs = rbLogs,
                downloads = downloads,
                installed = installedItem?.let {
                    with(application.packageManager) {
                        val isSystem = isSystemApplication(packageName)
                        val launcherActivities = if (isSelf) {
                            emptyList()
                        } else {
                            getLauncherActivities(packageName)
                        }
                        Installed(it, isSystem, launcherActivities)
                    }
                },
                installedItem = installedItem,
                isFavourite = packageName in initialSettings.favouriteApps,
                allowIncompatibleVersions = initialSettings.incompatibleVersions,
                isSelf = isSelf,
                addressIfUnavailable = suggestedAddress,
            )
        }.flowOn(defaultDispatcher)

    val state: StateFlow<AppDetailUiState> = _state.asStateFlow(AppDetailUiState())

    val appDetailListState: Flow<AppDetailListState> = _state.map { state ->
        createAppDetailListState(
            context = application,
            packageName = packageName,
            suggestedRepo = state.addressIfUnavailable,
            products = state.products,
            rbLogs = state.rbLogs,
            downloads = state.downloads,
            installedItem = state.installedItem,
            isFavourite = state.isFavourite,
            allowIncompatibleVersion = state.allowIncompatibleVersions,
        )
    }.flowOn(defaultDispatcher)

    fun shizukuState(context: Context): ShizukuState? {
        val isSelected = runBlocking {
            settingsRepository.getInitial().installerType == InstallerType.SHIZUKU
        }

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
    @JvmField
    val isNotInstalled: Boolean,
    @JvmField
    val isNotGranted: Boolean,
    @JvmField
    val isNotAlive: Boolean,
) {
    val check: Boolean
        get() = isNotInstalled || isNotAlive || isNotGranted
}

data class Installed(
    @JvmField
    val installedItem: InstalledItem,
    @JvmField
    val isSystem: Boolean,
    @JvmField
    val launcherActivities: List<Pair<String, String>>,
)

data class AppDetailUiState(
    @JvmField
    val products: List<Pair<Product, Repository>>,
    @JvmField
    val rbLogs: List<RBLogEntity>,
    @JvmField
    val downloads: Long,
    @JvmField
    val installed: Installed?,
    @JvmField
    val installedItem: InstalledItem?,
    @JvmField
    val isSelf: Boolean,
    @JvmField
    val isFavourite: Boolean,
    @JvmField
    val allowIncompatibleVersions: Boolean,
    @JvmField
    val addressIfUnavailable: String?,
) {
    constructor(): this(
        products = emptyList(),
        rbLogs = emptyList(),
        downloads = -1,
        installed = null,
        installedItem = null,
        isSelf = false,
        isFavourite = false,
        allowIncompatibleVersions = false,
        addressIfUnavailable = null,
    )
}
