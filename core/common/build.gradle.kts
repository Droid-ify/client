import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
	id("looker.android.library")
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.core.common"
	defaultConfig {
		minSdk = Android.minSdk
		vectorDrawables.useSupportLibrary = true
		testInstrumentationRunner = Test.jUnitRunner
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
	kotlin.jvmToolchain(17)
	kotlinOptions {
		freeCompilerArgs += "-Xcontext-receivers"
	}
	buildFeatures {
		aidl = false
		renderScript = false
		shaders = false
		resValues = false
	}
}

dependencies {
	recyclerView()
	coroutines()
	lifecycle()

	implementation(Core.core)

	implementation(Jackson.core)

	testImplementation(kotlin("test"))
	testImplementation(Test.jUnit)
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
