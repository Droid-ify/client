package com.looker.installer

import android.content.Context
import com.looker.core.common.extension.onEach
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.distinctMap
import com.looker.core.datastore.model.InstallerType
import com.looker.core.model.newer.PackageName
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallItemState
import com.looker.installer.model.InstallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class Installer(
	private val context: Context,
	private val userPreferencesRepository: UserPreferencesRepository
) {

	private val userPreferenceFlow get() = userPreferencesRepository.userPreferencesFlow

	private val installItems = Channel<InstallItem>()
	private val uninstallItems = Channel<PackageName>()
	private val installState = MutableStateFlow(InstallItemState.EMPTY)

	suspend operator fun invoke() = coroutineScope {
		launch {
			userPreferenceFlow.distinctMap { it.installerType }.collectLatest { installerType ->
				launch {
					installer(
						context = context,
						installerType = installerType,
						installItems = installItems,
						installState = installState
					)
				}
				launch {
					uninstaller(
						context = context,
						installerType = installerType,
						uninstallItems = uninstallItems
					)
				}
			}
		}
	}

	fun close() {
		uninstallItems.close()
		installItems.close()
	}

	suspend operator fun plus(installItem: InstallItem) {
		installItems.send(installItem)
	}

	suspend operator fun minus(packageName: PackageName) {
		uninstallItems.send(packageName)
	}

	infix fun stateOf(installItem: InstallItem): Flow<InstallState> = installState
		.filter { it.installedItem == installItem }
		.map { it.state }

	private fun CoroutineScope.installer(
		context: Context,
		installerType: InstallerType,
		installItems: ReceiveChannel<InstallItem>,
		installState: MutableStateFlow<InstallItemState>
	) = launch {
		onEach(installItems) { item ->
			installState.emit(InstallItemState(item, InstallState.Queued))
		}.consumeEach {
			when (installerType) {
				InstallerType.LEGACY -> TODO()
				InstallerType.SHIZUKU -> TODO()
				InstallerType.SESSION -> TODO()
				InstallerType.ROOT -> TODO()
			}
		}
	}

	private fun CoroutineScope.uninstaller(
		context: Context,
		installerType: InstallerType,
		uninstallItems: ReceiveChannel<PackageName>
	) = launch {
		uninstallItems.consumeEach {
			when (installerType) {
				InstallerType.SESSION -> TODO()
				else -> TODO()
			}
		}
	}
}
