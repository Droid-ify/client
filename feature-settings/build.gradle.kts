plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	id(Hilt.plugin)
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.feature_settings"
	defaultConfig {
		minSdk = Android.minSdk
		targetSdk = Android.compileSdk
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
		}
		all {
			buildConfigField(
				type = "String",
				name = "VERSION_NAME",
				value = "\"v${Android.versionName}\""
			)
			buildConfigField(
				type = "int",
				name = "VERSION_CODE",
				value = "${Android.versionCode}"
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
	buildFeatures {
		viewBinding = true
	}
}

dependencies {
	implementation(project(Modules.coreCommon))
	implementation(project(Modules.coreDatastore))

	kotlin("stdlib")
	implementation(Core.core)
	implementation(AndroidX.appCompat)
	implementation(AndroidX.material)
	implementation(Lifecycle.fragment)
	implementation(Lifecycle.viewmodel)

	implementation(Hilt.android)
	kapt(Hilt.compiler)

	implementation(Others.libsu)
	implementation(Others.shizukuApi)
	implementation(Others.shizukuProvider)
}

kapt {
	correctErrorTypes = true
}