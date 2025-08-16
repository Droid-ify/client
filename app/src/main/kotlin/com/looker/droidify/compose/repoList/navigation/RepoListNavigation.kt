package com.looker.droidify.compose.repoList.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.looker.droidify.compose.repoList.RepoListScreen
import com.looker.droidify.compose.repoList.RepoListViewModel
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
    onRepoClick: (Int) -> Unit = { _ -> },
) {
    composable<RepoList> {
        val viewModel: RepoListViewModel = hiltViewModel()
        RepoListScreen(onRepoClick = onRepoClick, viewModel = viewModel)
    }
}
