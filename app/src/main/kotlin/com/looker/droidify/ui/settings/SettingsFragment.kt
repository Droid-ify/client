package com.looker.droidify.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.droidify.compose.settings.SettingsScreen
import com.looker.droidify.compose.settings.SettingsViewModel
import com.looker.droidify.compose.theme.DroidifyTheme
import com.looker.droidify.datastore.model.Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val isDarkTheme = when (settings.theme) {
                    Theme.DARK, Theme.AMOLED -> true
                    Theme.LIGHT -> false
                    Theme.SYSTEM, Theme.SYSTEM_BLACK -> isSystemInDarkTheme()
                }
                DroidifyTheme(
                    darkTheme = isDarkTheme,
                    dynamicColor = settings.dynamicTheme
                ) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBackClick = { activity?.onBackPressedDispatcher?.onBackPressed() },
                    )
                }
            }
        }
    }
}
