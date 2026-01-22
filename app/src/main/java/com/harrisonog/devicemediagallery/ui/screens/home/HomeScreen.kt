package com.harrisonog.devicemediagallery.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harrisonog.devicemediagallery.ui.components.AlbumGridItem
import com.harrisonog.devicemediagallery.ui.components.FolderGridItem
import com.harrisonog.devicemediagallery.ui.components.MediaGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFolders: () -> Unit = {},
    onNavigateToFolder: (String) -> Unit = {},
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onNavigateToDuplicates: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text("${uiState.selectedItems.size} selected")
                    } else {
                        Text("Gallery")
                    }
                },
                navigationIcon = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.Done, contentDescription = "Select all")
                        }
                    } else {
                        IconButton(onClick = onNavigateToDuplicates) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Find duplicate photos and videos"
                            )
                        }
                        IconButton(onClick = onNavigateToTrash) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = if (uiState.trashCount > 0) {
                                    "View trash, ${uiState.trashCount} items"
                                } else {
                                    "View trash"
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (uiState.isSelectionMode) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.mediaItems.isEmpty() && uiState.folders.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No photos or videos found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    // Folders section
                    if (uiState.folders.isNotEmpty() && !uiState.isSelectionMode) {
                        SectionHeader(
                            title = "Folders",
                            actionText = "View All",
                            onActionClick = onNavigateToFolders
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = uiState.folders.take(10),
                                key = { it.path }
                            ) { folder ->
                                FolderGridItem(
                                    folder = folder,
                                    onClick = { onNavigateToFolder(folder.path) }
                                )
                            }
                        }
                    }

                    // Albums section
                    if (uiState.albums.isNotEmpty() && !uiState.isSelectionMode) {
                        SectionHeader(
                            title = "Albums",
                            actionText = "View All",
                            onActionClick = onNavigateToAlbums
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = uiState.albums.take(10),
                                key = { it.id }
                            ) { album ->
                                AlbumGridItem(
                                    album = album,
                                    onClick = { onNavigateToAlbum(album.id) }
                                )
                            }
                        }
                    }

                    // Media grid
                    if (uiState.mediaItems.isNotEmpty()) {
                        if (uiState.folders.isNotEmpty() && !uiState.isSelectionMode) {
                            SectionHeader(
                                title = "Recent",
                                actionText = null,
                                onActionClick = {}
                            )
                        }

                        MediaGrid(
                            mediaItems = uiState.mediaItems,
                            selectedItems = uiState.selectedItems,
                            onItemClick = { item ->
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleItemSelection(item)
                                } else {
                                    // TODO: Navigate to viewer
                                }
                            },
                            onItemLongClick = { item ->
                                if (!uiState.isSelectionMode) {
                                    viewModel.enterSelectionMode(item)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String?,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        if (actionText != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}
