plugins {
    alias(libs.plugins.looker.jvm.library)
    alias(libs.plugins.looker.lint)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
