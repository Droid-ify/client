plugins {
	id("com.android.application") version "8.0.1" apply false
	id("com.android.library") version "8.0.1" apply false
	id("org.jetbrains.kotlin.android") version Version.kotlin apply false
	kotlin("plugin.serialization") version Version.kotlin apply false
	id("com.google.devtools.ksp") version Version.ksp apply false
	id("com.google.dagger.hilt.android") version Hilt.version apply false
}

tasks.register("clean", Delete::class) {
	delete(rootProject.buildDir)
}