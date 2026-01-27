package com.looker.droidify.ui.repository

import android.os.Handler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.looker.droidify.database.createRepositoryPagingSource
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.ignoreSignatureFlow
import com.looker.droidify.di.IoDispatcher
import com.looker.droidify.di.MainHandler
import com.looker.droidify.model.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RepositoriesViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    @MainHandler
    mainHandler: Handler,
    @IoDispatcher
    ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    @JvmField
    val listFlow: Flow<PagingData<Repository>> = settingsRepository.ignoreSignatureFlow()
        .flowOn(ioDispatcher)
        .flatMapLatest {
            Pager(
                config = PagingConfig(
                    pageSize = 60,
                    enablePlaceholders = false,
                ),
                pagingSourceFactory = {
                    createRepositoryPagingSource(mainHandler, ioDispatcher)
                },
            ).flow
        }.cachedIn(viewModelScope)
}
