-dontobfuscate

# Disable ServiceLoader reproducibility-breaking optimizations
-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory

-dontwarn kotlinx.serialization.KSerializer
-dontwarn kotlinx.serialization.Serializable
-dontwarn org.slf4j.impl.StaticLoggerBinder
#
#-dontwarn com.looker.core.common.BuildConfig
#-dontwarn com.looker.core.common.DeeplinkType$AddRepository
#-dontwarn com.looker.core.common.DeeplinkType$AppDetail
#-dontwarn com.looker.core.common.DeeplinkType
#-dontwarn com.looker.core.common.DeeplinksKt
#-dontwarn com.looker.core.common.Exporter
#-dontwarn com.looker.core.common.NotificationKt
#-dontwarn com.looker.core.common.PermissionsKt
#-dontwarn com.looker.core.common.Scroller
#-dontwarn com.looker.core.common.SdkCheck
#-dontwarn com.looker.core.common.Singleton
#-dontwarn com.looker.core.common.TextKt
#-dontwarn com.looker.core.common.cache.Cache
#-dontwarn com.looker.core.common.cache.Cache
#-dontwarn com.looker.core.common.device.Huawei
#-dontwarn com.looker.core.common.extension.ContextKt
#-dontwarn com.looker.core.common.extension.CursorKt
#-dontwarn com.looker.core.common.extension.DateTimeKt
#-dontwarn com.looker.core.common.extension.FingerprintKt
#-dontwarn com.looker.core.common.extension.FlowKt
#-dontwarn com.looker.core.common.extension.InsetsKt
#-dontwarn com.looker.core.common.extension.IntentKt
#-dontwarn com.looker.core.common.extension.Json
#-dontwarn com.looker.core.common.extension.JsonKt
#-dontwarn com.looker.core.common.extension.KeyToken
#-dontwarn com.looker.core.common.extension.LocaleKt
#-dontwarn com.looker.core.common.extension.NumberKt
#-dontwarn com.looker.core.common.extension.PackageInfoKt
#-dontwarn com.looker.core.common.extension.SQLiteDatabaseKt
#-dontwarn com.looker.core.common.extension.ServiceKt
#-dontwarn com.looker.core.common.extension.ViewKt
#-dontwarn com.looker.core.common.result.Result$Error
#-dontwarn com.looker.core.common.result.Result$Success
#-dontwarn com.looker.core.common.result.Result
#-dontwarn com.looker.core.common.signature.Hash
#-dontwarn com.looker.core.common.signature.HashCheckerKt
#
#-dontwarn com.looker.core.datastore.Settings
#-dontwarn com.looker.core.datastore.SettingsRepository
#-dontwarn com.looker.core.datastore.di.DatastoreModule_ProvidePreferenceDatastoreFactory
#-dontwarn com.looker.core.datastore.di.DatastoreModule_ProvideProtoDatastoreFactory
#-dontwarn com.looker.core.datastore.di.DatastoreModule_ProvideSettingsExporterFactory
#-dontwarn com.looker.core.datastore.di.DatastoreModule_ProvideSettingsRepositoryFactory
#-dontwarn com.looker.core.datastore.extension.PreferencesKt
#-dontwarn com.looker.core.datastore.model.AutoSync
#-dontwarn com.looker.core.datastore.model.InstallerType$Companion
#-dontwarn com.looker.core.datastore.model.InstallerType
#-dontwarn com.looker.core.datastore.model.ProxyPreference
#-dontwarn com.looker.core.datastore.model.ProxyType
#-dontwarn com.looker.core.datastore.model.SortOrder
#-dontwarn com.looker.core.datastore.model.Theme
#
#-dontwarn com.looker.installer.InstallManager
#-dontwarn com.looker.installer.InstallModule_ProvideRootPermissionHandlerFactory
#-dontwarn com.looker.installer.InstallModule_ProvideShizukuPermissionHandlerFactory
#-dontwarn com.looker.installer.InstallModule_ProvidesInstallerFactory
#-dontwarn com.looker.installer.installers.root.RootPermissionHandler
#-dontwarn com.looker.installer.installers.session.SessionInstallerReceiver_GeneratedInjector
#-dontwarn com.looker.installer.installers.shizuku.ShizukuPermissionHandler$State
#-dontwarn com.looker.installer.installers.shizuku.ShizukuPermissionHandler
#-dontwarn com.looker.installer.model.InstallItem
#-dontwarn com.looker.installer.model.InstallItemKt
#-dontwarn com.looker.installer.model.InstallState
#-dontwarn com.looker.installer.notification.InstallNotificationKt
