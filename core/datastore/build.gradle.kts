plugins {
	id("looker.android.library")
	id("looker.hilt")
}

android {
	namespace = "com.looker.core.datastore"

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
	modules(Modules.coreCommon)
	coroutines()
	implementation(Datastore.datastore)
	implementation(Kotlin.datetime)
}