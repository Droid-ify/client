plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.hilt)
    alias(libs.plugins.looker.lint)
}

android {
    namespace = "com.looker.network"

    defaultConfig {
        buildConfigField(
            type = "String",
            name = "VERSION_NAME",
            value = "\"${DefaultConfig.versionName}\""
        )
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    modules(Modules.coreCommon)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ktor.core)
    implementation(libs.ktor.okhttp)
}
