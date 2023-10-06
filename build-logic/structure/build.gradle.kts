
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    compileOnly("com.android.tools.build:gradle:8.1.2")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
    compileOnly("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.10-1.0.13")
}

gradlePlugin {
    plugins {
        register("hiltPlugin") {
            id = "looker.hilt"
            implementationClass = "AndroidHiltPlugin"
        }
        register("hiltWorkPlugin") {
            id = "looker.hilt.work"
            implementationClass = "AndroidHiltWorkerPlugin"
        }
        register("roomPlugin") {
            id = "looker.room"
            implementationClass = "AndroidRoomPlugin"
        }
        register("androidApplicationPlugin") {
            id = "looker.android.application"
            implementationClass = "AndroidApplicationPlugin"
        }
        register("androidLibraryPlugin") {
            id = "looker.android.library"
            implementationClass = "AndroidLibraryPlugin"
        }
    }
}