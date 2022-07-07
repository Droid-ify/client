package com.looker.droidify.utility.extension

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Take from https://github.com/gildor/kotlin-coroutines-okhttp
suspend fun Call.await(recordStack: Boolean = isRecordStack): Response {
	val callStack = if (recordStack) {
		IOException().apply {
			stackTrace = stackTrace.copyOfRange(1, stackTrace.size)
		}
	} else null
	return suspendCancellableCoroutine { continuation ->
		enqueue(object : Callback {
			override fun onResponse(call: Call, response: Response) {
				continuation.resume(response)
			}

			override fun onFailure(call: Call, e: IOException) {
				if (continuation.isCancelled) return
				callStack?.initCause(e)
				continuation.resumeWithException(callStack ?: e)
			}
		})

		continuation.invokeOnCancellation {
			try {
				cancel()
			} catch (ex: Throwable) {
			}
		}
	}
}

const val OKHTTP_STACK_RECORDER_PROPERTY = "com.looker.droidify.okhttp.stackrecorder"

const val OKHTTP_STACK_RECORDER_ON = "on"

const val OKHTTP_STACK_RECORDER_OFF = "off"

@JvmField
val isRecordStack = when (System.getProperty(OKHTTP_STACK_RECORDER_PROPERTY)) {
	OKHTTP_STACK_RECORDER_ON -> true
	OKHTTP_STACK_RECORDER_OFF, null, "" -> false
	else -> error(
		"System property '$OKHTTP_STACK_RECORDER_PROPERTY' has unrecognized value '${
			System.getProperty(
				OKHTTP_STACK_RECORDER_PROPERTY
			)
		}'"
	)
}