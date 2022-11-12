import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.core.common"
	defaultConfig {
		minSdk = Android.minSdk
		targetSdk = Android.compileSdk
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
		aidl = false
		renderScript = false
		shaders = false
		resValues = false
	}
}

dependencies {
	implementation(kotlin("stdlib"))

	implementation(AndroidX.material)
	implementation(Core.core)

	implementation(Coroutines.core)
	implementation(Coroutines.android)

	implementation(Coil.coil)

	api(Jackson.core)

	implementation(Kotlin.datetime)
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