package com.looker.droidify.compose.repoDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.looker.droidify.compose.repoDetail.navigation.RepoDetail
import com.looker.droidify.data.model.Repo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RepoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route: RepoDetail = savedStateHandle.toRoute()
    val repoId = route.repoId

    // TODO(sqldelight): reimplement with SQLDelight-backed repository
    val repo: StateFlow<Repo?> = MutableStateFlow(null)

    fun enableRepository(enable: Boolean) {
        // TODO(sqldelight): reimplement with SQLDelight-backed repository
    }

    fun deleteRepository(onDelete: () -> Unit) {
        // TODO(sqldelight): reimplement with SQLDelight-backed repository
    }
}
