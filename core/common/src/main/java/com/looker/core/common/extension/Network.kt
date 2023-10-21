package com.looker.core.common.extension

import androidx.core.net.toUri

val String.isOnion: Boolean
    get() = toUri().host?.endsWith(".onion") == true
