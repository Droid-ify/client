package com.looker.installer.model

import androidx.annotation.IntDef

@IntDef(
	TYPE_LEGACY,
	TYPE_SESSION,
	TYPE_ROOT,
	TYPE_SHIZUKU
)
@Retention(AnnotationRetention.SOURCE)
annotation class InstallerType

const val TYPE_LEGACY = 145
const val TYPE_SESSION = 435
const val TYPE_ROOT = 563
const val TYPE_SHIZUKU = 234