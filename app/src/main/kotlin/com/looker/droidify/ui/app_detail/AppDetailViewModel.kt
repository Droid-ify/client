package com.looker.droidify.ui.app_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.installer.Installer
import com.looker.installer.model.InstallerQueueState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
	installer: Installer
) : ViewModel(){

	val installerState = installer.getStatus().stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = InstallerQueueState.EMPTY
	)

}