package com.looker.core.common

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
inline fun sdkAbove(sdk: Int, onSuccessful: () -> Unit) {
    if (Build.VERSION.SDK_INT >= sdk) onSuccessful()
}

object SdkCheck {

    val sdk: Int
        get() = Build.VERSION.SDK_INT

    // Allows auto install if target sdk of apk is one less then current sdk
    fun canAutoInstall(targetSdk: Int) = targetSdk >= sdk - 1 && isSnowCake

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isTiramisu: Boolean = sdk >= Build.VERSION_CODES.TIRAMISU

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    val isR: Boolean = sdk >= Build.VERSION_CODES.R

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val isPie: Boolean = sdk >= Build.VERSION_CODES.P

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    val isOreo: Boolean = sdk >= Build.VERSION_CODES.O

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val isSnowCake: Boolean = sdk >= Build.VERSION_CODES.S

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    val isNougat: Boolean = sdk >= Build.VERSION_CODES.N
}
