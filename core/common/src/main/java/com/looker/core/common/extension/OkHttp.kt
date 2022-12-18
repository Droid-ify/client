package com.looker.core.common.extension

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
	enqueue(
		object : Callback {
			override fun onResponse(call: Call, response: Response) {
				continuation.resume(response)
			}

			override fun onFailure(call: Call, e: IOException) {
				if (continuation.isCancelled) return
				continuation.resumeWithException(e)
			}
		}
	)

	continuation.invokeOnCancellation { cancel() }
}