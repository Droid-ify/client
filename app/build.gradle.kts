import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose)
}

android {
    val latestVersionName = "0.6.8"
    namespace = "com.looker.droidify"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.looker.droidify"
        minSdk = 23
        versionName = latestVersionName
        versionCode = versionCode(versionName)

        testInstrumentationRunner = "com.looker.droidify.TestRunner"
    }

    compileOptions.isCoreLibraryDesugaringEnabled = true
    androidResources.generateLocaleConfig = true

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.generateKotlin", "true")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard.pro",
            )
        }
        create("alpha") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".alpha"
            versionNameSuffix = ".a"
            isMinifyEnabled = true
            isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = ".d"
        }
        all {
            buildConfigField(
                type = "String",
                name = "VERSION_NAME",
                value = "\"v$latestVersionName\"",
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
                "/META-INF/{AL2.0,LGPL2.1,LICENSE*}",
                "/META-INF/versions/9/previous-**.bin",
            )
        }
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-Xcontext-parameters")
            optIn.add("kotlin.RequiresOptIn")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.material)
    implementation(libs.core.ktx)
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.viewModel)
    implementation(libs.recyclerview)
    implementation(libs.sqlite.ktx)
    implementation(libs.paging)

    implementation(libs.image.viewer)
    implementation(libs.bundles.coil)

    implementation(libs.datastore.core)
    implementation(libs.datastore.proto)

    implementation(libs.kotlin.stdlib)
    implementation(libs.datetime)

    implementation(libs.bundles.coroutines)

    implementation(libs.libsu.core)
    implementation(libs.bundles.shizuku)

    implementation(libs.jackson.core)
    implementation(libs.serialization)

    implementation(libs.bundles.ktor)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    implementation(libs.work.ktx)

    implementation(libs.hilt.core)
    implementation(libs.hilt.android)
    implementation(libs.hilt.ext.work)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.ext.compiler)

    // Compose dependencies
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.room.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.test.core)
    testImplementation(libs.test.core.ktx)
    testRuntimeOnly(libs.junit.platform)
    androidTestImplementation(libs.hilt.test)
    androidTestImplementation(libs.room.test)
    androidTestImplementation(libs.bundles.test.android)
    kspAndroidTest(libs.hilt.compiler)

//    debugImplementation(libs.leakcanary)
}

// using a task as a preBuild dependency instead of a function that takes some time insures that it runs
// in /res are (almost) all languages that have a translated string is saved. this is safer and saves some time
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

fun versionCode(version: String?): Int? {
    if (version == null) return null
    val (major, minor, patch) = version
        .substringBefore('-')
        .trim()
        .split('.')
        .map { it.toUIntOrNull() }

    require(major != null && minor != null && patch != null) {
        "Each segment must be within 0..99 for mapping, was: '$version'"
    }

    return (major * 1000u + minor * 100u + patch * 10u).toInt()
}
