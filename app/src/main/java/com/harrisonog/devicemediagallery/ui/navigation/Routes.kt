package com.harrisonog.devicemediagallery.ui.navigation

import android.net.Uri

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Folders : Routes("folders")
    data object FolderDetail : Routes("folder/{folderPath}") {
        fun createRoute(folderPath: String): String = "folder/${Uri.encode(folderPath)}"
    }

    data object MediaViewer : Routes("viewer/{folderPath}/{initialIndex}") {
        fun createRoute(folderPath: String, initialIndex: Int): String =
            "viewer/${Uri.encode(folderPath)}/$initialIndex"
    }

    data object Albums : Routes("albums")

    data object AlbumDetail : Routes("album/{albumId}") {
        fun createRoute(albumId: Long): String = "album/$albumId"
    }

    data object AlbumViewer : Routes("album_viewer/{albumId}/{initialIndex}") {
        fun createRoute(albumId: Long, initialIndex: Int): String =
            "album_viewer/$albumId/$initialIndex"
    }

    data object Trash : Routes("trash")
    data object Duplicates : Routes("duplicates")
}
