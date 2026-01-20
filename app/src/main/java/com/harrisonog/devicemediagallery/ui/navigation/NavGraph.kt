package com.harrisonog.devicemediagallery.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.harrisonog.devicemediagallery.ui.screens.folders.FoldersScreen
import com.harrisonog.devicemediagallery.ui.screens.home.HomeScreen

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
            val encodedPath = backStackEntry.arguments?.getString("folderPath") ?: ""
            val folderPath = Uri.decode(encodedPath)
            // FolderDetailScreen will be added in Phase 2.3
            PlaceholderScreen(
                title = "Folder: ${folderPath.substringAfterLast("/")}",
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Coming soon...")
        }
    }
}
