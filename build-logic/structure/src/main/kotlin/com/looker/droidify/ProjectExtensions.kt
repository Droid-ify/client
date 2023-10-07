package com.looker.droidify

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency

val Project.libs
	get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.getPlugin(alias: String): Provider<PluginDependency> =
	findPlugin(alias).get()

fun VersionCatalog.getLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
	findLibrary(alias).get()