package com.looker.droidify.installer.installers.dhizuku;

interface IDhizukuInstallerService {
    int installPackage(in android.os.ParcelFileDescriptor pfd, long fileSize, String expectedPackageName, long expectedVersionCode, String installerPackageName);
    void destroy();
    int uninstallPackage(String packageName);
}
