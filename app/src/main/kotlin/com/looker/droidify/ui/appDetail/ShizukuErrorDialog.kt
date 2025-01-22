package com.looker.droidify.ui.appDetail

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.looker.droidify.R.string as stringRes

fun shizukuDialog(
    context: Context,
    shizukuState: ShizukuState,
    openShizuku: () -> Unit,
    switchInstaller: () -> Unit
) = with(MaterialAlertDialogBuilder(context)) {
    when {
        shizukuState.isNotAlive -> {
            setTitle(stringRes.error_shizuku_service_unavailable)
            setMessage(stringRes.error_shizuku_not_running_DESC)
        }

        shizukuState.isNotGranted -> {
            setTitle(stringRes.error_shizuku_not_granted)
            setMessage(stringRes.error_shizuku_not_granted_DESC)
        }

        shizukuState.isNotInstalled -> {
            setTitle(stringRes.error_shizuku_not_installed)
            setMessage(stringRes.error_shizuku_not_installed_DESC)
        }
    }
    setPositiveButton(stringRes.switch_to_default_installer) { _, _ ->
        switchInstaller()
    }
    setNeutralButton(stringRes.open_shizuku) { _, _ ->
        openShizuku()
    }
    setNegativeButton(stringRes.cancel, null)
}
