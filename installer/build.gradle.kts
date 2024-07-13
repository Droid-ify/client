plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.hilt)
    alias(libs.plugins.looker.lint)
}

android {
    namespace = "com.looker.installer"
}

dependencies {
    modules(
        Modules.coreCommon,
        Modules.coreDatastore,
        Modules.coreDomain,
    )

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.libsu.core)
    implementation(libs.shizuku.api)
    api(libs.shizuku.provider)
}
