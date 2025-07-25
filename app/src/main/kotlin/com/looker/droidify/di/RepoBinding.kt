package com.looker.droidify.di

import com.looker.droidify.data.AppRepository
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.data.local.repos.LocalAppRepository
import com.looker.droidify.data.local.repos.LocalRepoRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoBinding {

    @Singleton
    abstract fun bindAppRepository(local: LocalAppRepository): AppRepository

    @Singleton
    abstract fun bindRepoRepository(local: LocalRepoRepository): RepoRepository

}
