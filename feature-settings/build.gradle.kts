plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	id(Hilt.plugin)
}

android {
	namespace = "com.looker.feature_settings"
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
	implementation(project(Modules.installer))

	kotlin("stdlib")
	implementation(Core.core)
	implementation(AndroidX.appCompat)
	implementation(AndroidX.material)
	implementation(Lifecycle.fragment)
	implementation(Lifecycle.viewmodel)

	implementation(Hilt.android)
	kapt(Hilt.compiler)
}
