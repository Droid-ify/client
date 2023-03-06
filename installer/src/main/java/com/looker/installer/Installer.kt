package com.looker.installer

import android.content.Context
import com.looker.core.common.Constants
import com.looker.core.common.extension.filter
import com.looker.core.common.extension.notificationManager
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
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class Installer(
	private val context: Context,
	private val userPreferencesRepository: UserPreferencesRepository
) {
	private val installItems = Channel<InstallItem>()
	private val uninstallItems = Channel<PackageName>()
	private val installState = MutableStateFlow(InstallItemState.EMPTY)
	private val installQueue = MutableStateFlow(emptySet<String>())
	private var baseInstaller: BaseInstaller? = null

	suspend operator fun invoke() = coroutineScope {
		baseInstaller =
			when (userPreferencesRepository.fetchInitialPreferences().installerType) {
				InstallerType.LEGACY -> LegacyInstaller(context)
				InstallerType.SESSION -> SessionInstaller(context)
				InstallerType.SHIZUKU -> ShizukuInstaller(context)
				InstallerType.ROOT -> RootInstaller(context)
			}
		installer(
			context = context,
			baseInstaller = baseInstaller!!,
			installItems = installItems,
			installQueue = installQueue,
			installState = installState
		)
		uninstaller(
			baseInstaller = baseInstaller!!,
			uninstallItems = uninstallItems
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

	fun getStatus() = combine(installState, installQueue) { current, queue ->
		InstallerQueueState(
			currentItem = current,
			queued = queue
		)
	}

	private fun CoroutineScope.installer(
		context: Context,
		baseInstaller: BaseInstaller,
		installItems: ReceiveChannel<InstallItem>,
		installQueue: MutableStateFlow<Set<String>>,
		installState: MutableStateFlow<InstallItemState>
	) = launch {
		val requested = mutableSetOf<String>()
		filter(installItems) { item ->
			val isAdded = requested.add(item.packageName.name)
			if (isAdded) {
				installQueue.update {
					val newSet = it.toMutableSet()
					newSet.add(item.packageName.name)
					newSet
				}
			}
			isAdded
		}.consumeEach { item ->
			installQueue.update {
				val newSet = it.toMutableSet()
				newSet.remove(item.packageName.name)
				newSet
			}
			installState.emit(item statesTo InstallState.Installing)
			val success = async { baseInstaller.performInstall(item) }
			installState.emit(item statesTo success.await())
			requested.remove(item.packageName.name)
			context.notificationManager.cancel(
				"download-${item.packageName.name}",
				Constants.NOTIFICATION_ID_DOWNLOADING
			)
		}
	}

	private fun CoroutineScope.uninstaller(
		baseInstaller: BaseInstaller,
		uninstallItems: ReceiveChannel<PackageName>
	) = launch {
		uninstallItems.consumeEach {
			baseInstaller.performUninstall(it)
		}
	}
}

data class InstallerQueueState(
	val currentItem: InstallItemState,
	val queued: Set<String>
) {
	companion object {
		val EMPTY = InstallerQueueState(InstallItemState.EMPTY, emptySet())
	}
}
