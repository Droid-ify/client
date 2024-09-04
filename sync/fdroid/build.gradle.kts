plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.serialization)
    alias(libs.plugins.looker.lint)
}

android {
    namespace = "com.looker.sync.fdroid"
}

dependencies {
    modules(
        Modules.coreCommon,
        Modules.coreDomain,
        Modules.coreNetwork,
    )

    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.ktor.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(kotlin("test"))
}
