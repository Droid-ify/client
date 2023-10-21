import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

class AndroidLintPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jlleitschuh.gradle.ktlint")
            }

            tasks.getByPath("preBuild").dependsOn("ktlintFormat")
            extensions.configure<KtlintExtension> {
                android.set(true)
                ignoreFailures.set(false)
                debug.set(true)
                reporters {
                    reporter(ReporterType.HTML)
                }
                filter {
                    exclude("**/generated/**")
                }
            }
        }
    }
}
