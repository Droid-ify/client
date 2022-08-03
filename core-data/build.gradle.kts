plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("plugin.serialization") version Version.kotlin
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.core_data"
	defaultConfig {
		minSdk = Android.minSdk
		targetSdk = Android.compileSdk
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
}

dependencies {
	implementation(project(Modules.coreModel))
	implementation(project(Modules.coreCommon))
	implementation(project(Modules.coreDatabase))
	implementation(Core.core)
	implementation(Coroutines.core)
	implementation(Coroutines.android)
	implementation(OkHttp.okhttp)
}