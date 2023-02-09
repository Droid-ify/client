plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.core.common"
	defaultConfig {
		minSdk = Android.minSdk
		vectorDrawables.useSupportLibrary = true
	}

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
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
	buildFeatures {
		aidl = false
		renderScript = false
		shaders = false
		resValues = false
	}
}

dependencies {
	implementation(kotlin("stdlib"))

	implementation(AndroidX.material)
	implementation(Core.core)

	implementation(Coroutines.core)
	implementation(Coroutines.android)

	implementation(Jackson.core)

	implementation(Kotlin.datetime)

	implementation(OkHttp.okhttp)
}
