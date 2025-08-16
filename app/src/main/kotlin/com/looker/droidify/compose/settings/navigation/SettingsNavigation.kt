package com.looker.droidify.compose.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.looker.droidify.compose.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object Settings

fun NavController.navigateToSettings() {
    this.navigate(Settings)
}

fun NavGraphBuilder.settings(
    onBackClick: () -> Unit,
) {
    composable<Settings> {
        SettingsScreen(onBackClick = onBackClick)
    }
}
