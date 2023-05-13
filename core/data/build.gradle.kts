plugins {
	id("looker.android.library")
	kotlin("plugin.serialization")
	id("looker.hilt.work")
}

android {
	namespace = "com.looker.core.data"
	compileSdk = Android.compileSdk
	defaultConfig {
		minSdk = Android.minSdk
		testInstrumentationRunner = Test.jUnitRunner
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
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlin.jvmToolchain(17)
	buildFeatures {
		buildConfig = false
		aidl = false
		renderScript = false
		shaders = false
		resValues = false
	}
}

dependencies {
	modules(Modules.coreCommon, Modules.coreDatabase, Modules.coreDatastore, Modules.coreModel)

	coroutines()
	fdroid()
	ktor()

	implementation(Core.core)
	implementation(Kotlin.serialization)
	implementation(Work.manager)

	testImplementation(kotlin("test"))
	testImplementation(Test.jUnit)
}