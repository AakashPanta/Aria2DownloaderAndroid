package com.aria2.downloader.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.aria2.downloader.ui.screens.about.AboutScreen
import com.aria2.downloader.ui.screens.active.ActiveScreen
import com.aria2.downloader.ui.screens.active.ActiveViewModel
import com.aria2.downloader.ui.screens.detail.DetailScreen
import com.aria2.downloader.ui.screens.detail.DetailViewModel
import com.aria2.downloader.ui.screens.history.HistoryScreen
import com.aria2.downloader.ui.screens.history.HistoryViewModel
import com.aria2.downloader.ui.screens.home.HomeScreen
import com.aria2.downloader.ui.screens.home.HomeViewModel
import com.aria2.downloader.ui.screens.newdownload.NewDownloadScreen
import com.aria2.downloader.ui.screens.newdownload.NewDownloadViewModel
import com.aria2.downloader.ui.screens.queued.QueuedScreen
import com.aria2.downloader.ui.screens.queued.QueuedViewModel
import com.aria2.downloader.ui.screens.settings.SettingsScreen
import com.aria2.downloader.ui.screens.settings.SettingsViewModel

sealed class Route(val route: String, val title: String) {
    data object Home : Route("home", "Home")
    data object Active : Route("active", "Active")
    data object Queued : Route("queued", "Queued")
    data object Completed : Route("completed", "Completed")
    data object Settings : Route("settings", "Settings")
    data object About : Route("about", "About")
    data object NewDownload : Route("new_download", "New Download")
    data object Detail : Route("detail/{downloadId}", "Details") {
        fun create(downloadId: String) = "detail/$downloadId"
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    val topLevel = listOf(
        Route.Home,
        Route.Active,
        Route.Queued,
        Route.Completed,
        Route.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = topLevel.any { item ->
        currentDestination?.hierarchy?.any { node -> node.route == item.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevel.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                val icon = when (item) {
                                    Route.Home -> Icons.Default.CloudDownload
                                    Route.Active -> Icons.Default.Download
                                    Route.Queued -> Icons.Default.Schedule
                                    Route.Completed -> Icons.Default.History
                                    Route.Settings -> Icons.Default.Settings
                                    else -> Icons.Default.CloudDownload
                                }
                                Icon(icon, contentDescription = item.title)
                            },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.route
        ) {
            composable(Route.Home.route) {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    paddingTop = paddingValues.calculateTopPadding(),
                    viewModel = viewModel,
                    onNewDownload = { navController.navigate(Route.NewDownload.route) },
                    onOpenActive = { navController.navigate(Route.Active.route) },
                    onOpenCompleted = { navController.navigate(Route.Completed.route) },
                    onOpenDetail = { navController.navigate(Route.Detail.create(it)) }
                )
            }

            composable(Route.Active.route) {
                val viewModel: ActiveViewModel = hiltViewModel()
                ActiveScreen(
                    paddingTop = paddingValues.calculateTopPadding(),
                    viewModel = viewModel,
                    onNewDownload = { navController.navigate(Route.NewDownload.route) },
                    onOpenDetail = { navController.navigate(Route.Detail.create(it)) }
                )
            }

            composable(Route.Queued.route) {
                val viewModel: QueuedViewModel = hiltViewModel()
                QueuedScreen(
                    paddingTop = paddingValues.calculateTopPadding(),
                    viewModel = viewModel,
                    onOpenDetail = { navController.navigate(Route.Detail.create(it)) }
                )
            }

            composable(Route.Completed.route) {
                val viewModel: HistoryViewModel = hiltViewModel()
                HistoryScreen(
                    paddingTop = paddingValues.calculateTopPadding(),
                    viewModel = viewModel,
                    onOpenDetail = { navController.navigate(Route.Detail.create(it)) }
                )
            }

            composable(Route.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    paddingTop = paddingValues.calculateTopPadding(),
                    viewModel = viewModel
                )
            }

            composable(Route.About.route) {
                AboutScreen(
                    paddingTop = paddingValues.calculateTopPadding()
                )
            }

            composable(Route.NewDownload.route) {
                val viewModel: NewDownloadViewModel = hiltViewModel()
                NewDownloadScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Route.Detail.route,
                arguments = listOf(navArgument("downloadId") { type = NavType.StringType })
            ) { backStackEntry ->
                val viewModel: DetailViewModel = hiltViewModel(backStackEntry)
                DetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
