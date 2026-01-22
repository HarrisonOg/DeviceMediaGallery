package com.harrisonog.devicemediagallery.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.devicemediagallery.data.repository.AlbumRepository
import com.harrisonog.devicemediagallery.data.repository.MediaRepository
import com.harrisonog.devicemediagallery.domain.model.MediaFolder
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import com.harrisonog.devicemediagallery.domain.model.VirtualAlbum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val mediaItems: List<MediaItem> = emptyList(),
    val folders: List<MediaFolder> = emptyList(),
    val albums: List<VirtualAlbum> = emptyList(),
    val selectedItems: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val error: String? = null,
    val hasMoreItems: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val pageSize = 100

    init {
        loadMedia()
    }

    fun loadMedia() {
        currentOffset = 0
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasMoreItems = true)

            try {
                mediaRepository.getMediaItems(limit = pageSize, offset = 0).collect { items ->
                    currentOffset = items.size
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        mediaItems = items,
                        hasMoreItems = items.size >= pageSize
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load media"
                )
            }
        }

        viewModelScope.launch {
            try {
                mediaRepository.getFolders().collect { folders ->
                    _uiState.value = _uiState.value.copy(folders = folders)
                }
            } catch (e: Exception) {
                // Folders loading error - non-critical
            }
        }

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

    fun loadMoreMedia() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMoreItems) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            try {
                mediaRepository.getMediaItems(limit = pageSize, offset = currentOffset).collect { newItems ->
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

    fun toggleItemSelection(item: MediaItem) {
        val currentSelected = _uiState.value.selectedItems.toMutableSet()
        val itemUri = item.uri.toString()

        if (itemUri in currentSelected) {
            currentSelected.remove(itemUri)
        } else {
            currentSelected.add(itemUri)
        }

        _uiState.value = _uiState.value.copy(
            selectedItems = currentSelected,
            isSelectionMode = currentSelected.isNotEmpty()
        )
    }

    fun enterSelectionMode(item: MediaItem) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedItems = setOf(item.uri.toString())
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedItems = emptySet()
        )
    }

    fun selectAll() {
        val allUris = _uiState.value.mediaItems.map { it.uri.toString() }.toSet()
        _uiState.value = _uiState.value.copy(selectedItems = allUris)
    }
}
