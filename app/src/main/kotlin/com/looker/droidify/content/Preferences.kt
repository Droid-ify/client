package com.looker.droidify.content

import android.content.Context
import android.content.SharedPreferences

object Preferences {
	private lateinit var preferences: SharedPreferences

	fun init(context: Context) {
		preferences =
			context.getSharedPreferences(
				"${context.packageName}_preferences",
				Context.MODE_PRIVATE
			)
	}

	sealed class Value<T> {
		abstract val value: T

		internal abstract fun get(
			preferences: SharedPreferences,
			key: String,
			defaultValue: Value<T>,
		): T

		internal abstract fun set(preferences: SharedPreferences, key: String, value: T)

		class BooleanValue(override val value: Boolean) : Value<Boolean>() {
			override fun get(
				preferences: SharedPreferences,
				key: String,
				defaultValue: Value<Boolean>,
			): Boolean {
				return preferences.getBoolean(key, defaultValue.value)
			}

			override fun set(preferences: SharedPreferences, key: String, value: Boolean) {
				preferences.edit().putBoolean(key, value).apply()
			}
		}
	}

	sealed class Key<T>(val name: String, val default: Value<T>) {
		object UpdateUnstable : Key<Boolean>("update_unstable", Value.BooleanValue(false))
	}

	operator fun <T> get(key: Key<T>): T {
		return key.default.get(preferences, key.name, key.default)
	}

	operator fun <T> set(key: Key<T>, value: T) {
		key.default.set(preferences, key.name, value)
	}
}
