package com.looker.droidify.compose.appList.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.looker.droidify.compose.appList.AppListScreen
import com.looker.droidify.compose.appList.AppListViewModel
import kotlinx.serialization.Serializable

@Serializable
object AppList

fun NavController.navigateToAppList() {
    this.navigate(AppList, navOptions {
        launchSingleTop = true
        restoreState = true
    })
}

fun NavGraphBuilder.appList(
    onAppClick: (String) -> Unit = { _ -> },
) {
    composable<AppList> {
        val viewModel: AppListViewModel = hiltViewModel()
        AppListScreen(onAppClick = onAppClick, viewModel = viewModel)
    }
}
