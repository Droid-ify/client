package com.looker.droidify

import org.gradle.api.Plugin
import org.gradle.api.Project

class JvmLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
                apply("looker.lint")
            }
            configureKotlinJvm()
        }
    }
}
