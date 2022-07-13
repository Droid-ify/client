import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
}

android {
	compileSdk = 32
	namespace = "com.looker.droidify"
	defaultConfig {
		applicationId = "com.looker.droidify"
		minSdk = 23
		targetSdk = 32
		versionCode = 46
		versionName = "0.4.6"
		vectorDrawables.useSupportLibrary = true
	}

	sourceSets.forEach { source ->
		val javaDir = source.java.srcDirs.find { it.name == "java" }
		source.java {
			srcDir(File(javaDir?.parentFile, "kotlin"))
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}

	kotlinOptions {
		jvmTarget = "1.8"
	}

	buildTypes {
		debug {
			applicationIdSuffix = ".debug"
			resValue("string", "application_name", "Droid-ify-Debug")
		}
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			resValue("string", "application_name", "Droid-ify")
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard.pro"
			)
		}
	}
	packagingOptions {
		jniLibs {
			excludes += "/okhttp3/internal/publicsuffix/*"
		}
		resources {
			excludes += listOf(
				"/DebugProbesKt.bin",
				"/kotlin/**.kotlin_builtins",
				"/kotlin/**.kotlin_metadata",
				"/META-INF/**.kotlin_module",
				"/META-INF/**.pro",
				"/META-INF/**.version",
				"/okhttp3/internal/publicsuffix/*"
			)
		}
	}

	buildFeatures {
		viewBinding = true
	}
}

dependencies {

	// Core
	implementation(kotlin("stdlib"))
	implementation("androidx.core:core-ktx:1.8.0")
	implementation("androidx.appcompat:appcompat:1.4.2")
	implementation("androidx.fragment:fragment-ktx:1.5.0")
	implementation("androidx.activity:activity-ktx:1.5.0")
	implementation("androidx.preference:preference-ktx:1.2.0")
	implementation("me.zhanghai.android.fastscroll:library:1.1.8")

	// Material3
	implementation("com.google.android.material:material:1.6.1")

	// Coil
	implementation("io.coil-kt:coil:2.1.0")

	// OkHttps
	implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.9")

	// RxJava
	implementation("io.reactivex.rxjava3:rxjava:3.1.5")
	implementation("io.reactivex.rxjava3:rxandroid:3.0.0")

	// LibSu
	implementation("com.github.topjohnwu.libsu:core:3.2.1")

	// JackSon
	implementation("com.fasterxml.jackson.core:jackson-core:2.13.3")

	// Coroutines / Lifecycle
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.3")
}

// using a task as a preBuild dependency instead of a function that takes some time insures that it runs
task("detectAndroidLocals") {
	val langsList: MutableSet<String> = HashSet()

	// in /res are (almost) all languages that have a translated string is saved. this is safer and saves some time
	fileTree("src/main/res").visit {
		if (this.file.path.endsWith("strings.xml")
			&& this.file.canonicalFile.readText().contains("<string")
		) {
			var languageCode = this.file.parentFile.name.replace("values-", "")
			languageCode = if (languageCode == "values") "en" else languageCode
			langsList.add(languageCode)
		}
	}
	val langsListString = "{${langsList.joinToString(",") { "\"${it}\"" }}}"
	android.defaultConfig.buildConfigField("String[]", "DETECTED_LOCALES", langsListString)
}
tasks.preBuild.dependsOn("detectAndroidLocals")