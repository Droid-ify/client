package com.looker.droidify.utility.common.device

object Huawei {
    val isHuaweiEmui: Boolean
        get() {
            return try {
                Class.forName("com.huawei.android.os.BuildEx")
                true
            } catch (e: Exception) {
                false
            }
        }
}
