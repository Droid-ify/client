package com.looker.droidify.compose.repoEdit.navigation

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
data class RepoEdit(val repoId: Int)

fun NavController.navigateToRepoEdit(repoId: Int) {
    this.navigate(RepoEdit(repoId))
}

fun NavGraphBuilder.repoEdit(
    onBackClick: () -> Unit,
    onSaveClick: (Int) -> Unit,
) {
    composable<RepoEdit> { backStackEntry ->
        val repoEdit = backStackEntry.toRoute<RepoEdit>()
        RepoEditScreen(
            repoId = repoEdit.repoId,
            onSaveClick = { onSaveClick(repoEdit.repoId) }
        )
    }
}

@Composable
private fun RepoEditScreen(
    repoId: Int,
    onSaveClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "REPO EDIT SCREEN: [repoId: $repoId]")
    }
}
