package com.looker.sync.di

import com.looker.core.data.utils.SyncStatusMonitor
import com.looker.sync.status.WorkManagerSyncStatusMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {
	@Binds
	fun bindsSyncStatusMonitor(
		syncStatusMonitor: WorkManagerSyncStatusMonitor
	): SyncStatusMonitor
}