plugins {
    alias(libs.plugins.looker.jvm.library)
    alias(libs.plugins.looker.hilt)
    alias(libs.plugins.looker.lint)
    alias(libs.plugins.ktor)
}

dependencies {
    modules(Modules.coreDI)
    implementation(libs.coroutines.core)
    implementation(libs.ktor.core)
    implementation(libs.ktor.okhttp)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.ktor.mock)
    testImplementation(libs.coroutines.test)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.junit.platform)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
