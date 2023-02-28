plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	id(Hilt.plugin)
}

android {
	namespace = "com.looker.feature.repo"
	compileSdk = Android.compileSdk
	defaultConfig.minSdk = Android.minSdk

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
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	kotlinOptions.jvmTarget = "11"
	buildFeatures {
		viewBinding = true
	}
}

dependencies {
	implementation(project(Modules.coreCommon))
	implementation(project(Modules.coreData))
	implementation(project(Modules.coreModel))

	implementation(Core.core)
	implementation(AndroidX.material)
	implementation(Lifecycle.fragment)
	implementation(Lifecycle.viewmodel)

	implementation(Hilt.android)
	kapt(Hilt.compiler)
}