plugins {
	id("looker.android.library")
	id("looker.hilt.work")
}

android {
	namespace = "com.looker.core.data"

	buildTypes {
		release {
			// TODO: Enable once using
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
		}
		create("alpha") {
			initWith(getByName("debug"))
			isMinifyEnabled = true
		}
	}
}

dependencies {
	modules(
		Modules.coreCommon,
		Modules.coreDatabase,
		Modules.coreDatastore,
		Modules.coreModel,
		Modules.coreNetwork
	)

	coroutines()
	fdroid()
}