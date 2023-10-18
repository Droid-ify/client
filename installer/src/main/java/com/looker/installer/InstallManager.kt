package com.looker.installer

import android.content.Context
import com.looker.core.common.Constants
import com.looker.core.common.PackageName
import com.looker.core.common.extension.*
import com.looker.core.datastore.SettingsRepository
import com.looker.core.datastore.model.InstallerType
import com.looker.installer.installers.Installer
import com.looker.installer.installers.LegacyInstaller
import com.looker.installer.installers.root.RootInstaller
import com.looker.installer.installers.session.SessionInstaller
import com.looker.installer.installers.shizuku.ShizukuInstaller
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// TODO: Add cancellation and fix the stuck state
class InstallManager(
	private val context: Context,
	settingsRepository: SettingsRepository
) {

	private val installItems = Channel<InstallItem>()
	private val uninstallItems = Channel<PackageName>()

	val state = MutableStateFlow<Map<PackageName, InstallState>>(emptyMap())

	private var _installer: Installer? = null
		set(value) {
			field?.cleanup()
			field = value
		}
	private val installer: Installer get() = _installer!!

	private val lock = Mutex()
	private val installerPreference = settingsRepository.get { installerType }

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

	suspend infix fun install(installItem: InstallItem) {
		installItems.send(installItem)
	}

	suspend infix fun uninstall(packageName: PackageName) {
		uninstallItems.send(packageName)
	}

	infix fun remove(packageName: PackageName) {
		updateState { remove(packageName) }
	}

	private fun CoroutineScope.setupInstaller() = launch {
		installerPreference.collectLatest(::setInstaller)
	}

	private fun CoroutineScope.installer() = launch {
		val currentQueue = mutableSetOf<String>()
		installItems.filter { item ->
			currentQueue.addAndCompute(item.packageName.name) { isAdded ->
				if (isAdded) {
					updateState { put(item.packageName, InstallState.Pending) }
				}
			}
		}.consumeEach { item ->
			if (state.value[item.packageName] != null) {
				updateState { put(item.packageName, InstallState.Installing) }
				val success = installer.install(item)
				updateState { put(item.packageName, success) }
				currentQueue.remove(item.packageName.name)
				context.notificationManager?.cancel(
					"download-${item.packageName.name}",
					Constants.NOTIFICATION_ID_DOWNLOADING
				)
			}
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

	private inline fun updateState(block: MutableMap<PackageName, InstallState>.() -> Unit) {
		state.update { it.updateAsMutable(block) }
	}
}
