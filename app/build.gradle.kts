plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	id(Hilt.plugin)
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.droidify"
	defaultConfig {
		applicationId = Android.appId
		minSdk = Android.minSdk
		targetSdk = Android.compileSdk
		versionCode = Android.versionCode
		versionName = Android.versionName
		vectorDrawables.useSupportLibrary = true

		resourceConfigurations += mutableListOf(
			"nn",
			"zh-rCN",
			"de",
			"hi",
			"fi",
			"ru",
			"pt",
			"bg",
			"ar-rSA",
			"lt",
			"hr",
			"lv",
			"fr",
			"hu",
			"si",
			"uk",
			"ca",
			"kn",
			"or",
			"gl",
			"in",
			"el",
			"zh-rTW",
			"en",
			"nb-rNO",
			"it",
			"es",
			"iw",
			"pt-rBR",
			"cs",
			"ar",
			"vi",
			"ja",
			"tl",
			"fa",
			"pl",
			"nl",
			"tr"
		)
	}

	sourceSets.forEach { source ->
		val javaDir = source.java.srcDirs.find { it.name == "java" }
		source.java {
			srcDir(File(javaDir?.parentFile, "kotlin"))
		}
	}

	compileOptions {
		isCoreLibraryDesugaringEnabled = true

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
			excludes += Excludes.jniExclude
		}
		resources {
			excludes += Excludes.listExclude
		}
	}
	buildFeatures {
		aidl = false
		renderScript = false
		shaders = false
	}
	buildFeatures {
		viewBinding = true
	}
}

dependencies {

	coreLibraryDesugaring(AndroidX.desugar)

	implementation(project(Modules.coreModel))
	implementation(project(Modules.coreCommon))
	implementation(project(Modules.coreDatastore))
	implementation(project(Modules.downloader))
	implementation(project(Modules.featureSettings))
	implementation(project(Modules.installer))

	implementation(kotlin("stdlib"))
	implementation(Core.core)

	implementation(AndroidX.appCompat)
	implementation(AndroidX.preference)
	implementation(AndroidX.recyclerView)
	implementation(AndroidX.material)

	implementation(Coil.coil)

	implementation(Coroutines.core)
	implementation(Coroutines.android)

	implementation(Hilt.android)
	implementation(Hilt.work)
	kapt(Hilt.compiler)
	kapt(Hilt.androidX)

	implementation(Jackson.core)

	implementation(Kotlin.datetime)
	implementation(Ktor.okhttp)
	implementation(Ktor.logging)

	implementation(Lifecycle.fragment)
	implementation(Lifecycle.activity)
	implementation(Lifecycle.runtime)

	implementation(OkHttp.okhttp)

	implementation(Others.zoomage)

	implementation(RxJava.android)
	implementation(RxJava.rxjava)

	implementation(Work.manager)
}