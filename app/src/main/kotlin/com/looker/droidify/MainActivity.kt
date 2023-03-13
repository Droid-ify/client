package com.looker.droidify

import android.content.Intent
import com.looker.core.common.extension.getPackageName
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ScreenActivity() {
	companion object {
		const val ACTION_UPDATES = "${BuildConfig.APPLICATION_ID}.intent.action.UPDATES"
		const val ACTION_INSTALL = "${BuildConfig.APPLICATION_ID}.intent.action.INSTALL"
		const val EXTRA_CACHE_FILE_NAME =
			"${BuildConfig.APPLICATION_ID}.intent.extra.CACHE_FILE_NAME"
	}

	override fun handleIntent(intent: Intent?) {
		when (intent?.action) {
			ACTION_UPDATES -> handleSpecialIntent(SpecialIntent.Updates)
			ACTION_INSTALL -> handleSpecialIntent(
				SpecialIntent.Install(
					intent.getPackageName(),
					intent.getStringExtra(EXTRA_CACHE_FILE_NAME)
				)
			)
			else -> super.handleIntent(intent)
		}
	}
}