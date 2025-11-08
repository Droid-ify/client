package com.looker.droidify.compose.appDetail.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
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
    composable<AppDetail> {
        AppDetailScreen(
            onBackClick = onBackClick,
            viewModel = hiltViewModel(),
        )
    }
}
