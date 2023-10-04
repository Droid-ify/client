import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltWorkerPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			with(pluginManager) {
				apply(Hilt.plugin)
				apply(Ksp.plugin)
			}

			dependencies {
				hilt(includeWork = true)
				add("implementation", Work.manager)
			}
		}
	}
}