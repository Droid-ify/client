package com.looker.droidify

import android.content.Context
import android.content.Intent
import com.looker.droidify.ContextWrapperX.Companion.wrap
import com.looker.droidify.screen.ScreenActivity

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
                    intent.packageName,
                    intent.getStringExtra(EXTRA_CACHE_FILE_NAME)
                )
            )
            else -> super.handleIntent(intent)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(wrap(newBase))
    }
}
