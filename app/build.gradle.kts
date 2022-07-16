import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
}

android {
	compileSdk = Android.compileSdk
	namespace = "com.looker.droidify"
	defaultConfig {
		applicationId = Android.appId
		minSdk = Android.minSdk
		targetSdk = Android.compileSdk
		versionCode = Android.versionCode
		versionName = Android.versionName
		vectorDrawables.useSupportLibrary = true
	}

	sourceSets.forEach { source ->
		val javaDir = source.java.srcDirs.find { it.name == "java" }
		source.java {
			srcDir(File(javaDir?.parentFile, "kotlin"))
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}

	kotlinOptions {
		jvmTarget = "1.8"
	}

	buildTypes {
		debug {
			applicationIdSuffix = ".debug"
			resValue("string", "application_name", "Droid-ify-Debug")
		}
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			resValue("string", "application_name", "Droid-ify")
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard.pro"
			)
		}
	}
	packagingOptions {
		jniLibs {
			excludes += Excludes.jniExclude
		}
		resources {
			excludes += Excludes.listExclude
		}
	}

	buildFeatures {
		viewBinding = true
	}
}

dependencies {

	implementation(project(Modules.coreModel))
	implementation(project(Modules.coreCommon))
	implementation(project(Modules.coreDatastore))
	implementation(project(Modules.installer))

	implementation(kotlin("stdlib"))
	implementation(Core.core)
	implementation(AndroidX.appCompat)

	implementation(AndroidX.fragment)
	implementation(AndroidX.activity)
	implementation(AndroidX.preference)
	implementation(Lifecycle.runtime)

	implementation(AndroidX.material)

	implementation(Coil.coil)

	implementation(Coroutines.core)
	implementation(Coroutines.android)

	implementation(OkHttp.okhttp)

	implementation(RxJava.android)
	implementation(RxJava.rxjava)

	implementation(Jackson.core)

	implementation(Others.fastScroller)
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