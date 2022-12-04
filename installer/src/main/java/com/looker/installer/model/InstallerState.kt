package com.looker.installer.model

import androidx.annotation.IntRange
import androidx.annotation.StringRes

sealed interface InstallerState {

	object Queued : InstallerState

	data class Installing(
		@IntRange(from = 0L, to = 100L)
		val percent: Int
	) : InstallerState

	object Installed : InstallerState

	data class Error(@StringRes val message: Int) : InstallerState

}