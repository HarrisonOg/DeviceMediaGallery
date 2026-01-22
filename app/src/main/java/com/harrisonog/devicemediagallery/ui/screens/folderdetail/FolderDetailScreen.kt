package com.harrisonog.devicemediagallery.ui.screens.folderdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.harrisonog.devicemediagallery.ui.components.AddToAlbumBottomSheet
import com.harrisonog.devicemediagallery.ui.components.CreateAlbumDialog
import com.harrisonog.devicemediagallery.ui.components.CreateTagDialog
import com.harrisonog.devicemediagallery.ui.components.MediaGrid
import com.harrisonog.devicemediagallery.ui.components.TagSelectionBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (Int) -> Unit,
    viewModel: FolderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text("${uiState.selectedItems.size} selected")
                    } else {
                        Text(uiState.folderName)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.isSelectionMode) {
                                viewModel.clearSelection()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.isSelectionMode) {
                                Icons.Default.Close
                            } else {
                                Icons.AutoMirrored.Filled.ArrowBack
                            },
                            contentDescription = if (uiState.isSelectionMode) {
                                "Clear selection"
                            } else {
                                "Back"
                            }
                        )
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.showTagSheet() }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Add tags"
                            )
                        }
                        IconButton(onClick = { viewModel.showAddToAlbumSheet() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add to album"
                            )
                        }
                        IconButton(onClick = { viewModel.showDeleteConfirmDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && !uiState.isRefreshing -> {
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

                uiState.mediaItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No media in this folder",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    MediaGrid(
                        mediaItems = uiState.mediaItems,
                        selectedItems = uiState.selectedItems,
                        onItemClick = { item ->
                            if (uiState.isSelectionMode) {
                                viewModel.toggleItemSelection(item)
                            } else {
                                val index = uiState.mediaItems.indexOf(item)
                                if (index >= 0) {
                                    onNavigateToViewer(index)
                                }
                            }
                        },
                        onItemLongClick = { item ->
                            viewModel.toggleItemSelection(item)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (uiState.showAddToAlbumSheet) {
        AddToAlbumBottomSheet(
            albums = uiState.albums,
            onDismiss = { viewModel.hideAddToAlbumSheet() },
            onAlbumSelected = { album ->
                viewModel.addSelectedToAlbum(album.id)
            },
            onCreateNewAlbum = {
                viewModel.showCreateAlbumDialog()
            }
        )
    }

    if (uiState.showCreateAlbumDialog) {
        CreateAlbumDialog(
            onDismiss = { viewModel.hideCreateAlbumDialog() },
            onConfirm = { name ->
                viewModel.createAlbumAndAddSelected(name)
            }
        )
    }

    if (uiState.showTagSheet) {
        TagSelectionBottomSheet(
            tags = uiState.tags,
            onDismiss = { viewModel.hideTagSheet() },
            onApplyTags = { tagIds ->
                viewModel.applyTagsToSelected(tagIds)
            },
            onCreateNewTag = {
                viewModel.showCreateTagDialog()
            }
        )
    }

    if (uiState.showCreateTagDialog) {
        CreateTagDialog(
            onDismiss = { viewModel.hideCreateTagDialog() },
            onConfirm = { name, color ->
                viewModel.createTag(name, color)
            }
        )
    }

    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirmDialog() },
            title = { Text("Move to Trash?") },
            text = {
                Text(
                    "Move ${uiState.selectedItems.size} " +
                            "item${if (uiState.selectedItems.size > 1) "s" else ""} " +
                            "to trash? Items will be permanently deleted after 30 days."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.moveSelectedToTrash() }) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirmDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
