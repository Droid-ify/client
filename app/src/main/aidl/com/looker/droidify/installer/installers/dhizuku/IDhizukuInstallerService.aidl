package com.looker.droidify.installer.installers.dhizuku;

/**
 * Runs inside Dhizuku's device-owner process. Because the implementation executes with
 * device-owner privileges, it can drive the standard PackageInstaller silently.
 *
 * Both methods block until the install/uninstall result is known and return a
 * PackageInstaller.STATUS_* code (STATUS_SUCCESS == 0 on success).
 */
interface IDhizukuInstallerService {

    // IMPORTANT: Dhizuku's UserService protocol transacts lifecycle signals on this binder at
    // FIRST_CALL_TRANSACTION+1 ("created") and FIRST_CALL_TRANSACTION+2 ("destroy"). AIDL's "= N"
    // is an OFFSET added to FIRST_CALL_TRANSACTION, so a method at offset 1 or 2 collides with those
    // signals — the "created" signal would then invoke that method with an empty parcel, throwing
    // and aborting the bind handshake (onServiceConnected never fires). Keep both methods off
    // offsets 1 and 2.
    int install(in ParcelFileDescriptor apk, long size, String installerPackageName) = 3;

    int uninstall(String packageName) = 4;
}
