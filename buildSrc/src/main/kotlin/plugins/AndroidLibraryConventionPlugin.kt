import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class AndroidLibraryConventionPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			with(pluginManager) {
				apply("com.android.library")
				apply("org.jetbrains.kotlin.android")
			}

			dependencies {
				add("implementation", platform("org.jetbrains.kotlin:kotlin-bom:${Kotlin.version}"))
				add("coreLibraryDesugaring", AndroidX.desugar)
			}
		}
	}
}