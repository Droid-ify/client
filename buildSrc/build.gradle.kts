plugins {
	`kotlin-dsl`
}

repositories {
	google()
	gradlePluginPortal()
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
	compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
	compileOnly("com.android.tools.build:gradle-api:7.4.2")
}

gradlePlugin {
	plugins {
		register("hiltPlugin") {
			id = "looker.hilt"
			implementationClass = "HiltConventionPlugin"
		}
		register("hiltWorkPlugin") {
			id = "looker.hilt.work"
			implementationClass = "HiltWorkerConventionPlugin"
		}
		register("androidLibraryPlugin") {
			id = "looker.android.library"
			implementationClass = "AndroidLibraryConventionPlugin"
		}
	}
}