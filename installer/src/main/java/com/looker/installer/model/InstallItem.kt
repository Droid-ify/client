package com.looker.installer.model

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.*

data class InstallItem(
	val name: String,
	val packageName: String,
	val fileUri: Uri,
	val file: File,
	val id: String = UUID.randomUUID().toString()
) {
	private val _state = MutableStateFlow<InstallerState>(InstallerState.Queued)
	val state = _state.asStateFlow()

	suspend fun updateState(state: InstallerState) = _state.emit(state)
}
