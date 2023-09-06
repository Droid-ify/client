plugins {
	id("looker.android.library")
	id("looker.hilt")
}

android {
	namespace = "com.looker.network"

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
}

dependencies {
	modules(Modules.coreCommon, Modules.coreDatastore, Modules.coreModel)

	coroutines()
	desugar()
	ktor()
}