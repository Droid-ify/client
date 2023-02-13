package com.looker.core.common.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> CoroutineScope.filter(
	channel: ReceiveChannel<T>,
	block: suspend (T) -> Boolean
): ReceiveChannel<T> = produce(capacity = Channel.UNLIMITED) {
	for (item in channel) {
		if (block(item)) send(item)
	}
}