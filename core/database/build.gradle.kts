plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.room)
    alias(libs.plugins.looker.hilt)
    alias(libs.plugins.looker.serialization)
}

android {
    namespace = "com.looker.core.database"
}

dependencies {
    modules(Modules.coreCommon, Modules.coreDomain)

    implementation(libs.coroutines.android)
    implementation(libs.core.ktx)
}
