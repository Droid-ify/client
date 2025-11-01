package com.looker.droidify.compose.repoList.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.looker.droidify.compose.repoList.RepoListScreen
import kotlinx.serialization.Serializable

@Serializable
object RepoList

fun NavController.navigateToRepoList() {
    this.navigate(RepoList, navOptions {
        launchSingleTop = true
        restoreState = true
    })
}

fun NavGraphBuilder.repoList(
    onRepoClick: (Int) -> Unit,
    onBackClick: () -> Unit,
) {
    composable<RepoList> {
        RepoListScreen(onRepoClick = onRepoClick, onBackClick = onBackClick, viewModel = hiltViewModel())
    }
}
