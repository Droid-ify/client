package com.looker.installer

import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.distinctMap
import com.looker.core.datastore.model.InstallerType
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallItemState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class Installer(
	private val context: Context,
	private val userPreferencesRepository: UserPreferencesRepository
) {

	private val userPreferenceFlow
		get() = userPreferencesRepository.userPreferencesFlow

	private val installChannel = Channel<InstallItem>()
	private val installState = MutableStateFlow(InstallItemState.EMPTY)

	operator fun invoke() {
		installerScope.launch {
			userPreferenceFlow.distinctMap { it.installerType }.collectLatest { installerType ->
				installer(
					installerType = installerType,
					installItems = installChannel,
					installState = installState
				)
			}
		}
	}

	fun close() {
		installerScope.cancel()
		installChannel.close()
	}

	suspend fun addToInstallQueue(installItem: InstallItem) {
		installChannel.send(installItem)
	}

	private fun CoroutineScope.installer(
		installerType: InstallerType,
		installItems: Channel<InstallItem>,
		installState: Flow<InstallItemState>
	) = launch {
		installItems.consumeEach {
			when (installerType) {
				InstallerType.LEGACY -> TODO()
				InstallerType.SESSION -> TODO()
				InstallerType.SHIZUKU -> TODO()
				InstallerType.ROOT -> TODO()
			}
		}
	}
}
