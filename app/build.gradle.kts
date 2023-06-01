plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("looker.hilt.work")
}

android {
	compileSdk = Android.compileSdk
	buildToolsVersion = "33.0.2"
	namespace = "com.looker.droidify"
	defaultConfig {
		applicationId = Android.appId
		minSdk = Android.minSdk
		targetSdk = Android.compileSdk
		versionCode = Android.versionCode
		versionName = Android.versionName
		vectorDrawables.useSupportLibrary = true

		resourceConfigurations += mutableListOf(
			/* locale list begin */
			"ar",
			"az",
			"bg",
			"bn",
			"ca",
			"cs",
			"de",
			"el",
			"es",
			"fa",
			"fi",
			"fr",
			"gl",
			"hi",
			"hr",
			"hu",
			"in",
			"it",
			"iw",
			"ja",
			"kn",
			"ko",
			"lt",
			"lv",
			"ml",
			"nb-rNO",
			"nl",
			"nn",
			"or",
			"pa",
			"pl",
			"pt",
			"pt-rBR",
			"ro",
			"ru",
			"sr",
			"sv",
			"tl",
			"tr",
			"uk",
			"vi",
			"zh-rCN",
			"zh-rTW"
			/* locale list end */
		)
	}

	sourceSets.forEach { source ->
		val javaDir = source.java.srcDirs.find { it.name == "java" }
		source.java {
			srcDir(File(javaDir?.parentFile, "kotlin"))
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlin.jvmToolchain(17)

	buildTypes {
		getByName("debug") {
			applicationIdSuffix = ".debug"
			resValue("string", "application_name", "Droid-ify-Debug")
		}
		getByName("release") {
			isMinifyEnabled = true
			isShrinkResources = true
			resValue("string", "application_name", "Droid-ify")
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard.pro"
			)
		}
		create("alpha") {
			initWith(getByName("debug"))
			applicationIdSuffix = ".alpha"
			resValue("string", "application_name", "Droid-ify Alpha")
			isDebuggable = true
			isMinifyEnabled = true
		}
		all {
			buildConfigField(
				type = "String",
				name = "VERSION_NAME",
				value = "\"v${Android.versionName}\""
			)
		}
	}
	packaging {
		jniLibs {
			excludes += Excludes.jniExclude
		}
		resources {
			excludes += Excludes.listExclude
		}
	}
	buildFeatures {
		viewBinding = true
		aidl = false
		renderScript = false
		shaders = false
	}
}

dependencies {
	modules(
		Modules.coreModel,
		Modules.coreCommon,
		Modules.coreData,
		Modules.coreDatastore,
		Modules.installer
	)

	implementation(kotlin("stdlib"))
	implementation(Core.core)

	androidX()
	coroutines()
	ktor()
	lifecycle()

	implementation(Coil.coil)

	implementation(Jackson.core)

	implementation(Kotlin.datetime)

	implementation(OkHttp.okhttp)

	implementation(Others.zoomage)

	implementation(SQLite.ktx)

	implementation(Work.manager)
}