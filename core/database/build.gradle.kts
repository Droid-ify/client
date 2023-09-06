plugins {
	id("looker.android.library")
	kotlin("plugin.serialization")
	id("looker.room")
	id("looker.hilt")
}

android {
	namespace = "com.looker.core.database"

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
	modules(Modules.coreCommon, Modules.coreModel)

	coroutines()

	implementation(Core.core)
	implementation(Kotlin.serialization)
}