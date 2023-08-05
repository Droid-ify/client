package com.looker.installer

import android.content.Context
import com.looker.core.common.Constants
import com.looker.core.common.extension.filter
import com.looker.core.common.extension.notificationManager
import com.looker.core.common.extension.updateAsMutable
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

class InstallManager(
	private val context: Context,
	userPreferencesRepository: UserPreferencesRepository
) {

	private val installItems = Channel<InstallItem>()
	private val uninstallItems = Channel<PackageName>()

	private val installState = MutableStateFlow(InstallItemState.EMPTY)
	private val installQueue = MutableStateFlow(emptySet<String>())

	private var _installer: Installer? = null
		set(value) {
			field?.cleanup()
			field = value
		}
	private val installer: Installer get() = _installer!!

	private val lock = Mutex()
	private val installerPreference = userPreferencesRepository
		.userPreferencesFlow
		.distinctMap { it.installerType }

	suspend operator fun invoke() = coroutineScope {
		setupInstaller()
		installer()
		uninstaller()
	}

	fun close() {
		_installer = null
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

	private fun CoroutineScope.setupInstaller() = launch {
		installerPreference.collectLatest(::setInstaller)
	}

	private fun CoroutineScope.installer() = launch {
		val currentQueue = mutableSetOf<String>()
		filter(installItems) { item ->
			val isAdded = lock.withLock { currentQueue.add(item.packageName.name) }
			if (isAdded) {
				installQueue.update {
					it.updateAsMutable { add(item.packageName.name) }
				}
			}
			isAdded
		}.consumeEach { item ->
			installQueue.update {
				it.updateAsMutable { remove(item.packageName.name) }
			}
			installState.emit(item statesTo InstallState.Installing)
			val success = withTimeoutOrNull(20_000) {
				installer.install(item)
			} ?: InstallState.Failed
			installState.emit(item statesTo success)
			lock.withLock { currentQueue.remove(item.packageName.name) }
			context.notificationManager.cancel(
				"download-${item.packageName.name}",
				Constants.NOTIFICATION_ID_DOWNLOADING
			)
		}
	}

	private fun CoroutineScope.uninstaller() = launch {
		uninstallItems.consumeEach {
			installer.uninstall(it)
		}
	}

	private suspend fun setInstaller(installerType: InstallerType) {
		lock.withLock {
			_installer = when (installerType) {
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
