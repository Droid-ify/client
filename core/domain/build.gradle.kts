plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.lint)
    id("kotlin-parcelize")
}

android {
    namespace = "com.looker.core.domain"

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
}
