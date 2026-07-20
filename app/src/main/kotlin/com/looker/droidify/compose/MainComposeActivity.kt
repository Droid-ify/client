package com.looker.droidify.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.looker.droidify.compose.appDetail.navigation.appDetail
import com.looker.droidify.compose.appDetail.navigation.navigateToAppDetail
import com.looker.droidify.compose.appList.navigation.AppList
import com.looker.droidify.compose.appList.navigation.appList
import com.looker.droidify.compose.repoDetail.navigation.navigateToRepoDetail
import com.looker.droidify.compose.repoDetail.navigation.repoDetail
import com.looker.droidify.compose.repoEdit.navigation.navigateToRepoEdit
import com.looker.droidify.compose.repoEdit.navigation.repoEdit
import com.looker.droidify.compose.repoList.navigation.navigateToRepoList
import com.looker.droidify.compose.repoList.navigation.repoList
import com.looker.droidify.compose.settings.navigation.navigateToSettings
import com.looker.droidify.compose.settings.navigation.settings
import com.looker.droidify.compose.theme.DroidifyTheme
import com.looker.droidify.utility.common.requestNotificationPermission
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainComposeActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO(sqldelight): seed default repositories via SQLDelight-backed repository
        enableEdgeToEdge()
        requestNotificationPermission(request = notificationPermission::launch)
        setContent {
            DroidifyTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        modifier = Modifier.padding(innerPadding),
                        navController = navController,
                        startDestination = AppList,
                    ) {
                        appList(
                            onAppClick = { packageName ->
                                navController.navigateToAppDetail(packageName)
                            },
                            onNavigateToRepos = { navController.navigateToRepoList() },
                            onNavigateToSettings = { navController.navigateToSettings() },
                        )

                        repoList(
                            onRepoClick = { repoId -> navController.navigateToRepoDetail(repoId) },
                            onBackClick = { navController.popBackStack() },
                        )

                        appDetail(
                            onBackClick = { navController.popBackStack() },
                        )

                        repoDetail(
                            onBackClick = { navController.popBackStack() },
                            onEditClick = { repoId ->
                                navController.navigateToRepoEdit(repoId)
                            },
                        )

                        repoEdit(onBackClick = { navController.popBackStack() })

                        settings(onBackClick = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
