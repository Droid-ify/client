package com.looker.droidify.utility

import android.content.Context
import android.content.pm.PackageItemInfo
import android.content.pm.PermissionInfo
import android.content.res.Resources
import com.looker.droidify.utility.extension.android.Android
import java.util.*

object PackageItemResolver {
	class LocalCache {
		internal val resources = mutableMapOf<String, Resources>()
	}

	private data class CacheKey(val locales: List<Locale>, val packageName: String, val resId: Int)

	private val cache = mutableMapOf<CacheKey, String?>()

	private fun load(
		context: Context, localCache: LocalCache, packageName: String,
		nonLocalized: CharSequence?, resId: Int,
	): CharSequence? {
		return when {
			nonLocalized != null -> {
				nonLocalized
			}
			resId != 0 -> {
				val locales = if (Android.sdk(24)) {
					val localesList = context.resources.configuration.locales
					(0 until localesList.size()).map(localesList::get)
				} else {
					@Suppress("DEPRECATION")
					listOf(context.resources.configuration.locale)
				}
				val cacheKey = CacheKey(locales, packageName, resId)
				if (cache.containsKey(cacheKey)) {
					cache[cacheKey]
				} else {
					val resources = localCache.resources[packageName] ?: run {
						val resources = try {
							val resources =
								context.packageManager.getResourcesForApplication(packageName)
							@Suppress("DEPRECATION")
							resources.updateConfiguration(context.resources.configuration, null)
							resources
						} catch (e: Exception) {
							null
						}
						resources?.let { localCache.resources[packageName] = it }
						resources
					}
					val label = resources?.getString(resId)
					cache[cacheKey] = label
					label
				}
			}
			else -> {
				null
			}
		}
	}

	fun loadLabel(
		context: Context,
		localCache: LocalCache,
		packageItemInfo: PackageItemInfo,
	): CharSequence? {
		return load(
			context, localCache, packageItemInfo.packageName,
			packageItemInfo.nonLocalizedLabel, packageItemInfo.labelRes
		)
	}

	fun loadDescription(
		context: Context,
		localCache: LocalCache,
		permissionInfo: PermissionInfo,
	): CharSequence? {
		return load(
			context, localCache, permissionInfo.packageName,
			permissionInfo.nonLocalizedDescription, permissionInfo.descriptionRes
		)
	}

	fun getPermissionGroup(permissionInfo: PermissionInfo): String? {
		return if (Android.sdk(29)) {
			// Copied from package installer (Utils.java)
			when (permissionInfo.name) {
				android.Manifest.permission.READ_CONTACTS,
				android.Manifest.permission.WRITE_CONTACTS,
				android.Manifest.permission.GET_ACCOUNTS,
				->
					android.Manifest.permission_group.CONTACTS
				android.Manifest.permission.READ_CALENDAR,
				android.Manifest.permission.WRITE_CALENDAR,
				->
					android.Manifest.permission_group.CALENDAR
				android.Manifest.permission.SEND_SMS,
				android.Manifest.permission.RECEIVE_SMS,
				android.Manifest.permission.READ_SMS,
				android.Manifest.permission.RECEIVE_MMS,
				android.Manifest.permission.RECEIVE_WAP_PUSH,
				"android.permission.READ_CELL_BROADCASTS",
				->
					android.Manifest.permission_group.SMS
				android.Manifest.permission.READ_EXTERNAL_STORAGE,
				android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
				android.Manifest.permission.ACCESS_MEDIA_LOCATION,
				->
					android.Manifest.permission_group.STORAGE
				android.Manifest.permission.ACCESS_FINE_LOCATION,
				android.Manifest.permission.ACCESS_COARSE_LOCATION,
				android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
				->
					android.Manifest.permission_group.LOCATION
				android.Manifest.permission.READ_CALL_LOG,
				android.Manifest.permission.WRITE_CALL_LOG,
				@Suppress("DEPRECATION") android.Manifest.permission.PROCESS_OUTGOING_CALLS,
				->
					android.Manifest.permission_group.CALL_LOG
				android.Manifest.permission.READ_PHONE_STATE,
				android.Manifest.permission.READ_PHONE_NUMBERS,
				android.Manifest.permission.CALL_PHONE,
				android.Manifest.permission.ADD_VOICEMAIL,
				android.Manifest.permission.USE_SIP,
				android.Manifest.permission.ANSWER_PHONE_CALLS,
				android.Manifest.permission.ACCEPT_HANDOVER,
				->
					android.Manifest.permission_group.PHONE
				android.Manifest.permission.RECORD_AUDIO ->
					android.Manifest.permission_group.MICROPHONE
				android.Manifest.permission.ACTIVITY_RECOGNITION ->
					android.Manifest.permission_group.ACTIVITY_RECOGNITION
				android.Manifest.permission.CAMERA ->
					android.Manifest.permission_group.CAMERA
				android.Manifest.permission.BODY_SENSORS ->
					android.Manifest.permission_group.SENSORS
				else -> null
			}
		} else {
			permissionInfo.group
		}
	}
}
