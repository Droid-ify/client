import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import com.looker.droidify.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.embeddedKotlin

class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("looker.lint")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = DefaultConfig.compileSdk
                buildFeatures {
                    aidl = false
                    renderScript = false
                    shaders = false
                    resValues = false
                }
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                beforeVariants {
                    it.enableAndroidTest = it.enableAndroidTest
                        && project.projectDir.resolve("src/androidTest").exists()
                }
            }
            dependencies {
                add("implementation", embeddedKotlin("stdlib"))
                add("implementation", embeddedKotlin("reflect"))
                add("testImplementation", embeddedKotlin("test"))
                add("androidTestImplementation", embeddedKotlin("test"))
            }
        }
    }
}
