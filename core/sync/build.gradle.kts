plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	id(Hilt.plugin)
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.sync"
	defaultConfig {
		minSdk = Android.minSdk
		targetSdk = Android.compileSdk

		testInstrumentationRunner = Test.jUnitRunner
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
	implementation(project(Modules.coreData))
	implementation(project(Modules.coreDatastore))
	implementation(project(Modules.coreModel))

	implementation(Core.core)
	implementation(Lifecycle.livedata)

	implementation(Work.manager)
	implementation(Hilt.android)
	implementation(Hilt.work)

	kapt(Hilt.compiler)

	testImplementation(Test.jUnit)
	androidTestImplementation(Test.androidJUnit)
	androidTestImplementation(Test.espresso)
}