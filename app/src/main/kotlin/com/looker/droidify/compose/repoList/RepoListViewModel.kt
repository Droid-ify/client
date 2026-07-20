package com.looker.droidify.compose.repoList

import androidx.lifecycle.ViewModel
import com.looker.droidify.data.model.Repo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RepoListViewModel @Inject constructor() : ViewModel() {

    // TODO(sqldelight): reimplement with SQLDelight-backed repository
    val stream: StateFlow<List<Repo>> = MutableStateFlow(emptyList())

    fun toggleRepo(repo: Repo) {
        // TODO(sqldelight): reimplement with SQLDelight-backed repository
    }

    fun deleteRepo(repoId: Int) {
        // TODO(sqldelight): reimplement with SQLDelight-backed repository
    }
}
