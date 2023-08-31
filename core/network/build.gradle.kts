plugins {
	id("looker.android.library")
	id("looker.hilt")
}

android {
	namespace = "com.looker.network"
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
		isCoreLibraryDesugaringEnabled = true
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlin.jvmToolchain(17)
	kotlinOptions {
		freeCompilerArgs += "-Xcontext-receivers"
	}
}

dependencies {
	modules(Modules.coreCommon, Modules.coreDatastore, Modules.coreModel)

	coroutines()
	desugar()
	ktor()

	testImplementation(kotlin("test"))
	testImplementation(Test.jUnit)
}