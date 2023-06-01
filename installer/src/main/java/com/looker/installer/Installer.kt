package com.looker.installer

import android.content.Context
import com.looker.core.common.Constants
import com.looker.core.common.extension.filter
import com.looker.core.common.extension.notificationManager
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.distinctMap
import com.looker.core.datastore.model.InstallerType
import com.looker.core.model.newer.PackageName
import com.looker.installer.installers.*
import com.looker.installer.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Installer(
	private val context: Context,
	userPreferencesRepository: UserPreferencesRepository
) {

	private val installItems = Channel<InstallItem>()
	private val uninstallItems = Channel<PackageName>()

	private val installState = MutableStateFlow(InstallItemState.EMPTY)
	private val installQueue = MutableStateFlow(emptySet<String>())

	private var _baseInstaller: BaseInstaller? = null
	private val baseInstaller: BaseInstaller get() = _baseInstaller!!

	private val lock = Mutex()
	private val installerPreference =
		userPreferencesRepository.userPreferencesFlow.distinctMap { it.installerType }

	suspend operator fun invoke() = coroutineScope {
		launch {
			installerPreference.collectLatest(::setInstaller)
		}
		installer()
		uninstaller()
	}

	fun close() {
		baseInstaller.cleanup()
		_baseInstaller = null
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

	private fun CoroutineScope.installer() = launch {
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
			val success = withTimeoutOrNull(20_000) {
				baseInstaller.performInstall(item)
			}
			if (success == null) {
				installState.emit(item statesTo InstallState.Failed)
			} else {
				installState.emit(item statesTo success)
			}
			requested.remove(item.packageName.name)
			context.notificationManager.cancel(
				"download-${item.packageName.name}",
				Constants.NOTIFICATION_ID_DOWNLOADING
			)
		}
	}

	private fun CoroutineScope.uninstaller() = launch {
		uninstallItems.consumeEach {
			baseInstaller.performUninstall(it)
		}
	}

	private suspend fun setInstaller(installerType: InstallerType) {
		lock.withLock {
			_baseInstaller = when (installerType) {
				InstallerType.LEGACY -> LegacyInstaller(context)
				InstallerType.SESSION -> SessionInstaller(context)
				InstallerType.SHIZUKU -> ShizukuInstaller(context)
				InstallerType.ROOT -> RootInstaller(context)
			}
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
