package com.harrisonog.devicemediagallery.ui.screens.folderdetail

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

    private val folderPath: String = savedStateHandle.get<String>("folderPath") ?: ""

    private val _uiState = MutableStateFlow(FolderDetailUiState())
    val uiState: StateFlow<FolderDetailUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val pageSize = 100

    init {
        val folderName = folderPath.substringAfterLast("/")
        _uiState.value = _uiState.value.copy(folderName = folderName)
        loadMedia()
        loadAlbums()
        loadTags()
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            try {
                albumRepository.getVirtualAlbums().collect { albums ->
                    _uiState.value = _uiState.value.copy(albums = albums)
                }
            } catch (e: Exception) {
                // Albums loading error - non-critical
            }
        }
    }

    fun loadMedia() {
        currentOffset = 0
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasMoreItems = true)

            try {
                mediaRepository.getMediaItems(folderPath, limit = pageSize, offset = 0).collect { items ->
                    currentOffset = items.size
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        mediaItems = items,
                        hasMoreItems = items.size >= pageSize
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load media"
                )
            }
        }
    }

    fun loadMoreMedia() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMoreItems) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            try {
                mediaRepository.getMediaItems(folderPath, limit = pageSize, offset = currentOffset).collect { newItems ->
                    val currentItems = _uiState.value.mediaItems
                    currentOffset += newItems.size
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        mediaItems = currentItems + newItems,
                        hasMoreItems = newItems.size >= pageSize
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load more media"
                )
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadMedia()
    }

    fun toggleSelectionMode() {
        val newSelectionMode = !_uiState.value.isSelectionMode
        _uiState.value = _uiState.value.copy(
            isSelectionMode = newSelectionMode,
            selectedItems = if (newSelectionMode) _uiState.value.selectedItems else emptySet()
        )
    }

    fun toggleItemSelection(item: MediaItem) {
        val itemUri = item.uri.toString()
        val currentSelected = _uiState.value.selectedItems
        val newSelected = if (itemUri in currentSelected) {
            currentSelected - itemUri
        } else {
            currentSelected + itemUri
        }

        _uiState.value = _uiState.value.copy(
            selectedItems = newSelected,
            isSelectionMode = newSelected.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedItems = emptySet(),
            isSelectionMode = false
        )
    }

    fun selectAll() {
        val allUris = _uiState.value.mediaItems.map { it.uri.toString() }.toSet()
        _uiState.value = _uiState.value.copy(
            selectedItems = allUris,
            isSelectionMode = true
        )
    }

    fun showAddToAlbumSheet() {
        _uiState.value = _uiState.value.copy(showAddToAlbumSheet = true)
    }

    fun hideAddToAlbumSheet() {
        _uiState.value = _uiState.value.copy(showAddToAlbumSheet = false)
    }

    fun showCreateAlbumDialog() {
        _uiState.value = _uiState.value.copy(showCreateAlbumDialog = true)
    }

    fun hideCreateAlbumDialog() {
        _uiState.value = _uiState.value.copy(showCreateAlbumDialog = false)
    }

    fun addSelectedToAlbum(albumId: Long) {
        viewModelScope.launch {
            try {
                albumRepository.addMediaToAlbum(albumId, _uiState.value.selectedItems.toList())
                hideAddToAlbumSheet()
                clearSelection()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to add to album"
                )
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
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create album"
                )
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            try {
                tagRepository.getAllTags().collect { tags ->
                    _uiState.value = _uiState.value.copy(tags = tags)
                }
            } catch (e: Exception) {
                // Tags loading error - non-critical
            }
        }
    }

    fun showTagSheet() {
        _uiState.value = _uiState.value.copy(showTagSheet = true)
    }

    fun hideTagSheet() {
        _uiState.value = _uiState.value.copy(showTagSheet = false)
    }

    fun showCreateTagDialog() {
        _uiState.value = _uiState.value.copy(showCreateTagDialog = true)
    }

    fun hideCreateTagDialog() {
        _uiState.value = _uiState.value.copy(showCreateTagDialog = false)
    }

    fun applyTagsToSelected(tagIds: List<Long>) {
        viewModelScope.launch {
            try {
                tagRepository.applyTagsToMedia(_uiState.value.selectedItems.toList(), tagIds)
                hideTagSheet()
                clearSelection()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to apply tags"
                )
            }
        }
    }

    fun createTag(name: String, color: Int) {
        viewModelScope.launch {
            try {
                tagRepository.createTag(name, color)
                hideCreateTagDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create tag"
                )
            }
        }
    }

    fun showDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true)
    }

    fun hideDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    fun moveSelectedToTrash() {
        viewModelScope.launch {
            val selectedUris = _uiState.value.selectedItems
            val itemsToDelete = _uiState.value.mediaItems.filter {
                it.uri.toString() in selectedUris
            }

            trashRepository.moveToTrash(itemsToDelete)
                .onSuccess {
                    clearSelection()
                    hideDeleteConfirmDialog()
                    loadMedia()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to move items to trash"
                    )
                    hideDeleteConfirmDialog()
                }
        }
    }
}
