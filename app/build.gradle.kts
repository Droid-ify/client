plugins {
	id("looker.android.application")
	id("looker.hilt.work")
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
			excludes += Excludes.listExclude
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

	implementation(Core.core)

	androidX()
	coroutines()
	desugar()
	lifecycle()

	implementation(Coil.coil)

	implementation(Jackson.core)

	implementation(Kotlin.datetime)

	implementation(Others.zoomage)

	implementation(SQLite.ktx)
}
