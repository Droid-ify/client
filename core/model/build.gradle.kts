plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
}

android {
	namespace = "com.looker.core.model"
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
		buildConfig = false
		aidl = false
		renderScript = false
		shaders = false
		resValues = false
	}
}

dependencies {
	implementation(project(Modules.coreCommon))
	implementation(Jackson.core)
}