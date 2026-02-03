package com.looker.droidify.compose.settings.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.looker.droidify.compose.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object Settings

fun NavController.navigateToSettings() {
    navigate(Settings, navOptions {
        launchSingleTop = true
        restoreState = true
    })
}

fun NavGraphBuilder.settings(
    onBackClick: () -> Unit,
) {
    composable<Settings> {
        SettingsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
        )
    }
}
