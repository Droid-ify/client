package com.looker.core.common

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@ChecksSdkIntAtLeast(parameter = 0, lambda = 2)
inline fun <T> sdkAbove(sdk: Int, orElse: () -> T, onSuccessful: () -> T): T =
	if (Build.VERSION.SDK_INT >= sdk) onSuccessful()
	else orElse()

@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
inline fun sdkAbove(sdk: Int, onSuccessful: () -> Unit) =
	sdkAbove(sdk = sdk, orElse = {}, onSuccessful = onSuccessful)