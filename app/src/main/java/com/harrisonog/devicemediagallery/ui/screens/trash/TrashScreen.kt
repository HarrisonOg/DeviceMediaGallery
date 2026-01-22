package com.harrisonog.devicemediagallery.ui.screens.trash

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.harrisonog.devicemediagallery.domain.model.TrashItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text("${uiState.selectedItems.size} selected")
                    } else {
                        Text("Trash")
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
                        // Restore button
                        IconButton(onClick = { viewModel.restoreSelected() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restore"
                            )
                        }
                        // Delete permanently button
                        IconButton(onClick = { viewModel.showDeleteConfirmDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete permanently"
                            )
                        }
                    } else if (uiState.trashItems.isNotEmpty()) {
                        // Empty trash button
                        IconButton(onClick = { viewModel.showEmptyTrashDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Empty trash"
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.trashItems.isEmpty() -> {
                    Text(
                        text = "Trash is empty",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    TrashGrid(
                        trashItems = uiState.trashItems,
                        selectedItems = uiState.selectedItems,
                        onItemClick = { item ->
                            viewModel.toggleItemSelection(item)
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

    // Empty Trash confirmation dialog
    if (uiState.showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideEmptyTrashDialog() },
            title = { Text("Empty Trash?") },
            text = {
                Text(
                    "This will permanently delete ${uiState.trashItems.size} " +
                            "item${if (uiState.trashItems.size > 1) "s" else ""}. " +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyTrash() }) {
                    Text("Empty Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideEmptyTrashDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete permanently confirmation dialog
    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirmDialog() },
            title = { Text("Delete Permanently?") },
            text = {
                Text(
                    "This will permanently delete ${uiState.selectedItems.size} " +
                            "item${if (uiState.selectedItems.size > 1) "s" else ""}. " +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSelectedPermanently() }) {
                    Text("Delete")
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

@Composable
private fun TrashGrid(
    trashItems: List<TrashItem>,
    selectedItems: Set<String>,
    onItemClick: (TrashItem) -> Unit,
    onItemLongClick: (TrashItem) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        items(
            items = trashItems,
            key = { it.originalUri.toString() }
        ) { item ->
            TrashGridItem(
                trashItem = item,
                isSelected = item.originalUri.toString() in selectedItems,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashGridItem(
    trashItem: TrashItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(trashItem.trashUri)
                .crossfade(true)
                .build(),
            contentDescription = trashItem.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video indicator
        if (trashItem.isVideo) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
            )
        }

        // Days until deletion overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${trashItem.daysUntilDeletion}d",
                style = MaterialTheme.typography.labelSmall,
                color = if (trashItem.daysUntilDeletion <= 7) {
                    Color(0xFFFF6B6B) // Red warning for items expiring soon
                } else {
                    Color.White
                }
            )
        }

        // Selection overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
            )
        }
    }
}
