package com.looker.core_common.device

import android.annotation.SuppressLint
import android.util.Log

object Miui {
	val isMiui by lazy {
		getSystemProperty("ro.miui.ui.version.name")?.isNotEmpty() ?: false
	}

	@SuppressLint("PrivateApi")
	fun isMiuiOptimizationDisabled(): Boolean {
		val sysProp = getSystemProperty("persist.sys.miui_optimization")
		if (sysProp == "0" || sysProp == "false") {
			return true
		}

		return try {
			Class.forName("android.miui.AppOpsUtils")
				.getDeclaredMethod("isXOptMode")
				.invoke(null) as Boolean
		} catch (e: Exception) {
			false
		}
	}

	@SuppressLint("PrivateApi")
	private fun getSystemProperty(key: String?): String? {
		return try {
			Class.forName("android.os.SystemProperties")
				.getDeclaredMethod("get", String::class.java)
				.invoke(null, key) as String
		} catch (e: Exception) {
			Log.e("Miui", "Unable to use SystemProperties.get()", e)
			null
		}
	}
}
