package com.looker.droidify.utility

import android.content.Context
import com.looker.core.common.PackageName
import com.looker.core.datastore.SettingsRepository
import com.looker.core.domain.InstalledItem
import com.looker.core.domain.Product
import com.looker.core.domain.Repository
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.utility.extension.startUpdate
import com.looker.installer.InstallManager
import com.looker.installer.model.InstallItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class InstallUtils {

    companion object {
        fun install(
            context: Context,
            settingsRepository: SettingsRepository,
            scope: CoroutineScope,
            installedItem: InstalledItem,
            products: List<Pair<Product, Repository>>
        ) {
            Connection(
                serviceClass = DownloadService::class.java,
                onBind = { _, binder ->
                    scope.launch {

                        val state = binder.downloadState.value

                        if (state.currentItem is DownloadService.State.Success) {

                            InstallManager(context, settingsRepository)
                                .install(
                                    InstallItem(
                                        PackageName(state.currentItem.packageName),
                                        state.currentItem.release.cacheFileName
                                    )
                                )
                        }
                    }
                }
            ).startUpdate(installedItem.packageName, installedItem, products)
        }
    }

}
