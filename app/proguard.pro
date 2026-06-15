-dontobfuscate

# Disable ServiceLoader reproducibility-breaking optimizations
-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory

-dontwarn kotlinx.serialization.KSerializer
-dontwarn kotlinx.serialization.Serializable
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Dhizuku UserService — class name is passed via ComponentName; R8 must not rename it.
-keep class com.looker.droidify.installer.installers.dhizuku.DroidifyDhizukuInstallerService { *; }
-keepclassmembers class com.looker.droidify.installer.installers.dhizuku.DroidifyDhizukuInstallerService {
    <init>(android.content.Context);
    <init>();
}

# AIDL-generated Stub/Proxy classes for IPC between app and Dhizuku process
-keep class com.looker.droidify.installer.installers.dhizuku.IDhizukuInstallerService { *; }
-keep class com.looker.droidify.installer.installers.dhizuku.IDhizukuInstallerService$Stub { *; }
-keep class com.looker.droidify.installer.installers.dhizuku.IDhizukuInstallerService$Stub$Proxy { *; }

# Dhizuku library internals
-keep class com.rosan.dhizuku.** { *; }
-dontwarn com.rosan.dhizuku.**
