plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.serialization)
    alias(libs.plugins.looker.lint)
}

android {
    namespace = "com.looker.sync.fdroid"
}

dependencies {
    modules(
        Modules.coreCommon,
        Modules.coreDomain,
        Modules.coreNetwork,
    )

    implementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.bundles.test.android)
}
