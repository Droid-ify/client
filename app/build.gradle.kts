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
			excludes += Excludes.jniExclude
		}
		resources {
			excludes += Excludes.listExclude
		}
	}
	buildFeatures {
		viewBinding = true
	}
}

dependencies {

	implementation(project(Modules.coreModel))
	implementation(project(Modules.coreCommon))
	implementation(project(Modules.coreDatastore))
	implementation(project(Modules.featureSettings))
	implementation(project(Modules.installer))

	implementation(kotlin("stdlib"))
	implementation(Core.core)
	implementation(AndroidX.appCompat)

	implementation(AndroidX.fragment)
	implementation(AndroidX.activity)
	implementation(AndroidX.preference)
	implementation(Lifecycle.runtime)

	implementation(AndroidX.material)

	implementation(Coil.coil)

	implementation(Coroutines.core)
	implementation(Coroutines.android)

	implementation(OkHttp.okhttp)

	implementation(RxJava.android)
	implementation(RxJava.rxjava)

	implementation(Jackson.core)

	implementation(Others.fastScroller)

	implementation(Hilt.android)
	implementation(Hilt.work)
	kapt(Hilt.compiler)
	kapt(Hilt.androidX)

	// WorkManager
	implementation(Work.manager)
}