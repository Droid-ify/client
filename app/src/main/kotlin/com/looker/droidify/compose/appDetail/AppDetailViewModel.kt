package com.looker.droidify.compose.appDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.looker.droidify.data.AppRepository
import com.looker.droidify.data.model.App
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val appRepository: AppRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val packageName: String = requireNotNull(savedStateHandle["packageName"]) {
        "Required argument 'packageName' was not found in SavedStateHandle"
    }

    val state: StateFlow<AppDetailUiState> = appRepository
        .getApp(PackageName(packageName))
        .map { apps ->
            val suggested = apps.maxByOrNull { it.metadata.suggestedVersionCode }
            AppDetailUiState(app = suggested)
        }
        .asStateFlow(AppDetailUiState())
}

data class AppDetailUiState(
    val app: App? = null,
)
