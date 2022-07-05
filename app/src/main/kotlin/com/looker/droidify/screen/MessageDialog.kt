package com.looker.droidify.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.looker.droidify.R
import com.looker.droidify.entity.Release
import com.looker.droidify.utility.KParcelable
import com.looker.droidify.utility.PackageItemResolver
import com.looker.droidify.utility.extension.android.Android
import com.looker.droidify.utility.extension.text.nullIfEmpty

class MessageDialog() : DialogFragment() {
	companion object {
		private const val EXTRA_MESSAGE = "message"
	}

	sealed class Message : KParcelable {
		object DeleteRepositoryConfirm : Message() {
			@Suppress("unused")
			@JvmField
			val CREATOR = KParcelable.creator { DeleteRepositoryConfirm }
		}

		object CantEditSyncing : Message() {
			@Suppress("unused")
			@JvmField
			val CREATOR = KParcelable.creator { CantEditSyncing }
		}

		class Link(val uri: Uri) : Message() {
			override fun writeToParcel(dest: Parcel, flags: Int) {
				dest.writeString(uri.toString())
			}

			companion object {
				@Suppress("unused")
				@JvmField
				val CREATOR = KParcelable.creator {
					val uri = Uri.parse(it.readString()!!)
					Link(uri)
				}
			}
		}

		class Permissions(val group: String?, val permissions: List<String>) : Message() {
			override fun writeToParcel(dest: Parcel, flags: Int) {
				dest.writeString(group)
				dest.writeStringList(permissions)
			}

			companion object {
				@Suppress("unused")
				@JvmField
				val CREATOR = KParcelable.creator {
					val group = it.readString()
					val permissions = it.createStringArrayList()!!
					Permissions(group, permissions)
				}
			}
		}

		class ReleaseIncompatible(
			val incompatibilities: List<Release.Incompatibility>,
			val platforms: List<String>, val minSdkVersion: Int, val maxSdkVersion: Int,
		) : Message() {
			override fun writeToParcel(dest: Parcel, flags: Int) {
				dest.writeInt(incompatibilities.size)
				for (incompatibility in incompatibilities) {
					when (incompatibility) {
						is Release.Incompatibility.MinSdk -> {
							dest.writeInt(0)
						}
						is Release.Incompatibility.MaxSdk -> {
							dest.writeInt(1)
						}
						is Release.Incompatibility.Platform -> {
							dest.writeInt(2)
						}
						is Release.Incompatibility.Feature -> {
							dest.writeInt(3)
							dest.writeString(incompatibility.feature)
						}
					}::class
				}
				dest.writeStringList(platforms)
				dest.writeInt(minSdkVersion)
				dest.writeInt(maxSdkVersion)
			}

			companion object {
				@Suppress("unused")
				@JvmField
				val CREATOR = KParcelable.creator {
					val count = it.readInt()
					val incompatibilities = generateSequence {
						when (it.readInt()) {
							0 -> Release.Incompatibility.MinSdk
							1 -> Release.Incompatibility.MaxSdk
							2 -> Release.Incompatibility.Platform
							3 -> Release.Incompatibility.Feature(it.readString()!!)
							else -> throw RuntimeException()
						}
					}.take(count).toList()
					val platforms = it.createStringArrayList()!!
					val minSdkVersion = it.readInt()
					val maxSdkVersion = it.readInt()
					ReleaseIncompatible(incompatibilities, platforms, minSdkVersion, maxSdkVersion)
				}
			}
		}

		object ReleaseOlder : Message() {
			@Suppress("unused")
			@JvmField
			val CREATOR = KParcelable.creator { ReleaseOlder }
		}

		object ReleaseSignatureMismatch : Message() {
			@Suppress("unused")
			@JvmField
			val CREATOR = KParcelable.creator { ReleaseSignatureMismatch }
		}
	}

	constructor(message: Message) : this() {
		arguments = Bundle().apply {
			putParcelable(EXTRA_MESSAGE, message)
		}
	}

	fun show(fragmentManager: FragmentManager) {
		show(fragmentManager, this::class.java.name)
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
		val dialog = MaterialAlertDialogBuilder(requireContext())
		when (val message = requireArguments().getParcelable<Message>(EXTRA_MESSAGE)!!) {
			is Message.DeleteRepositoryConfirm -> {
				dialog.setTitle(R.string.confirmation)
				dialog.setMessage(R.string.delete_repository_DESC)
				dialog.setPositiveButton(R.string.delete) { _, _ -> (parentFragment as RepositoryFragment).onDeleteConfirm() }
				dialog.setNegativeButton(R.string.cancel, null)
			}
			is Message.CantEditSyncing -> {
				dialog.setTitle(R.string.action_failed)
				dialog.setMessage(R.string.cant_edit_sync_DESC)
				dialog.setPositiveButton(R.string.ok, null)
			}
			is Message.Link -> {
				dialog.setTitle(R.string.confirmation)
				dialog.setMessage(getString(R.string.open_DESC_FORMAT, message.uri.toString()))
				dialog.setPositiveButton(R.string.ok) { _, _ ->
					try {
						startActivity(Intent(Intent.ACTION_VIEW, message.uri))
					} catch (e: ActivityNotFoundException) {
						e.printStackTrace()
					}
				}
				dialog.setNegativeButton(R.string.cancel, null)
			}
			is Message.Permissions -> {
				val packageManager = requireContext().packageManager
				val builder = StringBuilder()
				val localCache = PackageItemResolver.LocalCache()
				val title = if (message.group != null) {
					val name = try {
						val permissionGroupInfo =
							packageManager.getPermissionGroupInfo(message.group, 0)
						PackageItemResolver.loadLabel(
							requireContext(),
							localCache,
							permissionGroupInfo
						)
							?.nullIfEmpty()?.let { if (it == message.group) null else it }
					} catch (e: Exception) {
						null
					}
					name ?: getString(R.string.unknown)
				} else {
					getString(R.string.other)
				}
				for (permission in message.permissions) {
					val description = try {
						val permissionInfo = packageManager.getPermissionInfo(permission, 0)
						PackageItemResolver.loadDescription(
							requireContext(),
							localCache,
							permissionInfo
						)
							?.nullIfEmpty()?.let { if (it == permission) null else it }
					} catch (e: Exception) {
						null
					}
					description?.let { builder.append(it).append("\n\n") }
				}
				if (builder.isNotEmpty()) {
					builder.delete(builder.length - 2, builder.length)
				} else {
					builder.append(getString(R.string.no_description_available_DESC))
				}
				dialog.setTitle(title)
				dialog.setMessage(builder)
				dialog.setPositiveButton(R.string.ok, null)
			}
			is Message.ReleaseIncompatible -> {
				val builder = StringBuilder()
				val minSdkVersion = if (Release.Incompatibility.MinSdk in message.incompatibilities)
					message.minSdkVersion else null
				val maxSdkVersion = if (Release.Incompatibility.MaxSdk in message.incompatibilities)
					message.maxSdkVersion else null
				if (minSdkVersion != null || maxSdkVersion != null) {
					val versionMessage = minSdkVersion?.let {
						getString(
							R.string.incompatible_api_min_DESC_FORMAT,
							it
						)
					}
						?: maxSdkVersion?.let {
							getString(
								R.string.incompatible_api_max_DESC_FORMAT,
								it
							)
						}
					builder.append(
						getString(
							R.string.incompatible_api_DESC_FORMAT,
							Android.name, Android.sdk, versionMessage.orEmpty()
						)
					).append("\n\n")
				}
				if (Release.Incompatibility.Platform in message.incompatibilities) {
					builder.append(
						getString(
							R.string.incompatible_platforms_DESC_FORMAT,
							Android.primaryPlatform ?: getString(R.string.unknown),
							message.platforms.joinToString(separator = ", ")
						)
					).append("\n\n")
				}
				val features =
					message.incompatibilities.mapNotNull { it as? Release.Incompatibility.Feature }
				if (features.isNotEmpty()) {
					builder.append(getString(R.string.incompatible_features_DESC))
					for (feature in features) {
						builder.append("\n\u2022 ").append(feature.feature)
					}
					builder.append("\n\n")
				}
				if (builder.isNotEmpty()) {
					builder.delete(builder.length - 2, builder.length)
				}
				dialog.setTitle(R.string.incompatible_version)
				dialog.setMessage(builder)
				dialog.setPositiveButton(R.string.ok, null)
			}
			is Message.ReleaseOlder -> {
				dialog.setTitle(R.string.incompatible_version)
				dialog.setMessage(R.string.incompatible_older_DESC)
				dialog.setPositiveButton(R.string.ok, null)
			}
			is Message.ReleaseSignatureMismatch -> {
				dialog.setTitle(R.string.incompatible_version)
				dialog.setMessage(R.string.incompatible_signature_DESC)
				dialog.setPositiveButton(R.string.ok, null)
			}
		}::class
		return dialog.create()
	}
}
