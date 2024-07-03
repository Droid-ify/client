plugins {
    alias(libs.plugins.looker.android.library)
    alias(libs.plugins.looker.hilt)
    alias(libs.plugins.looker.lint)
}

android {
    namespace = "com.looker.core.di"
}
