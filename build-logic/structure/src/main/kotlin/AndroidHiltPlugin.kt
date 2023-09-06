import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			with(pluginManager) {
				apply(Hilt.plugin)
				apply("org.jetbrains.kotlin.kapt")
			}

			dependencies {
				hilt(includeWork = false)
			}
		}
	}
}