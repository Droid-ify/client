plugins {
	alias(libs.plugins.looker.android.application)
	alias(libs.plugins.looker.android.hilt.work)
}

android {
	namespace = "com.looker.droidify"
	defaultConfig {
		vectorDrawables.useSupportLibrary = true

		resourceConfigurations += mutableListOf(
			/* locale list begin */
			"ar",
			"az",
			"be",
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
			"ia",
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
			"ryu",
			"si",
			"sl",
			"sr",
			"sv",
			"tl",
			"tr",
			"uk",
			"ur",
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
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard.pro"
			)
			isDebuggable = true
			isMinifyEnabled = true
		}
		all {
			buildConfigField(
				type = "String",
				name = "VERSION_NAME",
				value = "\"v${DefaultConfig.versionName}\""
			)
		}
	}
	packaging {
		resources {
			excludes += listOf(
				"/DebugProbesKt.bin",
				"/kotlin/**.kotlin_builtins",
				"/kotlin/**.kotlin_metadata",
				"/META-INF/**.kotlin_module",
				"/META-INF/**.pro",
				"/META-INF/**.version",
				"/META-INF/versions/9/previous-**.bin"
			)
		}
	}
	buildFeatures {
		viewBinding = true
		buildConfig = true
	}
}

dependencies {

	modules(
		Modules.coreModel,
		Modules.coreCommon,
		Modules.coreNetwork,
		Modules.coreDatastore,
		Modules.installer
	)

	implementation(libs.android.material)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.activity.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.fragment.ktx)
	implementation(libs.androidx.lifecycle.viewModel.ktx)
	implementation(libs.androidx.recyclerview)
	implementation(libs.androidx.sqlite.ktx)
	implementation(libs.coil.kt)
	implementation(libs.kotlinx.datetime)
	implementation(libs.kotlinx.coroutines.android)
	implementation(libs.jackson.core)
	implementation(libs.zoomage)
}
