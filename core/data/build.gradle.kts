plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.hilt.work)
    alias(libs.plugins.looker.lint)
}

android {
    namespace = "com.looker.core.data"
}

dependencies {
    modules(
        Modules.coreCommon,
        Modules.coreDatabase,
        Modules.coreDatastore,
        Modules.coreDI,
        Modules.coreDomain,
        Modules.coreNetwork,
        Modules.sync,
    )

    implementation(libs.kotlinx.coroutines.android)
}
