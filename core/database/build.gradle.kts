plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.room)
    alias(libs.plugins.looker.hilt)
    alias(libs.plugins.looker.serialization)
}

android {
    namespace = "com.looker.core.database"

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
    modules(Modules.coreCommon, Modules.coreModel)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
}
