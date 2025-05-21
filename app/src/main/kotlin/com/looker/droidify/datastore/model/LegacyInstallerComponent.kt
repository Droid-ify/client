package com.looker.droidify.datastore.model

import kotlinx.serialization.Serializable

@Serializable
data class LegacyInstallerComponent(
    val clazz: String,
    val activity: String,
) {
    fun update(
        newClazz: String? = null,
        newActivity: String? = null,
    ): LegacyInstallerComponent = copy(
        clazz = newClazz ?: clazz,
        activity = newActivity ?: activity
    )
}
