plugins {
    alias(libs.plugins.looker.android.application)
    alias(libs.plugins.looker.hilt.work)
    alias(libs.plugins.looker.lint)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.looker.droidify"
    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        generateLocaleConfig = true
    }

    sourceSets.forEach { source ->
        val javaDir = source.java.srcDirs.find { it.name == "java" }
        source.java {
            srcDir(File(javaDir?.parentFile, "kotlin"))
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            resValue("string", "application_name", "Droid-ify-Debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            resValue("string", "application_name", "Droid-ify")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard.pro"
            )
        }
        create("alpha") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".alpha"
            resValue("string", "application_name", "Droid-ify Alpha")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard.pro"
            )
            isDebuggable = true
            isMinifyEnabled = true
        }
        all {
            buildConfigField(
                type = "String",
                name = "VERSION_NAME",
                value = "\"v${DefaultConfig.versionName}\""
            )
        }
    }
    packaging {
        resources {
            excludes += listOf(
                "/DebugProbesKt.bin",
                "/kotlin/**.kotlin_builtins",
                "/kotlin/**.kotlin_metadata",
                "/META-INF/**.kotlin_module",
                "/META-INF/**.pro",
                "/META-INF/**.version",
                "/META-INF/versions/9/previous-**.bin"
            )
        }
    }
    buildFeatures {
        resValues = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.monitor)
    modules(
        Modules.coreDomain,
//        Modules.coreData,
        Modules.coreCommon,
        Modules.coreNetwork,
        Modules.coreDatastore,
        Modules.coreDI,
        Modules.installer,
    )

    implementation(libs.material)
    implementation(libs.core.ktx)
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.viewModel)
    implementation(libs.recyclerview)
    implementation(libs.sqlite.ktx)
    implementation(libs.coil.kt)
    implementation(libs.datetime)
    implementation(libs.coroutines.android)
    implementation(libs.jackson.core)
    implementation(libs.image.viewer)

    androidTestImplementation(platform(libs.junit.bom))
    androidTestImplementation(libs.bundles.test.android)

//    debugImplementation(libs.leakcanary)
}
