package com.looker.installer

import android.content.Context
import com.looker.core.common.extension.filter
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.model.InstallerType
import com.looker.core.model.newer.PackageName
import com.looker.installer.installers.BaseInstaller
import com.looker.installer.installers.LegacyInstaller
import com.looker.installer.installers.RootInstaller
import com.looker.installer.installers.SessionInstaller
import com.looker.installer.installers.ShizukuInstaller
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallItemState
import com.looker.installer.model.InstallState
import com.looker.installer.model.statesTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class Installer(
	private val context: Context, private val userPreferencesRepository: UserPreferencesRepository
) {
	private val installItems = Channel<InstallItem>()
	private val uninstallItems = Channel<PackageName>()
	private val installState = MutableStateFlow(InstallItemState.EMPTY)
	private var baseInstaller: BaseInstaller? = null

	suspend operator fun invoke() = coroutineScope {
		baseInstaller = when (userPreferencesRepository.fetchInitialPreferences().installerType) {
			InstallerType.LEGACY -> LegacyInstaller(context)
			InstallerType.SESSION -> SessionInstaller(context)
			InstallerType.SHIZUKU -> ShizukuInstaller(context)
			InstallerType.ROOT -> RootInstaller(context)
		}
		installer(
			baseInstaller = baseInstaller!!,
			installItems = installItems,
			installState = installState
		)
		uninstaller(
			baseInstaller = baseInstaller!!, uninstallItems = uninstallItems
		)

	}

	fun close() {
		baseInstaller?.cleanup()
		baseInstaller = null
		uninstallItems.close()
		installItems.close()
	}

	suspend operator fun plus(installItem: InstallItem) {
		installItems.send(installItem)
	}

	suspend operator fun minus(packageName: PackageName) {
		uninstallItems.send(packageName)
	}

	infix fun stateOf(packageName: PackageName): Flow<InstallState> =
		installState.filter { it.installedItem.packageName == packageName }.map { it.state }

	private fun CoroutineScope.installer(
		baseInstaller: BaseInstaller,
		installItems: ReceiveChannel<InstallItem>,
		installState: MutableStateFlow<InstallItemState>
	) = launch {
		val requested = mutableSetOf<String>()
		filter(installItems) {
			val shouldProcess = requested.add(it.packageName.name)
			if (shouldProcess) installState.emit(it statesTo InstallState.Queued)
			shouldProcess
		}.consumeEach {
			installState.emit(it statesTo InstallState.Installing)
			val success = baseInstaller.performInstall(it)
			installState.emit(it statesTo success)
			requested.remove(it.packageName.name)
		}
	}

	private fun CoroutineScope.uninstaller(
		baseInstaller: BaseInstaller, uninstallItems: ReceiveChannel<PackageName>
	) = launch {
		uninstallItems.consumeEach {
			baseInstaller.performUninstall(it)
		}
	}
}
