package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.MobileCodeStudioNavHost
import com.example.ui.theme.MobileCodeStudioTheme
import com.example.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as MobileCodeStudioApp).container

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    settingsRepository = appContainer.settingsRepository,
                    ideRepository = appContainer.ideRepository
                )
            )
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            MobileCodeStudioTheme(
                themeMode = settings.themeMode,
                language = settings.language
            ) {
                MobileCodeStudioNavHost(
                    container = appContainer,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
