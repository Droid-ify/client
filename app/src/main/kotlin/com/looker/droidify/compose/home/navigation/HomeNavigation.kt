package com.looker.droidify.compose.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.looker.droidify.compose.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
object Home

fun NavController.navigateToHome() {
    this.navigate(Home, navOptions {
        launchSingleTop = true
        restoreState = true
    })
}

fun NavGraphBuilder.home(
    onNavigateToApps: () -> Unit,
    onNavigateToRepos: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    composable<Home> {
        HomeScreen(
            onNavigateToApps = onNavigateToApps,
            onNavigateToRepos = onNavigateToRepos,
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}
