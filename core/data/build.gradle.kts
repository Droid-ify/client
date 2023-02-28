plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	kotlin("plugin.serialization") version Version.kotlin
	id(Hilt.plugin)
}

android {
	namespace = "com.looker.core.data"
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
	implementation(kotlin("stdlib"))
	implementation(project(Modules.coreCommon))
	implementation(project(Modules.coreDatabase))
	implementation(project(Modules.coreDatastore))
	implementation(project(Modules.coreModel))

	implementation(AndroidX.material)
	implementation(Core.core)

	implementation(Coroutines.core)
	implementation(Coroutines.android)

	implementation(Kotlin.serialization)

	implementation(Ktor.core)
	implementation(Ktor.okhttp)

	implementation(Work.manager)

	implementation(Hilt.android)
	implementation(Hilt.work)
	kapt(Hilt.androidX)
	kapt(Hilt.compiler)
}