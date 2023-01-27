pluginManagement {
	repositories {
		gradlePluginPortal()
		google()
		mavenCentral()
	}
}
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
		maven(url = "https://jitpack.io")
	}
}

rootProject.name = "Droid-ify"
include(
	":app",
	":core:common",
	":core:data",
	":core:database",
	":core:datastore",
	":core:model",
	":core:sync",
	":feature-settings",
	":installer"
)
