package com.looker.droidify.ui.viewmodels

import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import com.looker.droidify.content.Preferences
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku

private const val SHIZUKU_PERMISSION_REQUEST_CODE = 87263

class SettingsViewModel : ViewModel() {

	private val shizukuDeadListener = Shizuku.OnBinderDeadListener {
		Log.e("ShizukuInstaller", "Killed")
	}

	private val shizukuPermissionListener = object : Shizuku.OnRequestPermissionResultListener {
		override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
			if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
				if (grantResult == PackageManager.PERMISSION_GRANTED) {
				} else {
					Preferences[Preferences.Key.InstallerType] =
						Preferences.InstallerType.Session
				}
				Shizuku.removeRequestPermissionResultListener(this)
			}
		}
	}

	init {
		Shizuku.addBinderDeadListener(shizukuDeadListener)
	}

	fun installerSelected(installer: Preferences.InstallerType) {
		when (installer) {
			Preferences.InstallerType.Root -> {
				onRootSelected()
			}
			Preferences.InstallerType.Shizuku -> {
				onShizukuSelected()
			}
			else -> {}
		}
	}

	private fun onRootSelected() {
		val hasRoot = Shell.getCachedShell()?.isRoot
			?: Shell.getShell().isRoot
		if (!hasRoot) {
			Preferences[Preferences.Key.InstallerType] =
				Preferences.InstallerType.Session
		}
	}

	private fun onShizukuSelected() {
		if (Shizuku.pingBinder()) {
			if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
			} else {
				Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
				Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
			}
		}
	}

	override fun onCleared() {
		Shizuku.removeBinderDeadListener(shizukuDeadListener)
		Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
		super.onCleared()
	}
}