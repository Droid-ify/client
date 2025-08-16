package com.looker.droidify.compose.appDetail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.looker.droidify.compose.appDetail.AppDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data class AppDetail(val packageName: String)

fun NavController.navigateToAppDetail(packageName: String) {
    this.navigate(AppDetail(packageName), navOptions {
        launchSingleTop = true
    })
}

fun NavGraphBuilder.appDetail(
    onBackClick: () -> Unit,
) {
    composable<AppDetail> { backStackEntry ->
        val appDetail = backStackEntry.toRoute<AppDetail>()
        AppDetailScreen(packageName = appDetail.packageName, onBack = onBackClick)
    }
}
