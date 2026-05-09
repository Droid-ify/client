package com.looker.droidify.ui.history

import androidx.lifecycle.ViewModel
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.model.ProductItem
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class AppHistoryViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val historyApps: StateFlow<List<ProductItem>> =
        settingsRepository
            .get { installedAppsHistory }
            .map { history ->
                history.mapNotNull { app ->
                    val products = Database.ProductAdapter.get(app, null)
                    val product = products.firstOrNull() ?: return@mapNotNull null
                    val installed = Database.InstalledAdapter.get(app, null)

                    product.item().apply {
                        this.installedVersion = installed?.version.orEmpty()
                        this.canUpdate = product.canUpdate(installed)
                    }
                }
            }.asStateFlow(emptyList())
}
