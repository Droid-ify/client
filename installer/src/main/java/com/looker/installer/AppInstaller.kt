package com.looker.installer

import android.content.Context
import com.looker.installer.installer.LegacyInstaller
import com.looker.installer.installer.RootInstaller
import com.looker.installer.installer.SessionInstaller
import com.looker.installer.installer.ShizukuInstaller
import com.looker.installer.model.*
import com.looker.installer.utils.BaseInstaller

abstract class AppInstaller {
	abstract val defaultInstaller: BaseInstaller?

	companion object {
		@Volatile
		private var INSTANCE: AppInstaller? = null
		fun getInstance(context: Context?, @InstallerType installerType: Int): AppInstaller? {
			if (INSTANCE == null) {
				synchronized(AppInstaller::class.java) {
					context?.let {
						INSTANCE = object : AppInstaller() {
							override val defaultInstaller: BaseInstaller
								get() {
									return when (installerType) {
										TYPE_SHIZUKU -> ShizukuInstaller(it)
										TYPE_SESSION -> SessionInstaller(it)
										TYPE_ROOT -> RootInstaller(it)
										TYPE_LEGACY -> LegacyInstaller(it)
										else -> SessionInstaller(it)
									}
								}
						}
					}
				}
			}
			return INSTANCE
		}
	}
}
