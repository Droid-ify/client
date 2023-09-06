plugins {
	id("looker.android.library")
	kotlin("plugin.serialization")
	id("looker.hilt.work")
}

android {
	namespace = "com.looker.core.data"

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
	modules(
		Modules.coreCommon,
		Modules.coreDatabase,
		Modules.coreDatastore,
		Modules.coreModel,
		Modules.coreNetwork
	)

	coroutines()
	fdroid()
	ktor()

	implementation(Core.core)
	implementation(Kotlin.serialization)
}