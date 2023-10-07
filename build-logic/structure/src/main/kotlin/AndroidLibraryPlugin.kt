import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import com.looker.droidify.configureKotlinAndroid
import com.looker.droidify.getLibrary
import com.looker.droidify.libs
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
			extensions.configure<LibraryAndroidComponentsExtension> {
				beforeVariants {
					it.enableAndroidTest = it.enableAndroidTest
							&& project.projectDir.resolve("src/androidTest").exists()
				}
			}
			dependencies {
				add("implementation", platform(libs.getLibrary("kotlin.bom")))
				add("implementation", kotlin("stdlib"))
				add("implementation", kotlin("reflect"))
				add("testImplementation", kotlin("test"))
				add("androidTestImplementation", kotlin("test"))
			}
		}
	}
}