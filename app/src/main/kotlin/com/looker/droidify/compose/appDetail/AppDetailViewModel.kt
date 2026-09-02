package com.looker.droidify.compose.appDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.looker.droidify.data.model.App
import com.looker.droidify.data.model.Package
import com.looker.droidify.data.model.Repo
import com.looker.droidify.datastore.CustomButtonRepository
import com.looker.droidify.datastore.model.CustomButton
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val customButtonRepository: CustomButtonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val packageName: String = requireNotNull(savedStateHandle["packageName"]) {
        "Required argument 'packageName' was not found in SavedStateHandle"
    }

    val customButtons: StateFlow<List<CustomButton>> = customButtonRepository.buttons
        .asStateFlow(emptyList())

    // TODO(sqldelight): reimplement with SQLDelight-backed repository
    val state: StateFlow<AppDetailState> = MutableStateFlow(AppDetailState.Loading)
}

sealed interface AppDetailState {
    object Loading : AppDetailState
    data class Error(val message: String) : AppDetailState
    data class Success(
        val app: App,
        val packages: List<Pair<Package, Repo>>,
    ) : AppDetailState
}
