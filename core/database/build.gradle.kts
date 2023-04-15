plugins {
	id("looker.android.library")
	id("com.google.devtools.ksp") version Version.ksp
	kotlin("plugin.serialization") version Version.kotlin
	id("looker.hilt.work")
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.core.database"
	defaultConfig {
		minSdk = Android.minSdk

		javaCompileOptions {
			annotationProcessorOptions {
				ksp {
					arg("room.schemaLocation", "$projectDir/schemas")
					arg("room.incremental", "true")
				}
			}
		}
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
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	kotlinOptions.jvmTarget = "11"
	buildFeatures {
		buildConfig = false
		aidl = false
		renderScript = false
		shaders = false
		resValues = false
	}
}

dependencies {
	modules(Modules.coreCommon, Modules.coreModel)

	coroutines()
	room()

	implementation(Core.core)
	implementation(Kotlin.serialization)
}