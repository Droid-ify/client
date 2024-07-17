package com.looker.droidify.ui.appList

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.asStateFlow
import com.looker.core.common.toPackageName
import com.looker.core.datastore.SettingsRepository
import com.looker.core.datastore.get
import com.looker.core.datastore.model.SortOrder
import com.looker.core.domain.ProductItem
import com.looker.core.domain.ProductItem.Section.All
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.database.Database
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.extension.startUpdate
import com.looker.installer.InstallManager
import com.looker.installer.model.installFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel
@Inject constructor(
    val settingsRepository: SettingsRepository,
    val installManager: InstallManager
) : ViewModel() {

    val reposStream = Database.RepositoryAdapter
        .getAllStream()
        .asStateFlow(emptyList())

    val showUpdateAllButton = Database.ProductAdapter
        .getUpdatesStream()
        .map { it.isNotEmpty() }
        .asStateFlow(false)

    val sortOrderFlow = settingsRepository.get { sortOrder }
        .asStateFlow(SortOrder.UPDATED)

    private val downloadConnection = Connection(
        serviceClass = DownloadService::class.java,
        onBind = { _, binder ->
            viewModelScope.launch {

                val state = binder.downloadState.value

                if (state.currentItem is DownloadService.State.Success) {
                    installManager
                        .install(state.currentItem.packageName installFrom state.currentItem.release.cacheFileName)
                }
            }
        }
    )

    private val sections = MutableStateFlow<ProductItem.Section>(All)

    val searchQuery = MutableStateFlow("")

    val syncConnection = Connection(SyncService::class.java)

    fun updateAll() {
        viewModelScope.launch {
            syncConnection.binder?.updateAllApps()
        }
    }

    fun request(source: AppListFragment.Source): CursorOwner.Request {
        return when (source) {
            AppListFragment.Source.AVAILABLE -> CursorOwner.Request.ProductsAvailable(
                searchQuery.value,
                sections.value,
                sortOrderFlow.value
            )

            AppListFragment.Source.INSTALLED -> CursorOwner.Request.ProductsInstalled(
                searchQuery.value,
                sections.value,
                sortOrderFlow.value
            )

            AppListFragment.Source.UPDATES -> CursorOwner.Request.ProductsUpdates(
                searchQuery.value,
                sections.value,
                sortOrderFlow.value
            )
        }
    }

    fun setSection(newSection: ProductItem.Section, perform: () -> Unit) {
        viewModelScope.launch {
            if (newSection != sections.value) {
                sections.emit(newSection)
                launch(Dispatchers.Main) { perform() }
            }
        }
    }

    fun setSearchQuery(newSearchQuery: String, perform: () -> Unit) {
        viewModelScope.launch {
            if (newSearchQuery != searchQuery.value) {
                searchQuery.emit(newSearchQuery)
                launch(Dispatchers.Main) { perform() }
            }
        }
    }

    fun install(context: Context, productItem: ProductItem) {
        viewModelScope.launch(Dispatchers.Main) {
            val installedItem = Database.InstalledAdapter.get(productItem.packageName, null)

            val products = Database.ProductAdapter.get(productItem.packageName, null)
                .filter { it.repositoryId == productItem.repositoryId }
                .map { product ->
                    reposStream.value.find {
                        it.id == product.repositoryId
                    }!!.let { product to it }
                }

            downloadConnection.bind(context)

            downloadConnection.startUpdate(productItem.packageName, installedItem, products)
        }
    }

    fun uninstallPackage(productItem: ProductItem) {
        viewModelScope.launch(Dispatchers.Main) {
            installManager.uninstall(productItem.packageName.toPackageName())
        }
    }
}
