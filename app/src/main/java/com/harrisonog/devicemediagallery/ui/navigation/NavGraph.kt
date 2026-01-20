package com.harrisonog.devicemediagallery.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.harrisonog.devicemediagallery.ui.screens.folderdetail.FolderDetailScreen
import com.harrisonog.devicemediagallery.ui.screens.folders.FoldersScreen
import com.harrisonog.devicemediagallery.ui.screens.home.HomeScreen
import com.harrisonog.devicemediagallery.ui.screens.viewer.MediaViewerScreen

@Composable
fun GalleryNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Home.route) {
            HomeScreen(
                onNavigateToFolders = { navController.navigate(Routes.Folders.route) },
                onNavigateToFolder = { path ->
                    navController.navigate(Routes.FolderDetail.createRoute(path))
                }
            )
        }

        composable(Routes.Folders.route) {
            FoldersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFolder = { path ->
                    navController.navigate(Routes.FolderDetail.createRoute(path))
                }
            )
        }

        composable(
            route = Routes.FolderDetail.route,
            arguments = listOf(navArgument("folderPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderPath = backStackEntry.arguments?.getString("folderPath") ?: ""
            FolderDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { index ->
                    navController.navigate(
                        Routes.MediaViewer.createRoute(folderPath, index)
                    )
                }
            )
        }

        composable(
            route = Routes.MediaViewer.route,
            arguments = listOf(
                navArgument("folderPath") { type = NavType.StringType },
                navArgument("initialIndex") { type = NavType.IntType }
            )
        ) {
            MediaViewerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
