package com.looker.core.data.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.getSystemService
import com.looker.core.common.sdkAbove
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject

class ConnectivityManagerNetworkMonitor @Inject constructor(context: Context) : NetworkMonitor {
	override val isOnline: Flow<Boolean> = callbackFlow {
		val callback = object : ConnectivityManager.NetworkCallback() {
			override fun onAvailable(network: Network) {
				channel.trySend(true)
			}

			override fun onLost(network: Network) {
				channel.trySend(false)
			}
		}

		val connectivityManager = context.getSystemService<ConnectivityManager>()

		connectivityManager?.registerNetworkCallback(
			NetworkRequest.Builder()
				.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
				.build(),
			callback
		)

		channel.trySend(connectivityManager.isCurrentlyConnected())

		awaitClose {
			connectivityManager?.unregisterNetworkCallback(callback)
		}
	}.conflate()

	@Suppress("DEPRECATION")
	private fun ConnectivityManager?.isCurrentlyConnected() = when (this) {
		null -> false
		else -> sdkAbove(
			sdk = Build.VERSION_CODES.M,
			onSuccessful = {
				activeNetwork
					?.let(::getNetworkCapabilities)
					?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
					?: false
			},
			orElse = { activeNetworkInfo?.isConnected ?: false }
		)
	}
}