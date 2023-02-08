package com.looker.core.common.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

fun <T> Flow<T>.stateIn(
	scope: CoroutineScope,
	initial: T,
	started: SharingStarted = SharingStarted.WhileSubscribed(5_000)
): StateFlow<T> = stateIn(
	scope = scope,
	initialValue = initial,
	started = started
)

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> CoroutineScope.onEach(
	channel: ReceiveChannel<T>,
	block: suspend (T) -> Unit
): ReceiveChannel<T> = produce(capacity = Channel.UNLIMITED) {
	for (item in channel) {
		block(item)
		send(item)
	}
}