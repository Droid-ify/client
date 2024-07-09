import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.ktlint)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("lintPlugin") {
            id = "looker.lint"
            implementationClass = "AndroidLintPlugin"
        }
        register("serializationPlugin") {
            id = "looker.serialization"
            implementationClass = "AndroidSerializationPlugin"
        }
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
        register("jvmLibraryPlugin") {
            id = "looker.jvm.library"
            implementationClass = "JvmLibraryPlugin"
        }
    }
}
