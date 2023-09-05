import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class HiltConventionPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			with(pluginManager) {
				apply(Ksp.plugin)
				apply(Hilt.plugin)
			}

			dependencies {
				"implementation"(Hilt.android)
				"ksp"(Hilt.compiler)
			}
		}
	}
}

class HiltWorkerConventionPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			with(pluginManager) {
//				KSP version of hilt doesn't work with kAPT version of androidX compiler
//				apply(Ksp.plugin)
				apply(Hilt.plugin)
				apply("org.jetbrains.kotlin.kapt")
			}

			dependencies {
				hilt()
			}
		}
	}
}