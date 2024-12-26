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
    implementation(libs.datastore.core)
    implementation(libs.datastore.proto)
    implementation(libs.coroutines.android)
    implementation(libs.datetime)
}
