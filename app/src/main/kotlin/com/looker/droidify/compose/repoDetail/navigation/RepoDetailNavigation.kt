package com.looker.droidify.compose.repoDetail.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data class RepoDetail(val repoId: Int)

fun NavController.navigateToRepoDetail(repoId: Int) {
    this.navigate(RepoDetail(repoId))
}

fun NavGraphBuilder.repoDetail(
    onBackClick: () -> Unit,
    onEditClick: (Int) -> Unit,
) {
    composable<RepoDetail> { backStackEntry ->
        val repoDetail = backStackEntry.toRoute<RepoDetail>()
        RepoDetailScreen(
            repoId = repoDetail.repoId,
            onEditClick = { onEditClick(repoDetail.repoId) }
        )
    }
}

@Composable
private fun RepoDetailScreen(
    repoId: Int,
    onEditClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "REPO DETAIL SCREEN: [repoId: $repoId]")
    }
}
