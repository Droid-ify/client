package com.looker.droidify.compose.repoEdit.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.looker.droidify.compose.repoEdit.RepoEditScreen
import kotlinx.serialization.Serializable

@Serializable
data class RepoEdit(val repoId: Int? = null)

fun NavController.navigateToRepoEdit(repoId: Int? = null) {
    this.navigate(RepoEdit(repoId))
}

fun NavGraphBuilder.repoEdit(
    onBackClick: () -> Unit,
) {
    composable<RepoEdit> { backStackEntry ->
        val repoEdit = backStackEntry.toRoute<RepoEdit>()
        RepoEditScreen(
            repoId = repoEdit.repoId,
            onBackClick = onBackClick
        )
    }
}
