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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
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
            _uiState.update { it.copy(isLoading = true, error = null, hasMoreItems = true) }

            mediaRepository.getMediaItems(limit = pageSize, offset = 0)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load media") }
                }
                .collect { items ->
                    currentOffset = items.size
                    _uiState.update { it.copy(
                        isLoading = false,
                        mediaItems = items,
                        hasMoreItems = items.size >= pageSize
                    ) }
                }
        }

        viewModelScope.launch {
            mediaRepository.getFolders()
                .catch { /* Folders loading error - non-critical */ }
                .collect { folders ->
                    _uiState.update { it.copy(folders = folders) }
                }
        }

        viewModelScope.launch {
            albumRepository.getVirtualAlbums()
                .catch { /* Albums loading error - non-critical */ }
                .collect { albums ->
                    _uiState.update { it.copy(albums = albums) }
                }
        }
    }

    fun loadMoreMedia() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMoreItems) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            mediaRepository.getMediaItems(limit = pageSize, offset = currentOffset)
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

    fun toggleItemSelection(item: MediaItem) {
        _uiState.update { state ->
            val currentSelected = state.selectedItems.toMutableSet()
            val itemUri = item.uri.toString()

            if (itemUri in currentSelected) {
                currentSelected.remove(itemUri)
            } else {
                currentSelected.add(itemUri)
            }

            state.copy(
                selectedItems = currentSelected,
                isSelectionMode = currentSelected.isNotEmpty()
            )
        }
    }

    fun enterSelectionMode(item: MediaItem) {
        _uiState.update { it.copy(
            isSelectionMode = true,
            selectedItems = setOf(item.uri.toString())
        ) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(
            isSelectionMode = false,
            selectedItems = emptySet()
        ) }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allUris = state.mediaItems.map { it.uri.toString() }.toSet()
            state.copy(selectedItems = allUris)
        }
    }
}
