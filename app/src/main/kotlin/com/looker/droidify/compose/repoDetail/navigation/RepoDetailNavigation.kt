package com.looker.droidify.compose.repoDetail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.looker.droidify.compose.repoDetail.RepoDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data class RepoDetail(val repoId: Int)

fun NavController.navigateToRepoDetail(repoId: Int) {
    this.navigate(RepoDetail(repoId), navOptions {
        launchSingleTop = true
    })
}

fun NavGraphBuilder.repoDetail(
    onBackClick: () -> Unit,
    onEditClick: (Int) -> Unit,
) {
    composable<RepoDetail> { backStackEntry ->
        RepoDetailScreen(
            onBackClick = onBackClick,
            onEditClick = onEditClick,
        )
    }
}
