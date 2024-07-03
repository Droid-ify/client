plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.hilt)
    alias(libs.plugins.looker.lint)
    alias(libs.plugins.looker.serialization)
}

android {
    namespace = "com.looker.core.datastore"
}

dependencies {
    modules(Modules.coreCommon, Modules.coreDI)
    implementation(libs.androidx.dataStore.core)
    implementation(libs.androidx.dataStore.proto)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
}
