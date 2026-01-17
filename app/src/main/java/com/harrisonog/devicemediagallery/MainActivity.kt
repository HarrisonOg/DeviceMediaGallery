package com.harrisonog.devicemediagallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.harrisonog.devicemediagallery.ui.screens.home.HomeScreen
import com.harrisonog.devicemediagallery.ui.theme.GalleryTheme
import com.harrisonog.devicemediagallery.util.RequestMediaPermissions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var permissionsGranted by remember { mutableStateOf(false) }

                    RequestMediaPermissions(
                        onPermissionsGranted = { permissionsGranted = true }
                    ) {
                        HomeScreen()
                    }
                }
            }
        }
    }
}
