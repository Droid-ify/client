package com.looker.droidify.utility.http

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Call.await(recordStack: Boolean = isRecordStack): Response {
	val callStack = if (recordStack) {
		IOException().apply {
			// Remove unnecessary lines from stacktrace
			// This doesn't remove await$default, but better than nothing
			stackTrace = stackTrace.copyOfRange(1, stackTrace.size)
		}
	} else {
		null
	}
	return suspendCancellableCoroutine { continuation ->
		enqueue(object : Callback {
			override fun onResponse(call: Call, response: Response) {
				continuation.resume(response)
			}

			override fun onFailure(call: Call, e: IOException) {
				// Don't bother with resuming the continuation if it is already cancelled.
				if (continuation.isCancelled) return
				callStack?.initCause(e)
				continuation.resumeWithException(callStack ?: e)
			}
		})

		continuation.invokeOnCancellation {
			try {
				cancel()
			} catch (ex: Throwable) {
				//Ignore cancel exception
			}
		}
	}
}