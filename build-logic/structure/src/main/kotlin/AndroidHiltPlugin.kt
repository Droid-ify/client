import com.android.build.gradle.api.AndroidBasePlugin
import com.looker.droidify.getLibrary
import com.looker.droidify.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            dependencies {
                add("ksp", libs.getLibrary("hilt.compiler"))
                add("implementation", libs.getLibrary("hilt.core"))
            }

            /** Add support for Android modules, based on [AndroidBasePlugin] */
            pluginManager.withPlugin("com.android.base") {
                pluginManager.apply("dagger.hilt.android.plugin")
                dependencies {
                    add("implementation", libs.getLibrary("hilt.android"))
                }
            }
        }
    }
}
