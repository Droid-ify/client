package com.looker.droidify.di

import com.looker.droidify.data.AppRepository
import com.looker.droidify.data.InstalledRepository
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.data.local.repos.LocalAppRepository
import com.looker.droidify.data.local.repos.LocalInstalledRepository
import com.looker.droidify.data.local.repos.LocalRepoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoBinding {

    @Binds
    abstract fun bindAppRepository(local: LocalAppRepository): AppRepository

    @Binds
    abstract fun bindRepoRepository(local: LocalRepoRepository): RepoRepository

    @Binds
    abstract fun bindInstalledRepository(local: LocalInstalledRepository): InstalledRepository

}
