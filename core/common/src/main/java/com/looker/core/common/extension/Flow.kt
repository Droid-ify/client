package com.looker.core.common.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce

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