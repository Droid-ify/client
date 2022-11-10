package com.looker.core.data.utils

import kotlinx.coroutines.flow.Flow

interface SyncStatusMonitor {
	val isSyncing: Flow<Boolean>
}