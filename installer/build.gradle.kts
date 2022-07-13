plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
}

android {
	compileSdk = 32
	namespace = "com.looker.installer"
	defaultConfig {
		minSdk = 23
		targetSdk = 32

		consumerProguardFiles("consumer-rules.pro")
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
}

dependencies {
	implementation(project(":core-common"))

	implementation("androidx.core:core-ktx:1.8.0")
	implementation(kotlin("stdlib"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.3")

	api("com.github.topjohnwu.libsu:core:3.2.1")

	val shizukuVersion = "12.0.0"
	api("dev.rikka.shizuku:api:$shizukuVersion")
	api("dev.rikka.shizuku:provider:$shizukuVersion")
}