import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class HiltConventionPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			with(pluginManager) {
				apply(Hilt.plugin)
				// KAPT must go last to avoid build warnings.
				// See: https://stackoverflow.com/questions/70550883/warning-the-following-options-were-not-recognized-by-any-processor-dagger-f
				apply("org.jetbrains.kotlin.kapt")
			}

			dependencies {
				"implementation"(Hilt.android)
				"kapt"(Hilt.compiler)
			}
		}
	}
}

class HiltWorkerConventionPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			with(pluginManager) {
				apply(Hilt.plugin)
				apply("org.jetbrains.kotlin.kapt")
			}

			dependencies {
				"implementation"(Hilt.android)
				"implementation"(Hilt.work)
				"kapt"(Hilt.compiler)
				"kapt"(Hilt.androidX)
			}
		}
	}
}