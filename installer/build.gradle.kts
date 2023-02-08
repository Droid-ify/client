plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	id(Hilt.plugin)
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.installer"
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
	implementation(project(Modules.coreDatastore))
	implementation(project(Modules.coreModel))

	implementation(Core.core)
	implementation(kotlin("stdlib"))
	implementation(Coroutines.core)
	implementation(Coroutines.android)

	implementation(Others.libsu)

	api(Others.shizukuApi)
	api(Others.shizukuProvider)

	implementation(Hilt.android)
	kapt(Hilt.compiler)
}