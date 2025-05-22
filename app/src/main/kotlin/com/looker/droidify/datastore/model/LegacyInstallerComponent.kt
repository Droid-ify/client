package com.looker.droidify.datastore.model

import kotlinx.serialization.Serializable

@Serializable
sealed class LegacyInstallerComponent {
    @Serializable
    object Unspecified : LegacyInstallerComponent()

    @Serializable
    object AlwaysChoose : LegacyInstallerComponent()

    @Serializable
    data class Component(
        val clazz: String,
        val activity: String,
    ) : LegacyInstallerComponent() {
        fun update(
            newClazz: String? = null,
            newActivity: String? = null,
        ): Component = copy(
            clazz = newClazz ?: clazz,
            activity = newActivity ?: activity
        )
    }
}
