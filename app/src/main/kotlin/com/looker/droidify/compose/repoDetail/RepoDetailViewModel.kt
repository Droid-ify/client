package com.looker.droidify.compose.repoDetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.looker.droidify.compose.repoDetail.navigation.RepoDetail
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class RepoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repoRepository: RepoRepository,
) : ViewModel() {

    private val route: RepoDetail = savedStateHandle.toRoute()
    val repoId = route.repoId

    val repo = repoRepository.repo(repoId).asStateFlow(null)

    private val syncConnection = Connection(SyncService::class.java)

    fun bindService(context: Context) {
        syncConnection.bind(context)
    }

    fun unbindService(context: Context) {
        syncConnection.unbind(context)
    }

    fun enableRepository(enable: Boolean) {
        viewModelScope.launch {
            repo.value?.let { repoRepository.enableRepository(it, enable) }
        }
    }

    fun deleteRepository(onDelete: () -> Unit) {
        viewModelScope.launch {
            repoRepository.deleteRepo(repoId)
            onDelete()
        }
    }
}
