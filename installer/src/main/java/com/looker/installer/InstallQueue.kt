package com.looker.installer

import android.content.Context
import com.looker.core.datastore.model.InstallerType
import com.looker.installer.model.InstallEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach

/**
 *
 * This would be the only way to access installer and will also work as a queue manager
 *
 * This would provide a singleton so that only one instance is present in one instance of application
 *
 * lateinit var installQueue: InstallQueue
 *
 * You can pass [InstallEvent] in [enqueue] with the subsequent [InstallerType]
 *
 * You can init the installers by doing [installQueue()]
 */
class InstallQueue(context: Context) {

	private val queue: Channel<InstallInput> = Channel(50)

	suspend operator fun invoke() {
		queue.consumeEach { processEvent(it) }
	}

	suspend fun enqueue(installerType: InstallerType, vararg events: InstallEvent) {
		events.forEach { queue.send(InstallInput(it, installerType)) }
	}

	suspend fun enqueue(installerType: InstallerType, events: List<InstallEvent>) {
		enqueue(installerType, *events.toTypedArray())
	}

	private suspend fun processEvent(input: InstallInput) {
		// TODO: LMAO where is the installer
	}
}

private data class InstallInput(val event: InstallEvent, val installerType: InstallerType)