package com.looker.droidify.compose.appDetail.navigation

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
data class AppDetail(val packageName: String)

fun NavController.navigateToAppDetail(packageName: String) {
    this.navigate(AppDetail(packageName))
}

fun NavGraphBuilder.appDetail(
    onBackClick: () -> Unit,
) {
    composable<AppDetail> { backStackEntry ->
        val appDetail = backStackEntry.toRoute<AppDetail>()
        AppDetailScreen(packageName = appDetail.packageName)
    }
}

@Composable
private fun AppDetailScreen(packageName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "APP DETAIL SCREEN: [packageName: $packageName]")
    }
}
