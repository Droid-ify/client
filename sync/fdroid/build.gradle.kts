plugins {
    alias(libs.plugins.looker.jvm.library)
    alias(libs.plugins.looker.lint)
}

dependencies {
    modules(
        Modules.coreDomain,
        Modules.coreNetwork,
    )

    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit4)
}
