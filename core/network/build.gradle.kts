plugins {
    alias(libs.plugins.looker.jvm.library)
    alias(libs.plugins.looker.hilt)
    alias(libs.plugins.looker.lint)
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ktor.core)
    implementation(libs.ktor.okhttp)
}
