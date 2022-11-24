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
		targetSdk = Android.compileSdk

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
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
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

	api(Room.runtime)
	api(Room.ktx)
	ksp(Room.compiler)
}