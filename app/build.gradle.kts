import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    alias(libs.plugins.looker.android.application)
    alias(libs.plugins.looker.hilt.work)
    alias(libs.plugins.looker.lint)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.looker.serialization)
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
    implementation(libs.jackson.core)
    implementation(libs.image.viewer)

    implementation(libs.datastore.core)
    implementation(libs.datastore.proto)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.guava)
    implementation(libs.libsu.core)
    implementation(libs.shizuku.api)
    api(libs.shizuku.provider)

    implementation(libs.ktor.core)
    implementation(libs.ktor.okhttp)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.ktor.mock)
    testImplementation(libs.coroutines.test)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.junit.platform)

    androidTestImplementation(platform(libs.junit.bom))
    androidTestImplementation(libs.bundles.test.android)

//    debugImplementation(libs.leakcanary)
}

// using a task as a preBuild dependency instead of a function that takes some time insures that it runs
task("detectAndroidLocals") {
    val langsList: MutableSet<String> = HashSet()

    // in /res are (almost) all languages that have a translated string is saved. this is safer and saves some time
    fileTree("src/main/res").visit {
        if (this.file.path.endsWith("strings.xml") &&
            this.file.canonicalFile.readText().contains("<string")
        ) {
            var languageCode = this.file.parentFile.name.replace("values-", "")
            languageCode = if (languageCode == "values") "en" else languageCode
            langsList.add(languageCode)
        }
    }
    val langsListString = "{${langsList.sorted().joinToString(",") { "\"${it}\"" }}}"
    android.defaultConfig.buildConfigField("String[]", "DETECTED_LOCALES", langsListString)
}
tasks.preBuild.dependsOn("detectAndroidLocals")
