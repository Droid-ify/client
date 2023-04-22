plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	id("com.google.devtools.ksp") version Version.ksp
	kotlin("plugin.serialization") version Version.kotlin
	id(Hilt.plugin)
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
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlinOptions.jvmTarget = "17"
	buildFeatures {
		buildConfig = false
		aidl = false
		renderScript = false
		shaders = false
		resValues = false
	}
}

dependencies {
	implementation(kotlin("stdlib"))
	implementation(project(Modules.coreCommon))
	implementation(project(Modules.coreModel))

	implementation(Core.core)

	implementation(Coroutines.core)
	implementation(Coroutines.android)

	implementation(Hilt.android)
	kapt(Hilt.compiler)

	implementation(Kotlin.serialization)

	implementation(Room.runtime)
	implementation(Room.ktx)
	ksp(Room.compiler)
}