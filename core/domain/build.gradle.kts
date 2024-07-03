plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.lint)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.looker.core.domain"
}

dependencies {
    modules(Modules.coreCommon, Modules.coreNetwork)
}
