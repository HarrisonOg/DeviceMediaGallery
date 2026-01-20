package com.harrisonog.devicemediagallery.ui.components

import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun ZoomableImage(
    uri: Uri,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    resetZoomKey: Any? = null
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom when key changes (e.g., when swiping to new image)
    LaunchedEffect(resetZoomKey) {
        scale = 1f
        offset = Offset.Zero
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)

        // Only allow panning when zoomed in
        val newOffset = if (newScale > 1f) {
            val maxOffset = (newScale - 1) * 500f
            Offset(
                x = (offset.x + panChange.x).coerceIn(-maxOffset, maxOffset),
                y = (offset.y + panChange.y).coerceIn(-maxOffset, maxOffset)
            )
        } else {
            Offset.Zero
        }

        scale = newScale
        offset = newOffset
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Toggle between 1x and 2.5x zoom
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .transformable(state = transformableState),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = uri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
