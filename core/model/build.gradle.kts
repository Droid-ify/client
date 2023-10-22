plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.lint)
}

android {
    namespace = "com.looker.core.model"

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
