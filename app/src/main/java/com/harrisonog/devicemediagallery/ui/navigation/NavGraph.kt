package com.harrisonog.devicemediagallery.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.harrisonog.devicemediagallery.ui.screens.albumdetail.AlbumDetailScreen
import com.harrisonog.devicemediagallery.ui.screens.albums.AlbumsScreen
import com.harrisonog.devicemediagallery.ui.screens.folderdetail.FolderDetailScreen
import com.harrisonog.devicemediagallery.ui.screens.folders.FoldersScreen
import com.harrisonog.devicemediagallery.ui.screens.home.HomeScreen
import com.harrisonog.devicemediagallery.ui.screens.duplicates.DuplicatesScreen
import com.harrisonog.devicemediagallery.ui.screens.trash.TrashScreen
import com.harrisonog.devicemediagallery.ui.screens.viewer.AlbumViewerScreen
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
                },
                onNavigateToAlbums = { navController.navigate(Routes.Albums.route) },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToTrash = { navController.navigate(Routes.Trash.route) },
                onNavigateToDuplicates = { navController.navigate(Routes.Duplicates.route) }
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

        composable(Routes.Albums.route) {
            AlbumsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                }
            )
        }

        composable(
            route = Routes.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("albumId") ?: 0L
            AlbumDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { index ->
                    navController.navigate(
                        Routes.AlbumViewer.createRoute(albumId, index)
                    )
                }
            )
        }

        composable(
            route = Routes.AlbumViewer.route,
            arguments = listOf(
                navArgument("albumId") { type = NavType.LongType },
                navArgument("initialIndex") { type = NavType.IntType }
            )
        ) {
            AlbumViewerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Trash.route) {
            TrashScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Duplicates.route) {
            DuplicatesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
