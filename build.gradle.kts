plugins {
	id("com.android.application") version "7.2.1" apply false
	id("com.android.library") version "7.2.1" apply false
	id("org.jetbrains.kotlin.android") version Version.kotlin apply false
}

tasks.register("clean", Delete::class) {
	delete(rootProject.buildDir)
}