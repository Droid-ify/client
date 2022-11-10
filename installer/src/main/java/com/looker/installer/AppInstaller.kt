package com.looker.installer

import android.content.Context
import com.looker.core.datastore.model.InstallerType
import com.looker.installer.installer.LegacyInstaller
import com.looker.installer.installer.RootInstaller
import com.looker.installer.installer.SessionInstaller
import com.looker.installer.installer.ShizukuInstaller
import com.looker.installer.utils.BaseInstaller

abstract class AppInstaller {
	abstract val defaultInstaller: BaseInstaller?

	companion object {
		@Volatile
		private var INSTANCE: AppInstaller? = null
		fun getInstance(context: Context?, installerType: InstallerType): AppInstaller? {
			if (INSTANCE != null) return INSTANCE
			synchronized(AppInstaller::class.java) {
				context?.let {
					INSTANCE = object : AppInstaller() {
						override val defaultInstaller: BaseInstaller
							get() {
								return when (installerType) {
									InstallerType.LEGACY -> LegacyInstaller(it)
									InstallerType.SESSION -> SessionInstaller(it)
									InstallerType.SHIZUKU -> ShizukuInstaller(it)
									InstallerType.ROOT -> RootInstaller(it)
								}
							}
					}
				}
			}
			return INSTANCE
		}
	}
}
