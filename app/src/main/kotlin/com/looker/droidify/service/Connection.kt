package com.looker.droidify.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

class Connection<B : IBinder, S : ConnectionService<B>>(
	private val serviceClass: Class<S>,
	private val onBind: ((Connection<B, S>, B) -> Unit)? = null,
	private val onUnbind: ((Connection<B, S>, B) -> Unit)? = null,
) : ServiceConnection {
	var binder: B? = null
		private set

	private fun handleUnbind() {
		binder?.let {
			binder = null
			onUnbind?.invoke(this, it)
		}
	}

	override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
		@Suppress("UNCHECKED_CAST")
		binder as B
		this.binder = binder
		onBind?.invoke(this, binder)
	}

	override fun onServiceDisconnected(componentName: ComponentName) {
		handleUnbind()
	}

	fun bind(context: Context) {
		context.bindService(Intent(context, serviceClass), this, Context.BIND_AUTO_CREATE)
	}

	fun unbind(context: Context) {
		context.unbindService(this)
		handleUnbind()
	}
}
