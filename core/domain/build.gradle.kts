plugins {
    alias(libs.plugins.looker.jvm.library)
    alias(libs.plugins.looker.lint)
}

dependencies {
    modules(Modules.coreCommon, Modules.coreNetwork)
}
