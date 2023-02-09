package com.looker.droidify.utility

import android.Manifest
import android.content.Context
import android.content.pm.PackageItemInfo
import android.content.pm.PermissionInfo
import android.content.res.Resources
import android.os.Build
import com.looker.core.common.SdkCheck
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
				val locales = if (SdkCheck.isNougat) {
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

	fun getPermissionGroup(permissionInfo: PermissionInfo): String? =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			when (permissionInfo.name) {
				Manifest.permission.READ_CONTACTS,
				Manifest.permission.WRITE_CONTACTS,
				Manifest.permission.GET_ACCOUNTS,
				-> Manifest.permission_group.CONTACTS
				Manifest.permission.READ_CALENDAR,
				Manifest.permission.WRITE_CALENDAR,
				-> Manifest.permission_group.CALENDAR
				Manifest.permission.SEND_SMS,
				Manifest.permission.RECEIVE_SMS,
				Manifest.permission.READ_SMS,
				Manifest.permission.RECEIVE_MMS,
				Manifest.permission.RECEIVE_WAP_PUSH,
				"android.permission.READ_CELL_BROADCASTS",
				-> Manifest.permission_group.SMS
				Manifest.permission.READ_EXTERNAL_STORAGE,
				Manifest.permission.WRITE_EXTERNAL_STORAGE,
				Manifest.permission.ACCESS_MEDIA_LOCATION,
				-> Manifest.permission_group.STORAGE
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.ACCESS_COARSE_LOCATION,
				Manifest.permission.ACCESS_BACKGROUND_LOCATION,
				-> Manifest.permission_group.LOCATION
				Manifest.permission.READ_CALL_LOG,
				Manifest.permission.WRITE_CALL_LOG,
				@Suppress("DEPRECATION") Manifest.permission.PROCESS_OUTGOING_CALLS,
				-> Manifest.permission_group.CALL_LOG
				Manifest.permission.READ_PHONE_STATE,
				Manifest.permission.READ_PHONE_NUMBERS,
				Manifest.permission.CALL_PHONE,
				Manifest.permission.ADD_VOICEMAIL,
				Manifest.permission.USE_SIP,
				Manifest.permission.ANSWER_PHONE_CALLS,
				Manifest.permission.ACCEPT_HANDOVER,
				-> Manifest.permission_group.PHONE
				Manifest.permission.RECORD_AUDIO -> Manifest.permission_group.MICROPHONE
				Manifest.permission.ACTIVITY_RECOGNITION -> Manifest.permission_group.ACTIVITY_RECOGNITION
				Manifest.permission.CAMERA -> Manifest.permission_group.CAMERA
				Manifest.permission.BODY_SENSORS -> Manifest.permission_group.SENSORS
				else -> null
			}
		} else permissionInfo.group
}
