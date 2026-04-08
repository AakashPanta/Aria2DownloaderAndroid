package com.aria2.downloader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aria2.downloader.ui.screens.detail.DetailScreen
import com.aria2.downloader.ui.screens.detail.DetailViewModel
import com.aria2.downloader.ui.screens.history.HistoryScreen
import com.aria2.downloader.ui.screens.history.HistoryViewModel
import com.aria2.downloader.ui.screens.home.HomeScreen
import com.aria2.downloader.ui.screens.home.HomeViewModel
import com.aria2.downloader.ui.screens.newdownload.NewDownloadScreen
import com.aria2.downloader.ui.screens.newdownload.NewDownloadViewModel
import com.aria2.downloader.ui.screens.settings.SettingsScreen
import com.aria2.downloader.ui.screens.settings.SettingsViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NewDownload : Screen("new_download")
    object Detail : Screen("detail/{downloadId}") {
        fun createRoute(downloadId: String) = "detail/$downloadId"
    }
    object History : Screen("history")
    object Settings : Screen("settings")
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToNewDownload = {
                    navController.navigate(Screen.NewDownload.route)
                },
                onNavigateToDetail = { downloadId ->
                    navController.navigate(Screen.Detail.createRoute(downloadId))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.NewDownload.route) {
            val viewModel: NewDownloadViewModel = hiltViewModel()
            NewDownloadScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDownloadStarted = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            Screen.Detail.route,
            arguments = listOf(
                navArgument("downloadId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val downloadId = backStackEntry.arguments?.getString("downloadId") ?: return@composable
            val viewModel: DetailViewModel = hiltViewModel()
            DetailScreen(
                viewModel = viewModel,
                downloadId = downloadId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.History.route) {
            val viewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDownloadClicked = { downloadId ->
                    navController.navigate(Screen.Detail.createRoute(downloadId))
                }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
