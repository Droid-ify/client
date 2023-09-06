import com.android.build.gradle.LibraryExtension
import com.looker.droidify.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class AndroidLibraryPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			with(pluginManager) {
				apply("com.android.library")
				apply("org.jetbrains.kotlin.android")
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
			dependencies {
				add("testImplementation", kotlin("test"))
				add("androidTestImplementation", kotlin("test"))
			}
		}
	}
}