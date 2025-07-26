package com.looker.droidify.utility.common

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
inline fun sdkAbove(sdk: Int, onSuccessful: () -> Unit) {
    if (Build.VERSION.SDK_INT >= sdk) onSuccessful()
}

object SdkCheck {

    val sdk: Int = Build.VERSION.SDK_INT

    // Allows auto install if target sdk of apk is one less then current sdk
    fun canAutoInstall(targetSdk: Int) = targetSdk >= sdk - 1 && isSnowCake

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isTiramisu: Boolean get() = sdk >= Build.VERSION_CODES.TIRAMISU

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    val isR: Boolean get() = sdk >= Build.VERSION_CODES.R

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val isPie: Boolean get() = sdk >= Build.VERSION_CODES.P

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    val isOreo: Boolean get() = sdk >= Build.VERSION_CODES.O

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val isSnowCake: Boolean get() = sdk >= Build.VERSION_CODES.S

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    val isNougat: Boolean get() = sdk >= Build.VERSION_CODES.N
}

val sdkName by lazy {
    mapOf(
        1 to "1.0"
        2 to "1.1"
        3 to "1.5"
        4 to "1.6"
        5 to "2.0"
        6 to "2.0.1"
        7 to "2.1"
        8 to "2.2"
        9 to "2.3.0"
        10 to "2.3.3"
        11 to "3.0"
        12 to "3.1"
        13 to "3.2"
        14 to "4.0.1"
        15 to "4.0.3"
        16 to "4.1",
        17 to "4.2",
        18 to "4.3",
        19 to "4.4",
        21 to "5.0",
        22 to "5.1",
        23 to "6",
        24 to "7.0",
        25 to "7.1",
        26 to "8.0",
        27 to "8.1",
        28 to "9",
        29 to "10",
        30 to "11",
        31 to "12",
        32 to "12L",
        33 to "13",
        34 to "14",
        35 to "15",
        36 to "16",
    )
}
