import com.android.build.api.dsl.ApplicationExtension
import com.looker.droidify.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

class AndroidApplicationPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			with(pluginManager) {
				apply("com.android.application")
				apply("org.jetbrains.kotlin.android")
			}

			extensions.configure<ApplicationExtension> {
				configureKotlinAndroid(this)
				buildToolsVersion = DefaultConfig.buildTools
				defaultConfig {
					targetSdk = DefaultConfig.compileSdk
					applicationId = DefaultConfig.appId
					versionCode = DefaultConfig.versionCode
					versionName = DefaultConfig.versionName
				}
				buildFeatures {
					aidl = false
					renderScript = false
					shaders = false
				}
			}
			dependencies {
				add("implementation", embeddedKotlin("stdlib"))
				add("implementation", embeddedKotlin("reflect"))
			}
		}
	}
}