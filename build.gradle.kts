plugins {
	id("com.android.application") version "8.0.2" apply false
	id("com.android.library") version "8.0.2" apply false
	id("org.jetbrains.kotlin.android") version Kotlin.version apply false
	kotlin("plugin.serialization") version Kotlin.version apply false
	id(Ksp.plugin) version Ksp.version apply false
	id(Hilt.plugin) version Hilt.version apply false
}

tasks.register("clean", Delete::class) {
	delete(rootProject.buildDir)
}