package com.looker.droidify.ui.repository

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.asStateFlow
import com.looker.droidify.model.Repository
import com.looker.droidify.database.Database
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RepositoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val id: Long = savedStateHandle[ARG_REPO_ID] ?: -1

    private val repoStream = Database.RepositoryAdapter.getStream(id)

    private val countStream = Database.ProductAdapter.getCountStream(id)

    val state = combine(repoStream, countStream) { repo, count ->
        RepositoryPageItem(repo, count)
    }.asStateFlow(RepositoryPageItem())

    private val syncConnection = Connection(SyncService::class.java)

    fun bindService(context: Context) {
        syncConnection.bind(context)
    }

    fun unbindService(context: Context) {
        syncConnection.unbind(context)
    }

    fun enabledRepository(enable: Boolean) {
        viewModelScope.launch {
            val repo = repoStream.first { it != null }!!
            syncConnection.binder?.setEnabled(repo, enable)
        }
    }

    fun deleteRepository(onDelete: () -> Unit) {
        if (syncConnection.binder?.deleteRepository(id) == true) {
            onDelete()
        }
    }

    companion object {
        const val ARG_REPO_ID = "repo_id"
    }
}

data class RepositoryPageItem(
    val repo: Repository? = null,
    val appCount: Int = 0
)
