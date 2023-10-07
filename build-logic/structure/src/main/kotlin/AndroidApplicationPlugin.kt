import com.android.build.api.dsl.ApplicationExtension
import com.looker.droidify.configureKotlinAndroid
import com.looker.droidify.getLibrary
import com.looker.droidify.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

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
				add("implementation", platform(libs.getLibrary("kotlin.bom")))
				add("implementation", kotlin("stdlib"))
				add("implementation", kotlin("reflect"))
			}
		}
	}
}