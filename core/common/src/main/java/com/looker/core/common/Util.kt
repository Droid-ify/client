package com.looker.core.common

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
inline fun sdkAbove(sdk: Int, onSuccessful: () -> Unit) {
	if (Build.VERSION.SDK_INT >= sdk) onSuccessful()
}

object Util {

	private val sdk: Int
		get() = Build.VERSION.SDK_INT

	@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
	val isTiramisu: Boolean
		get() = sdk >= Build.VERSION_CODES.TIRAMISU

	@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
	val isPie: Boolean
		get() = sdk >= Build.VERSION_CODES.P

	@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
	val isOreo: Boolean
		get() = sdk >= Build.VERSION_CODES.O

	@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
	val isSnowCake: Boolean
		get() = sdk >= Build.VERSION_CODES.S

	@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
	val isNougat: Boolean
		get() = sdk >= Build.VERSION_CODES.N
}