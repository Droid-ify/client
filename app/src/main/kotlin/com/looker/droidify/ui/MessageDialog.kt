package com.looker.droidify.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.looker.core.common.SdkCheck
import com.looker.core.common.nullIfEmpty
import com.looker.droidify.model.Release
import com.looker.droidify.ui.repository.RepositoryFragment
import com.looker.droidify.utility.PackageItemResolver
import com.looker.droidify.utility.extension.android.Android
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import com.looker.core.common.R.string as stringRes

class MessageDialog() : DialogFragment() {
    companion object {
        private const val EXTRA_MESSAGE = "message"
    }

    constructor(message: Message) : this() {
        arguments = bundleOf(EXTRA_MESSAGE to message)
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, this::class.java.name)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val dialog = MaterialAlertDialogBuilder(requireContext())
        val message = if (SdkCheck.isTiramisu) {
            arguments?.getParcelable(EXTRA_MESSAGE, Message::class.java)!!
        } else {
            arguments?.getParcelable(EXTRA_MESSAGE)!!
        }
        when (message) {
            is Message.DeleteRepositoryConfirm -> {
                dialog.setTitle(stringRes.confirmation)
                dialog.setMessage(stringRes.delete_repository_DESC)
                dialog.setPositiveButton(stringRes.delete) { _, _ ->
                    (parentFragment as RepositoryFragment).onDeleteConfirm()
                }
                dialog.setNegativeButton(stringRes.cancel, null)
            }

            is Message.CantEditSyncing -> {
                dialog.setTitle(stringRes.action_failed)
                dialog.setMessage(stringRes.cant_edit_sync_DESC)
                dialog.setPositiveButton(stringRes.ok, null)
            }

            is Message.Link -> {
                dialog.setTitle(stringRes.confirmation)
                dialog.setMessage(getString(stringRes.open_DESC_FORMAT, message.uri.toString()))
                dialog.setPositiveButton(stringRes.ok) { _, _ ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, message.uri))
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
                dialog.setNegativeButton(stringRes.cancel, null)
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
                        )?.nullIfEmpty()?.let { if (it == message.group) null else it }
                    } catch (e: Exception) {
                        null
                    }
                    name ?: getString(stringRes.unknown)
                } else {
                    getString(stringRes.other)
                }
                for (permission in message.permissions) {
                    kotlin.runCatching {
                        val permissionInfo = packageManager.getPermissionInfo(permission, 0)
                        PackageItemResolver.loadDescription(
                            requireContext(),
                            localCache,
                            permissionInfo
                        )?.nullIfEmpty()?.let { if (it == permission) null else it }
                            ?: error("Invalid Permission Description")
                    }.onSuccess {
                        builder.append(it).append("\n\n")
                    }
                }
                if (builder.isNotEmpty()) {
                    builder.delete(builder.length - 2, builder.length)
                } else {
                    builder.append(getString(stringRes.no_description_available_DESC))
                }
                dialog.setTitle(title)
                dialog.setMessage(builder)
                dialog.setPositiveButton(stringRes.ok, null)
            }

            is Message.ReleaseIncompatible -> {
                val builder = StringBuilder()
                val minSdkVersion =
                    if (Release.Incompatibility.MinSdk in message.incompatibilities) {
                        message.minSdkVersion
                    } else {
                        null
                    }
                val maxSdkVersion =
                    if (Release.Incompatibility.MaxSdk in message.incompatibilities) {
                        message.maxSdkVersion
                    } else {
                        null
                    }
                if (minSdkVersion != null || maxSdkVersion != null) {
                    val versionMessage = minSdkVersion?.let {
                        getString(
                            stringRes.incompatible_api_min_DESC_FORMAT,
                            it
                        )
                    }
                        ?: maxSdkVersion?.let {
                            getString(
                                stringRes.incompatible_api_max_DESC_FORMAT,
                                it
                            )
                        }
                    builder.append(
                        getString(
                            stringRes.incompatible_api_DESC_FORMAT,
                            Android.name,
                            SdkCheck.sdk,
                            versionMessage.orEmpty()
                        )
                    ).append("\n\n")
                }
                if (Release.Incompatibility.Platform in message.incompatibilities) {
                    builder.append(
                        getString(
                            stringRes.incompatible_platforms_DESC_FORMAT,
                            Android.primaryPlatform ?: getString(stringRes.unknown),
                            message.platforms.joinToString(separator = ", ")
                        )
                    ).append("\n\n")
                }
                val features =
                    message.incompatibilities.mapNotNull { it as? Release.Incompatibility.Feature }
                if (features.isNotEmpty()) {
                    builder.append(getString(stringRes.incompatible_features_DESC))
                    for (feature in features) {
                        builder.append("\n\u2022 ").append(feature.feature)
                    }
                    builder.append("\n\n")
                }
                if (builder.isNotEmpty()) {
                    builder.delete(builder.length - 2, builder.length)
                }
                dialog.setTitle(stringRes.incompatible_version)
                dialog.setMessage(builder)
                dialog.setPositiveButton(stringRes.ok, null)
            }

            is Message.ReleaseOlder -> {
                dialog.setTitle(stringRes.incompatible_version)
                dialog.setMessage(stringRes.incompatible_older_DESC)
                dialog.setPositiveButton(stringRes.ok, null)
            }

            is Message.ReleaseSignatureMismatch -> {
                dialog.setTitle(stringRes.incompatible_version)
                dialog.setMessage(stringRes.incompatible_signature_DESC)
                dialog.setPositiveButton(stringRes.ok, null)
            }
        }::class
        return dialog.create()
    }
}

@Parcelize
sealed interface Message : Parcelable {
    @Parcelize
    data object DeleteRepositoryConfirm : Message

    @Parcelize
    data object CantEditSyncing : Message

    @Parcelize
    class Link(val uri: Uri) : Message

    @Parcelize
    class Permissions(val group: String?, val permissions: List<String>) : Message

    @Parcelize
    @TypeParceler<Release.Incompatibility, ReleaseIncompatibilityParceler>
    class ReleaseIncompatible(
        val incompatibilities: List<Release.Incompatibility>,
        val platforms: List<String>,
        val minSdkVersion: Int,
        val maxSdkVersion: Int
    ) : Message

    @Parcelize
    data object ReleaseOlder : Message

    @Parcelize
    data object ReleaseSignatureMismatch : Message
}

class ReleaseIncompatibilityParceler : Parceler<Release.Incompatibility> {

    private companion object {
        // Incompatibility indices in `Parcel`
        const val MIN_SDK_INDEX = 0
        const val MAX_SDK_INDEX = 1
        const val PLATFORM_INDEX = 2
        const val FEATURE_INDEX = 3
    }

    override fun create(parcel: Parcel): Release.Incompatibility {
        return when (parcel.readInt()) {
            MIN_SDK_INDEX -> Release.Incompatibility.MinSdk
            MAX_SDK_INDEX -> Release.Incompatibility.MaxSdk
            PLATFORM_INDEX -> Release.Incompatibility.Platform
            FEATURE_INDEX -> Release.Incompatibility.Feature(requireNotNull(parcel.readString()))
            else -> error("Invalid Index for Incompatibility")
        }
    }

    override fun Release.Incompatibility.write(parcel: Parcel, flags: Int) {
        when (this) {
            is Release.Incompatibility.MinSdk -> {
                parcel.writeInt(MIN_SDK_INDEX)
            }

            is Release.Incompatibility.MaxSdk -> {
                parcel.writeInt(MAX_SDK_INDEX)
            }

            is Release.Incompatibility.Platform -> {
                parcel.writeInt(PLATFORM_INDEX)
            }

            is Release.Incompatibility.Feature -> {
                parcel.writeInt(FEATURE_INDEX)
                parcel.writeString(feature)
            }
        }
    }
}
