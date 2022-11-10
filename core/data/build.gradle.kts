plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	id(Hilt.plugin)
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.core.data"
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
	implementation(kotlin("stdlib"))
	implementation(project(Modules.coreCommon))

	implementation(AndroidX.material)
	implementation(Core.core)

	implementation(Coroutines.core)
	implementation(Coroutines.android)

	implementation(Hilt.android)
	kapt(Hilt.compiler)
}