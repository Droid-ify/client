plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.hilt)
    alias(libs.plugins.looker.lint)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.looker.core.datastore"

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
        create("alpha") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
        }
    }
}

dependencies {
    modules(Modules.coreCommon)
    implementation(libs.androidx.dataStore.core)
    implementation(libs.androidx.dataStore.proto)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
}
