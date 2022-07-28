package com.looker.droidify.content

import android.content.Context
import android.content.SharedPreferences
import com.looker.droidify.utility.extension.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object Preferences {
	private lateinit var preferences: SharedPreferences

	private val _subject = MutableSharedFlow<Key<*>>()
	val subject = _subject.asSharedFlow()

	private val keys = sequenceOf(
		Key.IncompatibleVersions,
		Key.SortOrder,
		Key.UpdateNotify,
		Key.UpdateUnstable
	).map { Pair(it.name, it) }.toMap()

	fun init(context: Context) {
		preferences =
			context.getSharedPreferences(
				"${context.packageName}_preferences",
				Context.MODE_PRIVATE
			)
		preferences.registerOnSharedPreferenceChangeListener { _, keyString ->
			CoroutineScope(Dispatchers.Default).launch {
				keys[keyString]?.let {
					_subject.emit(it)
				}
			}
		}
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

		class EnumerationValue<T : Enumeration<T>>(override val value: T) : Value<T>() {
			override fun get(
				preferences: SharedPreferences,
				key: String,
				defaultValue: Value<T>,
			): T {
				val value = preferences.getString(key, defaultValue.value.valueString)
				return defaultValue.value.values.find { it.valueString == value }
					?: defaultValue.value
			}

			override fun set(preferences: SharedPreferences, key: String, value: T) {
				preferences.edit().putString(key, value.valueString).apply()
			}
		}
	}

	interface Enumeration<T> {
		val values: List<T>
		val valueString: String
	}

	sealed class Key<T>(val name: String, val default: Value<T>) {
		object IncompatibleVersions :
			Key<Boolean>("incompatible_versions", Value.BooleanValue(false))

		object SortOrder : Key<Preferences.SortOrder>(
			"sort_order",
			Value.EnumerationValue(Preferences.SortOrder.Update)
		)

		object UpdateNotify : Key<Boolean>("update_notify", Value.BooleanValue(true))
		object UpdateUnstable : Key<Boolean>("update_unstable", Value.BooleanValue(false))
	}

	sealed class SortOrder(override val valueString: String, val order: Order) :
		Enumeration<SortOrder> {
		override val values: List<SortOrder>
			get() = listOf(Name, Added, Update)

		object Name : SortOrder("name", Order.NAME)
		object Added : SortOrder("added", Order.DATE_ADDED)
		object Update : SortOrder("update", Order.LAST_UPDATE)
	}

	operator fun <T> get(key: Key<T>): T {
		return key.default.get(preferences, key.name, key.default)
	}

	operator fun <T> set(key: Key<T>, value: T) {
		key.default.set(preferences, key.name, value)
	}
}
