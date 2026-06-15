package com.looker.droidify.ui.appDetail

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.looker.droidify.R.string as stringRes

fun dhizukuDialog(
    context: Context,
    dhizukuState: DhizukuState,
    openDhizuku: () -> Unit,
    switchInstaller: () -> Unit,
) = with(MaterialAlertDialogBuilder(context)) {
    when {
        dhizukuState.isNotAlive -> {
            setTitle(stringRes.error_dhizuku_service_unavailable)
            setMessage(stringRes.error_dhizuku_not_running_DESC)
        }

        dhizukuState.isNotGranted -> {
            setTitle(stringRes.error_dhizuku_not_granted)
            setMessage(stringRes.error_dhizuku_not_granted_DESC)
        }

        dhizukuState.isNotInstalled -> {
            setTitle(stringRes.error_dhizuku_not_installed)
            setMessage(stringRes.error_dhizuku_not_installed_DESC)
        }
    }
    setPositiveButton(stringRes.switch_to_default_installer) { _, _ ->
        switchInstaller()
    }
    setNeutralButton(stringRes.open_dhizuku) { _, _ ->
        openDhizuku()
    }
    setNegativeButton(stringRes.cancel, null)
}
