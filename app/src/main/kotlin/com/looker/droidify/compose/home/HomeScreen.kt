package com.looker.droidify.compose.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToApps: () -> Unit,
    onNavigateToRepos: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Droid-ify") })
        },
        bottomBar = {
            androidx.compose.material3.Surface( tonalElevation = 3.dp ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    androidx.compose.material3.TextButton(onClick = onNavigateToApps) { Text("Apps") }
                    androidx.compose.material3.TextButton(onClick = onNavigateToRepos) { Text("Repos") }
                    androidx.compose.material3.TextButton(onClick = onNavigateToSettings) { Text("Settings") }
                }
            }
        }
    ) { padding ->
        HomeContent(
            paddingValues = padding,
            onNavigateToApps = onNavigateToApps,
            onNavigateToRepos = onNavigateToRepos,
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}

@Composable
private fun HomeContent(
    paddingValues: PaddingValues,
    onNavigateToApps: () -> Unit,
    onNavigateToRepos: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineSmall,
        )
        Button(onClick = onNavigateToApps, modifier = Modifier.fillMaxWidth()) {
            Text("Browse Apps")
        }
        Button(onClick = onNavigateToRepos, modifier = Modifier.fillMaxWidth()) {
            Text("Repositories")
        }
        Button(onClick = onNavigateToSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
        }
    }
}
