package com.looker.droidify.compose.repoList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.data.model.Repo
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class RepoListViewModel @Inject constructor(
    private val repository: RepoRepository,
) : ViewModel() {

    val stream = repository.repos
        .asStateFlow(emptyList())

    fun toggleRepo(repo: Repo) {
        viewModelScope.launch {
            repository.enableRepository(repo, !repo.enabled)
        }
    }

    fun deleteRepo(repoId: Int) {
        viewModelScope.launch {
            repository.deleteRepo(repoId)
        }
    }
}
