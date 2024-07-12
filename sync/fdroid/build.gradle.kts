plugins {
    alias(libs.plugins.looker.jvm.library)
    alias(libs.plugins.looker.lint)
}

dependencies {
    modules(
        Modules.coreCommon,
        Modules.coreDomain,
        Modules.coreNetwork,
    )

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.fdroid.index)
    implementation(libs.fdroid.download)
    testImplementation(libs.junit4)
}
