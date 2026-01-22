package com.harrisonog.devicemediagallery.ui.screens.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harrisonog.devicemediagallery.ui.components.VideoPlayer
import com.harrisonog.devicemediagallery.ui.components.ZoomableImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeViewerScreen(
    onNavigateBack: () -> Unit,
    viewModel: HomeViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showControls by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            uiState.error != null -> {
                Text(
                    text = uiState.error ?: "Unknown error",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.mediaItems.isNotEmpty() -> {
                val pagerState = rememberPagerState(
                    initialPage = uiState.initialIndex.coerceIn(0, uiState.mediaItems.lastIndex),
                    pageCount = { uiState.mediaItems.size }
                )

                // Pager for swiping between items
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showControls = !showControls
                        },
                    key = { uiState.mediaItems[it].id }
                ) { page ->
                    val mediaItem = uiState.mediaItems[page]
                    val isCurrentPage = pagerState.currentPage == page

                    if (mediaItem.isVideo) {
                        VideoPlayer(
                            uri = mediaItem.uri,
                            modifier = Modifier.fillMaxSize(),
                            isCurrentPage = isCurrentPage
                        )
                    } else {
                        ZoomableImage(
                            uri = mediaItem.uri,
                            contentDescription = mediaItem.fileName,
                            modifier = Modifier.fillMaxSize(),
                            resetZoomKey = page
                        )
                    }
                }

                // Controls overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / ${uiState.mediaItems.size}",
                                        color = Color.White
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = onNavigateBack) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color.White
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        }
                    ) { paddingValues ->
                        // Empty content - just needed for the top bar
                        Box(modifier = Modifier.padding(paddingValues))
                    }
                }
            }

            else -> {
                Text(
                    text = "No media to display",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
