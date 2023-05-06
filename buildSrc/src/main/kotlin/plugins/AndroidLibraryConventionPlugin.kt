import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class AndroidLibraryConventionPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			with(pluginManager) {
				apply("com.android.library")
				apply("org.jetbrains.kotlin.android")
			}

			tasks.withType(JavaCompile::class.java) {
				sourceCompatibility = JavaVersion.VERSION_17.toString()
				targetCompatibility = JavaVersion.VERSION_17.toString()
			}

			dependencies {
				add("implementation", kotlin("stdlib"))
			}
		}
	}
}