package com.looker.droidify.compose.settings.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
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
        SettingsScreen()
    }
}

@Composable
private fun SettingsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "SETTINGS SCREEN")
    }
}
