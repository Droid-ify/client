plugins {
	id("looker.android.library")
	id("looker.hilt")
}

android {
	namespace = "com.looker.installer"

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
	modules(Modules.coreCommon, Modules.coreDatastore)

	implementation(AndroidX.appCompat)
	coroutines()

	implementation(Others.libsu)

	implementation(Shizuku.api)
	api(Shizuku.provider)
}