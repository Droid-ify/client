package com.looker.installer.model

import androidx.annotation.IntDef

@IntDef(
	STATE_IDLE,
	STATE_PENDING,
	STATE_INSTALLING,
	STATE_INSTALLED,
	STATE_ERROR
)
@Retention(AnnotationRetention.SOURCE)
annotation class InstallerState

const val STATE_IDLE = 413
const val STATE_PENDING = 523
const val STATE_INSTALLING = 654
const val STATE_INSTALLED = 127
const val STATE_ERROR = 375