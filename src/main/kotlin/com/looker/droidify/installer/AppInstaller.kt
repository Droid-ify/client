package com.looker.droidify.installer

import android.content.Context
import com.looker.droidify.utility.Utils.rootInstallerEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class AppInstaller {
    abstract val defaultInstaller: BaseInstaller?

    companion object {
        @Volatile
        private var INSTANCE: AppInstaller? = null
        suspend fun getInstance(context: Context?): AppInstaller? {
            if (INSTANCE == null) {
                withContext(Dispatchers.IO) {
                    synchronized(AppInstaller::class.java) {
                        if (INSTANCE == null) {
                            context?.let {
                                INSTANCE = object : AppInstaller() {
                                    override val defaultInstaller: BaseInstaller
                                        get() {
                                            return when (rootInstallerEnabled) {
                                                false -> DefaultInstaller(it)
                                                true -> RootInstaller(it)
                                            }
                                        }
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
