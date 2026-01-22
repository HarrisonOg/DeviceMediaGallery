package com.harrisonog.devicemediagallery.ui.screens.folderdetail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.devicemediagallery.data.repository.AlbumRepository
import com.harrisonog.devicemediagallery.data.repository.MediaRepository
import com.harrisonog.devicemediagallery.data.repository.TagRepository
import com.harrisonog.devicemediagallery.data.repository.TrashRepository
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import com.harrisonog.devicemediagallery.domain.model.Tag
import com.harrisonog.devicemediagallery.domain.model.VirtualAlbum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderDetailUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val mediaItems: List<MediaItem> = emptyList(),
    val folderName: String = "",
    val error: String? = null,
    val selectedItems: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isRefreshing: Boolean = false,
    val albums: List<VirtualAlbum> = emptyList(),
    val showAddToAlbumSheet: Boolean = false,
    val showCreateAlbumDialog: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val showTagSheet: Boolean = false,
    val showCreateTagDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val hasMoreItems: Boolean = true
)

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val albumRepository: AlbumRepository,
    private val tagRepository: TagRepository,
    private val trashRepository: TrashRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val folderPath: String = savedStateHandle.get<String>("folderPath")
        ?.let { Uri.decode(it) } ?: ""

    private val _uiState = MutableStateFlow(FolderDetailUiState())
    val uiState: StateFlow<FolderDetailUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val pageSize = 100

    init {
        val folderName = folderPath.substringAfterLast("/")
        _uiState.update { it.copy(folderName = folderName) }
        loadMedia()
        loadAlbums()
        loadTags()
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            albumRepository.getVirtualAlbums()
                .catch { /* Albums loading error - non-critical */ }
                .collect { albums ->
                    _uiState.update { it.copy(albums = albums) }
                }
        }
    }

    fun loadMedia() {
        currentOffset = 0
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasMoreItems = true) }

            mediaRepository.getMediaItems(folderPath, limit = pageSize, offset = 0)
                .catch { e ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load media"
                    ) }
                }
                .collect { items ->
                    currentOffset = items.size
                    _uiState.update { it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        mediaItems = items,
                        hasMoreItems = items.size >= pageSize
                    ) }
                }
        }
    }

    fun loadMoreMedia() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMoreItems) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            mediaRepository.getMediaItems(folderPath, limit = pageSize, offset = currentOffset)
                .catch { e ->
                    _uiState.update { it.copy(isLoadingMore = false, error = e.message ?: "Failed to load more media") }
                }
                .collect { newItems ->
                    val currentItems = _uiState.value.mediaItems
                    currentOffset += newItems.size
                    _uiState.update { it.copy(
                        isLoadingMore = false,
                        mediaItems = currentItems + newItems,
                        hasMoreItems = newItems.size >= pageSize
                    ) }
                }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadMedia()
    }

    fun toggleSelectionMode() {
        _uiState.update { state ->
            val newSelectionMode = !state.isSelectionMode
            state.copy(
                isSelectionMode = newSelectionMode,
                selectedItems = if (newSelectionMode) state.selectedItems else emptySet()
            )
        }
    }

    fun toggleItemSelection(item: MediaItem) {
        _uiState.update { state ->
            val itemUri = item.uri.toString()
            val currentSelected = state.selectedItems
            val newSelected = if (itemUri in currentSelected) {
                currentSelected - itemUri
            } else {
                currentSelected + itemUri
            }

            state.copy(
                selectedItems = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(
            selectedItems = emptySet(),
            isSelectionMode = false
        ) }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allUris = state.mediaItems.map { it.uri.toString() }.toSet()
            state.copy(
                selectedItems = allUris,
                isSelectionMode = true
            )
        }
    }

    fun showAddToAlbumSheet() {
        _uiState.update { it.copy(showAddToAlbumSheet = true) }
    }

    fun hideAddToAlbumSheet() {
        _uiState.update { it.copy(showAddToAlbumSheet = false) }
    }

    fun showCreateAlbumDialog() {
        _uiState.update { it.copy(showCreateAlbumDialog = true) }
    }

    fun hideCreateAlbumDialog() {
        _uiState.update { it.copy(showCreateAlbumDialog = false) }
    }

    fun addSelectedToAlbum(albumId: Long) {
        viewModelScope.launch {
            try {
                albumRepository.addMediaToAlbum(albumId, _uiState.value.selectedItems.toList())
                hideAddToAlbumSheet()
                clearSelection()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add to album") }
            }
        }
    }

    fun createAlbumAndAddSelected(name: String) {
        viewModelScope.launch {
            try {
                val albumId = albumRepository.createAlbum(name)
                albumRepository.addMediaToAlbum(albumId, _uiState.value.selectedItems.toList())
                hideCreateAlbumDialog()
                hideAddToAlbumSheet()
                clearSelection()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to create album") }
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getAllTags()
                .catch { /* Tags loading error - non-critical */ }
                .collect { tags ->
                    _uiState.update { it.copy(tags = tags) }
                }
        }
    }

    fun showTagSheet() {
        _uiState.update { it.copy(showTagSheet = true) }
    }

    fun hideTagSheet() {
        _uiState.update { it.copy(showTagSheet = false) }
    }

    fun showCreateTagDialog() {
        _uiState.update { it.copy(showCreateTagDialog = true) }
    }

    fun hideCreateTagDialog() {
        _uiState.update { it.copy(showCreateTagDialog = false) }
    }

    fun applyTagsToSelected(tagIds: List<Long>) {
        viewModelScope.launch {
            try {
                tagRepository.applyTagsToMedia(_uiState.value.selectedItems.toList(), tagIds)
                hideTagSheet()
                clearSelection()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to apply tags") }
            }
        }
    }

    fun createTag(name: String, color: Int) {
        viewModelScope.launch {
            try {
                tagRepository.createTag(name, color)
                hideCreateTagDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to create tag") }
            }
        }
    }

    fun showDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    fun hideDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    fun moveSelectedToTrash() {
        viewModelScope.launch {
            val state = _uiState.value
            val selectedUris = state.selectedItems
            val itemsToDelete = state.mediaItems.filter {
                it.uri.toString() in selectedUris
            }

            trashRepository.moveToTrash(itemsToDelete)
                .onSuccess {
                    clearSelection()
                    hideDeleteConfirmDialog()
                    loadMedia()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to move items to trash") }
                    hideDeleteConfirmDialog()
                }
        }
    }
}
