plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	id(Hilt.plugin)
}

android {
	namespace = "com.looker.installer"
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
	implementation(project(Modules.coreDatastore))
	implementation(project(Modules.coreModel))

	implementation(kotlin("stdlib"))
	implementation(Coroutines.core)
	implementation(Coroutines.android)

	api(Others.libsu)

	api(Others.shizukuApi)
	api(Others.shizukuProvider)

	implementation(Hilt.android)
	kapt(Hilt.compiler)
}