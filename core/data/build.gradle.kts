plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	kotlin("plugin.serialization") version Version.kotlin
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
	buildFeatures {
		buildConfig = false
		aidl = false
		renderScript = false
		shaders = false
		resValues = false
	}
}

dependencies {
	implementation(kotlin("stdlib"))
	implementation(project(Modules.coreCommon))
	implementation(project(Modules.coreDatabase))
	implementation(project(Modules.coreModel))

	implementation(AndroidX.material)
	implementation(Core.core)

	implementation(Coroutines.core)
	implementation(Coroutines.android)

	implementation(Kotlin.serialization)

	implementation(Hilt.android)
	kapt(Hilt.compiler)
}

kapt {
	correctErrorTypes = true
}