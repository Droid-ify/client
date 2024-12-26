import com.looker.droidify.getLibrary
import com.looker.droidify.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltWorkerPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("looker.hilt")
            }

            dependencies {
                add("implementation", libs.getLibrary("work.ktx"))
                add("implementation", libs.getLibrary("hilt.ext.work"))
                add("ksp", libs.getLibrary("hilt.ext.compiler"))
            }
        }
    }
}
