plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.hilt)
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
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.dataStore.core)
    implementation(libs.kotlinx.datetime)
}
