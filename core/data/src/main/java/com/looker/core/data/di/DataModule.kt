package com.looker.core.data.di

import com.looker.core.domain.AppRepository
import com.looker.core.domain.RepoRepository
import com.looker.core.data.repository.OfflineFirstAppRepository
import com.looker.core.data.repository.OfflineFirstRepoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindsAppRepository(
        appRepository: OfflineFirstAppRepository
    ): AppRepository

    @Binds
    fun bindsRepoRepository(
        repoRepository: OfflineFirstRepoRepository
    ): RepoRepository

}
