plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
}

android {
	compileSdk = 32
	namespace = "com.looker.core_common"
	defaultConfig {
		minSdk = 23
		targetSdk = 32

		consumerProguardFiles("consumer-rules.pro")
	}

	buildTypes {
		release {
			isMinifyEnabled = false
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
	implementation(kotlin("stdlib"))
	implementation("androidx.core:core-ktx:1.8.0")
}