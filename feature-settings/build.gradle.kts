import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	kotlin("kapt")
	id(Hilt.plugin)
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.feature_settings"
	defaultConfig {
		minSdk = Android.minSdk
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
		all {
			buildConfigField(
				type = "String",
				name = "VERSION_NAME",
				value = "\"v${Android.versionName}\""
			)
			buildConfigField(
				type = "int",
				name = "VERSION_CODE",
				value = "${Android.versionCode}"
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
	buildFeatures {
		viewBinding = true
	}
}

dependencies {
	implementation(project(Modules.coreCommon))
	implementation(project(Modules.coreDatastore))
	implementation(project(Modules.installer))

	kotlin("stdlib")
	implementation(Core.core)
	implementation(AndroidX.appCompat)
	implementation(AndroidX.material)
	implementation(Lifecycle.fragment)
	implementation(Lifecycle.viewmodel)

	implementation(Hilt.android)
	kapt(Hilt.compiler)
}

// using a task as a preBuild dependency instead of a function that takes some time insures that it runs
task("detectAndroidLocals") {
	val langsList: MutableSet<String> = HashSet()

	// in /res are (almost) all languages that have a translated string is saved. this is safer and saves some time
	fileTree("src/main/res").visit {
		if (this.file.path.endsWith("strings.xml")
			&& this.file.canonicalFile.readText().contains("<string")
		) {
			var languageCode = this.file.parentFile.name.replace("values-", "")
			languageCode = if (languageCode == "values") "en" else languageCode
			langsList.add(languageCode)
		}
	}
	val langsListString = "{${langsList.joinToString(",") { "\"${it}\"" }}}"
	android.defaultConfig.buildConfigField("String[]", "DETECTED_LOCALES", langsListString)
}
tasks.preBuild.dependsOn("detectAndroidLocals")
