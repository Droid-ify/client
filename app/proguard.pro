-dontobfuscate

# Disable ServiceLoader reproducibility-breaking optimizations
-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory

-dontwarn kotlinx.serialization.KSerializer
-dontwarn kotlinx.serialization.Serializable
-dontwarn org.slf4j.impl.StaticLoggerBinder

-keep class com.rosan.dhizuku.** { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }
-dontwarn android.app.ActivityThread
-keep class android.content.pm.PackageInstaller { *; }
-keep class android.content.pm.IPackageInstallerSession { *; }
-keep class android.content.pm.IPackageInstallerSession$Stub { *; }
