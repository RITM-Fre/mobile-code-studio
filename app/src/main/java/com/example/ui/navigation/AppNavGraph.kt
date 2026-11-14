package com.example.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.core.AppLanguage
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PreviewScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.WorkspaceViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val PROJECTS = "projects"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val IDE_GRAPH = "ide/{projectId}?file={file}"
    const val WORKSPACE = "workspace/{projectId}?file={file}"
    const val PREVIEW = "preview?url={url}"

    fun ideGraph(projectId: String, initialFile: String? = null): String {
        val encodedFile = Uri.encode(initialFile.orEmpty())
        return "ide/$projectId?file=$encodedFile"
    }

    fun workspace(projectId: String, initialFile: String? = null): String {
        val encodedFile = Uri.encode(initialFile.orEmpty())
        return "workspace/$projectId?file=$encodedFile"
    }

    fun preview(url: String): String = "preview?url=${Uri.encode(url)}"
}

@Composable
fun MobileCodeStudioNavHost(
    container: AppContainer,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(container.ideRepository)
    )

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        enterTransition = {
            fadeIn(animationSpec = tween(240)) +
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) +
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(240))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(240)) +
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) +
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(240))
        }
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                currentLanguage = settings.language,
                onToggleLanguage = {
                    val next = if (settings.language == AppLanguage.ENGLISH) {
                        AppLanguage.PERSIAN
                    } else {
                        AppLanguage.ENGLISH
                    }
                    settingsViewModel.setLanguage(next)
                },
                onOpenProject = { projectId, initialFile ->
                    navController.navigate(Routes.ideGraph(projectId, initialFile))
                },
                onNavigateAllProjects = {
                    navController.navigate(Routes.PROJECTS)
                },
                onNavigateSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateAbout = {
                    navController.navigate(Routes.ABOUT)
                }
            )
        }

        composable(Routes.PROJECTS) {
            ProjectsScreen(
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() },
                onOpenProject = { projectId ->
                    navController.navigate(Routes.ideGraph(projectId))
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                language = settings.language,
                onBack = { navController.popBackStack() }
            )
        }

        navigation(
            route = Routes.IDE_GRAPH,
            startDestination = Routes.WORKSPACE,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("file") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            composable(
                route = Routes.WORKSPACE,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("file") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.IDE_GRAPH)
                }
                val workspaceViewModel: WorkspaceViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = WorkspaceViewModel.Factory(
                        repository = container.ideRepository,
                        serverManager = container.serverManager,
                        settingsFlow = settingsViewModel.settings
                    )
                )
                val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
                val initialFile = backStackEntry.arguments?.getString("file")?.takeIf { it.isNotBlank() }

                WorkspaceScreen(
                    projectId = projectId,
                    initialFile = initialFile,
                    viewModel = workspaceViewModel,
                    settings = settings,
                    onBack = {
                        container.serverManager.stopServer()
                        navController.popBackStack()
                    },
                    onOpenSettings = {
                        navController.navigate(Routes.SETTINGS)
                    },
                    onLaunchPreview = { previewUrl, _ ->
                        navController.navigate(Routes.preview(previewUrl))
                    }
                )
            }

            composable(
                route = Routes.PREVIEW,
                arguments = listOf(
                    navArgument("url") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                ),
                enterTransition = {
                    fadeIn(tween(220)) + scaleIn(initialScale = 0.94f, animationSpec = tween(240))
                },
                exitTransition = {
                    fadeOut(tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200))
                },
                popEnterTransition = {
                    fadeIn(tween(220))
                },
                popExitTransition = {
                    fadeOut(tween(180)) + scaleOut(targetScale = 0.94f, animationSpec = tween(220))
                }
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.IDE_GRAPH)
                }
                val workspaceViewModel: WorkspaceViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = WorkspaceViewModel.Factory(
                        repository = container.ideRepository,
                        serverManager = container.serverManager,
                        settingsFlow = settingsViewModel.settings
                    )
                )
                val url = backStackEntry.arguments?.getString("url").orEmpty()

                DisposableEffect(Unit) {
                    onDispose {
                        workspaceViewModel.stopLocalServer()
                    }
                }

                PreviewScreen(
                    previewUrl = url,
                    onExitPreview = {
                        workspaceViewModel.stopLocalServer()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
