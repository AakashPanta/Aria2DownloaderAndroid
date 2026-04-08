package com.aria2.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.aria2.downloader.ui.navigation.AppNavHost
import com.aria2.downloader.ui.screens.settings.SettingsViewModel
import com.aria2.downloader.ui.theme.Aria2DownloaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val darkModeEnabled by settingsViewModel.darkModeEnabled.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()

            Aria2DownloaderTheme(
                darkTheme = darkModeEnabled || systemDarkTheme,
                dynamicColor = true
            ) {
                AppNavHost(navController = navController)
            }
        }
    }
}
