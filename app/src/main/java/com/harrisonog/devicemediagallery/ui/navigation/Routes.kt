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
}
