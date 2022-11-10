package com.looker.core.data.di

import com.looker.core.data.utils.ConnectivityManagerNetworkMonitor
import com.looker.core.data.utils.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

	@Binds
	fun bindNetworkMonitor(
		networkMonitor: ConnectivityManagerNetworkMonitor
	): NetworkMonitor

}