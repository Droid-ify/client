import com.google.devtools.ksp.gradle.KspExtension
import com.looker.droidify.getLibrary
import com.looker.droidify.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

class AndroidRoomPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		with(target) {
			pluginManager.apply("com.google.devtools.ksp")

			extensions.configure<KspExtension> {
				// The schemas directory contains a schema file for each version of the Room database.
				// This is required to enable Room auto migrations.
				// See https://developer.android.com/reference/kotlin/androidx/room/AutoMigration.
				arg(RoomSchemaArgProvider(File(projectDir, "schemas")))
			}

			dependencies {
				add("implementation", libs.getLibrary("room.ktx"))
				add("implementation", libs.getLibrary("room.runtime"))
				add("ksp", libs.getLibrary("room.compiler"))
			}
		}
	}

	/**
	 * https://issuetracker.google.com/issues/132245929
	 * [Export schemas](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
	 */
	class RoomSchemaArgProvider(
		@get:InputDirectory
		@get:PathSensitive(PathSensitivity.RELATIVE)
		val schemaDir: File,
	) : CommandLineArgumentProvider {
		override fun asArguments() = listOf("room.schemaLocation=${schemaDir.path}")
	}
}