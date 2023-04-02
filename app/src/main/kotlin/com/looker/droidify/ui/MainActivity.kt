package com.looker.droidify.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.looker.core.data.fdroid.repository.RepoRepository
import com.looker.droidify.ui.app_list.AppList
import com.looker.droidify.ui.theme.DroidifyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	@Inject
	lateinit var repoRepository: RepoRepository

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			DroidifyTheme {
				Surface(Modifier.fillMaxSize()) {
					Box {
						val scope = rememberCoroutineScope()
						AppList()
						FloatingActionButton(onClick = {
							scope.launch {
								repoRepository.syncAll(this@MainActivity, false)
							}
						}) {
							Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
						}
					}
				}
			}
		}
	}
}