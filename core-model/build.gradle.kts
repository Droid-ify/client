plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("plugin.serialization") version "1.7.10"
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.core_model"
	defaultConfig {
		minSdk = Android.minSdk
		targetSdk = Android.compileSdk

		consumerProguardFiles("consumer-rules.pro")
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
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
	implementation(project(Modules.coreCommon))
	implementation(Core.core)
	api(Kotlin.serialization)
}